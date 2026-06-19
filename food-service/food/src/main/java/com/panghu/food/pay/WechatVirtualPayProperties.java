package com.panghu.food.pay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.virtual-pay")
public class WechatVirtualPayProperties {
    private String appKey;
    private String sandboxAppKey;
    private Integer env;
    private String offerId;
    private String productId;
    private String payNotifyUrl;
}
