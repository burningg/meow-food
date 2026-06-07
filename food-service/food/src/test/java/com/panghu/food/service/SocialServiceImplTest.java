package com.panghu.food.service;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.BuddyCircleDetailResponse;
import com.panghu.food.dto.BuddyCircleCreateRequest;
import com.panghu.food.dto.BuddyCircleMemberResponse;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.dto.FeedItemResponse;
import com.panghu.food.dto.FriendInvitationResponse;
import com.panghu.food.dto.FriendItemResponse;
import com.panghu.food.dto.UserMenuAccessResponse;
import com.panghu.food.entity.ActivityFeed;
import com.panghu.food.entity.BuddyCircle;
import com.panghu.food.entity.BuddyCircleMember;
import com.panghu.food.entity.DishIngredient;
import com.panghu.food.entity.FriendRequest;
import com.panghu.food.entity.FriendRelation;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.entity.UserProfileSettings;
import com.panghu.food.entity.UserVip;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.ActivityFeedMapper;
import com.panghu.food.mapper.BuddyCircleInviteMapper;
import com.panghu.food.mapper.BuddyCircleMapper;
import com.panghu.food.mapper.BuddyCircleMemberMapper;
import com.panghu.food.mapper.DishIngredientMapper;
import com.panghu.food.mapper.DishMapper;
import com.panghu.food.mapper.FriendRelationMapper;
import com.panghu.food.mapper.FriendRequestMapper;
import com.panghu.food.mapper.UserAccountMapper;
import com.panghu.food.mapper.UserDefaultVisibilityCircleMapper;
import com.panghu.food.mapper.UserProfileSettingsMapper;
import com.panghu.food.mapper.UserVipMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SocialServiceImplTest {
    private final UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
    private final UserProfileSettingsMapper userProfileSettingsMapper = mock(UserProfileSettingsMapper.class);
    private final FriendRequestMapper friendRequestMapper = mock(FriendRequestMapper.class);
    private final FriendRelationMapper friendRelationMapper = mock(FriendRelationMapper.class);
    private final DishMapper dishMapper = mock(DishMapper.class);
    private final DishIngredientMapper dishIngredientMapper = mock(DishIngredientMapper.class);
    private final ActivityFeedMapper activityFeedMapper = mock(ActivityFeedMapper.class);
    private final BuddyCircleMapper buddyCircleMapper = mock(BuddyCircleMapper.class);
    private final BuddyCircleMemberMapper buddyCircleMemberMapper = mock(BuddyCircleMemberMapper.class);
    private final BuddyCircleInviteMapper buddyCircleInviteMapper = mock(BuddyCircleInviteMapper.class);
    private final UserDefaultVisibilityCircleMapper userDefaultVisibilityCircleMapper = mock(UserDefaultVisibilityCircleMapper.class);
    private final UserVipMapper userVipMapper = mock(UserVipMapper.class);
    private final AuthService authService = mock(AuthService.class);
    private final VipService vipService = mock(VipService.class);
    private final MenuVisibilitySupport menuVisibilitySupport = mock(MenuVisibilitySupport.class);

    private final SocialServiceImpl socialService = new SocialServiceImpl(
            userAccountMapper,
            userProfileSettingsMapper,
            friendRequestMapper,
            friendRelationMapper,
            dishMapper,
            dishIngredientMapper,
            activityFeedMapper,
            buddyCircleMapper,
            buddyCircleMemberMapper,
            buddyCircleInviteMapper,
            authService,
            vipService,
            userVipMapper,
            userDefaultVisibilityCircleMapper,
            menuVisibilitySupport);

    SocialServiceImplTest() {
        when(vipService.getCircleLimit(any())).thenReturn(3);
        when(userVipMapper.selectList(any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void getFeedExcludesPublicFeedFromUsersOutsideCircles() {
        AuthContext.setUserId("viewer");
        ActivityFeed feed = publicFeed("feed-1", "stranger", "dish-1");

        when(activityFeedMapper.selectList(any())).thenReturn(List.of(feed));
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of());

        List<FeedItemResponse> result = socialService.getFeed("all");

        assertThat(result).isEmpty();
    }

    @Test
    void getFeedIncludesPublicFeedForCircleMembers() {
        AuthContext.setUserId("viewer");
        ActivityFeed feed = publicFeed("feed-1", "friend", "dish-1");

        when(activityFeedMapper.selectList(any())).thenReturn(List.of(feed));
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(circleMember("circle-1", "viewer")));
        when(userAccountMapper.selectBatchIds(List.of("friend"))).thenReturn(List.of(user("friend")));
        when(dishMapper.selectAllActive()).thenReturn(List.of(dish("dish-1", "friend")));

        List<FeedItemResponse> result = socialService.getFeed("all");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActorUserId()).isEqualTo("friend");
        assertThat(result.get(0).getDishId()).isEqualTo("dish-1");
    }

    @Test
    void getFeedLoadsSharedResourcesOnceForVisibleFeeds() {
        AuthContext.setUserId("viewer");
        ActivityFeed first = publicFeed("feed-1", "friend", "dish-1");
        ActivityFeed second = publicFeed("feed-2", "friend", "dish-1");

        when(activityFeedMapper.selectList(any())).thenReturn(List.of(first, second));
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(circleMember("circle-1", "viewer")));
        when(userAccountMapper.selectBatchIds(List.of("friend"))).thenReturn(List.of(user("friend")));
        when(dishMapper.selectAllActive()).thenReturn(List.of(dish("dish-1", "friend")));

        List<FeedItemResponse> result = socialService.getFeed("all");

        assertThat(result).hasSize(2);
        verify(userAccountMapper).selectBatchIds(List.of("friend"));
        verify(dishMapper).selectAllActive();
    }

    @Test
    void getFeedBatchesCircleMembershipChecksForCircleFeeds() {
        AuthContext.setUserId("viewer");
        ActivityFeed first = circleFeed("feed-1", "owner-1", "dish-1", "circle-1");
        ActivityFeed second = circleFeed("feed-2", "owner-2", "dish-2", "circle-2");

        when(activityFeedMapper.selectList(any())).thenReturn(List.of(first, second));
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(circleMember("circle-1", "viewer")));
        when(userAccountMapper.selectBatchIds(List.of("owner-1"))).thenReturn(List.of(user("owner-1")));
        when(dishMapper.selectAllActive()).thenReturn(List.of(
                dish("dish-1", "owner-1"),
                dish("dish-2", "owner-2")));
        when(buddyCircleMapper.selectBatchIds(List.of("circle-1"))).thenReturn(List.of(circle("circle-1")));

        List<FeedItemResponse> result = socialService.getFeed("circle");

        assertThat(result).extracting(FeedItemResponse::getId).containsExactly("feed-1");
        verify(buddyCircleMemberMapper).selectList(any());
        verify(buddyCircleMemberMapper, never()).selectCount(any());
    }

    @Test
    void getFeedBatchesFriendChecksForFriendFeeds() {
        AuthContext.setUserId("viewer");
        ActivityFeed first = friendFeed("feed-1", "friend-1", "dish-1");
        ActivityFeed second = friendFeed("feed-2", "stranger", "dish-2");

        when(activityFeedMapper.selectList(any())).thenReturn(List.of(first, second));
        when(friendRelationMapper.selectList(any())).thenReturn(List.of(friendRelation("viewer", "friend-1")));
        when(userAccountMapper.selectBatchIds(List.of("friend-1"))).thenReturn(List.of(user("friend-1")));
        when(dishMapper.selectAllActive()).thenReturn(List.of(
                dish("dish-1", "friend-1"),
                dish("dish-2", "stranger")));

        List<FeedItemResponse> result = socialService.getFeed("new");

        assertThat(result).extracting(FeedItemResponse::getId).containsExactly("feed-1");
        verify(friendRelationMapper).selectList(any());
        verify(friendRelationMapper, never()).selectCount(any());
    }

    @Test
    void getFriendsKeepsRelationOrderAndSkipsMissingUsers() {
        AuthContext.setUserId("viewer");
        when(friendRelationMapper.selectList(any())).thenReturn(List.of(
                friendRelation("viewer", "friend-1"),
                friendRelation("viewer", "missing"),
                friendRelation("viewer", "friend-2")));
        when(userAccountMapper.selectBatchIds(List.of("friend-1", "missing", "friend-2")))
                .thenReturn(List.of(user("friend-1"), user("friend-2")));

        List<FriendItemResponse> result = socialService.getFriends();

        assertThat(result).extracting(FriendItemResponse::getId).containsExactly("friend-1", "friend-2");
        assertThat(result).allMatch(FriendItemResponse::isFriend);
    }

    @Test
    void getCirclesBatchesCircleAndOwnerLookups() {
        AuthContext.setUserId("viewer");
        BuddyCircle first = circle("circle-1", "owner-1");
        BuddyCircle second = circle("circle-2", "owner-2");
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(
                circleMember("circle-1", "viewer"),
                circleMember("circle-2", "viewer")));
        when(buddyCircleMapper.selectBatchIds(List.of("circle-1", "circle-2"))).thenReturn(List.of(first, second));
        when(userAccountMapper.selectBatchIds(List.of("owner-1", "owner-2")))
                .thenReturn(List.of(user("owner-1"), user("owner-2")));

        List<?> result = socialService.getCircles();

        assertThat(result).hasSize(2);
        verify(buddyCircleMapper).selectBatchIds(List.of("circle-1", "circle-2"));
        verify(userAccountMapper).selectBatchIds(List.of("owner-1", "owner-2"));
        verify(userAccountMapper, never()).selectById(any());
    }


    @Test
    void acceptFriendInvitationCreatesAcceptedRequestAndRelations() {
        AuthContext.setUserId("target");
        when(userAccountMapper.selectById("inviter")).thenReturn(user("inviter"));
        when(userProfileSettingsMapper.selectById("inviter")).thenReturn(settings("inviter"));
        when(friendRequestMapper.selectOne(any())).thenReturn(null);
        when(menuVisibilitySupport.isFriend(any(), any())).thenReturn(false, false, false);

        FriendInvitationResponse result = socialService.acceptFriendInvitation("inviter");

        assertThat(result.getStatus()).isEqualTo("accepted");
        assertThat(result.getInviter().getId()).isEqualTo("inviter");
        verify(friendRequestMapper).insert(any(FriendRequest.class));
        verify(friendRequestMapper).updateById(any(FriendRequest.class));
        verify(friendRelationMapper, times(2)).insert(any(FriendRelation.class));
    }

    @Test
    void rejectFriendInvitationCreatesRejectedRequestWithoutRelations() {
        AuthContext.setUserId("target");
        when(userAccountMapper.selectById("inviter")).thenReturn(user("inviter"));
        when(userProfileSettingsMapper.selectById("inviter")).thenReturn(settings("inviter"));
        when(friendRequestMapper.selectOne(any())).thenReturn(null);
        when(menuVisibilitySupport.isFriend(any(), any())).thenReturn(false, false);

        FriendInvitationResponse result = socialService.rejectFriendInvitation("inviter");

        assertThat(result.getStatus()).isEqualTo("rejected");
        verify(friendRequestMapper).insert(any(FriendRequest.class));
        verify(friendRequestMapper).updateById(any(FriendRequest.class));
        verify(friendRelationMapper, never()).insert(any(FriendRelation.class));
    }

    @Test
    void acceptFriendInvitationReturnsAlreadyFriendWithoutCreatingRequest() {
        AuthContext.setUserId("target");
        when(userAccountMapper.selectById("inviter")).thenReturn(user("inviter"));
        when(userProfileSettingsMapper.selectById("inviter")).thenReturn(settings("inviter"));
        when(friendRequestMapper.selectOne(any())).thenReturn(null);
        when(menuVisibilitySupport.isFriend(any(), any())).thenReturn(true);

        FriendInvitationResponse result = socialService.acceptFriendInvitation("inviter");

        assertThat(result.getStatus()).isEqualTo("already_friend");
        verify(friendRequestMapper, never()).insert(any(FriendRequest.class));
        verify(friendRelationMapper, never()).insert(any(FriendRelation.class));
    }

    @Test
    void getUserMenuAccessWithoutCircleKeepsExistingVisibilityRules() {
        AuthContext.setUserId("viewer");
        when(userAccountMapper.selectById("owner-1")).thenReturn(user("owner-1"));
        when(menuVisibilitySupport.isFriend("viewer", "owner-1")).thenReturn(false);
        when(menuVisibilitySupport.hasSharedCircle("viewer", "owner-1")).thenReturn(true);
        when(menuVisibilitySupport.isUserInAnyCircle("viewer")).thenReturn(true);
        when(dishMapper.selectByOwnerUserId("owner-1")).thenReturn(List.of(
                dish("dish-public", "owner-1", LocalDateTime.of(2024, 1, 1, 12, 0), "public"),
                circleDish("dish-other-circle", "owner-1", LocalDateTime.of(2024, 1, 2, 12, 0), "circle", "circle-2")));
        mockHydrateSummaries();
        when(menuVisibilitySupport.canViewDish(any(DishSummaryResponse.class), org.mockito.Mockito.eq("viewer"), org.mockito.Mockito.eq(false)))
                .thenReturn(true, true);

        UserMenuAccessResponse result = socialService.getUserMenuAccess("owner-1", null);

        assertThat(result.getAccessibleCount()).isEqualTo(2);
        assertThat(result.getPrivateCount()).isEqualTo(0);
        assertThat(result.getMenus()).extracting(DishSummaryResponse::getId)
                .containsExactly("dish-public", "dish-other-circle");
    }

    @Test
    void getUserMenuAccessWithCircleOnlyReturnsMenusVisibleInCurrentCircle() {
        AuthContext.setUserId("viewer");
        when(userAccountMapper.selectById("owner-1")).thenReturn(user("owner-1"));
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(menuVisibilitySupport.isFriend("viewer", "owner-1")).thenReturn(false);
        when(menuVisibilitySupport.hasSharedCircle("viewer", "owner-1")).thenReturn(true);
        when(menuVisibilitySupport.isUserInAnyCircle("viewer")).thenReturn(true);
        when(dishMapper.selectByOwnerUserId("owner-1")).thenReturn(List.of(
                dish("dish-public", "owner-1", LocalDateTime.of(2024, 1, 1, 12, 0), "public"),
                circleDish("dish-current-circle", "owner-1", LocalDateTime.of(2024, 1, 2, 12, 0), "circle", "circle-1"),
                circleDish("dish-other-circle", "owner-1", LocalDateTime.of(2024, 1, 3, 12, 0), "circle", "circle-2")));
        mockHydrateSummaries();

        UserMenuAccessResponse result = socialService.getUserMenuAccess("owner-1", "circle-1");

        assertThat(result.getAccessibleCount()).isEqualTo(2);
        assertThat(result.getPrivateCount()).isEqualTo(1);
        assertThat(result.getMenus()).extracting(DishSummaryResponse::getId)
                .containsExactly("dish-public", "dish-current-circle");
    }

    @Test
    void getUserMenuAccessWithCircleRejectsViewerOutsideCircle() {
        AuthContext.setUserId("viewer");
        when(userAccountMapper.selectById("owner-1")).thenReturn(user("owner-1"));
        when(menuVisibilitySupport.isFriend("viewer", "owner-1")).thenReturn(false);
        when(menuVisibilitySupport.hasSharedCircle("viewer", "owner-1")).thenReturn(false);
        when(menuVisibilitySupport.isUserInAnyCircle("viewer")).thenReturn(true);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> socialService.getUserMenuAccess("owner-1", "circle-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("你还不在这个搭子圈里");
    }

    @Test
    void getFriendInvitationRejectsSelfInvite() {
        AuthContext.setUserId("target");

        assertThatThrownBy(() -> socialService.getFriendInvitation("target"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不能邀请自己成为好友");
    }

    @Test
    void getFriendInvitationRejectsMissingInviter() {
        AuthContext.setUserId("target");
        when(userAccountMapper.selectById("missing")).thenReturn(null);

        assertThatThrownBy(() -> socialService.getFriendInvitation("missing"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("邀请人不存在");
    }

    @Test
    void getCircleMenusSortsByCreatedAtDescAndIncludesIngredientNames() {
        AuthContext.setUserId("viewer");
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(circleMember("circle-1", "owner-1")));
        when(dishMapper.selectAllActive()).thenReturn(List.of(
                dish("dish-older", "owner-1", LocalDateTime.of(2024, 1, 1, 12, 0), "public"),
                circleDish("dish-newer", "owner-1", LocalDateTime.of(2024, 1, 2, 12, 0), "circle", "circle-1")));
        mockHydrateSummaries();
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of(
                ingredient("dish-newer", "土豆"),
                ingredient("dish-newer", "牛肉"),
                ingredient("dish-older", "番茄")));

        List<DishSummaryResponse> result = socialService.getCircleMenus("circle-1");

        assertThat(result).extracting(DishSummaryResponse::getId).containsExactly("dish-newer", "dish-older");
        assertThat(result.get(0).getIngredientNames()).containsExactly("土豆", "牛肉");
        assertThat(result.get(1).getIngredientNames()).containsExactly("番茄");
    }

    @Test
    void getCircleMenusKeepsInvisibleMenusFilteredWhenHydratingIngredients() {
        AuthContext.setUserId("viewer");
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(circleMember("circle-1", "owner-1")));
        when(dishMapper.selectAllActive()).thenReturn(List.of(
                dish("dish-public", "owner-1", LocalDateTime.of(2024, 1, 2, 12, 0), "public"),
                circleDish("dish-other-circle", "owner-1", LocalDateTime.of(2024, 1, 3, 12, 0), "circle", "circle-2")));
        mockHydrateSummaries();
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of(
                ingredient("dish-public", "鸡蛋"),
                ingredient("dish-other-circle", "秘密配方")));

        List<DishSummaryResponse> result = socialService.getCircleMenus("circle-1");

        assertThat(result).extracting(DishSummaryResponse::getId).containsExactly("dish-public");
        assertThat(result.get(0).getIngredientNames()).containsExactly("鸡蛋");
    }

    @Test
    void getCircleMenusHidesOwnersDishWhenCurrentCircleIsNotSelected() {
        AuthContext.setUserId("owner-1");
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(circleMember("circle-1", "owner-1")));
        when(dishMapper.selectAllActive()).thenReturn(List.of(
                circleDish("dish-current-circle", "owner-1", LocalDateTime.of(2024, 1, 2, 12, 0), "circle", "circle-1"),
                circleDish("dish-other-circle", "owner-1", LocalDateTime.of(2024, 1, 3, 12, 0), "circle", "circle-2")));
        mockHydrateSummaries();
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());

        List<DishSummaryResponse> result = socialService.getCircleMenus("circle-1");

        assertThat(result).extracting(DishSummaryResponse::getId).containsExactly("dish-current-circle");
    }

    @Test
    void getCircleMembersMarksActiveVipMembers() {
        AuthContext.setUserId("viewer");
        UserVip vip = activeVip("vip-user");
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(
                circleMember("circle-1", "vip-user"),
                circleMember("circle-1", "normal-user")));
        when(userAccountMapper.selectBatchIds(List.of("vip-user", "normal-user")))
                .thenReturn(List.of(user("vip-user"), user("normal-user")));
        when(userVipMapper.selectList(any())).thenReturn(List.of(vip));
        when(vipService.isVipActive(vip)).thenReturn(true);
        when(dishMapper.selectAllActive()).thenReturn(List.of(
                dish("dish-public", "vip-user", LocalDateTime.of(2024, 1, 1, 12, 0), "public"),
                circleDish("dish-circle", "vip-user", LocalDateTime.of(2024, 1, 2, 12, 0), "circle", "circle-1"),
                circleDish("dish-other-circle", "normal-user", LocalDateTime.of(2024, 1, 3, 12, 0), "circle", "circle-2")));
        mockHydrateSummaries();

        List<BuddyCircleMemberResponse> result = socialService.getCircleMembers("circle-1");

        assertThat(result).extracting(BuddyCircleMemberResponse::getId)
                .containsExactly("vip-user", "normal-user");
        assertThat(result.get(0).isVip()).isTrue();
        assertThat(result.get(1).isVip()).isFalse();
        assertThat(result.get(0).getSharedMenuCount()).isEqualTo(2);
        assertThat(result.get(1).getSharedMenuCount()).isZero();
        verify(dishMapper, never()).selectVisibleByOwnerUserIdAndCircleId(any(), any());
    }

    @Test
    void getCircleMembersSkipsUserRowsMissingFromBatchLookup() {
        AuthContext.setUserId("viewer");
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(circleMember("circle-1", "missing-user")));
        when(userAccountMapper.selectBatchIds(List.of("missing-user"))).thenReturn(List.of());

        List<BuddyCircleMemberResponse> result = socialService.getCircleMembers("circle-1");

        assertThat(result).isEmpty();
        verifyNoInteractions(dishMapper);
    }

    @Test
    void acceptCircleShareInvitationAddsMemberWithoutCreatingFriendship() {
        AuthContext.setUserId("target");
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(userAccountMapper.selectById("inviter")).thenReturn(user("inviter"));
        when(userAccountMapper.selectById("owner-1")).thenReturn(user("owner-1"));
        when(userAccountMapper.selectById("target")).thenReturn(user("target"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L, 0L, 1L, 2L, 1L, 1L, 1L, 1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(
                circleMember("circle-1", "owner-1"),
                circleMember("circle-1", "target")));
        when(activityFeedMapper.selectCount(any())).thenReturn(0L);
        when(dishMapper.selectAllActive()).thenReturn(List.of());
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());

        BuddyCircleDetailResponse result = socialService.acceptCircleShareInvitation("circle-1", "inviter");

        assertThat(result.getCircle().getId()).isEqualTo("circle-1");
        verify(buddyCircleMemberMapper).insert(any(BuddyCircleMember.class));
        verify(friendRequestMapper, never()).insert(any(FriendRequest.class));
        verify(friendRequestMapper, never()).updateById(any(FriendRequest.class));
        verify(friendRelationMapper, never()).insert(any(FriendRelation.class));
    }

    @Test
    void createCircleRejectsNormalUserAtCircleLimit() {
        AuthContext.setUserId("owner-1");
        BuddyCircleCreateRequest request = new BuddyCircleCreateRequest();
        request.setName("新圈子");
        request.setDescription("一起做饭");
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(3L);

        assertThatThrownBy(() -> socialService.createCircle(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("普通用户最多加入 3 个搭子圈");
        verify(buddyCircleMapper, never()).insert(any(BuddyCircle.class));
    }

    private ActivityFeed publicFeed(String id, String actorUserId, String dishId) {
        ActivityFeed feed = new ActivityFeed();
        feed.setId(id);
        feed.setActorUserId(actorUserId);
        feed.setDishId(dishId);
        feed.setActivityType("dish_created");
        feed.setVisibilityScope("public");
        feed.setCreatedAt(LocalDateTime.now());
        return feed;
    }

    private ActivityFeed friendFeed(String id, String actorUserId, String dishId) {
        ActivityFeed feed = publicFeed(id, actorUserId, dishId);
        feed.setVisibilityScope("friends");
        return feed;
    }

    private ActivityFeed circleFeed(String id, String actorUserId, String dishId, String circleId) {
        ActivityFeed feed = publicFeed(id, actorUserId, dishId);
        feed.setVisibilityScope("circle");
        feed.setCircleId(circleId);
        return feed;
    }

    private UserAccount user(String id) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setNickname("好友");
        return user;
    }

    private BuddyCircle circle(String id) {
        return circle(id, "owner-1");
    }

    private BuddyCircle circle(String id, String ownerUserId) {
        BuddyCircle circle = new BuddyCircle();
        circle.setId(id);
        circle.setOwnerUserId(ownerUserId);
        circle.setName("搭子圈");
        return circle;
    }

    private BuddyCircleMember circleMember(String circleId, String userId) {
        BuddyCircleMember member = new BuddyCircleMember();
        member.setCircleId(circleId);
        member.setUserId(userId);
        return member;
    }

    private FriendRelation friendRelation(String userId, String friendUserId) {
        FriendRelation relation = new FriendRelation();
        relation.setUserId(userId);
        relation.setFriendUserId(friendUserId);
        return relation;
    }

    private DishIngredient ingredient(String dishId, String name) {
        DishIngredient ingredient = new DishIngredient();
        ingredient.setDishId(dishId);
        ingredient.setName(name);
        return ingredient;
    }

    private DishSummaryResponse dish(String id, String ownerUserId) {
        DishSummaryResponse dish = new DishSummaryResponse();
        dish.setId(id);
        dish.setOwnerUserId(ownerUserId);
        dish.setName("番茄炒蛋");
        dish.setVisibility("public");
        return dish;
    }

    private DishSummaryResponse dish(String id, String ownerUserId, LocalDateTime createdAt, String visibility) {
        DishSummaryResponse dish = dish(id, ownerUserId);
        dish.setCreatedAt(createdAt);
        dish.setVisibility(visibility);
        return dish;
    }

    private DishSummaryResponse circleDish(String id, String ownerUserId, LocalDateTime createdAt, String visibility, String circleId) {
        DishSummaryResponse dish = dish(id, ownerUserId, createdAt, visibility);
        dish.setVisibilityCircleIds(List.of(circleId));
        dish.setEffectiveCircleIds(List.of(circleId));
        return dish;
    }

    private UserProfileSettings settings(String userId) {
        UserProfileSettings settings = new UserProfileSettings();
        settings.setUserId(userId);
        settings.setDefaultMenuVisibility("friends");
        return settings;
    }

    private UserVip activeVip(String userId) {
        UserVip vip = new UserVip();
        vip.setUserId(userId);
        vip.setIsVip(true);
        vip.setExpiresAt(LocalDateTime.now().plusDays(1));
        return vip;
    }

    private void mockHydrateSummaries() {
        org.mockito.Mockito.doAnswer(invocation -> {
            List<DishSummaryResponse> dishes = invocation.getArgument(0);
            for (DishSummaryResponse dish : dishes) {
                dish.setEffectiveVisibility(dish.getVisibility());
                if (!"circle".equals(dish.getVisibility())) {
                    dish.setEffectiveCircleIds(List.of());
                }
            }
            return null;
        }).when(menuVisibilitySupport).hydrateSummaries(any());
    }
}
