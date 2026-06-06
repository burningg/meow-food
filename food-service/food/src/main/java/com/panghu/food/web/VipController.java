package com.panghu.food.web;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.VipInfoResponse;
import com.panghu.food.dto.VipPaymentOrderResponse;
import com.panghu.food.dto.VipPaymentOrderStatusResponse;
import com.panghu.food.service.VipPaymentService;
import com.panghu.food.service.VipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/vip")
public class VipController {
    private final VipService vipService;
    private final VipPaymentService vipPaymentService;

    public VipController(VipService vipService, VipPaymentService vipPaymentService) {
        this.vipService = vipService;
        this.vipPaymentService = vipPaymentService;
    }

    @PostMapping("/free-trial")
    public ResponseEntity<VipInfoResponse> claimFreeTrial() {
        return ResponseEntity.ok(vipService.claimFreeTrial(AuthContext.requireUserId()));
    }

    @PostMapping("/orders")
    public ResponseEntity<VipPaymentOrderResponse> createOrder() {
        return ResponseEntity.ok(vipPaymentService.createOrder(AuthContext.requireUserId()));
    }

    @GetMapping("/orders/{outTradeNo}")
    public ResponseEntity<VipPaymentOrderStatusResponse> getOrder(@PathVariable String outTradeNo) {
        return ResponseEntity.ok(vipPaymentService.getOrder(AuthContext.requireUserId(), outTradeNo));
    }

    @PostMapping("/pay/notify")
    public ResponseEntity<Map<String, String>> payNotify(@RequestHeader Map<String, String> headers,
                                                         @RequestBody String body) {
        vipPaymentService.handleNotify(headers, body);
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
    }
}
