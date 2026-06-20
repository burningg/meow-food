package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.NotificationBootstrapResponse;
import com.panghu.food.dto.NotificationListResponse;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.entity.UserNotification;
import com.panghu.food.entity.UserNotificationBroadcastRead;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserAccountMapper;
import com.panghu.food.mapper.UserNotificationBroadcastReadMapper;
import com.panghu.food.mapper.UserNotificationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {
    private final UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
    private final UserNotificationMapper userNotificationMapper = mock(UserNotificationMapper.class);
    private final UserNotificationBroadcastReadMapper userNotificationBroadcastReadMapper = mock(UserNotificationBroadcastReadMapper.class);
    private final NotificationServiceImpl notificationService = new NotificationServiceImpl(
            userAccountMapper,
            userNotificationMapper,
            userNotificationBroadcastReadMapper);

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void bootstrapReturnsLatestUnreadImportantNotification() {
        AuthContext.setUserId("user-1");
        mockCurrentUser("user-1", LocalDateTime.of(2026, 6, 1, 9, 0));
        mockCurrentUserNotifications(
                List.of(),
                List.of(
                        notification("n-1", null, "broadcast", "important", null, LocalDateTime.of(2026, 6, 6, 9, 0)),
                        notification("n-2", null, "broadcast", "important", null, LocalDateTime.of(2026, 6, 6, 18, 0)),
                        notification("n-3", "user-1", "direct", "normal", null, LocalDateTime.of(2026, 6, 6, 19, 0))),
                List.of());

        NotificationBootstrapResponse response = notificationService.getBootstrap();

        assertThat(response.getHasUnread()).isTrue();
        assertThat(response.getImportantNotification()).isNotNull();
        assertThat(response.getImportantNotification().getId()).isEqualTo("n-2");
    }

    @Test
    void getNotificationsSortsUnreadFirstThenPublishedAtDesc() {
        AuthContext.setUserId("user-1");
        mockCurrentUser("user-1", LocalDateTime.of(2026, 6, 1, 9, 0));
        mockCurrentUserNotifications(
                List.of(
                        notification("read-late", "user-1", "direct", "normal", LocalDateTime.of(2026, 6, 6, 20, 0), LocalDateTime.of(2026, 6, 6, 20, 0)),
                        notification("unread-old", "user-1", "direct", "normal", null, LocalDateTime.of(2026, 6, 5, 20, 0))),
                List.of(
                        notification("unread-new", null, "broadcast", "important", null, LocalDateTime.of(2026, 6, 6, 21, 0)),
                        notification("read-old", null, "broadcast", "normal", null, LocalDateTime.of(2026, 6, 5, 21, 0))),
                List.of(broadcastRead("read-old", "user-1", LocalDateTime.of(2026, 6, 5, 21, 30))));

        NotificationListResponse response = notificationService.getNotifications();

        assertThat(response.getHasUnread()).isTrue();
        assertThat(response.getItems()).extracting("id")
                .containsExactly("unread-new", "unread-old", "read-late", "read-old");
    }

    @Test
    void getNotificationsHidesBroadcastPublishedBeforeUserRegistered() {
        AuthContext.setUserId("user-1");
        mockCurrentUser("user-1", LocalDateTime.of(2026, 6, 7, 9, 0));
        mockCurrentUserNotifications(
                List.of(notification("direct-1", "user-1", "direct", "normal", null, LocalDateTime.of(2026, 6, 6, 10, 0))),
                List.of(
                        notification("broadcast-all", null, "broadcast", "normal", null, LocalDateTime.of(2026, 6, 6, 12, 0)),
                        notification("broadcast-new", null, "broadcast", "normal", null, LocalDateTime.of(2026, 6, 7, 10, 0)),
                        notification("broadcast-existing", null, "broadcast", "important", null, LocalDateTime.of(2026, 6, 6, 11, 0), "existing_users_only", LocalDateTime.of(2026, 6, 6, 11, 0))),
                List.of());

        NotificationListResponse response = notificationService.getNotifications();

        assertThat(response.getItems()).extracting("id")
                .containsExactly("broadcast-new", "direct-1");
    }

    @Test
    void bootstrapSkipsInvisibleExistingUsersOnlyImportantNotification() {
        AuthContext.setUserId("user-1");
        mockCurrentUser("user-1", LocalDateTime.of(2026, 6, 7, 9, 0));
        mockCurrentUserNotifications(
                List.of(),
                List.of(
                        notification("important-hidden", null, "broadcast", "important", null, LocalDateTime.of(2026, 6, 6, 18, 0), "existing_users_only", LocalDateTime.of(2026, 6, 6, 18, 0)),
                        notification("important-visible", null, "broadcast", "important", null, LocalDateTime.of(2026, 6, 7, 10, 0))),
                List.of());

        NotificationBootstrapResponse response = notificationService.getBootstrap();

        assertThat(response.getImportantNotification()).isNotNull();
        assertThat(response.getImportantNotification().getId()).isEqualTo("important-visible");
    }

    @Test
    void markReadIsIdempotentForAlreadyReadNotification() {
        AuthContext.setUserId("user-1");
        mockCurrentUser("user-1", LocalDateTime.of(2026, 6, 1, 9, 0));
        UserNotification notification = notification("n-1", "user-1", "direct", "normal", LocalDateTime.of(2026, 6, 6, 18, 30), LocalDateTime.of(2026, 6, 6, 18, 0));
        when(userNotificationMapper.selectById("n-1")).thenReturn(notification);

        notificationService.markRead("n-1");

        verify(userNotificationMapper, never()).updateById(any(UserNotification.class));
    }

    @Test
    void markReadRejectsNotificationOwnedByAnotherUser() {
        AuthContext.setUserId("user-1");
        mockCurrentUser("user-1", LocalDateTime.of(2026, 6, 1, 9, 0));
        when(userNotificationMapper.selectById("n-1")).thenReturn(notification("n-1", "user-2", "direct", "normal", null, LocalDateTime.of(2026, 6, 6, 18, 0)));

        assertThatThrownBy(() -> notificationService.markRead("n-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("通知不存在");
    }

    @Test
    void markReadCreatesBroadcastReadRecord() {
        AuthContext.setUserId("user-1");
        mockCurrentUser("user-1", LocalDateTime.of(2026, 6, 1, 9, 0));
        when(userNotificationMapper.selectById("n-1")).thenReturn(notification("n-1", null, "broadcast", "important", null, LocalDateTime.of(2026, 6, 6, 18, 0)));
        when(userNotificationBroadcastReadMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        notificationService.markRead("n-1");

        verify(userNotificationBroadcastReadMapper).insert(any(UserNotificationBroadcastRead.class));
        verify(userNotificationMapper, never()).updateById(any(UserNotification.class));
    }

    @Test
    void markReadRejectsExistingUsersOnlyBroadcastForNewUsers() {
        AuthContext.setUserId("user-1");
        mockCurrentUser("user-1", LocalDateTime.of(2026, 6, 7, 9, 0));
        when(userNotificationMapper.selectById("n-1")).thenReturn(
                notification("n-1", null, "broadcast", "important", null, LocalDateTime.of(2026, 6, 6, 18, 0), "existing_users_only", LocalDateTime.of(2026, 6, 6, 18, 0)));

        assertThatThrownBy(() -> notificationService.markRead("n-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("通知不存在");
    }

    @Test
    void markReadRejectsBroadcastPublishedBeforeUserRegistered() {
        AuthContext.setUserId("user-1");
        mockCurrentUser("user-1", LocalDateTime.of(2026, 6, 7, 9, 0));
        when(userNotificationMapper.selectById("n-1")).thenReturn(
                notification("n-1", null, "broadcast", "important", null, LocalDateTime.of(2026, 6, 6, 18, 0)));

        assertThatThrownBy(() -> notificationService.markRead("n-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("通知不存在");
    }

    @Test
    void sendVipOpenedSuccessNotificationInsertsDirectNotification() {
        notificationService.sendVipOpenedSuccessNotification("user-1");

        ArgumentCaptor<UserNotification> notificationCaptor = ArgumentCaptor.forClass(UserNotification.class);
        verify(userNotificationMapper).insert(notificationCaptor.capture());
        UserNotification notification = notificationCaptor.getValue();
        assertThat(notification.getUserId()).isEqualTo("user-1");
        assertThat(notification.getTitle()).isEqualTo("感谢你成为 meoi 食堂 VIP");
        assertThat(notification.getSummary()).isEqualTo("感谢你成为 meoi 食堂 VIP");
        assertThat(notification.getBody()).contains("你的支持不仅帮助我们把这个社区持续做下去");
        assertThat(notification.getAudienceType()).isEqualTo("direct");
        assertThat(notification.getPriority()).isEqualTo("normal");
        assertThat(notification.getRecipientScope()).isEqualTo("existing_users_only");
        assertThat(notification.getPublishedAt()).isNotNull();
        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getUpdatedAt()).isNotNull();
        assertThat(notification.getReadAt()).isNull();
    }

    private UserNotification notification(String id,
                                          String userId,
                                          String audienceType,
                                          String priority,
                                          LocalDateTime readAt,
                                          LocalDateTime publishedAt) {
        return notification(id, userId, audienceType, priority, readAt, publishedAt, "all_users", null);
    }

    private UserNotification notification(String id,
                                          String userId,
                                          String audienceType,
                                          String priority,
                                          LocalDateTime readAt,
                                          LocalDateTime publishedAt,
                                          String recipientScope,
                                          LocalDateTime recipientCutoffAt) {
        UserNotification notification = new UserNotification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setTitle(id);
        notification.setSummary(id + "-summary");
        notification.setBody(id + "-body");
        notification.setAudienceType(audienceType);
        notification.setPriority(priority);
        notification.setRecipientScope(recipientScope);
        notification.setRecipientCutoffAt(recipientCutoffAt);
        notification.setReadAt(readAt);
        notification.setPublishedAt(publishedAt);
        notification.setCreatedAt(publishedAt);
        return notification;
    }

    private void mockCurrentUser(String userId, LocalDateTime createdAt) {
        when(userAccountMapper.selectById(userId)).thenReturn(user(userId, createdAt));
    }

    private UserAccount user(String id, LocalDateTime createdAt) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setCreatedAt(createdAt);
        return user;
    }

    private UserNotificationBroadcastRead broadcastRead(String notificationId,
                                                        String userId,
                                                        LocalDateTime readAt) {
        UserNotificationBroadcastRead read = new UserNotificationBroadcastRead();
        read.setNotificationId(notificationId);
        read.setUserId(userId);
        read.setReadAt(readAt);
        return read;
    }

    private void mockCurrentUserNotifications(List<UserNotification> directNotifications,
                                              List<UserNotification> broadcastNotifications,
                                              List<UserNotificationBroadcastRead> broadcastReads) {
        when(userNotificationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(directNotifications)
                .thenReturn(broadcastNotifications);
        when(userNotificationBroadcastReadMapper.selectList(any(QueryWrapper.class))).thenReturn(broadcastReads);
    }
}
