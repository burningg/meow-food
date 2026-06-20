package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.NotificationBootstrapResponse;
import com.panghu.food.dto.NotificationItemResponse;
import com.panghu.food.dto.NotificationListResponse;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.entity.UserNotification;
import com.panghu.food.entity.UserNotificationBroadcastRead;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserAccountMapper;
import com.panghu.food.mapper.UserNotificationBroadcastReadMapper;
import com.panghu.food.mapper.UserNotificationMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final String AUDIENCE_BROADCAST = "broadcast";
    private static final String AUDIENCE_DIRECT = "direct";
    private static final String PRIORITY_NORMAL = "normal";
    private static final String PRIORITY_IMPORTANT = "important";
    private static final String RECIPIENT_SCOPE_ALL_USERS = "all_users";
    private static final String RECIPIENT_SCOPE_EXISTING_USERS_ONLY = "existing_users_only";
    private static final String VIP_OPENED_TITLE = "感谢你成为 meoi 食堂 VIP";
    private static final String VIP_OPENED_BODY = "感谢你成为 meoi 食堂 VIP。你的支持不仅帮助我们把这个社区持续做下去，也让更多认真记录三餐、分享菜谱的人被看见。成为会员后，你将解锁更大的圈子与菜谱容量、菜谱的AI加持 、专属头像框和尊贵标识等权益。希望这份会员礼遇，能陪你更轻松地收藏灵感、记录好味道，也和更多同好一起把食堂经营得更热闹。";

    private final UserAccountMapper userAccountMapper;
    private final UserNotificationMapper userNotificationMapper;
    private final UserNotificationBroadcastReadMapper userNotificationBroadcastReadMapper;

    public NotificationServiceImpl(UserAccountMapper userAccountMapper,
                                   UserNotificationMapper userNotificationMapper,
                                   UserNotificationBroadcastReadMapper userNotificationBroadcastReadMapper) {
        this.userAccountMapper = userAccountMapper;
        this.userNotificationMapper = userNotificationMapper;
        this.userNotificationBroadcastReadMapper = userNotificationBroadcastReadMapper;
    }

    @Override
    public NotificationBootstrapResponse getBootstrap() {
        NotificationView view = getCurrentUserNotificationView();
        NotificationBootstrapResponse response = new NotificationBootstrapResponse();
        response.setHasUnread(view.notifications.stream().anyMatch(notification -> isUnread(notification, view.broadcastReadMap)));
        response.setImportantNotification(view.notifications.stream()
                .filter(notification -> isUnread(notification, view.broadcastReadMap))
                .filter(notification -> PRIORITY_IMPORTANT.equals(notification.getPriority()))
                .max(notificationTimeComparator())
                .map(notification -> toNotificationItem(notification, view.broadcastReadMap))
                .orElse(null));
        return response;
    }

    @Override
    public NotificationListResponse getNotifications() {
        NotificationView view = getCurrentUserNotificationView();
        List<UserNotification> notifications = view.notifications.stream()
                .sorted(notificationListComparator(view.broadcastReadMap))
                .collect(Collectors.toList());
        NotificationListResponse response = new NotificationListResponse();
        response.setHasUnread(notifications.stream().anyMatch(notification -> isUnread(notification, view.broadcastReadMap)));
        response.setItems(notifications.stream()
                .map(notification -> toNotificationItem(notification, view.broadcastReadMap))
                .collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional
    public void markRead(String notificationId) {
        String userId = AuthContext.requireUserId();
        UserAccount currentUser = requireCurrentUser(userId);
        UserNotification notification = userNotificationMapper.selectById(notificationId);
        if (notification == null || !canAccess(notification, currentUser)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "通知不存在");
        }
        if (isBroadcast(notification)) {
            UserNotificationBroadcastRead existing = userNotificationBroadcastReadMapper.selectOne(
                    new QueryWrapper<UserNotificationBroadcastRead>()
                            .eq("notification_id", notificationId)
                            .eq("user_id", userId)
                            .last("LIMIT 1"));
            if (existing != null) {
                return;
            }
            UserNotificationBroadcastRead read = new UserNotificationBroadcastRead();
            read.setNotificationId(notificationId);
            read.setUserId(userId);
            read.setReadAt(LocalDateTime.now());
            userNotificationBroadcastReadMapper.insert(read);
            return;
        }
        if (notification.getReadAt() != null) {
            return;
        }
        notification.setReadAt(LocalDateTime.now());
        userNotificationMapper.updateById(notification);
    }

    @Override
    public void sendVipOpenedSuccessNotification(String userId) {
        LocalDateTime now = LocalDateTime.now();
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setTitle(VIP_OPENED_TITLE);
        notification.setSummary(VIP_OPENED_TITLE);
        notification.setBody(VIP_OPENED_BODY);
        notification.setAudienceType(AUDIENCE_DIRECT);
        notification.setPriority(PRIORITY_NORMAL);
        notification.setRecipientScope(RECIPIENT_SCOPE_EXISTING_USERS_ONLY);
        notification.setPublishedAt(now);
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        userNotificationMapper.insert(notification);
    }

    private NotificationView getCurrentUserNotificationView() {
        String userId = AuthContext.requireUserId();
        UserAccount currentUser = requireCurrentUser(userId);
        List<UserNotification> directNotifications = userNotificationMapper.selectList(new QueryWrapper<UserNotification>()
                .eq("user_id", userId));
        QueryWrapper<UserNotification> broadcastQuery = new QueryWrapper<UserNotification>()
                .eq("audience_type", AUDIENCE_BROADCAST);
        if (currentUser.getCreatedAt() != null) {
            broadcastQuery.ge("published_at", currentUser.getCreatedAt());
        }
        List<UserNotification> broadcastNotifications = userNotificationMapper.selectList(broadcastQuery).stream()
                .filter(notification -> canAccessBroadcast(notification, currentUser))
                .collect(Collectors.toList());

        List<UserNotification> notifications = new ArrayList<>(directNotifications);
        notifications.addAll(broadcastNotifications);

        Set<String> broadcastIds = broadcastNotifications.stream().map(UserNotification::getId).collect(Collectors.toCollection(HashSet::new));
        Map<String, UserNotificationBroadcastRead> broadcastReadMap = broadcastIds.isEmpty()
                ? Collections.emptyMap()
                : userNotificationBroadcastReadMapper.selectList(new QueryWrapper<UserNotificationBroadcastRead>()
                        .eq("user_id", userId)
                        .in("notification_id", broadcastIds))
                .stream()
                .collect(Collectors.toMap(UserNotificationBroadcastRead::getNotificationId, read -> read, (left, right) -> left, HashMap::new));
        return new NotificationView(notifications, broadcastReadMap);
    }

    private NotificationItemResponse toNotificationItem(UserNotification notification,
                                                        Map<String, UserNotificationBroadcastRead> broadcastReadMap) {
        NotificationItemResponse item = new NotificationItemResponse();
        item.setId(notification.getId());
        item.setTitle(notification.getTitle());
        item.setSummary(notification.getSummary() == null || notification.getSummary().trim().isEmpty()
                ? notification.getBody()
                : notification.getSummary().trim());
        item.setBody(notification.getBody());
        item.setAudienceType(notification.getAudienceType());
        item.setPriority(notification.getPriority() == null ? PRIORITY_NORMAL : notification.getPriority());
        item.setRead(!isUnread(notification, broadcastReadMap));
        item.setPublishedAt(notification.getPublishedAt());
        return item;
    }

    private boolean isUnread(UserNotification notification,
                             Map<String, UserNotificationBroadcastRead> broadcastReadMap) {
        if (isBroadcast(notification)) {
            return !broadcastReadMap.containsKey(notification.getId());
        }
        return notification.getReadAt() == null;
    }

    private Comparator<UserNotification> notificationListComparator(Map<String, UserNotificationBroadcastRead> broadcastReadMap) {
        return Comparator.comparing((UserNotification notification) -> isRead(notification, broadcastReadMap))
                .thenComparing(notificationTimeComparator().reversed());
    }

    private Comparator<UserNotification> notificationTimeComparator() {
        return Comparator.comparing(
                        UserNotification::getPublishedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                        UserNotification::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private boolean isRead(UserNotification notification,
                           Map<String, UserNotificationBroadcastRead> broadcastReadMap) {
        return !isUnread(notification, broadcastReadMap);
    }

    private boolean canAccess(UserNotification notification, UserAccount currentUser) {
        return isBroadcast(notification)
                ? canAccessBroadcast(notification, currentUser)
                : Objects.equals(notification.getUserId(), currentUser.getId());
    }

    private boolean canAccessBroadcast(UserNotification notification, UserAccount currentUser) {
        // 全局通知只面向发布时已存在的用户，避免新注册用户看到历史弹窗。
        if (currentUser.getCreatedAt() != null
                && notification.getPublishedAt() != null
                && currentUser.getCreatedAt().isAfter(notification.getPublishedAt())) {
            return false;
        }
        String recipientScope = notification.getRecipientScope();
        if (recipientScope == null || RECIPIENT_SCOPE_ALL_USERS.equals(recipientScope)) {
            return true;
        }
        if (!RECIPIENT_SCOPE_EXISTING_USERS_ONLY.equals(recipientScope)) {
            return false;
        }
        return currentUser.getCreatedAt() != null
                && notification.getRecipientCutoffAt() != null
                && !currentUser.getCreatedAt().isAfter(notification.getRecipientCutoffAt());
    }

    private UserAccount requireCurrentUser(String userId) {
        UserAccount currentUser = userAccountMapper.selectById(userId);
        if (currentUser == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        return currentUser;
    }

    private boolean isBroadcast(UserNotification notification) {
        return AUDIENCE_BROADCAST.equals(notification.getAudienceType());
    }

    private static class NotificationView {
        private final List<UserNotification> notifications;
        private final Map<String, UserNotificationBroadcastRead> broadcastReadMap;

        private NotificationView(List<UserNotification> notifications,
                                 Map<String, UserNotificationBroadcastRead> broadcastReadMap) {
            this.notifications = notifications;
            this.broadcastReadMap = broadcastReadMap;
        }
    }
}
