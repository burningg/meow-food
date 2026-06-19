package com.panghu.food.pay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.message-push")
public class WechatMessagePushProperties {
    private String token;
    private String encodingAesKey;
}
