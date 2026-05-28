package com.panghu.food.web;

import com.panghu.food.dto.FeedAccessibleMenusResponse;
import com.panghu.food.dto.FeedItemResponse;
import com.panghu.food.service.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
public class FeedController {
    private final SocialService socialService;

    public FeedController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping
    public ResponseEntity<List<FeedItemResponse>> getFeed(@RequestParam(defaultValue = "all") String filter) {
        return ResponseEntity.ok(socialService.getFeed(filter));
    }

    @GetMapping("/accessible-menus")
    public ResponseEntity<FeedAccessibleMenusResponse> getAccessibleMenus() {
        return ResponseEntity.ok(socialService.getAccessibleMenus());
    }
}
