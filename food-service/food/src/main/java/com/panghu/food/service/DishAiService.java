package com.panghu.food.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.panghu.food.dto.DishAiAnalysisResponse;
import com.panghu.food.dto.IngredientItem;
import com.panghu.food.dto.StepItem;
import com.panghu.food.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DishAiService {
    private static final String AI_FAILURE_MESSAGE = "AI 识别失败，请稍后重试";

    @Value("${upload.base-path:./uploads}")
    private String basePath;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-5.4}")
    private String openaiModel;

    private final RestTemplate restTemplate;

    public DishAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public DishAiAnalysisResponse analyzeDish(String imageUrl, String dishName) {
        if (isBlank(openaiApiKey)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, AI_FAILURE_MESSAGE);
        }
        try {
            String imageDataUrl = toImageDataUrl(imageUrl);
            String responseText = requestOpenAi(imageDataUrl, dishName);
            return parseAiResponse(responseText);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, AI_FAILURE_MESSAGE);
        }
    }

    private String requestOpenAi(String imageDataUrl, String dishName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        List<Object> userContent = new ArrayList<>();
        userContent.add(inputText(buildPrompt(dishName)));
        userContent.add(inputImage(imageDataUrl));

        List<Object> input = new ArrayList<>();
        input.add(Map.of("role", "user", "content", userContent));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openaiModel);
        payload.put("input", input);

        ResponseEntity<String> response = restTemplate.exchange(
                openaiBaseUrl + "/responses",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class);

        if (!response.getStatusCode().is2xxSuccessful() || isBlank(response.getBody())) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, AI_FAILURE_MESSAGE);
        }

        return extractText(response.getBody());
    }

    private DishAiAnalysisResponse parseAiResponse(String responseText) {
        String normalized = stripMarkdownFence(responseText);
        JSONObject json = JSON.parseObject(normalized);
        DishAiAnalysisResponse response = new DishAiAnalysisResponse();

        String name = trimToNull(json.getString("name"));
        response.setName(name);

        JSONArray ingredients = json.getJSONArray("ingredients");
        if (ingredients != null) {
            List<IngredientItem> items = new ArrayList<>();
            for (int i = 0; i < ingredients.size(); i++) {
                JSONObject item = ingredients.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                String ingredientName = trimToNull(item.getString("name"));
                if (ingredientName == null) {
                    continue;
                }
                IngredientItem ingredient = new IngredientItem();
                ingredient.setName(ingredientName);
                ingredient.setAmount(defaultString(trimToNull(item.getString("amount")), "适量"));
                ingredient.setSort(i + 1);
                items.add(ingredient);
            }
            response.setIngredients(items);
        }

        JSONArray steps = json.getJSONArray("steps");
        if (steps != null) {
            List<StepItem> items = new ArrayList<>();
            for (int i = 0; i < steps.size(); i++) {
                String content = trimToNull(steps.getString(i));
                if (content == null) {
                    continue;
                }
                StepItem step = new StepItem();
                step.setStepNo(i + 1);
                step.setContent(content);
                items.add(step);
            }
            response.setSteps(items);
        }

        if (response.getIngredients().isEmpty() && response.getSteps().isEmpty() && response.getName() == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, AI_FAILURE_MESSAGE);
        }
        return response;
    }

    private String extractText(String responseBody) {
        JSONObject json = JSON.parseObject(responseBody);
        String outputText = trimToNull(json.getString("output_text"));
        if (outputText != null) {
            return outputText;
        }

        JSONArray output = json.getJSONArray("output");
        if (output == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, AI_FAILURE_MESSAGE);
        }
        for (int i = 0; i < output.size(); i++) {
            JSONObject item = output.getJSONObject(i);
            if (item == null) {
                continue;
            }
            JSONArray content = item.getJSONArray("content");
            if (content == null) {
                continue;
            }
            for (int j = 0; j < content.size(); j++) {
                JSONObject block = content.getJSONObject(j);
                if (block == null) {
                    continue;
                }
                String text = trimToNull(block.getString("text"));
                if (text != null) {
                    return text;
                }
            }
        }
        throw new ApiException(HttpStatus.BAD_GATEWAY, AI_FAILURE_MESSAGE);
    }

    private String buildPrompt(String dishName) {
        StringBuilder builder = new StringBuilder();
        builder.append("请根据图片识别这道菜，并结合菜名信息输出严格 JSON，不要输出 Markdown，不要输出解释。");
        builder.append(" JSON 格式必须是：");
        builder.append("{\"name\":\"菜名\",\"ingredients\":[{\"name\":\"食材名\",\"amount\":\"用量\"}],\"steps\":[\"步骤1\",\"步骤2\"]}");
        builder.append(" 如果图片无法完全判断，请给出尽量合理的家常菜做法。");
        if (!isBlank(dishName)) {
            builder.append(" 用户提供的菜名是：").append(dishName.trim()).append("。");
        } else {
            builder.append(" 用户没有提供菜名，请你自行判断。");
        }
        return builder.toString();
    }

    private Map<String, Object> inputText(String text) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "input_text");
        block.put("text", text);
        return block;
    }

    private Map<String, Object> inputImage(String imageUrl) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "input_image");
        block.put("image_url", imageUrl);
        return block;
    }

    private String toImageDataUrl(String imageUrl) throws IOException {
        String fileName = resolveFileName(imageUrl);
        File file = new File(basePath, fileName);
        if (!file.exists() || !file.isFile()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先上传菜品图片");
        }
        String extension = extension(fileName);
        String mimeType = "jpg".equals(extension) || "jpeg".equals(extension)
                ? "image/jpeg"
                : "image/" + extension;
        byte[] bytes = Files.readAllBytes(file.toPath());
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private String resolveFileName(String imageUrl) {
        if (isBlank(imageUrl)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先上传菜品图片");
        }
        String normalized = imageUrl.trim();
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == normalized.length() - 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先上传菜品图片");
        }
        return normalized.substring(lastSlash + 1);
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "jpeg";
        }
        return fileName.substring(index + 1).toLowerCase();
    }

    private String stripMarkdownFence(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return text;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String defaultString(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
