package com.panghu.food.pay;

import com.panghu.food.component.WechatComponent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WechatVirtualPayClientImplTest {
    private final WechatComponent wechatComponent = mock(WechatComponent.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final WechatVirtualPayProperties properties = new WechatVirtualPayProperties();
    private final WechatVirtualPayClientImpl client;

    WechatVirtualPayClientImplTest() {
        properties.setAppKey("app-key");
        properties.setSandboxAppKey("sandbox-app-key");
        client = new WechatVirtualPayClientImpl(wechatComponent, properties, restTemplate);
    }

    @Test
    void queryOrderPrefersWechatOrderIdInRequestBody() {
        when(wechatComponent.getAccessToken()).thenReturn("access-token");
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"errcode\":0,\"order\":{\"status\":1}}"));

        client.queryOrder("openid-1", 1, "order-1", "wx-order-1");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        String body = String.valueOf(entityCaptor.getValue().getBody());
        assertThat(body).contains("\"wx_order_id\":\"wx-order-1\"");
        assertThat(body).doesNotContain("\"order_id\":\"order-1\"");
    }
}
