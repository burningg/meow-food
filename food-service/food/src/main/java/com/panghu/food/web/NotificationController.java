package com.panghu.food.web;

import com.panghu.food.dto.NotificationBootstrapResponse;
import com.panghu.food.dto.NotificationListResponse;
import com.panghu.food.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<NotificationBootstrapResponse> getBootstrap() {
        return ResponseEntity.ok(notificationService.getBootstrap());
    }

    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications() {
        return ResponseEntity.ok(notificationService.getNotifications());
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable String notificationId) {
        notificationService.markRead(notificationId);
        return ResponseEntity.noContent().build();
    }
}
