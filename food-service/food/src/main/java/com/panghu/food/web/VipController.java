package com.panghu.food.web;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.VipCreateOrderRequest;
import com.panghu.food.dto.VipInfoResponse;
import com.panghu.food.dto.VipPaymentOrderResponse;
import com.panghu.food.dto.VipPaymentOrderStatusResponse;
import com.panghu.food.pay.WechatVirtualPayNotifyResult;
import com.panghu.food.service.WechatMessagePushService;
import com.panghu.food.service.VipPaymentService;
import com.panghu.food.service.VipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vip")
public class VipController {
    private final VipService vipService;
    private final VipPaymentService vipPaymentService;
    private final WechatMessagePushService wechatMessagePushService;

    public VipController(VipService vipService,
                         VipPaymentService vipPaymentService,
                         WechatMessagePushService wechatMessagePushService) {
        this.vipService = vipService;
        this.vipPaymentService = vipPaymentService;
        this.wechatMessagePushService = wechatMessagePushService;
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

    @GetMapping("/virtual-pay/notify")
    public ResponseEntity<String> verifyVirtualPayNotify(@RequestParam String signature,
                                                         @RequestParam String timestamp,
                                                         @RequestParam String nonce,
                                                         @RequestParam String echostr) {
        return ResponseEntity.ok(wechatMessagePushService.verifyUrl(signature, timestamp, nonce, echostr));
    }

    @PostMapping("/virtual-pay/notify")
    public ResponseEntity<String> virtualPayNotify(@RequestParam String timestamp,
                                                   @RequestParam String nonce,
                                                   @RequestParam(name = "encrypt_type") String encryptType,
                                                   @RequestParam(name = "msg_signature") String msgSignature,
                                                   @RequestBody String body) {
        WechatVirtualPayNotifyResult notify = wechatMessagePushService.parseVirtualPayNotify(
                timestamp,
                nonce,
                encryptType,
                msgSignature,
                body);
        return ResponseEntity.ok(vipPaymentService.handleVirtualPayNotify(notify));
    }

    @PostMapping("/pay/notify")
    public ResponseEntity<String> payNotify(@RequestBody String body) {
        return ResponseEntity.ok(body == null ? "" : body);
    }
}
