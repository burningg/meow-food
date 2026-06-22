package com.panghu.food.service;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.FeedbackSubmitRequest;
import com.panghu.food.entity.UserFeedback;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserFeedbackMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FeedbackServiceImplTest {
    private final UserFeedbackMapper userFeedbackMapper = mock(UserFeedbackMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final FeedbackServiceImpl feedbackService = new FeedbackServiceImpl(userFeedbackMapper, notificationService);

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void submitFeedbackRejectsBlankContent() {
        AuthContext.setUserId("user-1");
        FeedbackSubmitRequest request = new FeedbackSubmitRequest();
        request.setContent("   ");

        assertThatThrownBy(() -> feedbackService.submitFeedback(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("请填写反馈内容");

        verify(userFeedbackMapper, never()).insert(any(UserFeedback.class));
        verify(notificationService, never()).sendFeedbackReceivedNotification(any());
    }

    @Test
    void submitFeedbackRejectsContentLongerThanMaxLength() {
        AuthContext.setUserId("user-1");
        FeedbackSubmitRequest request = new FeedbackSubmitRequest();
        request.setContent("好".repeat(501));

        assertThatThrownBy(() -> feedbackService.submitFeedback(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("反馈内容不能超过500字");

        verify(userFeedbackMapper, never()).insert(any(UserFeedback.class));
        verify(notificationService, never()).sendFeedbackReceivedNotification(any());
    }
}
