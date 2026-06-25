package com.panghu.food.service;

import com.alibaba.fastjson.JSON;
import com.panghu.food.dto.DishAiAnalysisResponse;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.entity.RawMaterial;
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

    @Test
    void completeRawMaterialsAddsLowTemperaturePromptAndParsesResponse() {
        ReflectionTestUtils.setField(dishAiService, "openaiApiKey", "test-key");
        ReflectionTestUtils.setField(dishAiService, "openaiBaseUrl", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(dishAiService, "openaiModel", "gpt-5.4");
        String aiJson = "{\"materials\":["
                + "{\"name\":\"土豆\",\"commonNames\":[\"土豆\",\"马铃薯\",\"Solanum tuberosum\",\"洋芋\"],\"steamTime\":\"约15分钟\",\"boilTime\":\"约12分钟\",\"fryTime\":\"约5分钟\",\"bakeTime\":\"约25分钟\",\"stirFryTime\":\"约6分钟\",\"defaultHeatTemperature\":\"中火\",\"allergenFlag\":\"无常见过敏原\",\"nutritionInfo\":\"富含碳水和钾\",\"substituteIngredients\":\"可用红薯\",\"category\":\"蔬菜\"},"
                + "{\"name\":\"鸡蛋\",\"commonNames\":[\"鸡蛋\",\"蛋\",\"egg\"],\"steamTime\":\"约8分钟\",\"boilTime\":\"约6分钟\",\"fryTime\":\"约3分钟\",\"bakeTime\":\"不建议\",\"stirFryTime\":\"约2分钟\",\"defaultHeatTemperature\":\"中小火\",\"allergenFlag\":\"含蛋类过敏原\",\"nutritionInfo\":\"富含优质蛋白\",\"substituteIngredients\":\"可用豆腐\",\"category\":\"蛋类\"},"
                + "{\"name\":\"鸡腿肉\",\"commonNames\":[\"鸡腿肉\",\"鸡腿\",\"鸡肉\"],\"steamTime\":\"约20分钟\",\"boilTime\":\"约15分钟\",\"fryTime\":\"约8分钟\",\"bakeTime\":\"约25分钟\",\"stirFryTime\":\"约7分钟\",\"defaultHeatTemperature\":\"中火\",\"allergenFlag\":\"无常见过敏原\",\"nutritionInfo\":\"富含蛋白质\",\"substituteIngredients\":\"可用鸡胸肉\",\"category\":\"肉\"},"
                + "{\"name\":\"牛肉\",\"commonNames\":[\"牛肉\",\"beef\",\"黄牛肉\"],\"boilTime\":\"约40分钟\",\"fryTime\":\"约5分钟\",\"bakeTime\":\"约20分钟\",\"stirFryTime\":\"约4分钟\",\"defaultHeatTemperature\":\"大火\",\"allergenFlag\":\"无常见过敏原\",\"nutritionInfo\":\"富含蛋白质\",\"substituteIngredients\":\"可用羊肉\",\"category\":\"肉\"}"
                + "]}";
        when(restTemplate.exchange(
                eq("https://api.openai.com/v1/responses"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"output_text\":" + JSON.toJSONString(aiJson) + "}"));

        List<RawMaterial> materials = dishAiService.completeRawMaterials(List.of(" 土豆 ", "土豆", "鸡蛋", "牛肉"));

        assertThat(materials).hasSize(2);
        assertThat(materials.get(0).getName()).isEqualTo("土豆");
        assertThat(materials.get(0).getCommonNames()).isEqualTo("土豆,马铃薯,Solanum tuberosum,洋芋");
        assertThat(materials.get(0).getCategory()).isEqualTo("蔬菜");
        assertThat(materials.get(1).getName()).isEqualTo("鸡蛋");
        assertThat(materials.get(1).getCommonNames()).isEqualTo("鸡蛋,蛋,egg");
        assertThat(materials.get(1).getCategory()).isEqualTo("其他");
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://api.openai.com/v1/responses"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(String.class));
        assertThat(String.valueOf(captor.getValue().getBody()))
                .contains("temperature=0.2", "输出严格 JSON", "\"materials\"", "\"commonNames\"",
                        "name 必须完全等于输入食材名", "至少 3 个", "不超过30字",
                        "食材名列表：[\"土豆\",\"鸡蛋\",\"牛肉\"]")
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
