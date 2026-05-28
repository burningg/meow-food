package com.panghu.food.auth;

import com.panghu.food.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class JwtTokenUtil {
    private static final long EXPIRE_MILLIS = 7L * 24 * 60 * 60 * 1000;

    @Value("${auth.jwt-secret:food-social-secret}")
    private String secret;

    public String createToken(Long userId) {
        long expiresAt = System.currentTimeMillis() + EXPIRE_MILLIS;
        String payload = userId + ":" + expiresAt;
        String signature = sign(payload);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
    }

    public Long parseUserId(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 3) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "登录态无效");
            }
            String payload = parts[0] + ":" + parts[1];
            if (!sign(payload).equals(parts[2])) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "登录态校验失败");
            }
            long expiresAt = Long.parseLong(parts[1]);
            if (System.currentTimeMillis() > expiresAt) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "登录已过期");
            }
            return Long.parseLong(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "登录态无效");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "生成登录态失败");
        }
    }
}
