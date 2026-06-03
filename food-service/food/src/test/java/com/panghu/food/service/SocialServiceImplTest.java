package com.panghu.food.service;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.dto.FeedItemResponse;
import com.panghu.food.dto.FriendInvitationResponse;
import com.panghu.food.entity.ActivityFeed;
import com.panghu.food.entity.FriendRequest;
import com.panghu.food.entity.FriendRelation;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.entity.UserProfileSettings;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.ActivityFeedMapper;
import com.panghu.food.mapper.BuddyCircleInviteMapper;
import com.panghu.food.mapper.BuddyCircleMapper;
import com.panghu.food.mapper.BuddyCircleMemberMapper;
import com.panghu.food.mapper.DishMapper;
import com.panghu.food.mapper.FriendRelationMapper;
import com.panghu.food.mapper.FriendRequestMapper;
import com.panghu.food.mapper.UserAccountMapper;
import com.panghu.food.mapper.UserProfileSettingsMapper;
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
import static org.mockito.Mockito.when;

class SocialServiceImplTest {
    private final UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
    private final UserProfileSettingsMapper userProfileSettingsMapper = mock(UserProfileSettingsMapper.class);
    private final FriendRequestMapper friendRequestMapper = mock(FriendRequestMapper.class);
    private final FriendRelationMapper friendRelationMapper = mock(FriendRelationMapper.class);
    private final DishMapper dishMapper = mock(DishMapper.class);
    private final ActivityFeedMapper activityFeedMapper = mock(ActivityFeedMapper.class);
    private final BuddyCircleMapper buddyCircleMapper = mock(BuddyCircleMapper.class);
    private final BuddyCircleMemberMapper buddyCircleMemberMapper = mock(BuddyCircleMemberMapper.class);
    private final BuddyCircleInviteMapper buddyCircleInviteMapper = mock(BuddyCircleInviteMapper.class);
    private final AuthService authService = mock(AuthService.class);

    private final SocialServiceImpl socialService = new SocialServiceImpl(
            userAccountMapper,
            userProfileSettingsMapper,
            friendRequestMapper,
            friendRelationMapper,
            dishMapper,
            activityFeedMapper,
            buddyCircleMapper,
            buddyCircleMemberMapper,
            buddyCircleInviteMapper,
            authService);

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void getFeedExcludesPublicFeedFromNonFriends() {
        AuthContext.setUserId("viewer");
        ActivityFeed feed = publicFeed("feed-1", "stranger", "dish-1");

        when(activityFeedMapper.selectList(any())).thenReturn(List.of(feed));
        when(friendRelationMapper.selectCount(any())).thenReturn(0L);

        List<FeedItemResponse> result = socialService.getFeed("all");

        assertThat(result).isEmpty();
    }

    @Test
    void getFeedIncludesPublicFeedFromFriends() {
        AuthContext.setUserId("viewer");
        ActivityFeed feed = publicFeed("feed-1", "friend", "dish-1");

        when(activityFeedMapper.selectList(any())).thenReturn(List.of(feed));
        when(friendRelationMapper.selectCount(any())).thenReturn(1L);
        when(userAccountMapper.selectById("friend")).thenReturn(user("friend"));
        when(dishMapper.selectAllActive()).thenReturn(List.of(dish("dish-1", "friend")));
        when(userProfileSettingsMapper.selectById("friend")).thenReturn(settings("friend"));

        List<FeedItemResponse> result = socialService.getFeed("all");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActorUserId()).isEqualTo("friend");
        assertThat(result.get(0).getDishId()).isEqualTo("dish-1");
    }

    @Test
    void acceptFriendInvitationCreatesAcceptedRequestAndRelations() {
        AuthContext.setUserId("target");
        when(userAccountMapper.selectById("inviter")).thenReturn(user("inviter"));
        when(userProfileSettingsMapper.selectById("inviter")).thenReturn(settings("inviter"));
        when(friendRequestMapper.selectOne(any())).thenReturn(null);
        when(friendRelationMapper.selectCount(any())).thenReturn(0L);

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
        when(friendRelationMapper.selectCount(any())).thenReturn(0L);

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
        when(friendRelationMapper.selectCount(any())).thenReturn(1L);

        FriendInvitationResponse result = socialService.acceptFriendInvitation("inviter");

        assertThat(result.getStatus()).isEqualTo("already_friend");
        verify(friendRequestMapper, never()).insert(any(FriendRequest.class));
        verify(friendRelationMapper, never()).insert(any(FriendRelation.class));
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

    private UserAccount user(String id) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setNickname("好友");
        return user;
    }

    private DishSummaryResponse dish(String id, String ownerUserId) {
        DishSummaryResponse dish = new DishSummaryResponse();
        dish.setId(id);
        dish.setOwnerUserId(ownerUserId);
        dish.setName("番茄炒蛋");
        dish.setVisibility("public");
        return dish;
    }

    private UserProfileSettings settings(String userId) {
        UserProfileSettings settings = new UserProfileSettings();
        settings.setUserId(userId);
        settings.setDefaultMenuVisibility("friends");
        return settings;
    }
}
