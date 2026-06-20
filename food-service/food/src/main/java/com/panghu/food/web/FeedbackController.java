package com.panghu.food.web;

import com.panghu.food.dto.FeedbackSubmitRequest;
import com.panghu.food.service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<Void> submitFeedback(@RequestBody(required = false) FeedbackSubmitRequest request) {
        feedbackService.submitFeedback(request);
        return ResponseEntity.noContent().build();
    }
}
