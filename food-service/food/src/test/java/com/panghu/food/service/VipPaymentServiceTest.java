package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.component.WechatComponent;
import com.panghu.food.dto.VipCreateOrderRequest;
import com.panghu.food.dto.VipInfoResponse;
import com.panghu.food.dto.VipPaymentOrderResponse;
import com.panghu.food.dto.VipPaymentOrderStatusResponse;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.entity.VipPaymentOrder;
import com.panghu.food.entity.WechatAuthVO;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserAccountMapper;
import com.panghu.food.mapper.VipPaymentOrderMapper;
import com.panghu.food.pay.WechatVirtualPayClient;
import com.panghu.food.pay.WechatVirtualPayNotifyResult;
import com.panghu.food.pay.WechatVirtualPayOrderQueryResult;
import com.panghu.food.pay.WechatVirtualPayProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VipPaymentServiceTest {
    private final VipPaymentOrderMapper vipPaymentOrderMapper = mock(VipPaymentOrderMapper.class);
    private final UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
    private final VipService vipService = mock(VipService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final WechatComponent wechatComponent = mock(WechatComponent.class);
    private final WechatVirtualPayClient wechatVirtualPayClient = mock(WechatVirtualPayClient.class);
    private final WechatVirtualPayProperties virtualPayProperties = new WechatVirtualPayProperties();
    private final VipPaymentService vipPaymentService;

    VipPaymentServiceTest() {
        virtualPayProperties.setEnv(1);
        virtualPayProperties.setOfferId("offer-vip");
        virtualPayProperties.setProductId("product-vip");
        virtualPayProperties.setAppKey("app-key");
        virtualPayProperties.setSandboxAppKey("sandbox-app-key");
        vipPaymentService = new VipPaymentService(
                vipPaymentOrderMapper,
                userAccountMapper,
                vipService,
                notificationService,
                wechatComponent,
                wechatVirtualPayClient,
                virtualPayProperties);
    }

    @Test
    void createOrderForInactiveWechatUserReturnsVirtualPaymentParams() {
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(false));
        when(userAccountMapper.selectById("user-1")).thenReturn(user("user-1", "wx_openid-1"));
        when(wechatComponent.getWechatAuth("code-1")).thenReturn(wechatAuth("openid-1", "session-key-1"));
        when(wechatVirtualPayClient.signPay(any(), eq(1))).thenReturn("pay-sig-1");
        when(wechatVirtualPayClient.signUser(any(), eq("session-key-1"))).thenReturn("signature-1");

        VipPaymentOrderResponse response = vipPaymentService.createOrder("user-1", createOrderRequest("code-1"));

        assertThat(response.getOutTradeNo()).startsWith("VIP");
        assertThat(response.getAmountFen()).isEqualTo(290);
        assertThat(response.getPlanName()).isEqualTo("VIP 首年会员");
        assertThat(response.getOfferId()).isEqualTo("offer-vip");
        assertThat(response.getProductId()).isEqualTo("product-vip");
        assertThat(response.getEnv()).isEqualTo(1);
        assertThat(response.getMode()).isEqualTo("short_series_goods");
        assertThat(response.getPaySig()).isEqualTo("pay-sig-1");
        assertThat(response.getSignature()).isEqualTo("signature-1");
        assertThat(response.getSignData()).contains("\"productId\":\"product-vip\"");

        ArgumentCaptor<VipPaymentOrder> orderCaptor = ArgumentCaptor.forClass(VipPaymentOrder.class);
        verify(vipPaymentOrderMapper).insert(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(orderCaptor.getValue().getOpenId()).isEqualTo("openid-1");
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void createOrderRejectsActiveVip() {
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(true));

        assertThatThrownBy(() -> vipPaymentService.createOrder("user-1", createOrderRequest("code-1")))
                .isInstanceOf(ApiException.class)
                .hasMessage("你已开通 VIP");

        verify(vipPaymentOrderMapper, never()).insert(any(VipPaymentOrder.class));
    }

    @Test
    void createOrderRejectsNonWechatUser() {
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(false));
        when(userAccountMapper.selectById("user-1")).thenReturn(user("user-1", "alice"));

        assertThatThrownBy(() -> vipPaymentService.createOrder("user-1", createOrderRequest("code-1")))
                .isInstanceOf(ApiException.class)
                .hasMessage("请使用微信登录后开通");
    }

    @Test
    void createOrderRejectsInvalidWechatCode() {
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(false));
        when(userAccountMapper.selectById("user-1")).thenReturn(user("user-1", "wx_openid-1"));
        when(wechatComponent.getWechatAuth("code-1")).thenReturn(wechatAuth("openid-1", null));

        assertThatThrownBy(() -> vipPaymentService.createOrder("user-1", createOrderRequest("code-1")))
                .isInstanceOf(ApiException.class)
                .hasMessage("微信登录态已失效，请重新登录后支付");
    }

    @Test
    void handleVirtualPayNotifySuccessActivatesVipAndMarksOrderPaid() {
        VipPaymentOrder order = pendingOrder();
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);

        String response = vipPaymentService.handleVirtualPayNotify(successNotify(290));

        assertThat(response).isEqualTo("success");
        assertThat(order.getStatus()).isEqualTo("PAID");
        assertThat(order.getTransactionId()).isEqualTo("transaction-1");
        assertThat(order.getWechatPayMchOrderNo()).isEqualTo("mch-order-1");
        verify(vipPaymentOrderMapper).updateById(order);
        verify(vipService).activatePaidYear("user-1", BigDecimal.valueOf(290, 2));
        verify(notificationService).sendVipOpenedSuccessNotification("user-1");
    }

    @Test
    void handleVirtualPayNotifyIsIdempotentForAlreadyPaidOrder() {
        VipPaymentOrder order = pendingOrder();
        order.setStatus("PAID");
        order.setProvideStatus(VipPaymentService.PROVIDE_STATUS_SUCCESS);
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);

        vipPaymentService.handleVirtualPayNotify(successNotify(290));

        verify(vipService, never()).activatePaidYear(any(), any());
        verify(notificationService, never()).sendVipOpenedSuccessNotification(any());
    }

    @Test
    void getOrderQueriesWechatAndProvidesGoodsWhenCallbackMissing() {
        VipPaymentOrder order = pendingOrder();
        WechatVirtualPayOrderQueryResult remoteOrder = paidWechatOrder(2);
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);
        when(wechatVirtualPayClient.queryOrder("openid-1", 1, order.getOutTradeNo(), null)).thenReturn(remoteOrder);
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(false));

        VipPaymentOrderStatusResponse response = vipPaymentService.getOrder("user-1", order.getOutTradeNo());

        assertThat(response.getStatus()).isEqualTo(VipPaymentService.STATUS_PAID_PENDING_CALLBACK);
        assertThat(order.getWechatOrderId()).isEqualTo("wx-order-1");
        assertThat(order.getProvideStatus()).isEqualTo(VipPaymentService.PROVIDE_STATUS_SUCCESS);
        verify(wechatVirtualPayClient).notifyProvideGoods(order.getOutTradeNo(), "wx-order-1", 1);
        verify(vipPaymentOrderMapper, atLeastOnce()).updateById(order);
        verify(vipService, never()).activatePaidYear(any(), any());
        verify(notificationService, never()).sendVipOpenedSuccessNotification(any());
    }

    @Test
    void getOrderProvidesGoodsWhenWechatOrderIsProviding() {
        VipPaymentOrder order = pendingOrder();
        WechatVirtualPayOrderQueryResult remoteOrder = paidWechatOrder(3);
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);
        when(wechatVirtualPayClient.queryOrder("openid-1", 1, order.getOutTradeNo(), null)).thenReturn(remoteOrder);
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(false));

        VipPaymentOrderStatusResponse response = vipPaymentService.getOrder("user-1", order.getOutTradeNo());

        assertThat(response.getStatus()).isEqualTo(VipPaymentService.STATUS_PAID_PENDING_CALLBACK);
        assertThat(order.getProvideStatus()).isEqualTo(VipPaymentService.PROVIDE_STATUS_SUCCESS);
        assertThat(order.getPaidAt()).isNotNull();
        verify(wechatVirtualPayClient).notifyProvideGoods(order.getOutTradeNo(), "wx-order-1", 1);
        verify(vipService, never()).activatePaidYear(any(), any());
        verify(notificationService, never()).sendVipOpenedSuccessNotification(any());
    }

    @Test
    void getOrderMarksPendingCallbackWhenWechatOrderAlreadyProvided() {
        VipPaymentOrder order = pendingOrder();
        WechatVirtualPayOrderQueryResult remoteOrder = paidWechatOrder(4);
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);
        when(wechatVirtualPayClient.queryOrder("openid-1", 1, order.getOutTradeNo(), null)).thenReturn(remoteOrder);
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(false));

        VipPaymentOrderStatusResponse response = vipPaymentService.getOrder("user-1", order.getOutTradeNo());

        assertThat(response.getStatus()).isEqualTo(VipPaymentService.STATUS_PAID_PENDING_CALLBACK);
        assertThat(order.getProvideStatus()).isEqualTo(VipPaymentService.PROVIDE_STATUS_SUCCESS);
        verify(wechatVirtualPayClient, never()).notifyProvideGoods(any(), any(), any());
        verify(vipService, never()).activatePaidYear(any(), any());
    }

    @Test
    void getOrderMarksClosedWhenWechatOrderClosed() {
        VipPaymentOrder order = pendingOrder();
        WechatVirtualPayOrderQueryResult remoteOrder = paidWechatOrder(6);
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);
        when(wechatVirtualPayClient.queryOrder("openid-1", 1, order.getOutTradeNo(), null)).thenReturn(remoteOrder);
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(false));

        VipPaymentOrderStatusResponse response = vipPaymentService.getOrder("user-1", order.getOutTradeNo());

        assertThat(response.getStatus()).isEqualTo("CLOSED");
        verify(wechatVirtualPayClient, never()).notifyProvideGoods(any(), any(), any());
        verify(vipService, never()).activatePaidYear(any(), any());
    }

    @Test
    void getOrderDoesNotProvideGoodsWhenWechatOrderNotPaid() {
        VipPaymentOrder order = pendingOrder();
        WechatVirtualPayOrderQueryResult remoteOrder = paidWechatOrder(1);
        remoteOrder.setWxpayOrderId(null);
        remoteOrder.setChannelOrderId(null);
        remoteOrder.setWechatOrderId(null);
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);
        when(wechatVirtualPayClient.queryOrder("openid-1", 1, order.getOutTradeNo(), null)).thenReturn(remoteOrder);
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(false));

        VipPaymentOrderStatusResponse response = vipPaymentService.getOrder("user-1", order.getOutTradeNo());

        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(wechatVirtualPayClient, never()).notifyProvideGoods(any(), any(), any());
        verify(vipService, never()).activatePaidYear(any(), any());
        verify(notificationService, never()).sendVipOpenedSuccessNotification(any());
    }

    @Test
    void getOrderDoesNotRepeatProvideForLocalPaidOrder() {
        VipPaymentOrder order = pendingOrder();
        order.setStatus("PAID");
        order.setProvideStatus(VipPaymentService.PROVIDE_STATUS_SUCCESS);
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(true));

        VipPaymentOrderStatusResponse response = vipPaymentService.getOrder("user-1", order.getOutTradeNo());

        assertThat(response.getStatus()).isEqualTo("PAID");
        verify(wechatVirtualPayClient, never()).queryOrder(any(), any(), any(), any());
        verify(wechatVirtualPayClient, never()).notifyProvideGoods(any(), any(), any());
        verify(vipService, never()).activatePaidYear(any(), any());
    }

    @Test
    void getOrderPrefersWechatOrderIdOnLaterQuery() {
        VipPaymentOrder order = pendingOrder();
        order.setWechatOrderId("wx-order-1");
        WechatVirtualPayOrderQueryResult remoteOrder = paidWechatOrder(1);
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);
        when(wechatVirtualPayClient.queryOrder("openid-1", 1, order.getOutTradeNo(), "wx-order-1")).thenReturn(remoteOrder);
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo(false));

        vipPaymentService.getOrder("user-1", order.getOutTradeNo());

        verify(wechatVirtualPayClient, times(1)).queryOrder("openid-1", 1, order.getOutTradeNo(), "wx-order-1");
    }

    @Test
    void handleVirtualPayNotifyDoesNotActivateWhenAmountMismatch() {
        VipPaymentOrder order = pendingOrder();
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);

        assertThatThrownBy(() -> vipPaymentService.handleVirtualPayNotify(successNotify(1)))
                .isInstanceOf(ApiException.class)
                .hasMessage("微信虚拟支付金额不匹配");

        verify(vipPaymentOrderMapper, never()).updateById(any(VipPaymentOrder.class));
        verify(vipService, never()).activatePaidYear(any(), any());
    }

    @Test
    void handleVirtualPayNotifyDoesNotActivateWhenProductMismatch() {
        VipPaymentOrder order = pendingOrder();
        WechatVirtualPayNotifyResult notify = successNotify(290);
        notify.setProductId("other-product");
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);

        assertThatThrownBy(() -> vipPaymentService.handleVirtualPayNotify(notify))
                .isInstanceOf(ApiException.class)
                .hasMessage("微信虚拟支付商品不匹配");
    }

    @Test
    void handleVirtualPayNotifyDoesNotActivateWhenOpenIdMismatch() {
        VipPaymentOrder order = pendingOrder();
        WechatVirtualPayNotifyResult notify = successNotify(290);
        notify.setOpenId("openid-2");
        when(vipPaymentOrderMapper.selectOne(any(QueryWrapper.class))).thenReturn(order);

        assertThatThrownBy(() -> vipPaymentService.handleVirtualPayNotify(notify))
                .isInstanceOf(ApiException.class)
                .hasMessage("微信虚拟支付用户不匹配");
    }

    private VipCreateOrderRequest createOrderRequest(String code) {
        VipCreateOrderRequest request = new VipCreateOrderRequest();
        request.setCode(code);
        return request;
    }

    private VipPaymentOrder pendingOrder() {
        VipPaymentOrder order = new VipPaymentOrder();
        order.setId("order-1");
        order.setUserId("user-1");
        order.setOutTradeNo("VIP20260606010101ABCDEF123456");
        order.setPlanCode(VipPaymentService.PLAN_CODE);
        order.setProductId("product-vip");
        order.setAmountFen(290);
        order.setEnv(1);
        order.setOpenId("openid-1");
        order.setProvideStatus(VipPaymentService.PROVIDE_STATUS_NONE);
        order.setStatus("PENDING");
        return order;
    }

    private WechatVirtualPayOrderQueryResult paidWechatOrder(int status) {
        WechatVirtualPayOrderQueryResult result = new WechatVirtualPayOrderQueryResult();
        result.setOrderId("VIP20260606010101ABCDEF123456");
        result.setWechatOrderId("wx-order-1");
        result.setChannelOrderId("mch-order-1");
        result.setWxpayOrderId("transaction-1");
        result.setStatus(status);
        result.setPaidTime(1_718_000_000L);
        result.setProvideTime(1_718_000_100L);
        return result;
    }

    private WechatVirtualPayNotifyResult successNotify(int amountFen) {
        WechatVirtualPayNotifyResult notify = new WechatVirtualPayNotifyResult();
        notify.setEvent(VipPaymentService.EVENT_GOODS_DELIVER);
        notify.setOutTradeNo("VIP20260606010101ABCDEF123456");
        notify.setOpenId("openid-1");
        notify.setEnv(1);
        notify.setPayChannel("WECHAT");
        notify.setWechatPayMchOrderNo("mch-order-1");
        notify.setTransactionId("transaction-1");
        notify.setProductId("product-vip");
        notify.setActualPrice(amountFen);
        notify.setAttach("vip:user-1:1");
        return notify;
    }

    private WechatAuthVO wechatAuth(String openId, String sessionKey) {
        WechatAuthVO auth = new WechatAuthVO();
        auth.setOpenid(openId);
        auth.setSession_key(sessionKey);
        return auth;
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
