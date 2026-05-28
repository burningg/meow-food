import { http } from './http'
import type { AuthUser, MenuVisibility } from './auth-service'
import type { DishSummary } from './food-service'

export interface ProfileStats {
  friendCount: number
  menuCount: number
  circleCount: number
}

export interface FriendItem {
  id: number
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
  id: number
  requesterUserId: number
  targetUserId: number
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

export interface FeedItem {
  id: number
  actorUserId: number
  actorNickname: string
  actorAvatar: string
  activityType: 'dish_created' | 'dish_updated' | 'circle_shared'
  actionText: string
  visibilityScope: 'public' | 'friends' | 'circle'
  circleId?: number
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
  description: string
  accessibleCount: number
  privateCount: number
  accessRules: AccessRule[]
  menus: DishSummary[]
}

export interface BuddyCircleSummary {
  id: number
  name: string
  description: string
  ownerUserId: number
  ownerNickname: string
  memberCount: number
  sharedMenuCount: number
  weeklyUpdateCount: number
}

export interface BuddyCircleMember {
  id: number
  account: string
  nickname: string
  avatar: string
  role: 'owner' | 'member'
  sharedMenuCount: number
}

export interface BuddyCircleStats {
  memberCount: number
  sharedMenuCount: number
  weeklyUpdateCount: number
}

export interface BuddyCircleDetail {
  circle: BuddyCircleSummary
  stats: BuddyCircleStats
  members: BuddyCircleMember[]
  sharedMenus: DishSummary[]
}

export interface ProfileResponse {
  user: AuthUser
  stats: ProfileStats
  friendPreview: FriendItem[]
  feedPreview: FeedItem[]
  defaultMenuVisibility: Exclude<MenuVisibility, 'inherit'>
}

export class SocialService {
  getProfile() {
    return http.get<ProfileResponse>('/api/profile')
  }

  updateVisibility(defaultMenuVisibility: Exclude<MenuVisibility, 'inherit'>) {
    return http.put<AuthUser>('/api/profile/visibility', { defaultMenuVisibility })
  }

  getFriends() {
    return http.get<FriendItem[]>('/api/friends')
  }

  createFriendRequest(payload: { targetUserId?: number; targetAccount?: string; message?: string }) {
    return http.post<FriendRequestItem>('/api/friends/requests', payload)
  }

  getFriendRequests() {
    return http.get<FriendRequestsResponse>('/api/friends/requests')
  }

  acceptFriendRequest(requestId: number) {
    return http.post<FriendRequestItem>(`/api/friends/requests/${requestId}/accept`)
  }

  rejectFriendRequest(requestId: number) {
    return http.post<FriendRequestItem>(`/api/friends/requests/${requestId}/reject`)
  }

  getFeed(filter: 'all' | 'new' | 'circle' = 'all') {
    return http.get<FeedItem[]>('/api/feed', { params: { filter } })
  }

  getAccessibleMenus() {
    return http.get<{ menus: DishSummary[] }>('/api/feed/accessible-menus')
  }

  getUserMenuAccess(userId: number) {
    return http.get<UserMenuAccessResponse>(`/api/users/${userId}/menu-access`)
  }

  getUserMenus(userId: number) {
    return http.get<DishSummary[]>(`/api/users/${userId}/menus`)
  }

  getCircles() {
    return http.get<BuddyCircleSummary[]>('/api/circles')
  }

  createCircle(payload: { name: string; description: string; initialMemberIds?: number[] }) {
    return http.post<BuddyCircleDetail>('/api/circles', payload)
  }

  getCircleDetail(circleId: number) {
    return http.get<BuddyCircleDetail>(`/api/circles/${circleId}`)
  }

  inviteToCircle(circleId: number, payload: { inviteeUserId?: number; inviteeAccount?: string }) {
    return http.post<BuddyCircleDetail>(`/api/circles/${circleId}/invite`, payload)
  }
}
