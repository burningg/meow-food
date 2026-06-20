package com.panghu.food.service;

import com.panghu.food.dto.FeedbackSubmitRequest;

public interface FeedbackService {
    void submitFeedback(FeedbackSubmitRequest request);
}
