import { http } from './http'
import type { AuthUser, MenuVisibility } from './auth-service'
import type { DishSummary } from './food-service'

export interface ProfileStats {
  friendCount: number
  menuCount: number
  circleCount: number
}

export interface VipInfo {
  vip: boolean
  vipLevel?: string
  openedAt?: string
  expiresAt?: string
  openAmount?: number
  dailyRecipeAnalysisLimit: number
  dailyRecipeAnalysisUsed: number
  dailyRecipeAnalysisRemaining: number
}

export interface VipPaymentOrderResponse {
  outTradeNo: string
  amountFen: number
  planName: string
  timeStamp: string
  nonceStr: string
  payPackage: string
  signType: 'RSA'
  paySign: string
}

export interface VipPaymentOrderStatus {
  outTradeNo: string
  amountFen: number
  planName: string
  status: 'PENDING' | 'PAID' | 'FAILED' | 'CLOSED'
  paidAt?: string
  vipInfo: VipInfo
}

export interface FriendItem {
  id: string
  account: string
  nickname: string
  avatar: string
  bio: string
  friend: boolean
  visibleMenuCount: number
  sharedMenuCount: number
  memberInCircle: boolean
}

export interface FriendRequestItem {
  id: string
  requesterUserId: string
  targetUserId: string
  requesterNickname: string
  requesterAvatar: string
  targetNickname: string
  targetAvatar: string
  message: string
  status: 'pending' | 'accepted' | 'rejected' | 'cancelled'
  createdAt: string
}

export interface FriendRequestsResponse {
  incoming: FriendRequestItem[]
  outgoing: FriendRequestItem[]
}

export interface FriendInvitationResponse {
  inviter: AuthUser
  status: 'pending' | 'accepted' | 'rejected' | 'cancelled' | 'already_friend'
  request?: FriendRequestItem
}

export interface FeedItem {
  id: string
  actorUserId: string
  actorNickname: string
  actorAvatar: string
  activityType: 'dish_created' | 'dish_updated' | 'circle_shared'
  actionText: string
  visibilityScope: 'public' | 'friends' | 'circle'
  circleId?: string
  circleName?: string
  dishId: string
  dishName: string
  dishImage: string
  dishDescription: string
  createdAt: string
}

export interface AccessRule {
  label: string
  description: string
  state: string
}

export interface UserMenuAccessResponse {
  user: AuthUser
  friend: boolean
  sameCircle: boolean
  actionType: 'friend-request' | 'invite-circle'
  accessibleCount: number
  privateCount: number
  accessRules: AccessRule[]
  menus: DishSummary[]
}

export interface BuddyCircleSummary {
  id: string
  name: string
  description: string
  ownerUserId: string
  ownerNickname: string
  memberCount: number
  sharedMenuCount: number
}

export interface BuddyCircleMember {
  id: string
  account: string
  nickname: string
  avatar: string
  role: 'owner' | 'member'
  vip: boolean
  sharedMenuCount: number
}

export interface BuddyCircleStats {
  memberCount: number
  sharedMenuCount: number
}

export interface BuddyCircleDetail {
  circle: BuddyCircleSummary
  stats: BuddyCircleStats
  members: BuddyCircleMember[]
  sharedMenus: DishSummary[]
}

export interface BuddyCircleShareInvitation {
  inviter: AuthUser
  circle: BuddyCircleSummary
  friend: boolean
  member: boolean
  status: 'need_friend_accept' | 'friend_ready' | 'already_member'
}

export interface ProfileResponse {
  user: AuthUser
  stats: ProfileStats
  friendPreview: FriendItem[]
  defaultMenuVisibility: Exclude<MenuVisibility, 'inherit'>
  defaultMenuCircleIds: string[]
  lastSelectedCircleId?: string
  vipInfo: VipInfo
}

export interface ProfileUpdatePayload {
  nickname: string
  bio: string
  avatar?: string
}

export class SocialService {
  getProfile() {
    return http.get<ProfileResponse>('/api/profile')
  }

  updateProfile(payload: ProfileUpdatePayload) {
    return http.put<AuthUser>('/api/profile', payload)
  }

  updateVisibility(defaultMenuVisibility: Exclude<MenuVisibility, 'inherit'>, defaultMenuCircleIds: string[] = []) {
    return http.put<AuthUser>('/api/profile/visibility', { defaultMenuVisibility, defaultMenuCircleIds })
  }

  updateLastSelectedCircle(lastSelectedCircleId?: string) {
    return http.put<void>('/api/profile/last-selected-circle', { lastSelectedCircleId: lastSelectedCircleId || null })
  }

  claimVipFreeTrial() {
    return http.post<VipInfo>('/api/vip/free-trial')
  }

  createVipOrder() {
    return http.post<VipPaymentOrderResponse>('/api/vip/orders')
  }

  getVipOrder(outTradeNo: string) {
    return http.get<VipPaymentOrderStatus>(`/api/vip/orders/${outTradeNo}`)
  }

  getFriends() {
    return http.get<FriendItem[]>('/api/friends')
  }

  createFriendRequest(payload: { targetUserId?: string; targetAccount?: string; message?: string }) {
    return http.post<FriendRequestItem>('/api/friends/requests', payload)
  }

  getFriendRequests() {
    return http.get<FriendRequestsResponse>('/api/friends/requests')
  }

  getFriendInvitation(inviterUserId: string) {
    return http.get<FriendInvitationResponse>(`/api/friends/invitations/${inviterUserId}`)
  }

  acceptFriendInvitation(inviterUserId: string) {
    return http.post<FriendInvitationResponse>(`/api/friends/invitations/${inviterUserId}/accept`)
  }

  rejectFriendInvitation(inviterUserId: string) {
    return http.post<FriendInvitationResponse>(`/api/friends/invitations/${inviterUserId}/reject`)
  }

  acceptFriendRequest(requestId: string) {
    return http.post<FriendRequestItem>(`/api/friends/requests/${requestId}/accept`)
  }

  rejectFriendRequest(requestId: string) {
    return http.post<FriendRequestItem>(`/api/friends/requests/${requestId}/reject`)
  }

  getFeed(filter: 'all' | 'new' | 'circle' = 'all') {
    return http.get<FeedItem[]>('/api/feed', { params: { filter } })
  }

  getAccessibleMenus() {
    return http.get<{ menus: DishSummary[] }>('/api/feed/accessible-menus')
  }

  getUserMenuAccess(userId: string, circleId?: string) {
    return http.get<UserMenuAccessResponse>(`/api/users/${userId}/menu-access`, {
      params: circleId ? { circleId } : undefined,
    })
  }

  getUserMenus(userId: string) {
    return http.get<DishSummary[]>(`/api/users/${userId}/menus`)
  }

  getCircles() {
    return http.get<BuddyCircleSummary[]>('/api/circles')
  }

  createCircle(payload: { name: string; description: string; initialMemberIds?: string[] }) {
    return http.post<BuddyCircleDetail>('/api/circles', payload)
  }

  getCircleDetail(circleId: string) {
    return http.get<BuddyCircleDetail>(`/api/circles/${circleId}`)
  }

  getCircleShareInvitation(circleId: string, inviterUserId: string) {
    return http.get<BuddyCircleShareInvitation>(`/api/circles/${circleId}/share-invitations/${inviterUserId}`)
  }

  acceptCircleShareInvitation(circleId: string, inviterUserId: string) {
    return http.post<BuddyCircleDetail>(`/api/circles/${circleId}/share-invitations/${inviterUserId}/accept`)
  }

  inviteToCircle(circleId: string, payload: { inviteeUserId?: string; inviteeAccount?: string }) {
    return http.post<BuddyCircleDetail>(`/api/circles/${circleId}/invite`, payload)
  }
}
