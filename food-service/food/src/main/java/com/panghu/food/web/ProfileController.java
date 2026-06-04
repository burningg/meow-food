package com.panghu.food.web;

import com.panghu.food.dto.AuthUserResponse;
import com.panghu.food.dto.ProfileResponse;
import com.panghu.food.dto.ProfileLastSelectedCircleUpdateRequest;
import com.panghu.food.dto.ProfileUpdateRequest;
import com.panghu.food.dto.ProfileVisibilityUpdateRequest;
import com.panghu.food.service.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final SocialService socialService;

    public ProfileController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(socialService.getProfile());
    }

    @PutMapping
    public ResponseEntity<AuthUserResponse> updateProfile(@RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(socialService.updateProfile(request));
    }

    @PutMapping("/visibility")
    public ResponseEntity<AuthUserResponse> updateVisibility(@RequestBody ProfileVisibilityUpdateRequest request) {
        return ResponseEntity.ok(socialService.updateDefaultVisibility(request));
    }

    @PutMapping("/last-selected-circle")
    public ResponseEntity<Void> updateLastSelectedCircle(@RequestBody ProfileLastSelectedCircleUpdateRequest request) {
        socialService.updateLastSelectedCircle(request);
        return ResponseEntity.noContent().build();
    }
}
