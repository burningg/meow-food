package com.panghu.food.service;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.DishAiAnalysisRequest;
import com.panghu.food.dto.DishAiAnalysisResponse;
import com.panghu.food.dto.IngredientItem;
import com.panghu.food.dto.StepItem;
import com.panghu.food.dto.VipUsageResponse;
import com.panghu.food.exception.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DishServiceAiTest {
    private final VipService vipService = mock(VipService.class);
    private final DishAiService dishAiService = mock(DishAiService.class);
    private final DishServiceImpl dishService = new DishServiceImpl();

    DishServiceAiTest() {
        ReflectionTestUtils.setField(dishService, "vipService", vipService);
        ReflectionTestUtils.setField(dishService, "dishAiService", dishAiService);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void analyzeDishByAiRejectsMissingImage() {
        AuthContext.setUserId("user-1");
        DishAiAnalysisRequest request = new DishAiAnalysisRequest();

        assertThatThrownBy(() -> dishService.analyzeDishByAi(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("请先上传菜品图片");
    }

    @Test
    void analyzeDishByAiConsumesQuotaAfterSuccess() {
        AuthContext.setUserId("user-1");
        DishAiAnalysisRequest request = new DishAiAnalysisRequest();
        request.setImage("http://localhost:8080/uploads/dish.jpg");
        request.setName("番茄炒蛋");

        DishAiAnalysisResponse aiResponse = new DishAiAnalysisResponse();
        IngredientItem ingredient = new IngredientItem();
        ingredient.setName("鸡蛋");
        ingredient.setAmount("2个");
        aiResponse.setIngredients(List.of(ingredient));
        StepItem step = new StepItem();
        step.setContent("炒匀");
        aiResponse.setSteps(List.of(step));
        VipUsageResponse usage = new VipUsageResponse();
        usage.setDailyLimit(3);
        usage.setUsedToday(1);
        usage.setRemainingToday(2);

        doNothing().when(vipService).assertCanUseAi("user-1");
        when(dishAiService.analyzeDish(request.getImage(), request.getName())).thenReturn(aiResponse);
        when(vipService.consumeAiUsage("user-1")).thenReturn(usage);

        DishAiAnalysisResponse response = dishService.analyzeDishByAi(request);

        assertThat(response.getUsage().getRemainingToday()).isEqualTo(2);
        verify(vipService).assertCanUseAi("user-1");
        verify(vipService).consumeAiUsage("user-1");
    }
}
