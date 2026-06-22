package com.panghu.food.service;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.FeedbackSubmitRequest;
import com.panghu.food.entity.UserFeedback;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserFeedbackMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FeedbackServiceImpl implements FeedbackService {
    private static final int CONTENT_MAX_LENGTH = 500;

    private final UserFeedbackMapper userFeedbackMapper;
    private final NotificationService notificationService;

    public FeedbackServiceImpl(UserFeedbackMapper userFeedbackMapper,
                               NotificationService notificationService) {
        this.userFeedbackMapper = userFeedbackMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void submitFeedback(FeedbackSubmitRequest request) {
        String userId = AuthContext.requireUserId();
        String content = request == null || request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写反馈内容");
        }
        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "反馈内容不能超过500字");
        }

        LocalDateTime now = LocalDateTime.now();
        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setContent(content);
        feedback.setCreatedAt(now);
        feedback.setUpdatedAt(now);
        userFeedbackMapper.insert(feedback);
    }
}
