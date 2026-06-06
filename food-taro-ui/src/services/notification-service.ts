import { http } from './http'

export type NotificationAudienceType = 'broadcast' | 'direct'
export type NotificationPriority = 'normal' | 'important'

export interface NotificationItem {
  id: string
  title: string
  summary: string
  body: string
  audienceType: NotificationAudienceType
  priority: NotificationPriority
  read: boolean
  publishedAt: string
}

export interface NotificationBootstrapResponse {
  hasUnread: boolean
  importantNotification: NotificationItem | null
}

export interface NotificationListResponse {
  hasUnread: boolean
  items: NotificationItem[]
}

export class NotificationService {
  getBootstrap() {
    return http.get<NotificationBootstrapResponse>('/api/notifications/bootstrap')
  }

  getNotifications() {
    return http.get<NotificationListResponse>('/api/notifications')
  }

  markRead(notificationId: string) {
    return http.post<void>(`/api/notifications/${notificationId}/read`)
  }
}
