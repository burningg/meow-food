package com.panghu.food.service;

import com.panghu.food.dto.DishAiAnalysisResponse;
import com.panghu.food.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
}
