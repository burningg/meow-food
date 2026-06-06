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
    private final DishIngredientMapper dishIngredientMapper;
    private final ActivityFeedMapper activityFeedMapper;
    private final BuddyCircleMapper buddyCircleMapper;
    private final BuddyCircleMemberMapper buddyCircleMemberMapper;
    private final BuddyCircleInviteMapper buddyCircleInviteMapper;
    private final UserDefaultVisibilityCircleMapper userDefaultVisibilityCircleMapper;
    private final AuthService authService;
    private final VipService vipService;
    private final MenuVisibilitySupport menuVisibilitySupport;

    public SocialServiceImpl(UserAccountMapper userAccountMapper,
                             UserProfileSettingsMapper userProfileSettingsMapper,
                             FriendRequestMapper friendRequestMapper,
                             FriendRelationMapper friendRelationMapper,
                             DishMapper dishMapper,
                             DishIngredientMapper dishIngredientMapper,
                             ActivityFeedMapper activityFeedMapper,
                             BuddyCircleMapper buddyCircleMapper,
                             BuddyCircleMemberMapper buddyCircleMemberMapper,
                             BuddyCircleInviteMapper buddyCircleInviteMapper,
                             AuthService authService,
                             VipService vipService,
                             UserDefaultVisibilityCircleMapper userDefaultVisibilityCircleMapper,
                             MenuVisibilitySupport menuVisibilitySupport) {
        this.userAccountMapper = userAccountMapper;
        this.userProfileSettingsMapper = userProfileSettingsMapper;
        this.friendRequestMapper = friendRequestMapper;
        this.friendRelationMapper = friendRelationMapper;
        this.dishMapper = dishMapper;
        this.dishIngredientMapper = dishIngredientMapper;
        this.activityFeedMapper = activityFeedMapper;
        this.buddyCircleMapper = buddyCircleMapper;
        this.buddyCircleMemberMapper = buddyCircleMemberMapper;
        this.buddyCircleInviteMapper = buddyCircleInviteMapper;
        this.authService = authService;
        this.vipService = vipService;
        this.userDefaultVisibilityCircleMapper = userDefaultVisibilityCircleMapper;
        this.menuVisibilitySupport = menuVisibilitySupport;
    }

    @Override
    public ProfileResponse getProfile() {
        String userId = AuthContext.requireUserId();
        UserProfileSettings settings = getSettings(userId);
        ProfileResponse response = new ProfileResponse();
        response.setUser(authService.getCurrentUser());
        response.setStats(buildProfileStats(userId));
        response.setFriendPreview(getFriends().stream().limit(2).collect(Collectors.toList()));
        response.setDefaultMenuVisibility(VisibilityUtils.normalizeProfileVisibility(settings.getDefaultMenuVisibility()));
        response.setDefaultMenuCircleIds(menuVisibilitySupport.getDefaultMenuCircleIds(userId));
        response.setLastSelectedCircleId(settings.getLastSelectedCircleId());
        response.setVipInfo(vipService.getVipInfo(userId));
        return response;
    }

    @Override
    @Transactional
    public AuthUserResponse updateProfile(ProfileUpdateRequest request) {
        String userId = AuthContext.requireUserId();
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }

        String nickname = request.getNickname() == null ? "" : request.getNickname().trim();
        if (nickname.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "昵称不能为空");
        }
        if (nickname.length() > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "昵称不能超过 20 个字符");
        }

        String bio = request.getBio() == null ? "" : request.getBio().trim();
        if (bio.length() > 80) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "简介不能超过 80 个字符");
        }

        user.setNickname(nickname);
        user.setBio(bio);
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar().trim());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);
        return authService.getCurrentUser();
    }

    @Override
    @Transactional
    public AuthUserResponse updateDefaultVisibility(ProfileVisibilityUpdateRequest request) {
        String userId = AuthContext.requireUserId();
        UserProfileSettings settings = getSettings(userId);
        String visibility = VisibilityUtils.normalizeProfileVisibility(request.getDefaultMenuVisibility());
        validateProfileVisibility(visibility);
        List<String> circleIds = normalizeOwnedCircleIds(userId, request.getDefaultMenuCircleIds());
        if (VisibilityUtils.VISIBILITY_CIRCLE.equals(visibility) && circleIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "指定圈子权限至少选择一个圈子");
        }
        settings.setDefaultMenuVisibility(visibility);
        userProfileSettingsMapper.updateById(settings);
        replaceDefaultVisibilityCircles(userId, VisibilityUtils.VISIBILITY_CIRCLE.equals(visibility) ? circleIds : Collections.emptyList());
        return authService.getCurrentUser();
    }

    @Override
    @Transactional
    public void updateLastSelectedCircle(ProfileLastSelectedCircleUpdateRequest request) {
        String userId = AuthContext.requireUserId();
        UserProfileSettings settings = getSettings(userId);
        String circleId = request == null || request.getLastSelectedCircleId() == null
                ? null
                : request.getLastSelectedCircleId().trim();

        if (circleId != null && circleId.isEmpty()) {
            circleId = null;
        }

        if (circleId != null) {
            long memberCount = buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                    .eq("circle_id", circleId)
                    .eq("user_id", userId));
            if (memberCount == 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "只能保存你已加入的圈子");
            }
        }

        settings.setLastSelectedCircleId(circleId);
        userProfileSettingsMapper.updateById(settings);
    }

    @Override
    public List<FriendItemResponse> getFriends() {
        String currentUserId = AuthContext.requireUserId();
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
        String currentUserId = AuthContext.requireUserId();
        String targetUserId = resolveTargetUserId(request.getTargetUserId(), request.getTargetAccount());
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
        String currentUserId = AuthContext.requireUserId();
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
    public FriendInvitationResponse getFriendInvitation(String inviterUserId) {
        String currentUserId = AuthContext.requireUserId();
        UserAccount inviter = requireInvitationInviter(inviterUserId, currentUserId);
        FriendRequest request = findLatestInvitationRequest(inviterUserId, currentUserId);
        return buildInvitationResponse(inviter, currentUserId, request);
    }

    @Override
    @Transactional
    public FriendInvitationResponse acceptFriendInvitation(String inviterUserId) {
        String currentUserId = AuthContext.requireUserId();
        UserAccount inviter = requireInvitationInviter(inviterUserId, currentUserId);
        FriendRequest request = findLatestInvitationRequest(inviterUserId, currentUserId);
        if (isFriend(inviterUserId, currentUserId)) {
            return buildInvitationResponse(inviter, currentUserId, request);
        }
        if (request == null || !"pending".equals(request.getStatus())) {
            request = createInvitationRequest(inviterUserId, currentUserId);
        }
        request.setStatus("accepted");
        request.setHandledAt(LocalDateTime.now());
        friendRequestMapper.updateById(request);
        createFriendRelation(inviterUserId, currentUserId);
        createFriendRelation(currentUserId, inviterUserId);
        return buildInvitationResponse(inviter, currentUserId, request);
    }

    @Override
    @Transactional
    public FriendInvitationResponse rejectFriendInvitation(String inviterUserId) {
        String currentUserId = AuthContext.requireUserId();
        UserAccount inviter = requireInvitationInviter(inviterUserId, currentUserId);
        FriendRequest request = findLatestInvitationRequest(inviterUserId, currentUserId);
        if (isFriend(inviterUserId, currentUserId)) {
            return buildInvitationResponse(inviter, currentUserId, request);
        }
        if (request == null || !"pending".equals(request.getStatus())) {
            request = createInvitationRequest(inviterUserId, currentUserId);
        }
        request.setStatus("rejected");
        request.setHandledAt(LocalDateTime.now());
        friendRequestMapper.updateById(request);
        return buildInvitationResponse(inviter, currentUserId, request);
    }

    @Override
    @Transactional
    public FriendRequestItemResponse acceptFriendRequest(String requestId) {
        String currentUserId = AuthContext.requireUserId();
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || !Objects.equals(request.getTargetUserId(), currentUserId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "好友申请不存在");
        }
        if (!"pending".equals(request.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "该好友申请已经处理过了");
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
    public FriendRequestItemResponse rejectFriendRequest(String requestId) {
        String currentUserId = AuthContext.requireUserId();
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || !Objects.equals(request.getTargetUserId(), currentUserId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "好友申请不存在");
        }
        if (!"pending".equals(request.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "该好友申请已经处理过了");
        }
        request.setStatus("rejected");
        request.setHandledAt(LocalDateTime.now());
        friendRequestMapper.updateById(request);
        return toFriendRequestItem(request);
    }

    @Override
    public List<FeedItemResponse> getFeed(String filter) {
        String currentUserId = AuthContext.requireUserId();
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
        String currentUserId = AuthContext.requireUserId();
        FeedAccessibleMenusResponse response = new FeedAccessibleMenusResponse();
        List<DishSummaryResponse> all = dishMapper.selectAllActive();
        menuVisibilitySupport.hydrateSummaries(all);
        response.setMenus(all.stream()
                .filter(item -> !Objects.equals(item.getOwnerUserId(), currentUserId))
                .filter(item -> canViewDish(item, currentUserId, false))
                .limit(3)
                .collect(Collectors.toList()));
        return response;
    }

    @Override
    public UserMenuAccessResponse getUserMenuAccess(String targetUserId, String circleId) {
        String currentUserId = AuthContext.requireUserId();
        UserAccount targetUser = userAccountMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        boolean friend = isFriend(currentUserId, targetUserId);
        boolean sameCircle = hasSharedCircle(currentUserId, targetUserId);
        boolean viewerInAnyCircle = menuVisibilitySupport.isUserInAnyCircle(currentUserId);
        String scopedCircleId = circleId == null ? "" : circleId.trim();
        if (!scopedCircleId.isEmpty()) {
            requireCircleMember(scopedCircleId, currentUserId);
        }
        List<DishSummaryResponse> ownerMenus = dishMapper.selectByOwnerUserId(targetUserId);
        menuVisibilitySupport.hydrateSummaries(ownerMenus);
        List<DishSummaryResponse> visible = ownerMenus.stream()
                .filter(item -> scopedCircleId.isEmpty()
                        ? canViewDish(item, currentUserId, false)
                        : isVisibleInCircleContext(item, currentUserId, scopedCircleId))
                .collect(Collectors.toList());
        long privateCount = ownerMenus.size() - visible.size();

        UserMenuAccessResponse response = new UserMenuAccessResponse();
        response.setUser(toAuthUser(targetUser));
        response.setFriend(friend);
        response.setSameCircle(sameCircle);
        response.setActionType(friend ? "invite-circle" : "friend-request");
        response.setAccessibleCount(visible.size());
        response.setPrivateCount(privateCount);
        response.setMenus(visible);
        response.setAccessRules(buildAccessRules(friend, sameCircle, viewerInAnyCircle));
        return response;
    }

    @Override
    public List<DishSummaryResponse> getVisibleMenusByUser(String targetUserId) {
        return getUserMenuAccess(targetUserId, null).getMenus();
    }

    @Override
    public List<BuddyCircleSummaryResponse> getCircles() {
        String currentUserId = AuthContext.requireUserId();
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
        String currentUserId = AuthContext.requireUserId();
        assertCircleLimit(currentUserId);
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

        for (String memberId : request.getInitialMemberIds()) {
            if (memberId != null && !Objects.equals(memberId, currentUserId)) {
                addCircleMember(circle.getId(), currentUserId, memberId);
            }
        }
        return getCircleDetail(circle.getId());
    }

    @Override
    public BuddyCircleDetailResponse getCircleDetail(String circleId) {
        String currentUserId = AuthContext.requireUserId();
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
    public BuddyCircleShareInvitationResponse getCircleShareInvitation(String circleId, String inviterUserId) {
        String currentUserId = AuthContext.requireUserId();
        BuddyCircle circle = requireCircleShareInviter(circleId, inviterUserId, currentUserId);
        return buildCircleShareInvitationResponse(circle, inviterUserId, currentUserId);
    }

    @Override
    public List<BuddyCircleMemberResponse> getCircleMembers(String circleId) {
        String currentUserId = AuthContext.requireUserId();
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
    public List<DishSummaryResponse> getCircleMenus(String circleId) {
        String currentUserId = AuthContext.requireUserId();
        requireCircleMember(circleId, currentUserId);
        List<String> memberIds = buddyCircleMemberMapper.selectList(new QueryWrapper<BuddyCircleMember>()
                        .eq("circle_id", circleId))
                .stream()
                .map(BuddyCircleMember::getUserId)
                .collect(Collectors.toList());
        List<DishSummaryResponse> result = new ArrayList<>();
        for (String memberId : memberIds) {
            List<DishSummaryResponse> dishes = dishMapper.selectByOwnerUserId(memberId);
            menuVisibilitySupport.hydrateSummaries(dishes);
            for (DishSummaryResponse dish : dishes) {
                if (isVisibleInCircleContext(dish, currentUserId, circleId)) {
                    result.add(dish);
                }
            }
        }
        hydrateIngredientNames(result);
        result.sort(Comparator
                .comparing(DishSummaryResponse::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DishSummaryResponse::getId, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    @Override
    @Transactional
    public BuddyCircleDetailResponse inviteToCircle(String circleId, BuddyCircleInviteRequest request) {
        String currentUserId = AuthContext.requireUserId();
        requireCircleMember(circleId, currentUserId);
        String inviteeUserId = resolveTargetUserId(request.getInviteeUserId(), request.getInviteeAccount());
        if (Objects.equals(currentUserId, inviteeUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不用邀请自己，你已经在圈子里了");
        }
        if (!isFriend(currentUserId, inviteeUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "只能邀请自己的好友加入搭子圈");
        }
        addCircleMember(circleId, currentUserId, inviteeUserId);
        return getCircleDetail(circleId);
    }

    @Override
    @Transactional
    public BuddyCircleDetailResponse acceptCircleShareInvitation(String circleId, String inviterUserId) {
        String currentUserId = AuthContext.requireUserId();
        requireCircleShareInviter(circleId, inviterUserId, currentUserId);
        addCircleMember(circleId, inviterUserId, currentUserId);
        return getCircleDetail(circleId);
    }

    private ProfileStatsResponse buildProfileStats(String userId) {
        ProfileStatsResponse stats = new ProfileStatsResponse();
        stats.setFriendCount(friendRelationMapper.selectCount(new QueryWrapper<FriendRelation>()
                .eq("user_id", userId)));
        stats.setMenuCount(dishMapper.selectCount(new QueryWrapper<Dish>().eq("owner_user_id", userId).eq("status", 1)));
        stats.setCircleCount(buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>().eq("user_id", userId)));
        return stats;
    }

    private long countVisibleMenusForViewer(String ownerUserId, String viewerUserId, boolean circleMode) {
        List<DishSummaryResponse> dishes = dishMapper.selectByOwnerUserId(ownerUserId);
        menuVisibilitySupport.hydrateSummaries(dishes);
        return dishes.stream()
                .filter(item -> canViewDish(item, viewerUserId, circleMode))
                .count();
    }

    private boolean isFriend(String userId, String otherUserId) {
        return menuVisibilitySupport.isFriend(userId, otherUserId);
    }

    private boolean hasSharedCircle(String userId, String otherUserId) {
        return menuVisibilitySupport.hasSharedCircle(userId, otherUserId);
    }

    private String resolveTargetUserId(String targetUserId, String targetAccount) {
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

    private UserAccount requireInvitationInviter(String inviterUserId, String currentUserId) {
        if (Objects.equals(inviterUserId, currentUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能邀请自己成为好友");
        }
        UserAccount inviter = userAccountMapper.selectById(inviterUserId);
        if (inviter == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "邀请人不存在");
        }
        return inviter;
    }

    private FriendRequest findLatestInvitationRequest(String inviterUserId, String currentUserId) {
        return friendRequestMapper.selectOne(new QueryWrapper<FriendRequest>()
                .eq("requester_user_id", inviterUserId)
                .eq("target_user_id", currentUserId)
                .orderByDesc("created_at")
                .last("LIMIT 1"));
    }

    private FriendRequest createInvitationRequest(String inviterUserId, String currentUserId) {
        FriendRequest request = new FriendRequest();
        request.setRequesterUserId(inviterUserId);
        request.setTargetUserId(currentUserId);
        request.setMessage("通过小程序卡片邀请成为好友");
        request.setStatus("pending");
        request.setCreatedAt(LocalDateTime.now());
        friendRequestMapper.insert(request);
        return request;
    }

    private FriendInvitationResponse buildInvitationResponse(UserAccount inviter, String currentUserId, FriendRequest request) {
        FriendInvitationResponse response = new FriendInvitationResponse();
        response.setInviter(toAuthUser(inviter));
        response.setRequest(request == null ? null : toFriendRequestItem(request));
        if (isFriend(inviter.getId(), currentUserId)) {
            response.setStatus("already_friend");
        } else if (request != null) {
            response.setStatus(request.getStatus());
        } else {
            response.setStatus("pending");
        }
        return response;
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

    private void createFriendRelation(String userId, String friendUserId) {
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
            return "circle".equals(feed.getVisibilityScope());
        }
        if ("new".equalsIgnoreCase(filter)) {
            return !"circle".equals(feed.getVisibilityScope());
        }
        return true;
    }

    private boolean canSeeFeed(ActivityFeed feed, String viewerUserId) {
        if (Objects.equals(feed.getActorUserId(), viewerUserId)) {
            return true;
        }
        if ("public".equals(feed.getVisibilityScope())) {
            return menuVisibilitySupport.isUserInAnyCircle(viewerUserId);
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
        DishSummaryResponse dish = feed.getDishId() == null ? null : dishMapper.selectAllActive().stream()
                .filter(item -> item.getId().equals(feed.getDishId()))
                .findFirst()
                .orElse(null);
        if (dish != null) {
            menuVisibilitySupport.hydrateSummaries(List.of(dish));
        }
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
        item.setDefaultMenuVisibility(VisibilityUtils.normalizeProfileVisibility(getSettings(user.getId()).getDefaultMenuVisibility()));
        item.setDefaultMenuCircleIds(menuVisibilitySupport.getDefaultMenuCircleIds(user.getId()));
        return item;
    }

    private List<AccessRuleResponse> buildAccessRules(boolean friend, boolean sameCircle, boolean viewerInAnyCircle) {
        List<AccessRuleResponse> rules = new ArrayList<>();
        rules.add(rule(
                "圈内公开菜单",
                viewerInAnyCircle ? "只要你已加入任意圈子，就可以查看。" : "先加入任意圈子后才能查看。",
                viewerInAnyCircle ? "已开放" : "待解锁"));
        rules.add(rule(
                "指定圈子菜单",
                sameCircle ? "你们有共享圈子，命中的圈子菜单会开放。" : "只有被指定的圈子成员可见。",
                sameCircle ? "按圈子开放" : "按圈子锁定"));
        rules.add(rule(
                "好友可见菜单(旧数据)",
                friend ? "旧数据中的好友权限仍然对你开放。" : "仅历史好友权限数据继续兼容。",
                friend ? "已开放" : "旧数据兼容"));
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

    private BuddyCircleSummaryResponse buildCircleSummary(BuddyCircle circle, String viewerUserId) {
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

    private long countCircleWeeklyUpdates(String circleId) {
        LocalDateTime start = LocalDate.now().minusDays(7).atStartOfDay();
        return activityFeedMapper.selectCount(new QueryWrapper<ActivityFeed>()
                .eq("circle_id", circleId)
                .ge("created_at", start));
    }

    private BuddyCircle requireCircleMember(String circleId, String currentUserId) {
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

    private void hydrateIngredientNames(List<DishSummaryResponse> dishes) {
        List<String> dishIds = dishes.stream()
                .map(DishSummaryResponse::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (dishIds.isEmpty()) {
            return;
        }

        Map<String, List<String>> ingredientNamesByDishId = dishIngredientMapper.selectList(new QueryWrapper<DishIngredient>()
                        .in("dish_id", dishIds)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .filter(item -> item.getDishId() != null && item.getName() != null && !item.getName().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        DishIngredient::getDishId,
                        Collectors.mapping(DishIngredient::getName, Collectors.toList())));

        for (DishSummaryResponse dish : dishes) {
            dish.setIngredientNames(new ArrayList<>(ingredientNamesByDishId.getOrDefault(dish.getId(), Collections.emptyList())));
        }
    }

    private BuddyCircle requireCircleShareInviter(String circleId, String inviterUserId, String currentUserId) {
        if (Objects.equals(inviterUserId, currentUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能通过自己的分享卡片加入搭子圈");
        }
        BuddyCircle circle = buddyCircleMapper.selectById(circleId);
        if (circle == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "搭子圈不存在");
        }
        UserAccount inviter = userAccountMapper.selectById(inviterUserId);
        if (inviter == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "邀请人不存在");
        }
        long inviterInCircle = buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                .eq("circle_id", circleId)
                .eq("user_id", inviterUserId));
        if (inviterInCircle == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "邀请人不在这个搭子圈里");
        }
        return circle;
    }

    private BuddyCircleShareInvitationResponse buildCircleShareInvitationResponse(BuddyCircle circle, String inviterUserId, String currentUserId) {
        UserAccount inviter = userAccountMapper.selectById(inviterUserId);
        boolean friend = isFriend(inviterUserId, currentUserId);
        boolean member = isCircleMember(circle.getId(), currentUserId);
        BuddyCircleShareInvitationResponse response = new BuddyCircleShareInvitationResponse();
        response.setInviter(toAuthUser(inviter));
        response.setCircle(buildShareCircleSummary(circle));
        response.setFriend(friend);
        response.setMember(member);
        response.setStatus(member ? "already_member" : friend ? "friend_ready" : "need_friend_accept");
        return response;
    }

    private BuddyCircleSummaryResponse buildShareCircleSummary(BuddyCircle circle) {
        BuddyCircleSummaryResponse summary = new BuddyCircleSummaryResponse();
        summary.setId(circle.getId());
        summary.setName(circle.getName());
        summary.setDescription(circle.getDescription());
        summary.setOwnerUserId(circle.getOwnerUserId());
        UserAccount owner = userAccountMapper.selectById(circle.getOwnerUserId());
        summary.setOwnerNickname(owner == null ? "" : owner.getNickname());
        summary.setMemberCount(buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>().eq("circle_id", circle.getId())));
        summary.setSharedMenuCount(countCircleActiveMenus(circle.getId()));
        summary.setWeeklyUpdateCount(countCircleWeeklyUpdates(circle.getId()));
        return summary;
    }

    private boolean isCircleMember(String circleId, String userId) {
        return buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                .eq("circle_id", circleId)
                .eq("user_id", userId)) > 0;
    }

    private long countCircleActiveMenus(String circleId) {
        return buddyCircleMemberMapper.selectList(new QueryWrapper<BuddyCircleMember>().eq("circle_id", circleId))
                .stream()
                .map(BuddyCircleMember::getUserId)
                .mapToLong(memberId -> {
                    List<DishSummaryResponse> dishes = dishMapper.selectByOwnerUserId(memberId);
                    menuVisibilitySupport.hydrateSummaries(dishes);
                    return dishes.stream().filter(dish -> isSharedWithCircle(dish, circleId)).count();
                })
                .sum();
    }

    private void addCircleMember(String circleId, String inviterUserId, String inviteeUserId) {
        long exists = buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                .eq("circle_id", circleId).eq("user_id", inviteeUserId));
        if (exists > 0) {
            return;
        }
        assertCircleLimit(inviteeUserId);
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

    private void assertCircleLimit(String userId) {
        int limit = vipService.getCircleLimit(userId);
        Long count = buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>().eq("user_id", userId));
        if (count != null && count >= limit) {
            String message = limit <= 3
                    ? "普通用户最多加入 3 个搭子圈，开通 VIP 可提升至 10 个"
                    : "VIP 用户最多加入 10 个搭子圈";
            throw new ApiException(HttpStatus.FORBIDDEN, message);
        }
    }

    private UserProfileSettings getSettings(String userId) {
        UserProfileSettings settings = userProfileSettingsMapper.selectById(userId);
        if (settings == null) {
            settings = new UserProfileSettings();
            settings.setUserId(userId);
            settings.setDefaultMenuVisibility(VisibilityUtils.DEFAULT_PROFILE_VISIBILITY);
            settings.setAllowFriendFeed(true);
            userProfileSettingsMapper.insert(settings);
        }
        return settings;
    }

    private DishSummaryResponse withEffectiveVisibility(DishSummaryResponse item, String ownerUserId) {
        if (item == null) {
            return item;
        }
        menuVisibilitySupport.hydrateSummaries(List.of(item));
        return item;
    }

    private boolean canViewDish(DishSummaryResponse dish, String viewerUserId, boolean circleMode) {
        return menuVisibilitySupport.canViewDish(dish, viewerUserId, circleMode);
    }

    private boolean isVisibleInCircleContext(DishSummaryResponse dish, String viewerUserId, String circleId) {
        if (dish == null) {
            return false;
        }
        if (VisibilityUtils.VISIBILITY_PUBLIC.equals(dish.getEffectiveVisibility())) {
            return true;
        }
        if (VisibilityUtils.VISIBILITY_CIRCLE.equals(dish.getEffectiveVisibility())) {
            return dish.getEffectiveCircleIds().contains(circleId);
        }
        return canViewDish(dish, viewerUserId, true);
    }

    private boolean isSharedWithCircle(DishSummaryResponse dish, String circleId) {
        if (dish == null) {
            return false;
        }
        if (VisibilityUtils.VISIBILITY_PUBLIC.equals(dish.getEffectiveVisibility())) {
            return true;
        }
        if (VisibilityUtils.VISIBILITY_CIRCLE.equals(dish.getEffectiveVisibility())) {
            return dish.getEffectiveCircleIds().contains(circleId);
        }
        return VisibilityUtils.VISIBILITY_FRIENDS.equals(dish.getEffectiveVisibility());
    }

    private void validateProfileVisibility(String visibility) {
        if (!VisibilityUtils.isSupportedProfileVisibility(visibility) || VisibilityUtils.DISH_VISIBILITY_INHERIT.equals(visibility)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "无效的默认菜单权限");
        }
    }

    private List<String> normalizeOwnedCircleIds(String userId, List<String> circleIds) {
        List<String> normalized = circleIds == null
                ? Collections.emptyList()
                : circleIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (normalized.isEmpty()) {
            return normalized;
        }
        long ownedCount = buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                .eq("user_id", userId)
                .in("circle_id", normalized));
        if (ownedCount != normalized.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "指定圈子里包含你未加入的圈子");
        }
        return normalized;
    }

    private void replaceDefaultVisibilityCircles(String userId, List<String> circleIds) {
        userDefaultVisibilityCircleMapper.delete(new QueryWrapper<UserDefaultVisibilityCircle>().eq("user_id", userId));
        for (String circleId : circleIds) {
            UserDefaultVisibilityCircle relation = new UserDefaultVisibilityCircle();
            relation.setUserId(userId);
            relation.setCircleId(circleId);
            relation.setCreatedAt(LocalDateTime.now());
            userDefaultVisibilityCircleMapper.insert(relation);
        }
    }
}
