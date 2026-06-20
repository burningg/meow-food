package com.panghu.food.service;

import com.panghu.food.dto.DishAiAnalysisResponse;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DishAiServiceTest {
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final DishAiService dishAiService = new DishAiService(restTemplate);

    @TempDir
    Path tempDir;

    @Test
    void analyzeDishParsesStructuredResponse() throws Exception {
        File imageFile = tempDir.resolve("dish.jpg").toFile();
        Files.write(imageFile.toPath(), "image".getBytes());
        ReflectionTestUtils.setField(dishAiService, "basePath", tempDir.toString());
        ReflectionTestUtils.setField(dishAiService, "openaiApiKey", "test-key");
        ReflectionTestUtils.setField(dishAiService, "openaiBaseUrl", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(dishAiService, "openaiModel", "gpt-5.4");
        when(restTemplate.exchange(
                eq("https://api.openai.com/v1/responses"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"output_text\":\"{\\\"name\\\":\\\"红烧肉\\\",\\\"ingredients\\\":[{\\\"name\\\":\\\"五花肉\\\",\\\"amount\\\":\\\"500g\\\"}],\\\"steps\\\":[\\\"焯水\\\",\\\"炖煮\\\"]}\"}"));

        DishAiAnalysisResponse response = dishAiService.analyzeDish("http://localhost:8080/uploads/dish.jpg", "红烧肉");

        assertThat(response.getName()).isEqualTo("红烧肉");
        assertThat(response.getIngredients()).hasSize(1);
        assertThat(response.getSteps()).hasSize(2);
    }

    @Test
    void analyzeDishRejectsDirtyResponse() throws Exception {
        File imageFile = tempDir.resolve("dish.jpg").toFile();
        Files.write(imageFile.toPath(), "image".getBytes());
        ReflectionTestUtils.setField(dishAiService, "basePath", tempDir.toString());
        ReflectionTestUtils.setField(dishAiService, "openaiApiKey", "test-key");
        ReflectionTestUtils.setField(dishAiService, "openaiBaseUrl", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(dishAiService, "openaiModel", "gpt-5.4");
        when(restTemplate.exchange(
                eq("https://api.openai.com/v1/responses"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"output_text\":\"{}\"}"));

        assertThatThrownBy(() -> dishAiService.analyzeDish("http://localhost:8080/uploads/dish.jpg", null))
                .isInstanceOf(ApiException.class)
                .hasMessage("AI 识别失败，请稍后重试");
    }

    @Test
    void importDishParsesIngredientsAndStepsWithName() {
        ReflectionTestUtils.setField(dishAiService, "openaiApiKey", "test-key");
        ReflectionTestUtils.setField(dishAiService, "openaiBaseUrl", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(dishAiService, "openaiModel", "gpt-5.4");
        when(restTemplate.exchange(
                eq("https://api.openai.com/v1/responses"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"output_text\":\"{\\\"name\\\":\\\"不应填充\\\",\\\"ingredients\\\":[{\\\"name\\\":\\\"鸡腿\\\",\\\"amount\\\":\\\"2只\\\"}],\\\"steps\\\":[\\\"腌制\\\",\\\"焖煮\\\"]}\"}"));

        DishAiAnalysisResponse response = dishAiService.importDish("鸡腿切块，加土豆焖煮。", List.of());

        assertThat(response.getName()).isEqualTo("不应填充");
        assertThat(response.getIngredients()).hasSize(1);
        assertThat(response.getSteps()).hasSize(2);
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://api.openai.com/v1/responses"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(String.class));
        assertThat(String.valueOf(captor.getValue().getBody()))
                .contains("菜谱导入助手", "鸡腿切块")
                .doesNotContain("web_search", "tool_choice", "reasoning");
    }

    @Test
    void importDishEnablesWebSearchWhenTextContainsUrl() {
        ReflectionTestUtils.setField(dishAiService, "openaiApiKey", "test-key");
        ReflectionTestUtils.setField(dishAiService, "openaiBaseUrl", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(dishAiService, "openaiModel", "gpt-5.4");
        when(restTemplate.exchange(
                eq("https://api.openai.com/v1/responses"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"output_text\":\"{\\\"ingredients\\\":[{\\\"name\\\":\\\"鸡腿\\\",\\\"amount\\\":\\\"2只\\\"}],\\\"steps\\\":[\\\"腌制\\\"]}\"}"));

        dishAiService.importDish("https://example.com/recipe/123", List.of());

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://api.openai.com/v1/responses"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(String.class));
        assertThat(String.valueOf(captor.getValue().getBody()))
                .contains("web_search", "tool_choice=required", "reasoning={effort=low}",
                        "读取 URL 页面内容", "https://example.com/recipe/123")
                .doesNotContain("tool_choice=auto");
    }

    @Test
    void arrangePlanAddsTemperatureAndHistoryPreferenceToRequest() {
        ReflectionTestUtils.setField(dishAiService, "openaiApiKey", "test-key");
        ReflectionTestUtils.setField(dishAiService, "openaiBaseUrl", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(dishAiService, "openaiModel", "gpt-5.4");
        when(restTemplate.exchange(
                eq("https://api.openai.com/v1/responses"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"output_text\":\"{\\\"title\\\":\\\"今天午餐\\\",\\\"recipes\\\":[{\\\"dishId\\\":\\\"dish-1\\\",\\\"reason\\\":\\\"最近常吃，午餐也合适\\\"}]}\"}"));

        DishAiService.PlanArrangementAiResult response = dishAiService.arrangePlan(
                "午餐",
                LocalDate.of(2026, 6, 21),
                1,
                "荤素搭配",
                List.of(dishSummary("dish-1", "番茄牛腩")),
                List.of(new DishAiService.PlanArrangementHistory("周末拼饭", "2026-06-20", List.of("番茄牛腩"))),
                Map.of("dish-1", 3L));

        assertThat(response.recipes()).hasSize(1);
        assertThat(response.recipes().get(0).reason()).isEqualTo("最近常吃，午餐也合适");
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://api.openai.com/v1/responses"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(String.class));
        assertThat(String.valueOf(captor.getValue().getBody()))
                .contains("temperature=1.1", "重点根据用户历史口味", "\"historyCount\":3",
                        "每个 recipes 项都必须有 reason")
                .doesNotContain("web_search", "tool_choice");
    }

    private DishSummaryResponse dishSummary(String id, String name) {
        DishSummaryResponse dish = new DishSummaryResponse();
        dish.setId(id);
        dish.setName(name);
        dish.setCategoryName("家常菜");
        dish.setDescription("软烂下饭");
        dish.setCookTimeMinutes(45);
        dish.setDifficulty("中等");
        dish.setServings(2);
        dish.setCreatedAt(LocalDateTime.of(2026, 6, 20, 12, 0));
        dish.setIngredientNames(List.of("牛腩", "番茄"));
        return dish;
    }
}
