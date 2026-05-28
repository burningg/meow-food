package com.panghu.food.service;

import com.panghu.food.dto.*;

import java.util.List;

public interface SocialService {
    ProfileResponse getProfile();

    AuthUserResponse updateDefaultVisibility(ProfileVisibilityUpdateRequest request);

    List<FriendItemResponse> getFriends();

    FriendRequestItemResponse createFriendRequest(FriendRequestActionRequest request);

    FriendRequestsResponse getFriendRequests();

    FriendRequestItemResponse acceptFriendRequest(Long requestId);

    FriendRequestItemResponse rejectFriendRequest(Long requestId);

    List<FeedItemResponse> getFeed(String filter);

    FeedAccessibleMenusResponse getAccessibleMenus();

    UserMenuAccessResponse getUserMenuAccess(Long targetUserId);

    List<DishSummaryResponse> getVisibleMenusByUser(Long targetUserId);

    List<BuddyCircleSummaryResponse> getCircles();

    BuddyCircleDetailResponse createCircle(BuddyCircleCreateRequest request);

    BuddyCircleDetailResponse getCircleDetail(Long circleId);

    List<BuddyCircleMemberResponse> getCircleMembers(Long circleId);

    List<DishSummaryResponse> getCircleMenus(Long circleId);

    BuddyCircleDetailResponse inviteToCircle(Long circleId, BuddyCircleInviteRequest request);
}
