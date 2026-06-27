package com.panghu.food.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.component.WechatComponent;
import com.panghu.food.dto.CookingTimerCreateRequest;
import com.panghu.food.dto.CookingTimerCreateResponse;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PreDestroy;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class CookingTimerService {
    private static final Logger log = LoggerFactory.getLogger(CookingTimerService.class);
    private static final String API_BASE_URL = "https://api.weixin.qq.com";
    private static final String SUBSCRIBE_SEND_URI = "/cgi-bin/message/subscribe/send";
    private static final String TEMPLATE_ID = "6uWQ2nw0Wr1R0Tlpm1kxYf2G6NEokTXxBFNAw7jk34E";
    private static final int MIN_SECONDS = 1;
    private static final int MAX_SECONDS = 4 * 60 * 60;
    private static final String WECHAT_ACCOUNT_PREFIX = "wx_";

    private final UserAccountMapper userAccountMapper;
    private final WechatComponent wechatComponent;
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService scheduler;
    private final Map<String, CookingTimerTask> timerTasks = new ConcurrentHashMap<>();

    @Autowired
    public CookingTimerService(UserAccountMapper userAccountMapper,
                               WechatComponent wechatComponent,
                               RestTemplate restTemplate) {
        this(userAccountMapper, wechatComponent, restTemplate, Executors.newScheduledThreadPool(2));
    }

    CookingTimerService(UserAccountMapper userAccountMapper,
                        WechatComponent wechatComponent,
                        RestTemplate restTemplate,
                        ScheduledExecutorService scheduler) {
        this.userAccountMapper = userAccountMapper;
        this.wechatComponent = wechatComponent;
        this.restTemplate = restTemplate;
        this.scheduler = scheduler;
    }

    public CookingTimerCreateResponse createTimer(CookingTimerCreateRequest request) {
        String userId = AuthContext.requireUserId();
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }

        String openId = parseOpenId(user.getAccount());
        int seconds = normalizeSeconds(request == null ? null : request.getSeconds());
        String timerId = UUID.randomUUID().toString();
        CookingTimerTask task = new CookingTimerTask(
                timerId,
                userId,
                openId,
                limitText(request == null ? null : request.getDishName(), "烹饪计时", 20),
                limitText(request == null ? null : request.getStepText(), "", 60),
                normalizePage(request == null ? null : request.getPage()));

        // 内存计时只负责本次运行期的长时间提醒；服务重启后任务会自然丢失。
        ScheduledFuture<?> future = scheduler.schedule(() -> triggerTimer(timerId), seconds, TimeUnit.SECONDS);
        task.setFuture(future);
        timerTasks.put(timerId, task);

        CookingTimerCreateResponse response = new CookingTimerCreateResponse();
        response.setTimerId(timerId);
        return response;
    }

    public void cancelTimer(String timerId) {
        if (isBlank(timerId)) {
            return;
        }
        String userId = AuthContext.requireUserId();
        CookingTimerTask task = timerTasks.get(timerId.trim());
        if (task == null) {
            return;
        }
        if (!userId.equals(task.getUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "不能取消别人的烹饪计时");
        }
        removeTimer(timerId.trim(), true);
    }

    private void triggerTimer(String timerId) {
        CookingTimerTask task = removeTimer(timerId, false);
        if (task == null) {
            return;
        }
        try {
            sendSubscribeMessage(task);
        } catch (Exception error) {
            log.warn("发送烹饪计时订阅消息失败，timerId={}", timerId, error);
        }
    }

    private void sendSubscribeMessage(CookingTimerTask task) {
        JSONObject payload = new JSONObject(true);
        payload.put("touser", task.getOpenId());
        payload.put("template_id", TEMPLATE_ID);
        if (!isBlank(task.getPage())) {
            payload.put("page", task.getPage());
        }
        payload.put("miniprogram_state", "formal");
        payload.put("lang", "zh_CN");

        JSONObject data = new JSONObject(true);
        data.put("thing1", valueObject(task.getDishName()));
        data.put("thing3", valueObject("计时结束啦，记得查看锅里的状态"));
        payload.put("data", data);

        URI uri = UriComponentsBuilder.fromHttpUrl(API_BASE_URL)
                .path(SUBSCRIBE_SEND_URI)
                .queryParam("access_token", wechatComponent.getAccessToken())
                .build()
                .encode()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                uri,
                HttpMethod.POST,
                new HttpEntity<>(JSON.toJSONString(payload), headers),
                String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "调用微信订阅消息接口失败");
        }
        JSONObject json = isBlank(response.getBody()) ? new JSONObject() : JSON.parseObject(response.getBody());
        Integer errCode = json.getInteger("errcode");
        if (errCode != null && errCode != 0) {
            String errMsg = json.getString("errmsg");
            throw new ApiException(HttpStatus.BAD_GATEWAY, isBlank(errMsg) ? "调用微信订阅消息接口失败" : errMsg.trim());
        }
    }

    private JSONObject valueObject(String value) {
        JSONObject object = new JSONObject(true);
        object.put("value", value);
        return object;
    }

    private CookingTimerTask removeTimer(String timerId, boolean cancelFuture) {
        CookingTimerTask task = timerTasks.remove(timerId);
        if (task != null && cancelFuture && task.getFuture() != null) {
            task.getFuture().cancel(false);
        }
        return task;
    }

    private String parseOpenId(String account) {
        if (isBlank(account) || !account.startsWith(WECHAT_ACCOUNT_PREFIX)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "当前账号无法接收微信订阅消息");
        }
        String openId = account.substring(WECHAT_ACCOUNT_PREFIX.length()).trim();
        if (openId.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "当前账号无法接收微信订阅消息");
        }
        return openId;
    }

    private int normalizeSeconds(Integer seconds) {
        if (seconds == null || seconds < MIN_SECONDS || seconds > MAX_SECONDS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "计时时长需在 10 秒到 4 小时之间");
        }
        return seconds;
    }

    private String normalizePage(String page) {
        if (isBlank(page)) {
            return null;
        }
        String value = page.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value.length() > 128 ? value.substring(0, 128) : value;
    }

    private String limitText(String text, String fallback, int maxLength) {
        String value = isBlank(text) ? fallback : text.trim();
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    int activeTimerCount() {
        return timerTasks.size();
    }

    void triggerTimerForTest(String timerId) {
        CookingTimerTask task = timerTasks.get(timerId);
        if (task != null && task.getFuture() != null) {
            task.getFuture().cancel(false);
        }
        triggerTimer(timerId);
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
        timerTasks.clear();
    }

    private static class CookingTimerTask {
        private final String timerId;
        private final String userId;
        private final String openId;
        private final String dishName;
        private final String stepText;
        private final String page;
        private ScheduledFuture<?> future;

        CookingTimerTask(String timerId, String userId, String openId, String dishName, String stepText, String page) {
            this.timerId = timerId;
            this.userId = userId;
            this.openId = openId;
            this.dishName = dishName;
            this.stepText = stepText;
            this.page = page;
        }

        String getTimerId() {
            return timerId;
        }

        String getUserId() {
            return userId;
        }

        String getOpenId() {
            return openId;
        }

        String getDishName() {
            return dishName;
        }

        String getStepText() {
            return stepText;
        }

        String getPage() {
            return page;
        }

        ScheduledFuture<?> getFuture() {
            return future;
        }

        void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }
    }
}
