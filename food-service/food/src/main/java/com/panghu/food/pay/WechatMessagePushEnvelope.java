package com.panghu.food.pay;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class WechatMessagePushEnvelope {
    @JSONField(name = "Encrypt")
    private String encrypt;
}
