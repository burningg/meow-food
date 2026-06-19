package com.panghu.food.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.panghu.food.exception.ApiException;
import com.panghu.food.pay.WechatMessagePushEnvelope;
import com.panghu.food.pay.WechatMessagePushProperties;
import com.panghu.food.pay.WechatVirtualPayClient;
import com.panghu.food.pay.WechatVirtualPayNotifyResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

@Service
public class WechatMessagePushService {
    private static final String ENCRYPT_TYPE_AES = "aes";
    private static final int BLOCK_SIZE = 32;
    private static final int RANDOM_PREFIX_LENGTH = 16;
    private static final int MESSAGE_LENGTH_BYTES = 4;

    private final WechatMessagePushProperties properties;
    private final WechatVirtualPayClient wechatVirtualPayClient;
    private final String appId;

    public WechatMessagePushService(WechatMessagePushProperties properties,
                                    WechatVirtualPayClient wechatVirtualPayClient,
                                    @Value("${wechat.appid}") String appId) {
        this.properties = properties;
        this.wechatVirtualPayClient = wechatVirtualPayClient;
        this.appId = appId;
    }

    public String verifyUrl(String signature, String timestamp, String nonce, String echostr) {
        if (isBlank(signature) || isBlank(timestamp) || isBlank(nonce) || echostr == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送URL校验参数不完整");
        }
        String expectedSignature = sha1Sorted(requireToken(), timestamp.trim(), nonce.trim());
        if (!expectedSignature.equals(signature.trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送URL验签失败");
        }
        return echostr;
    }

    public WechatVirtualPayNotifyResult parseVirtualPayNotify(String timestamp,
                                                              String nonce,
                                                              String encryptType,
                                                              String msgSignature,
                                                              String body) {
        if (isBlank(timestamp) || isBlank(nonce)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送缺少时间戳或随机数");
        }
        if (!ENCRYPT_TYPE_AES.equalsIgnoreCase(trimToEmpty(encryptType))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送加密类型不正确");
        }

        WechatMessagePushEnvelope envelope;
        try {
            envelope = JSON.parseObject(body, WechatMessagePushEnvelope.class);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送JSON解析失败");
        }
        if (envelope == null || isBlank(envelope.getEncrypt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送缺少Encrypt字段");
        }
        if (isBlank(msgSignature)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送缺少消息签名");
        }

        String encrypt = envelope.getEncrypt().trim();
        String expectedSignature = sha1Sorted(requireToken(), timestamp.trim(), nonce.trim(), encrypt);
        if (!expectedSignature.equals(msgSignature.trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送消息签名校验失败");
        }

        String plainText = decrypt(encrypt);
        JSONObject payload;
        try {
            payload = JSON.parseObject(plainText);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送解密后JSON解析失败");
        }
        return wechatVirtualPayClient.parsePayNotify(payload);
    }

    private String decrypt(String encrypt) {
        byte[] aesKey = decodeAesKey(requireEncodingAesKey());
        byte[] encryptedBytes;
        try {
            encryptedBytes = Base64.getDecoder().decode(encrypt);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送密文Base64解码失败");
        }

        byte[] plainBytes;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(aesKey, 0, 16));
            plainBytes = unpad(cipher.doFinal(encryptedBytes));
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送解密失败");
        }

        if (plainBytes.length < RANDOM_PREFIX_LENGTH + MESSAGE_LENGTH_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送明文结构不完整");
        }

        int messageLength = ByteBuffer.wrap(plainBytes, RANDOM_PREFIX_LENGTH, MESSAGE_LENGTH_BYTES)
                .order(ByteOrder.BIG_ENDIAN)
                .getInt();
        int messageStart = RANDOM_PREFIX_LENGTH + MESSAGE_LENGTH_BYTES;
        int appIdStart = messageStart + messageLength;
        if (messageLength < 0 || appIdStart > plainBytes.length) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送明文长度非法");
        }

        String actualAppId = new String(plainBytes, appIdStart, plainBytes.length - appIdStart, StandardCharsets.UTF_8);
        if (!appId.equals(actualAppId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送appid不匹配");
        }
        return new String(plainBytes, messageStart, messageLength, StandardCharsets.UTF_8);
    }

    private byte[] unpad(byte[] padded) {
        if (padded.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送明文为空");
        }
        int pad = padded[padded.length - 1] & 0xFF;
        if (pad < 1 || pad > BLOCK_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送PKCS7填充不合法");
        }
        int contentLength = padded.length - pad;
        if (contentLength < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信消息推送PKCS7填充长度非法");
        }
        byte[] result = new byte[contentLength];
        System.arraycopy(padded, 0, result, 0, contentLength);
        return result;
    }

    private byte[] decodeAesKey(String encodingAesKey) {
        try {
            return Base64.getDecoder().decode(encodingAesKey + "=");
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "wechat.message-push.encoding-aes-key 配置不合法");
        }
    }

    private String sha1Sorted(String... values) {
        List<String> parts = new ArrayList<>(values.length);
        for (String value : values) {
            parts.add(value);
        }
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
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "微信消息推送签名计算失败");
        }
    }

    private String requireToken() {
        if (isBlank(properties.getToken())) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "wechat.message-push.token 未配置");
        }
        return properties.getToken().trim();
    }

    private String requireEncodingAesKey() {
        if (isBlank(properties.getEncodingAesKey())) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "wechat.message-push.encoding-aes-key 未配置");
        }
        return properties.getEncodingAesKey().trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
