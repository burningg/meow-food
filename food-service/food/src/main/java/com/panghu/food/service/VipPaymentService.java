package com.panghu.food.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class VipPaymentService {
    private static final Logger log = LoggerFactory.getLogger(VipPaymentService.class);

    public static final String PLAN_CODE = "VIP_FIRST_YEAR";
    public static final String PLAN_NAME = "VIP 首年会员";
    public static final int AMOUNT_FEN = 290;
    public static final int BUY_QUANTITY = 1;
    public static final String CURRENCY_TYPE = "CNY";
    public static final String MODE_SHORT_SERIES_GOODS = "short_series_goods";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String PROVIDE_STATUS_NONE = "NONE";
    public static final String PROVIDE_STATUS_PROCESSING = "PROCESSING";
    public static final String PROVIDE_STATUS_SUCCESS = "SUCCESS";
    public static final String EVENT_GOODS_DELIVER = "xpay_goods_deliver_notify";
    public static final String EVENT_REFUND = "xpay_refund_notify";

    private static final int WECHAT_ORDER_STATUS_PAID = 2;
    private static final int WECHAT_ORDER_STATUS_PROVIDING = 3;
    private static final int WECHAT_ORDER_STATUS_PROVIDED = 4;
    private static final int WECHAT_ORDER_STATUS_REFUNDED = 5;
    private static final int WECHAT_ORDER_STATUS_CLOSED = 6;
    private static final int WECHAT_ORDER_STATUS_USER_REFUNDED = 8;
    private static final DateTimeFormatter TRADE_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VipPaymentOrderMapper vipPaymentOrderMapper;
    private final UserAccountMapper userAccountMapper;
    private final VipService vipService;
    private final NotificationService notificationService;
    private final WechatComponent wechatComponent;
    private final WechatVirtualPayClient wechatVirtualPayClient;
    private final WechatVirtualPayProperties virtualPayProperties;

    public VipPaymentService(VipPaymentOrderMapper vipPaymentOrderMapper,
                             UserAccountMapper userAccountMapper,
                             VipService vipService,
                             NotificationService notificationService,
                             WechatComponent wechatComponent,
                             WechatVirtualPayClient wechatVirtualPayClient,
                             WechatVirtualPayProperties virtualPayProperties) {
        this.vipPaymentOrderMapper = vipPaymentOrderMapper;
        this.userAccountMapper = userAccountMapper;
        this.vipService = vipService;
        this.notificationService = notificationService;
        this.wechatComponent = wechatComponent;
        this.wechatVirtualPayClient = wechatVirtualPayClient;
        this.virtualPayProperties = virtualPayProperties;
    }

    @Transactional
    public VipPaymentOrderResponse createOrder(String userId, VipCreateOrderRequest request) {
        VipInfoResponse vipInfo = vipService.getVipInfo(userId);
        if (Boolean.TRUE.equals(vipInfo.getVip())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "你已开通 VIP");
        }
        if (request == null || isBlank(request.getCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信登录凭证不能为空");
        }

        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        String openId = parseOpenid(user.getAccount());
        WechatAuthVO auth = wechatComponent.getWechatAuth(request.getCode().trim());
        if (auth == null || isBlank(auth.getOpenid()) || isBlank(auth.getSession_key())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, auth == null || isBlank(auth.getErrmsg()) ? "微信登录态已失效，请重新登录后支付" : auth.getErrmsg());
        }
        if (!openId.equals(auth.getOpenid().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "支付用户与当前登录账号不一致");
        }

        Integer env = resolveEnv();
        String productId = requireProperty("wechat.virtual-pay.product-id", virtualPayProperties.getProductId());
        String offerId = requireProperty("wechat.virtual-pay.offer-id", virtualPayProperties.getOfferId());

        VipPaymentOrder order = new VipPaymentOrder();
        LocalDateTime now = LocalDateTime.now();
        order.setUserId(userId);
        order.setOutTradeNo(newOutTradeNo());
        order.setPlanCode(PLAN_CODE);
        order.setProductId(productId);
        order.setAmountFen(AMOUNT_FEN);
        order.setEnv(env);
        order.setOpenId(openId);
        order.setAttach(newAttach(userId));
        order.setProvideStatus(PROVIDE_STATUS_NONE);
        order.setStatus(STATUS_PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        vipPaymentOrderMapper.insert(order);

        String signData = buildSignData(offerId, productId, order);
        String paySig = wechatVirtualPayClient.signPay(signData, env);
        // 虚拟支付签名依赖当前这次登录换回来的 session_key，不落库，避免引入长期敏感态存储。
        String signature = wechatVirtualPayClient.signUser(signData, auth.getSession_key());

        VipPaymentOrderResponse response = new VipPaymentOrderResponse();
        response.setOutTradeNo(order.getOutTradeNo());
        response.setAmountFen(order.getAmountFen());
        response.setPlanName(PLAN_NAME);
        response.setOfferId(offerId);
        response.setProductId(productId);
        response.setBuyQuantity(BUY_QUANTITY);
        response.setEnv(env);
        response.setCurrencyType(CURRENCY_TYPE);
        response.setGoodsPrice(AMOUNT_FEN);
        response.setAttach(order.getAttach());
        response.setSignData(signData);
        response.setPaySig(paySig);
        response.setSignature(signature);
        response.setMode(MODE_SHORT_SERIES_GOODS);
        response.setSuccessTip("支付完成后会员权益将自动生效");
        return response;
    }

    @Transactional
    public VipPaymentOrderStatusResponse getOrder(String userId, String outTradeNo) {
        VipPaymentOrder order = findUserOrder(userId, outTradeNo);
        if (order == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        if (shouldSyncWechatOrder(order)) {
            syncOrderFromWechat(order);
        }
        return toStatus(order, vipService.getVipInfo(userId));
    }

    @Transactional
    public String handleVirtualPayNotify(String body) {
        WechatVirtualPayNotifyResult notify = wechatVirtualPayClient.parsePayNotify(body);
        if (EVENT_GOODS_DELIVER.equals(notify.getEvent())) {
            handlePaySuccess(notify);
        } else if (EVENT_REFUND.equals(notify.getEvent())) {
            handleRefund(notify);
        } else {
            log.info("忽略未处理的虚拟支付事件，event={}", notify.getEvent());
        }
        return "<xml><ErrCode>0</ErrCode><ErrMsg>success</ErrMsg></xml>";
    }

    private void handlePaySuccess(WechatVirtualPayNotifyResult notify) {
        VipPaymentOrder order = findByOutTradeNo(notify.getOutTradeNo());
        validateNotify(order, notify);
        markOrderPaid(order, notify.getTransactionId(), notify.getWechatPayMchOrderNo(), notify.getPayChannel(),
                LocalDateTime.now(), PROVIDE_STATUS_SUCCESS, LocalDateTime.now());
    }

    private void handleRefund(WechatVirtualPayNotifyResult notify) {
        VipPaymentOrder order = findByOutTradeNo(notify.getOutTradeNo());
        if (STATUS_REFUNDED.equals(order.getStatus())) {
            return;
        }
        order.setStatus(STATUS_REFUNDED);
        order.setUpdatedAt(LocalDateTime.now());
        vipPaymentOrderMapper.updateById(order);
        log.info("虚拟支付退款回调已记录，outTradeNo={}, userId={}", order.getOutTradeNo(), order.getUserId());
    }

    private VipPaymentOrder findByOutTradeNo(String outTradeNo) {
        VipPaymentOrder order = vipPaymentOrderMapper.selectOne(new QueryWrapper<VipPaymentOrder>()
                .eq("out_trade_no", outTradeNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VIP 支付订单不存在");
        }
        return order;
    }

    private VipPaymentOrder findUserOrder(String userId, String outTradeNo) {
        return vipPaymentOrderMapper.selectOne(new QueryWrapper<VipPaymentOrder>()
                .eq("user_id", userId)
                .eq("out_trade_no", outTradeNo)
                .last("LIMIT 1"));
    }

    private boolean shouldSyncWechatOrder(VipPaymentOrder order) {
        return !STATUS_PAID.equals(order.getStatus())
                && !STATUS_REFUNDED.equals(order.getStatus())
                && !STATUS_CLOSED.equals(order.getStatus());
    }

    private void syncOrderFromWechat(VipPaymentOrder order) {
        WechatVirtualPayOrderQueryResult remoteOrder;
        try {
            remoteOrder = wechatVirtualPayClient.queryOrder(
                    order.getOpenId(),
                    order.getEnv(),
                    order.getOutTradeNo(),
                    order.getWechatOrderId());
        } catch (Exception exception) {
            // 查单补偿是兜底逻辑，微信接口抖动时保留本地订单继续给前端轮询。
            log.warn("虚拟支付查单失败，outTradeNo={}, userId={}", order.getOutTradeNo(), order.getUserId(), exception);
            return;
        }
        mergeRemoteOrderSnapshot(order, remoteOrder);

        Integer remoteStatus = remoteOrder.getStatus();
        if (remoteStatus == null) {
            persistOrder(order);
            return;
        }
        if (remoteStatus == WECHAT_ORDER_STATUS_REFUNDED || remoteStatus == WECHAT_ORDER_STATUS_USER_REFUNDED) {
            markRefunded(order);
            return;
        }
        if (remoteStatus == WECHAT_ORDER_STATUS_CLOSED) {
            markClosed(order);
            return;
        }
        if (remoteStatus == WECHAT_ORDER_STATUS_PAID) {
            // 关键补偿逻辑：支付已成功但回调未到时，主动补发货通知，再落本地权益。
            order.setProvideStatus(PROVIDE_STATUS_PROCESSING);
            persistOrder(order);
            try {
                wechatVirtualPayClient.notifyProvideGoods(order.getOutTradeNo(), order.getWechatOrderId(), order.getEnv());
            } catch (Exception exception) {
                log.warn("虚拟支付补发货失败，outTradeNo={}, userId={}", order.getOutTradeNo(), order.getUserId(), exception);
                return;
            }
            markOrderPaid(order, remoteOrder.getWxpayOrderId(), remoteOrder.getChannelOrderId(), order.getPayChannel(),
                    resolveDateTime(remoteOrder.getPaidTime(), LocalDateTime.now()),
                    PROVIDE_STATUS_SUCCESS,
                    resolveDateTime(remoteOrder.getProvideTime(), LocalDateTime.now()));
            return;
        }
        if (remoteStatus == WECHAT_ORDER_STATUS_PROVIDING) {
            markOrderProviding(order,
                    remoteOrder.getWxpayOrderId(),
                    remoteOrder.getChannelOrderId(),
                    order.getPayChannel(),
                    resolveDateTime(remoteOrder.getPaidTime(), LocalDateTime.now()));
            return;
        }
        if (remoteStatus == WECHAT_ORDER_STATUS_PROVIDED) {
            markOrderPaid(order, remoteOrder.getWxpayOrderId(), remoteOrder.getChannelOrderId(), order.getPayChannel(),
                    resolveDateTime(remoteOrder.getPaidTime(), LocalDateTime.now()),
                    PROVIDE_STATUS_SUCCESS,
                    resolveDateTime(remoteOrder.getProvideTime(), LocalDateTime.now()));
            return;
        }
        persistOrder(order);
    }

    private void mergeRemoteOrderSnapshot(VipPaymentOrder order, WechatVirtualPayOrderQueryResult remoteOrder) {
        if (!isBlank(remoteOrder.getWechatOrderId())) {
            order.setWechatOrderId(remoteOrder.getWechatOrderId().trim());
        }
        if (!isBlank(remoteOrder.getChannelOrderId())) {
            order.setWechatPayMchOrderNo(remoteOrder.getChannelOrderId().trim());
        }
        if (!isBlank(remoteOrder.getWxpayOrderId())) {
            order.setTransactionId(remoteOrder.getWxpayOrderId().trim());
        }
        LocalDateTime paidAt = resolveDateTime(remoteOrder.getPaidTime(), null);
        if (paidAt != null) {
            order.setPaidAt(paidAt);
        }
        if (remoteOrder.getStatus() != null && remoteOrder.getStatus() == WECHAT_ORDER_STATUS_PROVIDING) {
            order.setProvideStatus(PROVIDE_STATUS_PROCESSING);
        }
        if (remoteOrder.getStatus() != null && remoteOrder.getStatus() == WECHAT_ORDER_STATUS_PROVIDED) {
            order.setProvideStatus(PROVIDE_STATUS_SUCCESS);
            order.setProvidedAt(resolveDateTime(remoteOrder.getProvideTime(), LocalDateTime.now()));
        }
    }

    private void markOrderProviding(VipPaymentOrder order,
                                    String transactionId,
                                    String wechatPayMchOrderNo,
                                    String payChannel,
                                    LocalDateTime paidAt) {
        if (!isBlank(transactionId)) {
            order.setTransactionId(transactionId.trim());
        }
        if (!isBlank(wechatPayMchOrderNo)) {
            order.setWechatPayMchOrderNo(wechatPayMchOrderNo.trim());
        }
        if (!isBlank(payChannel)) {
            order.setPayChannel(payChannel.trim());
        }
        if (paidAt != null) {
            order.setPaidAt(paidAt);
        }
        order.setProvideStatus(PROVIDE_STATUS_PROCESSING);
        persistOrder(order);
    }

    private void markOrderPaid(VipPaymentOrder order,
                               String transactionId,
                               String wechatPayMchOrderNo,
                               String payChannel,
                               LocalDateTime paidAt,
                               String provideStatus,
                               LocalDateTime providedAt) {
        boolean alreadyPaid = STATUS_PAID.equals(order.getStatus());
        order.setStatus(STATUS_PAID);
        if (!isBlank(transactionId)) {
            order.setTransactionId(transactionId.trim());
        }
        if (!isBlank(wechatPayMchOrderNo)) {
            order.setWechatPayMchOrderNo(wechatPayMchOrderNo.trim());
        }
        if (!isBlank(payChannel)) {
            order.setPayChannel(payChannel.trim());
        }
        if (paidAt != null) {
            order.setPaidAt(paidAt);
        }
        if (!isBlank(provideStatus)) {
            order.setProvideStatus(provideStatus);
        }
        if (providedAt != null) {
            order.setProvidedAt(providedAt);
        }
        if (order.getProvidedAt() == null && PROVIDE_STATUS_SUCCESS.equals(order.getProvideStatus())) {
            order.setProvidedAt(LocalDateTime.now());
        }
        persistOrder(order);
        if (alreadyPaid) {
            return;
        }

        vipService.activatePaidYear(order.getUserId(), amountFenToYuan(order.getAmountFen()));
        notificationService.sendVipOpenedSuccessNotification(order.getUserId());
        log.info("虚拟支付开通VIP成功，outTradeNo={}, userId={}", order.getOutTradeNo(), order.getUserId());
    }

    private void markRefunded(VipPaymentOrder order) {
        if (STATUS_REFUNDED.equals(order.getStatus())) {
            return;
        }
        order.setStatus(STATUS_REFUNDED);
        persistOrder(order);
    }

    private void markClosed(VipPaymentOrder order) {
        if (STATUS_CLOSED.equals(order.getStatus())) {
            return;
        }
        order.setStatus(STATUS_CLOSED);
        persistOrder(order);
    }

    private void persistOrder(VipPaymentOrder order) {
        order.setUpdatedAt(LocalDateTime.now());
        vipPaymentOrderMapper.updateById(order);
    }

    private void validateNotify(VipPaymentOrder order, WechatVirtualPayNotifyResult notify) {
        if (!amountFenEquals(order.getAmountFen(), notify.getActualPrice())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信虚拟支付金额不匹配");
        }
        if (!stringEquals(order.getProductId(), notify.getProductId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信虚拟支付商品不匹配");
        }
        if (!stringEquals(order.getOpenId(), notify.getOpenId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信虚拟支付用户不匹配");
        }
        if (notify.getEnv() != null && !notify.getEnv().equals(order.getEnv())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信虚拟支付环境不匹配");
        }
    }

    private VipPaymentOrderStatusResponse toStatus(VipPaymentOrder order, VipInfoResponse vipInfo) {
        VipPaymentOrderStatusResponse response = new VipPaymentOrderStatusResponse();
        response.setOutTradeNo(order.getOutTradeNo());
        response.setAmountFen(order.getAmountFen());
        response.setPlanName(PLAN_NAME);
        response.setProductId(order.getProductId());
        response.setStatus(order.getStatus());
        response.setPaidAt(order.getPaidAt());
        response.setVipInfo(vipInfo);
        return response;
    }

    private String buildSignData(String offerId, String productId, VipPaymentOrder order) {
        JSONObject json = new JSONObject(true);
        json.put("offerId", offerId);
        json.put("buyQuantity", BUY_QUANTITY);
        json.put("env", order.getEnv());
        json.put("currencyType", CURRENCY_TYPE);
        json.put("productId", productId);
        json.put("goodsPrice", AMOUNT_FEN);
        json.put("outTradeNo", order.getOutTradeNo());
        json.put("attach", order.getAttach());
        return JSON.toJSONString(json);
    }

    private Integer resolveEnv() {
        Integer env = virtualPayProperties.getEnv();
        return env == null ? 0 : env;
    }

    private boolean amountFenEquals(Integer orderAmount, Integer notifyAmount) {
        return orderAmount != null && orderAmount.equals(notifyAmount);
    }

    private BigDecimal amountFenToYuan(Integer amountFen) {
        return BigDecimal.valueOf(amountFen, 2);
    }

    private LocalDateTime resolveDateTime(Long seconds, LocalDateTime defaultValue) {
        if (seconds == null || seconds <= 0) {
            return defaultValue;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault());
    }

    private String parseOpenid(String account) {
        if (account == null || !account.startsWith("wx_") || account.length() <= 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请使用微信登录后开通");
        }
        return account.substring(3);
    }

    private String newOutTradeNo() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "VIP" + LocalDateTime.now().format(TRADE_NO_TIME) + random;
    }

    private String newAttach(String userId) {
        return "vip:" + userId + ":" + System.currentTimeMillis();
    }

    private String requireProperty(String key, String value) {
        if (isBlank(value)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, key + " 未配置");
        }
        return value.trim();
    }

    private boolean stringEquals(String left, String right) {
        return left != null && left.equals(right);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
