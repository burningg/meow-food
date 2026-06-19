package com.panghu.food.pay;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.panghu.food.component.WechatComponent;
import com.panghu.food.exception.ApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class WechatVirtualPayClientImpl implements WechatVirtualPayClient {
    private static final String MODE_WECHAT = "WECHAT";
    private static final String MODE_APPLE = "APPLE";
    private static final String API_BASE_URL = "https://api.weixin.qq.com";
    private static final String QUERY_ORDER_URI = "/xpay/query_order";
    private static final String NOTIFY_PROVIDE_GOODS_URI = "/xpay/notify_provide_goods";

    private final WechatComponent wechatComponent;
    private final WechatVirtualPayProperties properties;
    private final RestTemplate restTemplate;

    public WechatVirtualPayClientImpl(WechatComponent wechatComponent,
                                      WechatVirtualPayProperties properties,
                                      RestTemplate restTemplate) {
        this.wechatComponent = wechatComponent;
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public String createAccessToken() {
        return wechatComponent.getAccessToken();
    }

    @Override
    public String signPay(String signData, Integer env) {
        return hmacSha256(resolveAppKey(env), "requestVirtualPayment&" + signData);
    }

    @Override
    public String signUser(String signData, String sessionKey) {
        if (sessionKey == null || sessionKey.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信登录态已失效，请重新登录后支付");
        }
        return hmacSha256(sessionKey.trim(), signData);
    }

    @Override
    public WechatVirtualPayOrderQueryResult queryOrder(String openId, Integer env, String orderId, String wechatOrderId) {
        if (openId == null || openId.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "查询虚拟支付订单缺少 openid");
        }
        if (isBlank(orderId) && isBlank(wechatOrderId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "查询虚拟支付订单缺少订单号");
        }

        JSONObject payload = new JSONObject(true);
        payload.put("openid", openId.trim());
        payload.put("env", env == null ? 0 : env);
        putPreferredOrderId(payload, orderId, wechatOrderId);

        JSONObject response = postJson(QUERY_ORDER_URI, payload, true);
        JSONObject order = objectValue(response, "order");
        if (order == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "微信虚拟支付查单结果缺少订单信息");
        }

        WechatVirtualPayOrderQueryResult result = new WechatVirtualPayOrderQueryResult();
        result.setOrderId(value(order, "order_id"));
        result.setWechatOrderId(value(order, "wx_order_id"));
        result.setChannelOrderId(value(order, "channel_order_id"));
        result.setWxpayOrderId(value(order, "wxpay_order_id"));
        result.setStatus(intValue(order, "status"));
        result.setPaidFee(intValue(order, "paid_fee"));
        result.setPaidTime(longValue(order, "paid_time"));
        result.setProvideTime(longValue(order, "provide_time"));
        result.setOrderType(intValue(order, "order_type"));
        return result;
    }

    @Override
    public void notifyProvideGoods(String orderId, String wechatOrderId, Integer env) {
        if (isBlank(orderId) && isBlank(wechatOrderId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "通知虚拟支付发货缺少订单号");
        }

        JSONObject payload = new JSONObject(true);
        putPreferredOrderId(payload, orderId, wechatOrderId);
        payload.put("env", env == null ? 0 : env);
        postJson(NOTIFY_PROVIDE_GOODS_URI, payload, false);
    }

    @Override
    public WechatVirtualPayNotifyResult parsePayNotify(String body) {
        if (body == null || body.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "虚拟支付回调内容不能为空");
        }
        JSONObject xml = XmlUtils.parseXmlToJson(body);
        String event = value(xml, "Event");
        if (event == null || event.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "虚拟支付回调事件不能为空");
        }

        WechatVirtualPayNotifyResult result = new WechatVirtualPayNotifyResult();
        result.setEvent(event.trim());
        result.setOpenId(value(xml, "OpenId"));
        result.setOutTradeNo(value(xml, "OutTradeNo"));
        result.setEnv(intValue(xml, "Env"));

        JSONObject wechatPayInfo = objectValue(xml, "WeChatPayInfo");
        if (wechatPayInfo != null) {
            result.setWechatPayMchOrderNo(value(wechatPayInfo, "MchOrderNo"));
            result.setTransactionId(value(wechatPayInfo, "TransactionId"));
        }

        JSONObject goodsInfo = objectValue(xml, "GoodsInfo");
        if (goodsInfo != null) {
            result.setQuantity(intValue(goodsInfo, "Quantity"));
            result.setOrigPrice(intValue(goodsInfo, "OrigPrice"));
            result.setActualPrice(intValue(goodsInfo, "ActualPrice"));
            result.setAttach(value(goodsInfo, "Attach"));
            result.setProductId(value(goodsInfo, "ProductId"));
        }

        String payChannel = MODE_WECHAT;
        if (objectValue(xml, "AppleSubscriptionInfo") != null) {
            payChannel = MODE_APPLE;
        }
        result.setPayChannel(payChannel);
        return result;
    }

    private JSONObject postJson(String uri, JSONObject payload, boolean includePaySig) {
        String body = JSON.toJSONString(payload);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_BASE_URL)
                .path(uri)
                .queryParam("access_token", createAccessToken());
        if (includePaySig) {
            builder.queryParam("pay_sig", signServerApi(uri, body, payload.getInteger("env")));
        }

        URI requestUri = builder.build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                requestUri,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "调用微信虚拟支付接口失败");
        }
        if (isBlank(response.getBody())) {
            return new JSONObject();
        }
        JSONObject json = JSON.parseObject(response.getBody());
        Integer errCode = json.getInteger("errcode");
        if (errCode != null && errCode != 0) {
            String errMsg = value(json, "errmsg");
            throw new ApiException(HttpStatus.BAD_GATEWAY, isBlank(errMsg) ? "调用微信虚拟支付接口失败" : errMsg.trim());
        }
        return json;
    }

    private String resolveAppKey(Integer env) {
        boolean sandbox = env != null && env == 1;
        String value = sandbox ? properties.getSandboxAppKey() : properties.getAppKey();
        if (value == null || value.trim().isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, sandbox ? "wechat.virtual-pay.sandbox-app-key 未配置" : "wechat.virtual-pay.app-key 未配置");
        }
        return value.trim();
    }

    private String signServerApi(String uri, String requestBody, Integer env) {
        return hmacSha256(resolveAppKey(env), uri + "&" + requestBody);
    }

    private void putPreferredOrderId(JSONObject payload, String orderId, String wechatOrderId) {
        if (!isBlank(wechatOrderId)) {
            payload.put("wx_order_id", wechatOrderId.trim());
            return;
        }
        if (!isBlank(orderId)) {
            payload.put("order_id", orderId.trim());
        }
    }

    private String hmacSha256(String key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format(Locale.ROOT, "%02x", current));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "虚拟支付签名失败");
        }
    }

    private JSONObject objectValue(JSONObject source, String key) {
        Object value = source.get(key);
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.startsWith("{")) {
                return JSON.parseObject(text);
            }
        }
        return null;
    }

    private String value(JSONObject source, String key) {
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer intValue(JSONObject source, String key) {
        String value = value(source, key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Integer.valueOf(value.trim());
    }

    private Long longValue(JSONObject source, String key) {
        String value = value(source, key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Long.valueOf(value.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
