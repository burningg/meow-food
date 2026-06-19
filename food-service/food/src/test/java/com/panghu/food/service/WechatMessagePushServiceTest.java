package com.panghu.food.service;

import com.alibaba.fastjson.JSONObject;
import com.panghu.food.exception.ApiException;
import com.panghu.food.pay.WechatMessagePushProperties;
import com.panghu.food.pay.WechatVirtualPayClient;
import com.panghu.food.pay.WechatVirtualPayNotifyResult;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WechatMessagePushServiceTest {
    private static final String APP_ID = "wx1234567890";
    private static final String TOKEN = "push-token";
    private static final String ENCODING_AES_KEY = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

    private final WechatMessagePushProperties properties = new WechatMessagePushProperties();
    private final WechatVirtualPayClient wechatVirtualPayClient = mock(WechatVirtualPayClient.class);
    private final WechatMessagePushService service;

    WechatMessagePushServiceTest() {
        properties.setToken(TOKEN);
        properties.setEncodingAesKey(ENCODING_AES_KEY);
        service = new WechatMessagePushService(properties, wechatVirtualPayClient, APP_ID);
    }

    @Test
    void verifyUrlReturnsEchoStringWhenSignatureMatches() {
        String timestamp = "1714036504";
        String nonce = "1514711492";
        String echostr = "4375120948345356249";
        String signature = sha1Sorted(TOKEN, timestamp, nonce);

        String result = service.verifyUrl(signature, timestamp, nonce, echostr);

        assertThat(result).isEqualTo(echostr);
    }

    @Test
    void verifyUrlRejectsInvalidSignature() {
        assertThatThrownBy(() -> service.verifyUrl("bad", "1714036504", "1514711492", "echo"))
                .isInstanceOf(ApiException.class)
                .hasMessage("微信消息推送URL验签失败");
    }

    @Test
    void parseVirtualPayNotifyParsesEncryptedJsonPayload() throws Exception {
        String timestamp = "1714112445";
        String nonce = "415670741";
        String plainJson = "{\"Event\":\"xpay_goods_deliver_notify\",\"OpenId\":\"openid-1\",\"OutTradeNo\":\"trade-1\",\"Env\":1,\"GoodsInfo\":{\"ProductId\":\"product-vip\",\"ActualPrice\":290}}";
        String encrypt = encrypt(plainJson, APP_ID, ENCODING_AES_KEY);
        String msgSignature = sha1Sorted(TOKEN, timestamp, nonce, encrypt);
        WechatVirtualPayNotifyResult parsed = new WechatVirtualPayNotifyResult();
        parsed.setEvent("xpay_goods_deliver_notify");
        when(wechatVirtualPayClient.parsePayNotify(any(JSONObject.class))).thenReturn(parsed);

        WechatVirtualPayNotifyResult result = service.parseVirtualPayNotify(
                timestamp,
                nonce,
                "aes",
                msgSignature,
                "{\"Encrypt\":\"" + encrypt + "\"}");

        assertThat(result.getEvent()).isEqualTo("xpay_goods_deliver_notify");
        verify(wechatVirtualPayClient).parsePayNotify(any(JSONObject.class));
    }

    @Test
    void parseVirtualPayNotifyRejectsInvalidMsgSignature() throws Exception {
        String encrypt = encrypt("{\"Event\":\"xpay_goods_deliver_notify\"}", APP_ID, ENCODING_AES_KEY);

        assertThatThrownBy(() -> service.parseVirtualPayNotify(
                "1714112445",
                "415670741",
                "aes",
                "bad-signature",
                "{\"Encrypt\":\"" + encrypt + "\"}"))
                .isInstanceOf(ApiException.class)
                .hasMessage("微信消息推送消息签名校验失败");
    }

    @Test
    void parseVirtualPayNotifyRejectsAppIdMismatch() throws Exception {
        String timestamp = "1714112445";
        String nonce = "415670741";
        String encrypt = encrypt("{\"Event\":\"xpay_goods_deliver_notify\"}", "wx-other", ENCODING_AES_KEY);
        String msgSignature = sha1Sorted(TOKEN, timestamp, nonce, encrypt);

        assertThatThrownBy(() -> service.parseVirtualPayNotify(
                timestamp,
                nonce,
                "aes",
                msgSignature,
                "{\"Encrypt\":\"" + encrypt + "\"}"))
                .isInstanceOf(ApiException.class)
                .hasMessage("微信消息推送appid不匹配");
    }

    private String encrypt(String message, String appId, String encodingAesKey) throws Exception {
        byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
        byte[] random = "1234567890abcdef".getBytes(StandardCharsets.UTF_8);
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] appIdBytes = appId.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(random.length + 4 + messageBytes.length + appIdBytes.length);
        buffer.put(random);
        buffer.putInt(messageBytes.length);
        buffer.put(messageBytes);
        buffer.put(appIdBytes);
        byte[] plain = pkcs7Pad(buffer.array());

        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(aesKey, 0, 16));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain));
    }

    private byte[] pkcs7Pad(byte[] source) {
        int pad = 32 - (source.length % 32);
        if (pad == 0) {
            pad = 32;
        }
        byte[] target = new byte[source.length + pad];
        System.arraycopy(source, 0, target, 0, source.length);
        for (int index = source.length; index < target.length; index++) {
            target[index] = (byte) pad;
        }
        return target;
    }

    private String sha1Sorted(String... values) {
        List<String> parts = new ArrayList<>();
        Collections.addAll(parts, values);
        Collections.sort(parts);
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                hex.append(String.format("%02x", current));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
