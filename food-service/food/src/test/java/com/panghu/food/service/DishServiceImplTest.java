package com.panghu.food.service;

import com.panghu.food.dto.DishUpsertRequest;
import com.panghu.food.dto.IngredientItem;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.DishDetailResponse;
import com.panghu.food.entity.DishIngredient;
import com.panghu.food.mapper.BuddyCircleMemberMapper;
import com.panghu.food.mapper.DishIngredientMapper;
import com.panghu.food.mapper.DishMapper;
import com.panghu.food.mapper.DishVisibilityCircleMapper;
import com.panghu.food.mapper.DishStepMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class DishServiceImplTest {
    private final DishMapper dishMapper = mock(DishMapper.class);
    private final DishIngredientMapper dishIngredientMapper = mock(DishIngredientMapper.class);
    private final DishStepMapper dishStepMapper = mock(DishStepMapper.class);
    private final BuddyCircleMemberMapper buddyCircleMemberMapper = mock(BuddyCircleMemberMapper.class);
    private final DishVisibilityCircleMapper dishVisibilityCircleMapper = mock(DishVisibilityCircleMapper.class);
    private final MenuVisibilitySupport menuVisibilitySupport = mock(MenuVisibilitySupport.class);

    private final DishServiceImpl dishService = new DishServiceImpl();

    DishServiceImplTest() {
        ReflectionTestUtils.setField(dishService, "dishMapper", dishMapper);
        ReflectionTestUtils.setField(dishService, "dishIngredientMapper", dishIngredientMapper);
        ReflectionTestUtils.setField(dishService, "dishStepMapper", dishStepMapper);
        ReflectionTestUtils.setField(dishService, "buddyCircleMemberMapper", buddyCircleMemberMapper);
        ReflectionTestUtils.setField(dishService, "dishVisibilityCircleMapper", dishVisibilityCircleMapper);
        ReflectionTestUtils.setField(dishService, "menuVisibilitySupport", menuVisibilitySupport);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void getDishByIdAllowsViewerWhenVisibilitySupportGrantsAccess() {
        AuthContext.setUserId("viewer");
        when(dishMapper.selectDishDetailById("dish-1")).thenReturn(dish("dish-1", "owner"));
        when(menuVisibilitySupport.canViewDish(any(DishDetailResponse.class), any(), any(Boolean.class))).thenReturn(true);
        when(dishIngredientMapper.selectByDishId("dish-1")).thenReturn(List.of());
        when(dishStepMapper.selectByDishId("dish-1")).thenReturn(List.of());

        DishDetailResponse result = dishService.getDishById("dish-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("dish-1");
    }

    @Test
    void replaceChildrenKeepsIngredientWhenAmountIsBlank() {
        DishUpsertRequest request = new DishUpsertRequest();
        IngredientItem ingredientItem = new IngredientItem();
        ingredientItem.setName("土豆");
        ingredientItem.setAmount("");
        request.setIngredients(List.of(ingredientItem));

        ReflectionTestUtils.invokeMethod(dishService, "replaceChildren", "dish-1", request);

        ArgumentCaptor<DishIngredient> captor = ArgumentCaptor.forClass(DishIngredient.class);
        verify(dishIngredientMapper).insert(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("土豆");
        assertThat(captor.getValue().getAmount()).isEqualTo("");
    }

    @Test
    void createDishRejectsCircleVisibilityWithoutCircleIds() {
        AuthContext.setUserId("owner");
        DishUpsertRequest request = new DishUpsertRequest();
        request.setVisibility("circle");

        assertThatThrownBy(() -> dishService.createDish(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("指定圈子权限至少选择一个圈子");
    }

    @Test
    void createDishFallsBackToConcreteDefaultVisibilityInsteadOfPersistingInherit() {
        AuthContext.setUserId("owner");
        DishUpsertRequest request = new DishUpsertRequest();
        request.setName("红烧肉");
        request.setImage("img");
        request.setDescription("desc");
        request.setCategoryId("c1");
        request.setDifficulty("easy");
        request.setServings(2);
        when(menuVisibilitySupport.resolveDefaultVisibility("owner")).thenReturn("public");
        when(dishMapper.insert(any())).thenAnswer(invocation -> {
            var dish = invocation.getArgument(0, com.panghu.food.entity.Dish.class);
            dish.setId("dish-1");
            return 1;
        });
        com.panghu.food.entity.Dish storedDish = new com.panghu.food.entity.Dish();
        storedDish.setId("dish-1");
        storedDish.setOwnerUserId("owner");
        storedDish.setVisibility("public");
        when(dishMapper.selectById("dish-1")).thenReturn(storedDish);
        when(dishMapper.selectDishDetailById("dish-1")).thenReturn(dish("dish-1", "owner"));
        when(menuVisibilitySupport.resolveDish("owner", "public", "dish-1"))
                .thenReturn(new MenuVisibilitySupport.ResolvedDishVisibility("public", List.of(), "public", List.of()));
        when(menuVisibilitySupport.canViewDish(any(DishDetailResponse.class), any(), any(Boolean.class))).thenReturn(true);
        when(dishIngredientMapper.selectByDishId("dish-1")).thenReturn(List.of());
        when(dishStepMapper.selectByDishId("dish-1")).thenReturn(List.of());

        DishDetailResponse response = dishService.createDish(request);

        assertThat(response.getId()).isEqualTo("dish-1");
        ArgumentCaptor<com.panghu.food.entity.Dish> dishCaptor = ArgumentCaptor.forClass(com.panghu.food.entity.Dish.class);
        verify(dishMapper).insert(dishCaptor.capture());
        assertThat(dishCaptor.getValue().getVisibility()).isEqualTo("public");
    }

    @Test
    void createDishStoresEffectiveCircleVisibilityWithoutCreatingFeedData() {
        AuthContext.setUserId("owner");
        DishUpsertRequest request = new DishUpsertRequest();
        request.setName("红烧肉");
        request.setImage("img");
        request.setDescription("desc");
        request.setCategoryId("c1");
        request.setDifficulty("easy");
        request.setServings(2);
        request.setVisibility("circle");
        request.setVisibilityCircleIds(List.of("circle-1", "circle-2"));

        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(2L);
        when(dishMapper.insert(any())).thenAnswer(invocation -> {
            var dish = invocation.getArgument(0, com.panghu.food.entity.Dish.class);
            dish.setId("dish-1");
            return 1;
        });
        when(dishMapper.selectDishDetailById("dish-1")).thenReturn(dish("dish-1", "owner"));
        when(menuVisibilitySupport.canViewDish(any(DishDetailResponse.class), any(), any(Boolean.class))).thenReturn(true);
        when(dishIngredientMapper.selectByDishId("dish-1")).thenReturn(List.of());
        when(dishStepMapper.selectByDishId("dish-1")).thenReturn(List.of());

        DishDetailResponse response = dishService.createDish(request);

        assertThat(response.getId()).isEqualTo("dish-1");
        verify(dishVisibilityCircleMapper, times(2)).insert(any());
    }

    private DishDetailResponse dish(String id, String ownerUserId) {
        DishDetailResponse dish = new DishDetailResponse();
        dish.setId(id);
        dish.setOwnerUserId(ownerUserId);
        dish.setName("红烧肉");
        dish.setVisibility("friends");
        return dish;
    }
}
