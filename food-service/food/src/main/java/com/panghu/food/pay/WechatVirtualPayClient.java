package com.panghu.food.pay;

import com.alibaba.fastjson.JSONObject;

public interface WechatVirtualPayClient {
    String createAccessToken();

    String signPay(String signData, Integer env);

    String signUser(String signData, String sessionKey);

    WechatVirtualPayOrderQueryResult queryOrder(String openId, Integer env, String orderId, String wechatOrderId);

    void notifyProvideGoods(String orderId, String wechatOrderId, Integer env);

    WechatVirtualPayNotifyResult parsePayNotify(JSONObject payload);
}
