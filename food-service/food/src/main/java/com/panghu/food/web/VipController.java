package com.panghu.food.web;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.VipCreateOrderRequest;
import com.panghu.food.dto.VipInfoResponse;
import com.panghu.food.dto.VipPaymentOrderResponse;
import com.panghu.food.dto.VipPaymentOrderStatusResponse;
import com.panghu.food.service.VipPaymentService;
import com.panghu.food.service.VipService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<VipPaymentOrderResponse> createOrder(@RequestBody VipCreateOrderRequest request) {
        return ResponseEntity.ok(vipPaymentService.createOrder(AuthContext.requireUserId(), request));
    }

    @GetMapping("/orders/{outTradeNo}")
    public ResponseEntity<VipPaymentOrderStatusResponse> getOrder(@PathVariable String outTradeNo) {
        return ResponseEntity.ok(vipPaymentService.getOrder(AuthContext.requireUserId(), outTradeNo));
    }

    @PostMapping(value = "/virtual-pay/notify", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> virtualPayNotify(@RequestBody String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .body(vipPaymentService.handleVirtualPayNotify(body));
    }

    @PostMapping("/pay/notify")
    public ResponseEntity<String> payNotify(@RequestBody String body) {
        return ResponseEntity.ok(body == null ? "" : body);
    }
}
