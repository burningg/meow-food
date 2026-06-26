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
    void parseRawMaterialResponseKeepsCalorieEstimate() {
        String responseText = """
                {"materials":[{"name":"土豆","commonNames":["马铃薯","洋芋","Solanum tuberosum"],"steamTime":"约20分钟","boilTime":"约15分钟","fryTime":"约5分钟","bakeTime":"约30分钟","stirFryTime":"约4分钟","defaultHeatTemperature":"中火","allergenFlag":"无常见过敏原","calorieEstimate":"约77kcal/100g","nutritionInfo":"碳水、钾、维C、膳食纤维","substituteIngredients":"可用红薯","category":"蔬菜"}]}
                """;

        List<RawMaterial> materials = ReflectionTestUtils.invokeMethod(
                dishAiService,
                "parseRawMaterialResponse",
                responseText,
                List.of("土豆"));

        assertThat(materials).hasSize(1);
        assertThat(materials.get(0).getCalorieEstimate()).isEqualTo("约77kcal/100g");
        assertThat(materials.get(0).getNutritionInfo()).isEqualTo("碳水、钾、维C、膳食纤维");
    }

}
