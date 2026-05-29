package com.panghu.food.web;

import com.panghu.food.dto.*;
import com.panghu.food.service.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/circles")
public class BuddyCircleController {
    private final SocialService socialService;

    public BuddyCircleController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping
    public ResponseEntity<List<BuddyCircleSummaryResponse>> getCircles() {
        return ResponseEntity.ok(socialService.getCircles());
    }

    @PostMapping
    public ResponseEntity<BuddyCircleDetailResponse> createCircle(@RequestBody BuddyCircleCreateRequest request) {
        return ResponseEntity.ok(socialService.createCircle(request));
    }

    @GetMapping("/{circleId}")
    public ResponseEntity<BuddyCircleDetailResponse> getCircleDetail(@PathVariable String circleId) {
        return ResponseEntity.ok(socialService.getCircleDetail(circleId));
    }

    @PostMapping("/{circleId}/invite")
    public ResponseEntity<BuddyCircleDetailResponse> invite(@PathVariable String circleId,
                                                            @RequestBody BuddyCircleInviteRequest request) {
        return ResponseEntity.ok(socialService.inviteToCircle(circleId, request));
    }

    @GetMapping("/{circleId}/members")
    public ResponseEntity<List<BuddyCircleMemberResponse>> getMembers(@PathVariable String circleId) {
        return ResponseEntity.ok(socialService.getCircleMembers(circleId));
    }

    @GetMapping("/{circleId}/menus")
    public ResponseEntity<List<DishSummaryResponse>> getMenus(@PathVariable String circleId) {
        return ResponseEntity.ok(socialService.getCircleMenus(circleId));
    }
}
