package com.panghu.food.pay;

import com.panghu.food.exception.ApiException;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;

@Service
public class WechatPayClientImpl implements WechatPayClient {
    private final WechatPayProperties properties;
    private final String appid;

    private RSAAutoCertificateConfig config;
    private JsapiServiceExtension jsapiService;
    private NotificationParser notificationParser;

    public WechatPayClientImpl(WechatPayProperties properties, @Value("${wechat.appid:}") String appid) {
        this.properties = properties;
        this.appid = appid;
    }

    @Override
    public JsapiPaymentParams createJsapiPrepay(String openid, String outTradeNo, String description, Integer amountFen) {
        PrepayRequest request = new PrepayRequest();
        request.setAppid(requireConfig("wechat.appid", appid));
        request.setMchid(requireConfig("wechat.pay.mch-id", properties.getMchId()));
        request.setDescription(description);
        request.setOutTradeNo(outTradeNo);
        request.setNotifyUrl(requireConfig("wechat.pay.notify-url", properties.getNotifyUrl()));

        Amount amount = new Amount();
        amount.setTotal(amountFen);
        amount.setCurrency("CNY");
        request.setAmount(amount);

        Payer payer = new Payer();
        payer.setOpenid(openid);
        request.setPayer(payer);

        PrepayWithRequestPaymentResponse response = jsapiService().prepayWithRequestPayment(request);
        JsapiPaymentParams params = new JsapiPaymentParams();
        params.setTimeStamp(response.getTimeStamp());
        params.setNonceStr(response.getNonceStr());
        params.setPayPackage(response.getPackageVal());
        params.setSignType(response.getSignType());
        params.setPaySign(response.getPaySign());
        params.setPrepayId(parsePrepayId(response.getPackageVal()));
        return params;
    }

    @Override
    public WechatPayTransaction parseTransactionNotify(Map<String, String> headers, String body) {
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(header(headers, "Wechatpay-Serial"))
                .timestamp(header(headers, "Wechatpay-Timestamp"))
                .nonce(header(headers, "Wechatpay-Nonce"))
                .signature(header(headers, "Wechatpay-Signature"))
                .signType("WECHATPAY2-SHA256-RSA2048")
                .body(body)
                .build();
        Transaction transaction = notificationParser().parse(requestParam, Transaction.class);

        WechatPayTransaction result = new WechatPayTransaction();
        result.setOutTradeNo(transaction.getOutTradeNo());
        result.setTransactionId(transaction.getTransactionId());
        result.setTradeState(transaction.getTradeState() == null ? null : transaction.getTradeState().name());
        result.setMchid(transaction.getMchid());
        result.setOpenid(transaction.getPayer() == null ? null : transaction.getPayer().getOpenid());
        result.setAmountFen(transaction.getAmount() == null ? null : transaction.getAmount().getTotal());
        result.setSuccessTime(parseSuccessTime(transaction.getSuccessTime()));
        return result;
    }

    @Override
    public String getMchId() {
        return requireConfig("wechat.pay.mch-id", properties.getMchId());
    }

    private JsapiServiceExtension jsapiService() {
        if (jsapiService == null) {
            jsapiService = new JsapiServiceExtension.Builder()
                    .config(config())
                    .signType("RSA")
                    .build();
        }
        return jsapiService;
    }

    private NotificationParser notificationParser() {
        if (notificationParser == null) {
            notificationParser = new NotificationParser(config());
        }
        return notificationParser;
    }

    private RSAAutoCertificateConfig config() {
        if (config == null) {
            config = new RSAAutoCertificateConfig.Builder()
                    .merchantId(requireConfig("wechat.pay.mch-id", properties.getMchId()))
                    .merchantSerialNumber(requireConfig("wechat.pay.merchant-serial-number", properties.getMerchantSerialNumber()))
                    .privateKeyFromPath(requireConfig("wechat.pay.private-key-path", properties.getPrivateKeyPath()))
                    .apiV3Key(requireConfig("wechat.pay.api-v3-key", properties.getApiV3Key()))
                    .build();
        }
        return config;
    }

    private String header(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        String value = headers.get(name);
        if (value != null) {
            return value;
        }
        String lowerName = name.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).equals(lowerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String requireConfig(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, key + " 未配置");
        }
        return value.trim();
    }

    private String parsePrepayId(String payPackage) {
        if (payPackage == null) {
            return null;
        }
        String prefix = "prepay_id=";
        return payPackage.startsWith(prefix) ? payPackage.substring(prefix.length()) : payPackage;
    }

    private LocalDateTime parseSuccessTime(String successTime) {
        if (successTime == null || successTime.trim().isEmpty()) {
            return null;
        }
        return OffsetDateTime.parse(successTime).toLocalDateTime();
    }
}
