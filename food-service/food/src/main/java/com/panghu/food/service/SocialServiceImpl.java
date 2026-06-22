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
    private final UserVipMapper userVipMapper;
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
                             UserVipMapper userVipMapper,
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
        this.userVipMapper = userVipMapper;
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
        response.setDefaultMenuVisibility(VisibilityUtils.normalizeProfileVisibility(settings.getDefaultMenuVisibility()));
        response.setDefaultMenuCircleIds(menuVisibilitySupport.getDefaultMenuCircleIds(userId));
        response.setLastSelectedCircleId(settings.getLastSelectedCircleId());
        response.setVipInfo(vipService.getVipInfo(userId));
        response.setShowKnowledgeOnHome(enabledByDefault(settings.getShowKnowledgeOnHome()));
        response.setShowPetOnHome(enabledByDefault(settings.getShowPetOnHome()));
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
    public AuthUserResponse updateHomePreferences(ProfileHomePreferencesUpdateRequest request) {
        String userId = AuthContext.requireUserId();
        UserProfileSettings settings = getSettings(userId);
        settings.setShowKnowledgeOnHome(request == null || enabledByDefault(request.getShowKnowledgeOnHome()));
        settings.setShowPetOnHome(request == null || enabledByDefault(request.getShowPetOnHome()));
        userProfileSettingsMapper.updateById(settings);
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
        Map<String, UserAccount> usersById = selectUsersByIds(relations.stream()
                .map(FriendRelation::getFriendUserId)
                .collect(Collectors.toList()));
        List<FriendItemResponse> result = new ArrayList<>();
        for (FriendRelation relation : relations) {
            UserAccount user = usersById.get(relation.getFriendUserId());
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
        response.setIncoming(toFriendRequestItems(incoming));
        response.setOutgoing(toFriendRequestItems(outgoing));
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
        List<ActivityFeed> filteredFeeds = new ArrayList<>();
        List<ActivityFeed> visibleFeeds = new ArrayList<>();
        List<FeedItemResponse> result = new ArrayList<>();
        for (ActivityFeed feed : feeds) {
            if (!matchesFilter(feed, filter)) {
                continue;
            }
            filteredFeeds.add(feed);
        }

        FeedVisibilityContext visibilityContext = loadFeedVisibilityContext(filteredFeeds, currentUserId);
        for (ActivityFeed feed : filteredFeeds) {
            if (!canSeeFeed(feed, currentUserId, visibilityContext)) {
                continue;
            }
            visibleFeeds.add(feed);
        }

        FeedResourceLookup resources = loadFeedResources(visibleFeeds);
        for (ActivityFeed feed : visibleFeeds) {
            FeedItemResponse item = toFeedItem(feed, resources);
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
        Map<String, BuddyCircle> circlesById = selectCirclesByIds(members.stream()
                .map(BuddyCircleMember::getCircleId)
                .collect(Collectors.toList()));
        Map<String, UserAccount> ownersById = selectUsersByIds(members.stream()
                .map(BuddyCircleMember::getCircleId)
                .map(circlesById::get)
                .filter(Objects::nonNull)
                .map(BuddyCircle::getOwnerUserId)
                .collect(Collectors.toList()));
        List<BuddyCircleSummaryResponse> result = new ArrayList<>();
        for (BuddyCircleMember member : members) {
            BuddyCircle circle = circlesById.get(member.getCircleId());
            if (circle != null) {
                result.add(buildCircleListSummary(circle, ownersById));
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
        List<BuddyCircleMemberResponse> members = getCircleMembers(circleId);
        List<DishSummaryResponse> sharedMenus = getCircleMenus(circleId);
        response.setCircle(buildCircleDetailSummary(circle, members.size(), sharedMenus.size()));
        BuddyCircleStatsResponse stats = new BuddyCircleStatsResponse();
        stats.setMemberCount(members.size());
        stats.setSharedMenuCount(sharedMenus.size());
        response.setStats(stats);
        response.setMembers(members);
        response.setSharedMenus(sharedMenus);
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
        List<String> memberUserIds = members.stream()
                .map(BuddyCircleMember::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // 批量查询圈内成员 VIP 状态，避免为每个头像单独访问会员表。
        Set<String> activeVipUserIds = memberUserIds.isEmpty()
                ? Collections.emptySet()
                : userVipMapper.selectList(new QueryWrapper<UserVip>()
                        .in("user_id", memberUserIds)
                        .eq("is_vip", true))
                .stream()
                .filter(vipService::isVipActive)
                .map(UserVip::getUserId)
                .collect(Collectors.toSet());
        List<BuddyCircleMemberResponse> result = new ArrayList<>();
        Map<String, UserAccount> usersById = selectUsersByIds(memberUserIds);
        Map<String, Long> sharedMenuCountByUserId = countVisibleMenusByOwnerForCircle(circleId, usersById.keySet());
        for (BuddyCircleMember member : members) {
            UserAccount user = usersById.get(member.getUserId());
            if (user == null) {
                continue;
            }
            BuddyCircleMemberResponse item = new BuddyCircleMemberResponse();
            item.setId(user.getId());
            item.setAccount(user.getAccount());
            item.setNickname(user.getNickname());
            item.setAvatar(user.getAvatar());
            item.setRole(member.getRole());
            item.setVip(activeVipUserIds.contains(user.getId()));
            item.setSharedMenuCount(sharedMenuCountByUserId.getOrDefault(user.getId(), 0L));
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
        List<DishSummaryResponse> result = loadMemberMenus(memberIds).stream()
                .filter(dish -> isVisibleInCircleContext(dish, currentUserId, circleId))
                .collect(Collectors.toList());
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



    private Map<String, Long> countVisibleMenusByOwnerForCircle(String circleId, Collection<String> ownerUserIds) {
        List<String> normalizedOwnerUserIds = normalizeIds(ownerUserIds);
        if (normalizedOwnerUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 与 selectVisibleByOwnerUserIdAndCircleId 保持一致：只统计公开菜和明确授权给当前圈子的菜。
        return loadMemberMenus(normalizedOwnerUserIds).stream()
                .filter(dish -> VisibilityUtils.VISIBILITY_PUBLIC.equals(dish.getVisibility())
                        || dish.getVisibilityCircleIds().contains(circleId))
                .collect(Collectors.groupingBy(DishSummaryResponse::getOwnerUserId, Collectors.counting()));
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
        Map<String, UserAccount> usersById = selectUsersByIds(List.of(request.getRequesterUserId(), request.getTargetUserId()));
        return toFriendRequestItem(
                request,
                usersById.get(request.getRequesterUserId()),
                usersById.get(request.getTargetUserId()));
    }

    private List<FriendRequestItemResponse> toFriendRequestItems(List<FriendRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> userIds = new ArrayList<>();
        for (FriendRequest request : requests) {
            userIds.add(request.getRequesterUserId());
            userIds.add(request.getTargetUserId());
        }
        Map<String, UserAccount> usersById = selectUsersByIds(userIds);
        return requests.stream()
                .map(request -> toFriendRequestItem(
                        request,
                        usersById.get(request.getRequesterUserId()),
                        usersById.get(request.getTargetUserId())))
                .collect(Collectors.toList());
    }

    private FriendRequestItemResponse toFriendRequestItem(FriendRequest request, UserAccount requester, UserAccount target) {
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

    private boolean canSeeFeed(ActivityFeed feed, String viewerUserId, FeedVisibilityContext context) {
        if (Objects.equals(feed.getActorUserId(), viewerUserId)) {
            return true;
        }
        if ("public".equals(feed.getVisibilityScope())) {
            return context.viewerInAnyCircle();
        }
        if ("friends".equals(feed.getVisibilityScope())) {
            return context.friendUserIds().contains(feed.getActorUserId());
        }
        if ("circle".equals(feed.getVisibilityScope()) && feed.getCircleId() != null) {
            return context.viewerCircleIds().contains(feed.getCircleId());
        }
        return false;
    }

    private FeedVisibilityContext loadFeedVisibilityContext(List<ActivityFeed> feeds, String viewerUserId) {
        boolean needsAnyCircleCheck = feeds.stream()
                .anyMatch(feed -> !Objects.equals(feed.getActorUserId(), viewerUserId)
                        && "public".equals(feed.getVisibilityScope()));
        boolean needsCircleMembership = feeds.stream()
                .anyMatch(feed -> !Objects.equals(feed.getActorUserId(), viewerUserId)
                        && "circle".equals(feed.getVisibilityScope())
                        && feed.getCircleId() != null);
        boolean needsFriendCheck = feeds.stream()
                .anyMatch(feed -> !Objects.equals(feed.getActorUserId(), viewerUserId)
                        && "friends".equals(feed.getVisibilityScope()));
        if (!needsAnyCircleCheck && !needsCircleMembership && !needsFriendCheck) {
            return new FeedVisibilityContext(false, Collections.emptySet(), Collections.emptySet());
        }

        // 动态流先一次性取出当前用户所在圈，避免每条 circle 动态都 selectCount。
        List<BuddyCircleMember> memberships = needsAnyCircleCheck || needsCircleMembership
                ? buddyCircleMemberMapper.selectList(new QueryWrapper<BuddyCircleMember>().eq("user_id", viewerUserId))
                : Collections.emptyList();
        Set<String> viewerCircleIds = (memberships == null ? Collections.<BuddyCircleMember>emptyList() : memberships)
                .stream()
                .map(BuddyCircleMember::getCircleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean viewerInAnyCircle = !viewerCircleIds.isEmpty();
        Set<String> friendUserIds = needsFriendCheck
                ? loadFriendUserIds(viewerUserId)
                : Collections.emptySet();
        return new FeedVisibilityContext(viewerInAnyCircle, viewerCircleIds, friendUserIds);
    }

    private Set<String> loadFriendUserIds(String viewerUserId) {
        List<FriendRelation> relations = friendRelationMapper.selectList(new QueryWrapper<FriendRelation>()
                .eq("user_id", viewerUserId));
        return (relations == null ? Collections.<FriendRelation>emptyList() : relations)
                .stream()
                .map(FriendRelation::getFriendUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private FeedResourceLookup loadFeedResources(List<ActivityFeed> feeds) {
        if (feeds.isEmpty()) {
            return new FeedResourceLookup(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        }

        Map<String, UserAccount> actorsById = selectUsersByIds(feeds.stream()
                .map(ActivityFeed::getActorUserId)
                .collect(Collectors.toList()));

        // 批量缓存动态流需要的菜谱和圈子信息，避免每条动态重复扫表。
        Set<String> dishIds = feeds.stream()
                .map(ActivityFeed::getDishId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, DishSummaryResponse> dishesById;
        if (dishIds.isEmpty()) {
            dishesById = Collections.emptyMap();
        } else {
            List<DishSummaryResponse> dishes = dishMapper.selectAllActive().stream()
                    .filter(item -> dishIds.contains(item.getId()))
                    .collect(Collectors.toList());
            menuVisibilitySupport.hydrateSummaries(dishes);
            dishesById = dishes.stream()
                    .collect(Collectors.toMap(DishSummaryResponse::getId, item -> item, (left, right) -> left));
        }

        Set<String> circleIds = feeds.stream()
                .map(ActivityFeed::getCircleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, BuddyCircle> circlesById = selectCirclesByIds(circleIds);
        return new FeedResourceLookup(actorsById, dishesById, circlesById);
    }

    private FeedItemResponse toFeedItem(ActivityFeed feed, FeedResourceLookup resources) {
        UserAccount actor = resources.usersById().get(feed.getActorUserId());
        DishSummaryResponse dish = feed.getDishId() == null ? null : resources.dishesById().get(feed.getDishId());
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
            BuddyCircle circle = resources.circlesById().get(feed.getCircleId());
            if (circle != null) {
                item.setCircleId(circle.getId());
                item.setCircleName(circle.getName());
            }
        }
        return item;
    }

    private Map<String, UserAccount> selectUsersByIds(Collection<String> userIds) {
        List<String> normalized = normalizeIds(userIds);
        if (normalized.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserAccount> users = userAccountMapper.selectBatchIds(normalized);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        return users.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UserAccount::getId, item -> item, (left, right) -> left));
    }

    private Map<String, BuddyCircle> selectCirclesByIds(Collection<String> circleIds) {
        List<String> normalized = normalizeIds(circleIds);
        if (normalized.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BuddyCircle> circles = buddyCircleMapper.selectBatchIds(normalized);
        if (circles == null || circles.isEmpty()) {
            return Collections.emptyMap();
        }
        return circles.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(BuddyCircle::getId, item -> item, (left, right) -> left));
    }

    private List<String> normalizeIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.toList());
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

    private BuddyCircleSummaryResponse buildCircleListSummary(BuddyCircle circle) {
        return buildCircleListSummary(circle, Collections.emptyMap());
    }

    private BuddyCircleSummaryResponse buildCircleListSummary(BuddyCircle circle, Map<String, UserAccount> ownersById) {
        BuddyCircleSummaryResponse summary = new BuddyCircleSummaryResponse();
        summary.setId(circle.getId());
        summary.setName(circle.getName());
        summary.setDescription(circle.getDescription());
        summary.setOwnerUserId(circle.getOwnerUserId());
        UserAccount owner = ownersById.get(circle.getOwnerUserId());
        if (owner == null) {
            owner = userAccountMapper.selectById(circle.getOwnerUserId());
        }
        summary.setOwnerNickname(owner == null ? "" : owner.getNickname());
        return summary;
    }

    private BuddyCircleSummaryResponse buildCircleDetailSummary(BuddyCircle circle, long memberCount, long sharedMenuCount) {
        BuddyCircleSummaryResponse summary = new BuddyCircleSummaryResponse();
        summary.setId(circle.getId());
        summary.setName(circle.getName());
        summary.setDescription(circle.getDescription());
        summary.setOwnerUserId(circle.getOwnerUserId());
        UserAccount owner = userAccountMapper.selectById(circle.getOwnerUserId());
        summary.setOwnerNickname(owner == null ? "" : owner.getNickname());
        summary.setMemberCount(memberCount);
        summary.setSharedMenuCount(sharedMenuCount);
        return summary;
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

    private List<DishSummaryResponse> loadMemberMenus(Collection<String> memberIds) {
        List<String> normalizedMemberIds = normalizeIds(memberIds);
        if (normalizedMemberIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<DishSummaryResponse> dishes = dishMapper.selectAllActive().stream()
                .filter(dish -> normalizedMemberIds.contains(dish.getOwnerUserId()))
                .collect(Collectors.toList());
        menuVisibilitySupport.hydrateSummaries(dishes);
        return dishes;
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
        return summary;
    }

    private boolean isCircleMember(String circleId, String userId) {
        return buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                .eq("circle_id", circleId)
                .eq("user_id", userId)) > 0;
    }

    private long countCircleActiveMenus(String circleId) {
        List<String> memberIds = buddyCircleMemberMapper.selectList(new QueryWrapper<BuddyCircleMember>().eq("circle_id", circleId))
                .stream()
                .map(BuddyCircleMember::getUserId)
                .collect(Collectors.toList());
        return loadMemberMenus(memberIds).stream()
                .filter(dish -> isSharedWithCircle(dish, circleId))
                .count();
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
            settings.setShowKnowledgeOnHome(true);
            settings.setShowPetOnHome(true);
            userProfileSettingsMapper.insert(settings);
        }
        return settings;
    }

    private boolean enabledByDefault(Boolean value) {
        return value == null || value;
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

    private record FeedResourceLookup(
            Map<String, UserAccount> usersById,
            Map<String, DishSummaryResponse> dishesById,
            Map<String, BuddyCircle> circlesById) {
    }

    private record FeedVisibilityContext(
            boolean viewerInAnyCircle,
            Set<String> viewerCircleIds,
            Set<String> friendUserIds) {
    }
}
