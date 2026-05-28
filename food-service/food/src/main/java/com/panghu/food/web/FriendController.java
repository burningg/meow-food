package com.panghu.food.web;

import com.panghu.food.dto.FriendItemResponse;
import com.panghu.food.dto.FriendRequestActionRequest;
import com.panghu.food.dto.FriendRequestItemResponse;
import com.panghu.food.dto.FriendRequestsResponse;
import com.panghu.food.service.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private final SocialService socialService;

    public FriendController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping
    public ResponseEntity<List<FriendItemResponse>> getFriends() {
        return ResponseEntity.ok(socialService.getFriends());
    }

    @PostMapping("/requests")
    public ResponseEntity<FriendRequestItemResponse> createRequest(@RequestBody FriendRequestActionRequest request) {
        return ResponseEntity.ok(socialService.createFriendRequest(request));
    }

    @GetMapping("/requests")
    public ResponseEntity<FriendRequestsResponse> getRequests() {
        return ResponseEntity.ok(socialService.getFriendRequests());
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<FriendRequestItemResponse> accept(@PathVariable Long requestId) {
        return ResponseEntity.ok(socialService.acceptFriendRequest(requestId));
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<FriendRequestItemResponse> reject(@PathVariable Long requestId) {
        return ResponseEntity.ok(socialService.rejectFriendRequest(requestId));
    }
}
