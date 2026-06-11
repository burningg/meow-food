package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.dto.VipInfoResponse;
import com.panghu.food.dto.VipPaymentOrderResponse;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.entity.VipPaymentOrder;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserAccountMapper;
import com.panghu.food.mapper.VipPaymentOrderMapper;
import com.panghu.food.pay.JsapiPaymentParams;
import com.panghu.food.pay.WechatPayClient;
import com.panghu.food.pay.WechatPayTransaction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VipPaymentServiceTest {
    private final VipPaymentOrderMapper vipPaymentOrderMapper = mock(VipPaymentOrderMapper.class);
    private final UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
    private final VipService vipService = mock(VipService.class);
    private final WechatPayClient wechatPayClient = mock(WechatPayClient.class);
    private final VipPaymentService vipPaymentService = new VipPaymentService(
            vipPaymentOrderMapper,
            userAccountMapper,
            vipService,
            wechatPayClient);

    @Test
    void createOrderForInactiveWechatUserReturnsTaroPaymentParams() {
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(false));
        when(userAccountMapper.selectById("user-1")).thenReturn(user("user-1", "wx_openid-1"));
        when(wechatPayClient.createJsapiPrepay(eq("openid-1"), any(), eq(VipPaymentService.PLAN_NAME), eq(290)))
                .thenReturn(paymentParams());

        VipPaymentOrderResponse response = vipPaymentService.createOrder("user-1");

        assertThat(response.getOutTradeNo()).startsWith("VIP");
        assertThat(response.getAmountFen()).isEqualTo(290);
        assertThat(response.getPlanName()).isEqualTo("VIP 首年会员");
        assertThat(response.getTimeStamp()).isEqualTo("1710000000");
        assertThat(response.getNonceStr()).isEqualTo("nonce-1");
        assertThat(response.getPayPackage()).isEqualTo("prepay_id=prepay-1");
        assertThat(response.getSignType()).isEqualTo("RSA");
        assertThat(response.getPaySign()).isEqualTo("pay-sign-1");

        ArgumentCaptor<VipPaymentOrder> orderCaptor = ArgumentCaptor.forClass(VipPaymentOrder.class);
        verify(vipPaymentOrderMapper).insert(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void createOrderRejectsActiveVip() {
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(true));

        assertThatThrownBy(() -> vipPaymentService.createOrder("user-1"))
                .isInstanceOf(ApiException.class)
                .hasMessage("你已开通 VIP");

        verify(vipPaymentOrderMapper, never()).insert(any(VipPaymentOrder.class));
    }

    @Test
    void createOrderRejectsNonWechatUser() {
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(false));
        when(userAccountMapper.selectById("user-1")).thenReturn(user("user-1", "alice"));

        assertThatThrownBy(() -> vipPaymentService.createOrder("user-1"))
                .isInstanceOf(ApiException.class)
                .hasMessage("请使用微信登录后开通");
    }

    @Test
    void handleNotifySuccessActivatesVipAndMarksOrderPaid() {
        VipPaymentOrder order = pendingOrder();
        when(wechatPayClient.parseTransactionNotify(any(), eq("{}"))).thenReturn(successTransaction(290));
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);
        when(wechatPayClient.getMchId()).thenReturn("mch-1");

        vipPaymentService.handleNotify(Map.of("Wechatpay-Signature", "sig"), "{}");

        assertThat(order.getStatus()).isEqualTo("PAID");
        assertThat(order.getTransactionId()).isEqualTo("transaction-1");
        assertThat(order.getPaidAt()).isNotNull();
        verify(vipPaymentOrderMapper).updateById(order);
        verify(vipService).activatePaidYear("user-1", BigDecimal.valueOf(290, 2));
    }

    @Test
    void handleNotifyIsIdempotentForAlreadyPaidOrder() {
        VipPaymentOrder order = pendingOrder();
        order.setStatus("PAID");
        when(wechatPayClient.parseTransactionNotify(any(), eq("{}"))).thenReturn(successTransaction(290));
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);

        vipPaymentService.handleNotify(Map.of(), "{}");

        verify(vipPaymentOrderMapper, never()).updateById(any(VipPaymentOrder.class));
        verify(vipService, never()).activatePaidYear(any(), any());
    }

    @Test
    void handleNotifyUsesPersistedOrderAmount() {
        VipPaymentOrder order = pendingOrder();
        order.setAmountFen(1);
        when(wechatPayClient.parseTransactionNotify(any(), eq("{}"))).thenReturn(successTransaction(1));
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);
        when(wechatPayClient.getMchId()).thenReturn("mch-1");

        vipPaymentService.handleNotify(Map.of(), "{}");

        assertThat(order.getStatus()).isEqualTo("PAID");
        verify(vipPaymentOrderMapper).updateById(order);
        verify(vipService).activatePaidYear("user-1", BigDecimal.valueOf(1, 2));
    }

    @Test
    void handleNotifyDoesNotActivateWhenAmountMismatch() {
        VipPaymentOrder order = pendingOrder();
        when(wechatPayClient.parseTransactionNotify(any(), eq("{}"))).thenReturn(successTransaction(1));
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);
        when(wechatPayClient.getMchId()).thenReturn("mch-1");

        assertThatThrownBy(() -> vipPaymentService.handleNotify(Map.of(), "{}"))
                .isInstanceOf(ApiException.class)
                .hasMessage("微信支付金额不匹配");

        verify(vipPaymentOrderMapper, never()).updateById(any(VipPaymentOrder.class));
        verify(vipService, never()).activatePaidYear(any(), any());
    }

    private JsapiPaymentParams paymentParams() {
        JsapiPaymentParams params = new JsapiPaymentParams();
        params.setTimeStamp("1710000000");
        params.setNonceStr("nonce-1");
        params.setPayPackage("prepay_id=prepay-1");
        params.setSignType("RSA");
        params.setPaySign("pay-sign-1");
        params.setPrepayId("prepay-1");
        return params;
    }

    private VipPaymentOrder pendingOrder() {
        VipPaymentOrder order = new VipPaymentOrder();
        order.setId("order-1");
        order.setUserId("user-1");
        order.setOutTradeNo("VIP20260606010101ABCDEF123456");
        order.setPlanCode(VipPaymentService.PLAN_CODE);
        order.setAmountFen(290);
        order.setStatus("PENDING");
        return order;
    }

    private WechatPayTransaction successTransaction(int amountFen) {
        WechatPayTransaction transaction = new WechatPayTransaction();
        transaction.setOutTradeNo("VIP20260606010101ABCDEF123456");
        transaction.setTransactionId("transaction-1");
        transaction.setTradeState("SUCCESS");
        transaction.setMchid("mch-1");
        transaction.setOpenid("openid-1");
        transaction.setAmountFen(amountFen);
        transaction.setSuccessTime(LocalDateTime.now());
        return transaction;
    }

    private UserAccount user(String id, String account) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setAccount(account);
        return user;
    }

    private VipInfoResponse vipInfo(boolean active) {
        VipInfoResponse response = new VipInfoResponse();
        response.setVip(active);
        return response;
    }
}
