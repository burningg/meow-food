package com.panghu.food.web;

import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.dto.UserMenuAccessResponse;
import com.panghu.food.service.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserSocialController {
    private final SocialService socialService;

    public UserSocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping("/{userId}/menu-access")
    public ResponseEntity<UserMenuAccessResponse> getMenuAccess(@PathVariable Long userId) {
        return ResponseEntity.ok(socialService.getUserMenuAccess(userId));
    }

    @GetMapping("/{userId}/menus")
    public ResponseEntity<List<DishSummaryResponse>> getMenus(@PathVariable Long userId) {
        return ResponseEntity.ok(socialService.getVisibleMenusByUser(userId));
    }
}
