package com.panghu.food.pay;

import java.util.Map;

public interface WechatPayClient {
    JsapiPaymentParams createJsapiPrepay(String openid, String outTradeNo, String description, Integer amountFen);

    WechatPayTransaction parseTransactionNotify(Map<String, String> headers, String body);

    String getMchId();
}
