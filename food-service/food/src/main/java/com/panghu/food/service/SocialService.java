package com.panghu.food.service;

import com.panghu.food.dto.*;

import java.util.List;

public interface SocialService {
    ProfileResponse getProfile();

    AuthUserResponse updateProfile(ProfileUpdateRequest request);

    AuthUserResponse updateDefaultVisibility(ProfileVisibilityUpdateRequest request);

    void updateLastSelectedCircle(ProfileLastSelectedCircleUpdateRequest request);

    List<FriendItemResponse> getFriends();

    FriendRequestItemResponse createFriendRequest(FriendRequestActionRequest request);

    FriendRequestsResponse getFriendRequests();

    FriendInvitationResponse getFriendInvitation(String inviterUserId);

    FriendInvitationResponse acceptFriendInvitation(String inviterUserId);

    FriendInvitationResponse rejectFriendInvitation(String inviterUserId);

    FriendRequestItemResponse acceptFriendRequest(String requestId);

    FriendRequestItemResponse rejectFriendRequest(String requestId);

    List<FeedItemResponse> getFeed(String filter);

    FeedAccessibleMenusResponse getAccessibleMenus();

    UserMenuAccessResponse getUserMenuAccess(String targetUserId);

    List<DishSummaryResponse> getVisibleMenusByUser(String targetUserId);

    List<BuddyCircleSummaryResponse> getCircles();

    BuddyCircleDetailResponse createCircle(BuddyCircleCreateRequest request);

    BuddyCircleDetailResponse getCircleDetail(String circleId);

    BuddyCircleShareInvitationResponse getCircleShareInvitation(String circleId, String inviterUserId);

    List<BuddyCircleMemberResponse> getCircleMembers(String circleId);

    List<DishSummaryResponse> getCircleMenus(String circleId);

    BuddyCircleDetailResponse inviteToCircle(String circleId, BuddyCircleInviteRequest request);

    BuddyCircleDetailResponse acceptCircleShareInvitation(String circleId, String inviterUserId);
}
