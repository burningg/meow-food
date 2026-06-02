package com.panghu.food.component;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.alibaba.fastjson.JSON;
import com.panghu.food.entity.WechatAuthVO;

@Service
public class WechatComponent {
    @Value("${wechat.base-url}")
    private String baseUrl;

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    @Autowired
    private RestTemplate restTemplate;

    public WechatAuthVO getWechatAuth(String code) {
        WechatAuthVO result = new WechatAuthVO();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment("sns", "jscode2session")
                .queryParam("appid", appid)
                .queryParam("secret", secret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code");

        URI uri = builder.build().encode().toUri();
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(null),
                String.class);

        result = JSON.parseObject(response.getBody(), WechatAuthVO.class);
        
        return result;
    }
}
