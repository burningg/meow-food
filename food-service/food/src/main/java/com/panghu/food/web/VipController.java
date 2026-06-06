package com.panghu.food.web;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.VipInfoResponse;
import com.panghu.food.service.VipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vip")
public class VipController {
    private final VipService vipService;

    public VipController(VipService vipService) {
        this.vipService = vipService;
    }

    @PostMapping("/free-trial")
    public ResponseEntity<VipInfoResponse> claimFreeTrial() {
        return ResponseEntity.ok(vipService.claimFreeTrial(AuthContext.requireUserId()));
    }
}
