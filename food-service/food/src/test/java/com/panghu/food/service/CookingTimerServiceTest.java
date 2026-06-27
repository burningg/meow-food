package com.panghu.food.service;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.component.WechatComponent;
import com.panghu.food.dto.CookingTimerCreateRequest;
import com.panghu.food.dto.CookingTimerCreateResponse;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserAccountMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CookingTimerServiceTest {
    private final UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
    private final WechatComponent wechatComponent = mock(WechatComponent.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final CookingTimerService service = new CookingTimerService(
            userAccountMapper,
            wechatComponent,
            restTemplate,
            scheduler);

    @AfterEach
    void tearDown() {
        service.shutdown();
        AuthContext.clear();
    }

    @Test
    void createTimerReturnsTimerIdAndRegistersTask() {
        AuthContext.setUserId("user-1");
        when(userAccountMapper.selectById("user-1")).thenReturn(user("user-1", "wx_openid-1"));

        CookingTimerCreateResponse response = service.createTimer(request());

        assertThat(response.getTimerId()).isNotBlank();
        assertThat(service.activeTimerCount()).isEqualTo(1);
    }

    @Test
    void cancelTimerRemovesTaskAndDoesNotSendMessage() {
        AuthContext.setUserId("user-1");
        when(userAccountMapper.selectById("user-1")).thenReturn(user("user-1", "wx_openid-1"));

        CookingTimerCreateResponse response = service.createTimer(request());
        service.cancelTimer(response.getTimerId());
        service.triggerTimerForTest(response.getTimerId());

        assertThat(service.activeTimerCount()).isZero();
        verify(restTemplate, never()).exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void cancelTimerRejectsOtherUserTask() {
        AuthContext.setUserId("user-1");
        when(userAccountMapper.selectById("user-1")).thenReturn(user("user-1", "wx_openid-1"));
        CookingTimerCreateResponse response = service.createTimer(request());

        AuthContext.setUserId("user-2");

        assertThatThrownBy(() -> service.cancelTimer(response.getTimerId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("不能取消别人的烹饪计时");
        assertThat(service.activeTimerCount()).isEqualTo(1);
    }

    @Test
    void failedWechatSendStillCleansTask() {
        AuthContext.setUserId("user-1");
        when(userAccountMapper.selectById("user-1")).thenReturn(user("user-1", "wx_openid-1"));
        when(wechatComponent.getAccessToken()).thenReturn("access-token");
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"errcode\":43101,\"errmsg\":\"user refuse to accept the msg\"}"));

        CookingTimerCreateResponse response = service.createTimer(request());
        service.triggerTimerForTest(response.getTimerId());

        assertThat(service.activeTimerCount()).isZero();
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        String body = String.valueOf(entityCaptor.getValue().getBody());
        assertThat(body).contains("\"touser\":\"openid-1\"");
        assertThat(body).contains("\"template_id\":\"6uWQ2nw0Wr1R0Tlpm1kxYf2G6NEokTXxBFNAw7jk34E\"");
        assertThat(body).contains("\"thing1\":{\"value\":\"番茄炒蛋\"}");
    }

    private CookingTimerCreateRequest request() {
        CookingTimerCreateRequest request = new CookingTimerCreateRequest();
        request.setDishName("番茄炒蛋");
        request.setStepText("小火炖 20 分钟");
        request.setSeconds(60);
        request.setPage("pages/dish/dish-detail?id=dish-1");
        return request;
    }

    private UserAccount user(String id, String account) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setAccount(account);
        return user;
    }
}
