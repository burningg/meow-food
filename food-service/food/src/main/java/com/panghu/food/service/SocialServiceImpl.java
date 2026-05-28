package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.*;
import com.panghu.food.entity.*;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SocialServiceImpl implements SocialService {
    private final UserAccountMapper userAccountMapper;
    private final UserProfileSettingsMapper userProfileSettingsMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final FriendRelationMapper friendRelationMapper;
    private final DishMapper dishMapper;
    private final ActivityFeedMapper activityFeedMapper;
    private final BuddyCircleMapper buddyCircleMapper;
    private final BuddyCircleMemberMapper buddyCircleMemberMapper;
    private final BuddyCircleInviteMapper buddyCircleInviteMapper;
    private final AuthService authService;

    public SocialServiceImpl(UserAccountMapper userAccountMapper,
                             UserProfileSettingsMapper userProfileSettingsMapper,
                             FriendRequestMapper friendRequestMapper,
                             FriendRelationMapper friendRelationMapper,
                             DishMapper dishMapper,
                             ActivityFeedMapper activityFeedMapper,
                             BuddyCircleMapper buddyCircleMapper,
                             BuddyCircleMemberMapper buddyCircleMemberMapper,
                             BuddyCircleInviteMapper buddyCircleInviteMapper,
                             AuthService authService) {
        this.userAccountMapper = userAccountMapper;
        this.userProfileSettingsMapper = userProfileSettingsMapper;
        this.friendRequestMapper = friendRequestMapper;
        this.friendRelationMapper = friendRelationMapper;
        this.dishMapper = dishMapper;
        this.activityFeedMapper = activityFeedMapper;
        this.buddyCircleMapper = buddyCircleMapper;
        this.buddyCircleMemberMapper = buddyCircleMemberMapper;
        this.buddyCircleInviteMapper = buddyCircleInviteMapper;
        this.authService = authService;
    }

    @Override
    public ProfileResponse getProfile() {
        Long userId = AuthContext.requireUserId();
        ProfileResponse response = new ProfileResponse();
        response.setUser(authService.getCurrentUser());
        response.setStats(buildProfileStats(userId));
        response.setFriendPreview(getFriends().stream().limit(2).collect(Collectors.toList()));
        response.setFeedPreview(getFeed("all").stream().limit(1).collect(Collectors.toList()));
        response.setDefaultMenuVisibility(getSettings(userId).getDefaultMenuVisibility());
        return response;
    }

    @Override
    @Transactional
    public AuthUserResponse updateDefaultVisibility(ProfileVisibilityUpdateRequest request) {
        Long userId = AuthContext.requireUserId();
        UserProfileSettings settings = getSettings(userId);
        settings.setDefaultMenuVisibility(VisibilityUtils.normalizeProfileVisibility(request.getDefaultMenuVisibility()));
        userProfileSettingsMapper.updateById(settings);
        return authService.getCurrentUser();
    }

    @Override
    public List<FriendItemResponse> getFriends() {
        Long currentUserId = AuthContext.requireUserId();
        List<FriendRelation> relations = friendRelationMapper.selectList(new QueryWrapper<FriendRelation>()
                .eq("user_id", currentUserId));
        List<FriendItemResponse> result = new ArrayList<>();
        for (FriendRelation relation : relations) {
            UserAccount user = userAccountMapper.selectById(relation.getFriendUserId());
            if (user == null) {
                continue;
            }
            FriendItemResponse item = new FriendItemResponse();
            item.setId(user.getId());
            item.setAccount(user.getAccount());
            item.setNickname(user.getNickname());
            item.setAvatar(user.getAvatar());
            item.setBio(user.getBio());
            item.setFriend(true);
            item.setVisibleMenuCount(countVisibleMenusForViewer(user.getId(), currentUserId, false));
            item.setSharedMenuCount(countVisibleMenusForViewer(user.getId(), currentUserId, true));
            item.setMemberInCircle(hasSharedCircle(currentUserId, user.getId()));
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public FriendRequestItemResponse createFriendRequest(FriendRequestActionRequest request) {
        Long currentUserId = AuthContext.requireUserId();
        Long targetUserId = resolveTargetUserId(request.getTargetUserId(), request.getTargetAccount());
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能添加自己为好友");
        }
        if (isFriend(currentUserId, targetUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "你们已经是好友了");
        }
        FriendRequest existing = friendRequestMapper.selectOne(new QueryWrapper<FriendRequest>()
                .eq("requester_user_id", currentUserId)
                .eq("target_user_id", targetUserId)
                .eq("status", "pending")
                .last("LIMIT 1"));
        if (existing != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "好友申请已发送");
        }
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setRequesterUserId(currentUserId);
        friendRequest.setTargetUserId(targetUserId);
        friendRequest.setMessage(request.getMessage());
        friendRequest.setStatus("pending");
        friendRequest.setCreatedAt(LocalDateTime.now());
        friendRequestMapper.insert(friendRequest);
        return toFriendRequestItem(friendRequest);
    }

    @Override
    public FriendRequestsResponse getFriendRequests() {
        Long currentUserId = AuthContext.requireUserId();
        FriendRequestsResponse response = new FriendRequestsResponse();
        List<FriendRequest> incoming = friendRequestMapper.selectList(new QueryWrapper<FriendRequest>()
                .eq("target_user_id", currentUserId)
                .orderByDesc("created_at"));
        List<FriendRequest> outgoing = friendRequestMapper.selectList(new QueryWrapper<FriendRequest>()
                .eq("requester_user_id", currentUserId)
                .orderByDesc("created_at"));
        response.setIncoming(incoming.stream().map(this::toFriendRequestItem).collect(Collectors.toList()));
        response.setOutgoing(outgoing.stream().map(this::toFriendRequestItem).collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional
    public FriendRequestItemResponse acceptFriendRequest(Long requestId) {
        Long currentUserId = AuthContext.requireUserId();
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || !Objects.equals(request.getTargetUserId(), currentUserId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "好友申请不存在");
        }
        request.setStatus("accepted");
        request.setHandledAt(LocalDateTime.now());
        friendRequestMapper.updateById(request);
        createFriendRelation(request.getRequesterUserId(), request.getTargetUserId());
        createFriendRelation(request.getTargetUserId(), request.getRequesterUserId());
        return toFriendRequestItem(request);
    }

    @Override
    @Transactional
    public FriendRequestItemResponse rejectFriendRequest(Long requestId) {
        Long currentUserId = AuthContext.requireUserId();
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || !Objects.equals(request.getTargetUserId(), currentUserId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "好友申请不存在");
        }
        request.setStatus("rejected");
        request.setHandledAt(LocalDateTime.now());
        friendRequestMapper.updateById(request);
        return toFriendRequestItem(request);
    }

    @Override
    public List<FeedItemResponse> getFeed(String filter) {
        Long currentUserId = AuthContext.requireUserId();
        List<ActivityFeed> feeds = activityFeedMapper.selectList(new QueryWrapper<ActivityFeed>().orderByDesc("created_at"));
        List<FeedItemResponse> result = new ArrayList<>();
        for (ActivityFeed feed : feeds) {
            if (!matchesFilter(feed, filter)) {
                continue;
            }
            if (!canSeeFeed(feed, currentUserId)) {
                continue;
            }
            FeedItemResponse item = toFeedItem(feed);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public FeedAccessibleMenusResponse getAccessibleMenus() {
        Long currentUserId = AuthContext.requireUserId();
        FeedAccessibleMenusResponse response = new FeedAccessibleMenusResponse();
        List<DishSummaryResponse> all = dishMapper.selectAllActive();
        response.setMenus(all.stream()
                .filter(item -> !Objects.equals(item.getOwnerUserId(), currentUserId))
                .filter(item -> canViewDish(item, currentUserId, false))
                .limit(3)
                .map(item -> withEffectiveVisibility(item, item.getOwnerUserId()))
                .collect(Collectors.toList()));
        return response;
    }

    @Override
    public UserMenuAccessResponse getUserMenuAccess(Long targetUserId) {
        Long currentUserId = AuthContext.requireUserId();
        UserAccount targetUser = userAccountMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        boolean friend = isFriend(currentUserId, targetUserId);
        boolean sameCircle = hasSharedCircle(currentUserId, targetUserId);
        List<DishSummaryResponse> ownerMenus = dishMapper.selectByOwnerUserId(targetUserId);
        List<DishSummaryResponse> visible = ownerMenus.stream()
                .map(item -> withEffectiveVisibility(item, targetUserId))
                .filter(item -> canViewDish(item, currentUserId, false))
                .collect(Collectors.toList());
        long privateCount = ownerMenus.size() - visible.size();

        UserMenuAccessResponse response = new UserMenuAccessResponse();
        response.setUser(toAuthUser(targetUser));
        response.setFriend(friend);
        response.setSameCircle(sameCircle);
        response.setActionType(friend ? "invite-circle" : "friend-request");
        response.setDescription(friend
                ? "因为你们是好友，所以可以查看她开放的菜单。"
                : "先成为好友，才能查看她对好友开放的菜单。");
        response.setAccessibleCount(visible.size());
        response.setPrivateCount(privateCount);
        response.setMenus(visible);
        response.setAccessRules(buildAccessRules(friend));
        return response;
    }

    @Override
    public List<DishSummaryResponse> getVisibleMenusByUser(Long targetUserId) {
        return getUserMenuAccess(targetUserId).getMenus();
    }

    @Override
    public List<BuddyCircleSummaryResponse> getCircles() {
        Long currentUserId = AuthContext.requireUserId();
        List<BuddyCircleMember> members = buddyCircleMemberMapper.selectList(new QueryWrapper<BuddyCircleMember>()
                .eq("user_id", currentUserId));
        List<BuddyCircleSummaryResponse> result = new ArrayList<>();
        for (BuddyCircleMember member : members) {
            BuddyCircle circle = buddyCircleMapper.selectById(member.getCircleId());
            if (circle != null) {
                result.add(buildCircleSummary(circle, currentUserId));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public BuddyCircleDetailResponse createCircle(BuddyCircleCreateRequest request) {
        Long currentUserId = AuthContext.requireUserId();
        BuddyCircle circle = new BuddyCircle();
        circle.setName(request.getName());
        circle.setDescription(request.getDescription());
        circle.setOwnerUserId(currentUserId);
        circle.setCreatedAt(LocalDateTime.now());
        circle.setUpdatedAt(LocalDateTime.now());
        buddyCircleMapper.insert(circle);

        BuddyCircleMember ownerMember = new BuddyCircleMember();
        ownerMember.setCircleId(circle.getId());
        ownerMember.setUserId(currentUserId);
        ownerMember.setRole("owner");
        ownerMember.setJoinedAt(LocalDateTime.now());
        buddyCircleMemberMapper.insert(ownerMember);

        for (Long memberId : request.getInitialMemberIds()) {
            if (memberId != null && !Objects.equals(memberId, currentUserId)) {
                addCircleMember(circle.getId(), currentUserId, memberId);
            }
        }
        return getCircleDetail(circle.getId());
    }

    @Override
    public BuddyCircleDetailResponse getCircleDetail(Long circleId) {
        Long currentUserId = AuthContext.requireUserId();
        BuddyCircle circle = requireCircleMember(circleId, currentUserId);
        BuddyCircleDetailResponse response = new BuddyCircleDetailResponse();
        response.setCircle(buildCircleSummary(circle, currentUserId));
        BuddyCircleStatsResponse stats = new BuddyCircleStatsResponse();
        stats.setMemberCount(getCircleMembers(circleId).size());
        stats.setSharedMenuCount(getCircleMenus(circleId).size());
        stats.setWeeklyUpdateCount(countCircleWeeklyUpdates(circleId));
        response.setStats(stats);
        response.setMembers(getCircleMembers(circleId));
        response.setSharedMenus(getCircleMenus(circleId));
        return response;
    }

    @Override
    public List<BuddyCircleMemberResponse> getCircleMembers(Long circleId) {
        Long currentUserId = AuthContext.requireUserId();
        requireCircleMember(circleId, currentUserId);
        List<BuddyCircleMember> members = buddyCircleMemberMapper.selectList(new QueryWrapper<BuddyCircleMember>()
                .eq("circle_id", circleId).orderByAsc("joined_at"));
        List<BuddyCircleMemberResponse> result = new ArrayList<>();
        for (BuddyCircleMember member : members) {
            UserAccount user = userAccountMapper.selectById(member.getUserId());
            if (user == null) {
                continue;
            }
            BuddyCircleMemberResponse item = new BuddyCircleMemberResponse();
            item.setId(user.getId());
            item.setAccount(user.getAccount());
            item.setNickname(user.getNickname());
            item.setAvatar(user.getAvatar());
            item.setRole(member.getRole());
            item.setSharedMenuCount(countVisibleMenusForViewer(user.getId(), currentUserId, true));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<DishSummaryResponse> getCircleMenus(Long circleId) {
        Long currentUserId = AuthContext.requireUserId();
        requireCircleMember(circleId, currentUserId);
        List<Long> memberIds = buddyCircleMemberMapper.selectList(new QueryWrapper<BuddyCircleMember>()
                        .eq("circle_id", circleId))
                .stream()
                .map(BuddyCircleMember::getUserId)
                .collect(Collectors.toList());
        List<DishSummaryResponse> result = new ArrayList<>();
        for (Long memberId : memberIds) {
            List<DishSummaryResponse> dishes = dishMapper.selectByOwnerUserId(memberId);
            for (DishSummaryResponse dish : dishes) {
                DishSummaryResponse withVisibility = withEffectiveVisibility(dish, memberId);
                if (canViewDish(withVisibility, currentUserId, true)) {
                    result.add(withVisibility);
                }
            }
        }
        result.sort(Comparator.comparing(DishSummaryResponse::getId));
        return result;
    }

    @Override
    @Transactional
    public BuddyCircleDetailResponse inviteToCircle(Long circleId, BuddyCircleInviteRequest request) {
        Long currentUserId = AuthContext.requireUserId();
        requireCircleMember(circleId, currentUserId);
        Long inviteeUserId = resolveTargetUserId(request.getInviteeUserId(), request.getInviteeAccount());
        addCircleMember(circleId, currentUserId, inviteeUserId);
        return getCircleDetail(circleId);
    }

    private ProfileStatsResponse buildProfileStats(Long userId) {
        ProfileStatsResponse stats = new ProfileStatsResponse();
        stats.setFriendCount(friendRelationMapper.selectCount(new QueryWrapper<FriendRelation>()
                .eq("user_id", userId)));
        stats.setMenuCount(dishMapper.selectCount(new QueryWrapper<Dish>().eq("owner_user_id", userId).eq("status", 1)));
        stats.setCircleCount(buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>().eq("user_id", userId)));
        return stats;
    }

    private long countVisibleMenusForViewer(Long ownerUserId, Long viewerUserId, boolean circleMode) {
        return dishMapper.selectByOwnerUserId(ownerUserId).stream()
                .map(item -> withEffectiveVisibility(item, ownerUserId))
                .filter(item -> canViewDish(item, viewerUserId, circleMode))
                .count();
    }

    private boolean isFriend(Long userId, Long otherUserId) {
        return friendRelationMapper.selectCount(new QueryWrapper<FriendRelation>()
                .eq("user_id", userId)
                .eq("friend_user_id", otherUserId)) > 0;
    }

    private boolean hasSharedCircle(Long userId, Long otherUserId) {
        List<Long> myCircles = buddyCircleMemberMapper.selectList(new QueryWrapper<BuddyCircleMember>().eq("user_id", userId))
                .stream()
                .map(BuddyCircleMember::getCircleId)
                .collect(Collectors.toList());
        if (myCircles.isEmpty()) {
            return false;
        }
        return buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                .eq("user_id", otherUserId)
                .in("circle_id", myCircles)) > 0;
    }

    private Long resolveTargetUserId(Long targetUserId, String targetAccount) {
        if (targetUserId != null) {
            return targetUserId;
        }
        if (targetAccount != null && !targetAccount.trim().isEmpty()) {
            UserAccount user = userAccountMapper.selectOne(new QueryWrapper<UserAccount>()
                    .eq("account", targetAccount.trim()).last("LIMIT 1"));
            if (user != null) {
                return user.getId();
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "请选择有效的用户");
    }

    private FriendRequestItemResponse toFriendRequestItem(FriendRequest request) {
        UserAccount requester = userAccountMapper.selectById(request.getRequesterUserId());
        UserAccount target = userAccountMapper.selectById(request.getTargetUserId());
        FriendRequestItemResponse item = new FriendRequestItemResponse();
        item.setId(request.getId());
        item.setRequesterUserId(request.getRequesterUserId());
        item.setTargetUserId(request.getTargetUserId());
        item.setMessage(request.getMessage());
        item.setStatus(request.getStatus());
        item.setCreatedAt(request.getCreatedAt());
        if (requester != null) {
            item.setRequesterNickname(requester.getNickname());
            item.setRequesterAvatar(requester.getAvatar());
        }
        if (target != null) {
            item.setTargetNickname(target.getNickname());
            item.setTargetAvatar(target.getAvatar());
        }
        return item;
    }

    private void createFriendRelation(Long userId, Long friendUserId) {
        if (!isFriend(userId, friendUserId)) {
            FriendRelation relation = new FriendRelation();
            relation.setUserId(userId);
            relation.setFriendUserId(friendUserId);
            relation.setCreatedAt(LocalDateTime.now());
            friendRelationMapper.insert(relation);
        }
    }

    private boolean matchesFilter(ActivityFeed feed, String filter) {
        if ("circle".equalsIgnoreCase(filter)) {
            return "circle_shared".equals(feed.getActivityType());
        }
        if ("new".equalsIgnoreCase(filter)) {
            return !"circle_shared".equals(feed.getActivityType());
        }
        return true;
    }

    private boolean canSeeFeed(ActivityFeed feed, Long viewerUserId) {
        if (Objects.equals(feed.getActorUserId(), viewerUserId)) {
            return true;
        }
        if ("public".equals(feed.getVisibilityScope())) {
            return true;
        }
        if ("friends".equals(feed.getVisibilityScope())) {
            return isFriend(viewerUserId, feed.getActorUserId());
        }
        if ("circle".equals(feed.getVisibilityScope()) && feed.getCircleId() != null) {
            return buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                    .eq("circle_id", feed.getCircleId())
                    .eq("user_id", viewerUserId)) > 0;
        }
        return false;
    }

    private FeedItemResponse toFeedItem(ActivityFeed feed) {
        UserAccount actor = userAccountMapper.selectById(feed.getActorUserId());
        DishSummaryResponse dish = feed.getDishId() == null ? null : withEffectiveVisibility(
                dishMapper.selectAllActive().stream()
                        .filter(item -> item.getId().equals(feed.getDishId()))
                        .findFirst()
                        .orElse(null),
                actor == null ? null : actor.getId());
        if (actor == null || dish == null) {
            return null;
        }
        FeedItemResponse item = new FeedItemResponse();
        item.setId(feed.getId());
        item.setActorUserId(actor.getId());
        item.setActorNickname(actor.getNickname());
        item.setActorAvatar(actor.getAvatar());
        item.setActivityType(feed.getActivityType());
        item.setActionText(buildFeedActionText(feed.getActivityType(), feed.getCreatedAt()));
        item.setVisibilityScope(feed.getVisibilityScope());
        item.setDishId(dish.getId());
        item.setDishName(dish.getName());
        item.setDishImage(dish.getImage());
        item.setDishDescription(dish.getDescription());
        item.setCreatedAt(feed.getCreatedAt());
        if (feed.getCircleId() != null) {
            BuddyCircle circle = buddyCircleMapper.selectById(feed.getCircleId());
            if (circle != null) {
                item.setCircleId(circle.getId());
                item.setCircleName(circle.getName());
            }
        }
        return item;
    }

    private String buildFeedActionText(String activityType, LocalDateTime createdAt) {
        String prefix = timeAgo(createdAt);
        if ("circle_shared".equals(activityType)) {
            return prefix + "在搭子圈里共享菜单";
        }
        if ("dish_updated".equals(activityType)) {
            return prefix + "更新菜单";
        }
        return prefix + "发布了新菜";
    }

    private String timeAgo(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        long minutes = java.time.Duration.between(time, LocalDateTime.now()).toMinutes();
        if (minutes < 1) {
            return "刚刚";
        }
        if (minutes < 60) {
            return minutes + " 分钟前";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " 小时前";
        }
        long days = hours / 24;
        return days + " 天前";
    }

    private AuthUserResponse toAuthUser(UserAccount user) {
        AuthUserResponse item = new AuthUserResponse();
        item.setId(user.getId());
        item.setAccount(user.getAccount());
        item.setNickname(user.getNickname());
        item.setAvatar(user.getAvatar());
        item.setBio(user.getBio());
        item.setDefaultMenuVisibility(getSettings(user.getId()).getDefaultMenuVisibility());
        return item;
    }

    private List<AccessRuleResponse> buildAccessRules(boolean friend) {
        List<AccessRuleResponse> rules = new ArrayList<>();
        rules.add(rule("公开菜单", "任何人都能进入。", "可访问"));
        rules.add(rule("好友可见菜单", friend ? "你可以直接收藏与查看详情。" : "需要先成为好友。", friend ? "已开放" : "待解锁"));
        rules.add(rule("私密菜单", "仅菜单主人本人可见。", "锁定"));
        return rules;
    }

    private AccessRuleResponse rule(String label, String description, String state) {
        AccessRuleResponse rule = new AccessRuleResponse();
        rule.setLabel(label);
        rule.setDescription(description);
        rule.setState(state);
        return rule;
    }

    private BuddyCircleSummaryResponse buildCircleSummary(BuddyCircle circle, Long viewerUserId) {
        BuddyCircleSummaryResponse summary = new BuddyCircleSummaryResponse();
        summary.setId(circle.getId());
        summary.setName(circle.getName());
        summary.setDescription(circle.getDescription());
        summary.setOwnerUserId(circle.getOwnerUserId());
        UserAccount owner = userAccountMapper.selectById(circle.getOwnerUserId());
        summary.setOwnerNickname(owner == null ? "" : owner.getNickname());
        summary.setMemberCount(buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>().eq("circle_id", circle.getId())));
        summary.setSharedMenuCount(getCircleMenus(circle.getId()).size());
        summary.setWeeklyUpdateCount(countCircleWeeklyUpdates(circle.getId()));
        return summary;
    }

    private long countCircleWeeklyUpdates(Long circleId) {
        LocalDateTime start = LocalDate.now().minusDays(7).atStartOfDay();
        return activityFeedMapper.selectCount(new QueryWrapper<ActivityFeed>()
                .eq("circle_id", circleId)
                .ge("created_at", start));
    }

    private BuddyCircle requireCircleMember(Long circleId, Long currentUserId) {
        BuddyCircle circle = buddyCircleMapper.selectById(circleId);
        if (circle == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "搭子圈不存在");
        }
        long count = buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                .eq("circle_id", circleId)
                .eq("user_id", currentUserId));
        if (count == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "你还不在这个搭子圈里");
        }
        return circle;
    }

    private void addCircleMember(Long circleId, Long inviterUserId, Long inviteeUserId) {
        long exists = buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                .eq("circle_id", circleId).eq("user_id", inviteeUserId));
        if (exists > 0) {
            return;
        }
        BuddyCircleMember member = new BuddyCircleMember();
        member.setCircleId(circleId);
        member.setUserId(inviteeUserId);
        member.setRole("member");
        member.setJoinedAt(LocalDateTime.now());
        buddyCircleMemberMapper.insert(member);

        BuddyCircleInvite invite = new BuddyCircleInvite();
        invite.setCircleId(circleId);
        invite.setInviterUserId(inviterUserId);
        invite.setInviteeUserId(inviteeUserId);
        invite.setStatus("joined");
        invite.setCreatedAt(LocalDateTime.now());
        buddyCircleInviteMapper.insert(invite);
    }

    private UserProfileSettings getSettings(Long userId) {
        UserProfileSettings settings = userProfileSettingsMapper.selectById(userId);
        if (settings == null) {
            settings = new UserProfileSettings();
            settings.setUserId(userId);
            settings.setDefaultMenuVisibility("friends");
            settings.setAllowFriendFeed(true);
            userProfileSettingsMapper.insert(settings);
        }
        return settings;
    }

    private DishSummaryResponse withEffectiveVisibility(DishSummaryResponse item, Long ownerUserId) {
        if (item == null || ownerUserId == null) {
            return item;
        }
        item.setEffectiveVisibility(VisibilityUtils.effectiveVisibility(item.getVisibility(), getSettings(ownerUserId).getDefaultMenuVisibility()));
        return item;
    }

    private boolean canViewDish(DishSummaryResponse dish, Long viewerUserId, boolean circleMode) {
        if (dish == null) {
            return false;
        }
        if (Objects.equals(dish.getOwnerUserId(), viewerUserId)) {
            return true;
        }
        String effectiveVisibility = dish.getEffectiveVisibility();
        if (effectiveVisibility == null) {
            effectiveVisibility = VisibilityUtils.effectiveVisibility(dish.getVisibility(), getSettings(dish.getOwnerUserId()).getDefaultMenuVisibility());
            dish.setEffectiveVisibility(effectiveVisibility);
        }
        if ("public".equals(effectiveVisibility)) {
            return true;
        }
        if ("private".equals(effectiveVisibility)) {
            return false;
        }
        if ("friends".equals(effectiveVisibility)) {
            return isFriend(viewerUserId, dish.getOwnerUserId()) || (circleMode && hasSharedCircle(viewerUserId, dish.getOwnerUserId()));
        }
        return false;
    }
}
