package com.panghu.food.web;

import com.panghu.food.dto.CookingTimerCreateRequest;
import com.panghu.food.dto.CookingTimerCreateResponse;
import com.panghu.food.service.CookingTimerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cooking-timers")
public class CookingTimerController {
    private final CookingTimerService cookingTimerService;

    public CookingTimerController(CookingTimerService cookingTimerService) {
        this.cookingTimerService = cookingTimerService;
    }

    @PostMapping
    public ResponseEntity<CookingTimerCreateResponse> createTimer(@RequestBody CookingTimerCreateRequest request) {
        return ResponseEntity.ok(cookingTimerService.createTimer(request));
    }

    @PostMapping("/{timerId}/cancel")
    public ResponseEntity<Void> cancelTimer(@PathVariable String timerId) {
        cookingTimerService.cancelTimer(timerId);
        return ResponseEntity.noContent().build();
    }
}
