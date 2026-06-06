package com.panghu.food.pay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayProperties {
    private String mchId;
    private String merchantSerialNumber;
    private String privateKeyPath;
    private String apiV3Key;
    private String notifyUrl;
}
