package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.dto.VipInfoResponse;
import com.panghu.food.dto.VipPaymentOrderResponse;
import com.panghu.food.dto.VipPaymentOrderStatusResponse;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.entity.VipPaymentOrder;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserAccountMapper;
import com.panghu.food.mapper.VipPaymentOrderMapper;
import com.panghu.food.pay.JsapiPaymentParams;
import com.panghu.food.pay.WechatPayClient;
import com.panghu.food.pay.WechatPayTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
public class VipPaymentService {
    private static final Logger log = LoggerFactory.getLogger(VipPaymentService.class);

    public static final String PLAN_CODE = "VIP_FIRST_YEAR";
    public static final String PLAN_NAME = "VIP 首年会员";
    public static final int AMOUNT_FEN = 290;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String TRADE_SUCCESS = "SUCCESS";
    private static final DateTimeFormatter TRADE_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VipPaymentOrderMapper vipPaymentOrderMapper;
    private final UserAccountMapper userAccountMapper;
    private final VipService vipService;
    private final WechatPayClient wechatPayClient;

    public VipPaymentService(VipPaymentOrderMapper vipPaymentOrderMapper,
                             UserAccountMapper userAccountMapper,
                             VipService vipService,
                             WechatPayClient wechatPayClient) {
        this.vipPaymentOrderMapper = vipPaymentOrderMapper;
        this.userAccountMapper = userAccountMapper;
        this.vipService = vipService;
        this.wechatPayClient = wechatPayClient;
    }

    @Transactional
    public VipPaymentOrderResponse createOrder(String userId) {
        VipInfoResponse vipInfo = vipService.getVipInfo(userId);
        if (Boolean.TRUE.equals(vipInfo.getVip())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "你已开通 VIP");
        }

        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        String openid = parseOpenid(user.getAccount());

        VipPaymentOrder order = new VipPaymentOrder();
        LocalDateTime now = LocalDateTime.now();
        order.setUserId(userId);
        order.setOutTradeNo(newOutTradeNo());
        order.setPlanCode(PLAN_CODE);
        order.setAmountFen(AMOUNT_FEN);
        order.setStatus(STATUS_PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        vipPaymentOrderMapper.insert(order);

        JsapiPaymentParams paymentParams = wechatPayClient.createJsapiPrepay(openid, order.getOutTradeNo(), PLAN_NAME, AMOUNT_FEN);
        order.setPrepayId(paymentParams.getPrepayId());
        order.setUpdatedAt(LocalDateTime.now());
        vipPaymentOrderMapper.updateById(order);

        VipPaymentOrderResponse response = new VipPaymentOrderResponse();
        response.setOutTradeNo(order.getOutTradeNo());
        response.setAmountFen(order.getAmountFen());
        response.setPlanName(PLAN_NAME);
        response.setTimeStamp(paymentParams.getTimeStamp());
        response.setNonceStr(paymentParams.getNonceStr());
        response.setPayPackage(paymentParams.getPayPackage());
        response.setSignType(paymentParams.getSignType());
        response.setPaySign(paymentParams.getPaySign());
        return response;
    }

    @Transactional
    public VipPaymentOrderStatusResponse getOrder(String userId, String outTradeNo) {
        VipPaymentOrder order = vipPaymentOrderMapper.selectOne(new QueryWrapper<VipPaymentOrder>()
                .eq("user_id", userId)
                .eq("out_trade_no", outTradeNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        return toStatus(order, vipService.getVipInfo(userId));
    }

    @Transactional
    public void handleNotify(Map<String, String> headers, String body) {
        try {
            WechatPayTransaction transaction = wechatPayClient.parseTransactionNotify(headers, body);
            log.info("微信支付回调验签成功，outTradeNo={}, transactionId={}, tradeState={}",
                    transaction.getOutTradeNo(), transaction.getTransactionId(), transaction.getTradeState());
            if (!TRADE_SUCCESS.equals(transaction.getTradeState())) {
                return;
            }

            VipPaymentOrder order = vipPaymentOrderMapper.selectOne(new QueryWrapper<VipPaymentOrder>()
                    .eq("out_trade_no", transaction.getOutTradeNo())
                    .last("LIMIT 1"));
            if (order == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VIP 支付订单不存在");
            }
            if (STATUS_PAID.equals(order.getStatus())) {
                return;
            }
            validateTransaction(order, transaction);

            LocalDateTime paidAt = transaction.getSuccessTime() == null ? LocalDateTime.now() : transaction.getSuccessTime();
            order.setStatus(STATUS_PAID);
            order.setTransactionId(transaction.getTransactionId());
            order.setPaidAt(paidAt);
            order.setUpdatedAt(LocalDateTime.now());
            vipPaymentOrderMapper.updateById(order);

            vipService.activatePaidYear(order.getUserId(), amountFenToYuan(order.getAmountFen()));
            log.info("VIP 支付回调处理成功，outTradeNo={}, userId={}", order.getOutTradeNo(), order.getUserId());
        } catch (RuntimeException e) {
            log.warn("微信支付回调处理失败，微信会继续重试，serial={}, bodyLength={}, reason={}: {}",
                    header(headers, "Wechatpay-Serial"), body == null ? 0 : body.length(),
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    private void validateTransaction(VipPaymentOrder order, WechatPayTransaction transaction) {
        if (!wechatPayClient.getMchId().equals(transaction.getMchid())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信支付商户号不匹配");
        }
        // 回调金额只和订单创建时落库的金额比对，避免套餐价格调整后误判已创建订单。
        if (!amountFenEquals(order.getAmountFen(), transaction.getAmountFen())) {
            log.warn("微信支付金额不匹配，outTradeNo={}, orderAmountFen={}, notifyAmountFen={}",
                    order.getOutTradeNo(), order.getAmountFen(), transaction.getAmountFen());
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信支付金额不匹配");
        }
        if (!PLAN_CODE.equals(order.getPlanCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VIP 套餐不匹配");
        }
    }

    private boolean amountFenEquals(Integer orderAmount, Integer notifyAmount) {
        return orderAmount != null && orderAmount.equals(notifyAmount);
    }

    private BigDecimal amountFenToYuan(Integer amountFen) {
        return BigDecimal.valueOf(amountFen, 2);
    }

    private String header(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        String value = headers.get(name);
        if (value != null) {
            return value;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private VipPaymentOrderStatusResponse toStatus(VipPaymentOrder order, VipInfoResponse vipInfo) {
        VipPaymentOrderStatusResponse response = new VipPaymentOrderStatusResponse();
        response.setOutTradeNo(order.getOutTradeNo());
        response.setAmountFen(order.getAmountFen());
        response.setPlanName(PLAN_NAME);
        response.setStatus(order.getStatus());
        response.setPaidAt(order.getPaidAt());
        response.setVipInfo(vipInfo);
        return response;
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
}
