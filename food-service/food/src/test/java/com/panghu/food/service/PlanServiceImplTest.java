package com.panghu.food.service;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.dto.PlanCreateRequest;
import com.panghu.food.dto.PlanDetailResponse;
import com.panghu.food.dto.PlanRecipesUpdateRequest;
import com.panghu.food.dto.PlanShoppingListResponse;
import com.panghu.food.entity.BuddyCircle;
import com.panghu.food.entity.CirclePlan;
import com.panghu.food.entity.CirclePlanRecipe;
import com.panghu.food.entity.CirclePlanShoppingItem;
import com.panghu.food.entity.CirclePlanShoppingItemSource;
import com.panghu.food.entity.CirclePlanShoppingList;
import com.panghu.food.entity.Dish;
import com.panghu.food.entity.DishIngredient;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.BuddyCircleMapper;
import com.panghu.food.mapper.BuddyCircleMemberMapper;
import com.panghu.food.mapper.CirclePlanMapper;
import com.panghu.food.mapper.CirclePlanRecipeMapper;
import com.panghu.food.mapper.CirclePlanShoppingItemMapper;
import com.panghu.food.mapper.CirclePlanShoppingItemSourceMapper;
import com.panghu.food.mapper.CirclePlanShoppingListMapper;
import com.panghu.food.mapper.DishIngredientMapper;
import com.panghu.food.mapper.DishMapper;
import com.panghu.food.mapper.UserAccountMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanServiceImplTest {
    private final CirclePlanMapper circlePlanMapper = mock(CirclePlanMapper.class);
    private final CirclePlanRecipeMapper circlePlanRecipeMapper = mock(CirclePlanRecipeMapper.class);
    private final CirclePlanShoppingListMapper circlePlanShoppingListMapper = mock(CirclePlanShoppingListMapper.class);
    private final CirclePlanShoppingItemMapper circlePlanShoppingItemMapper = mock(CirclePlanShoppingItemMapper.class);
    private final CirclePlanShoppingItemSourceMapper circlePlanShoppingItemSourceMapper = mock(CirclePlanShoppingItemSourceMapper.class);
    private final BuddyCircleMapper buddyCircleMapper = mock(BuddyCircleMapper.class);
    private final BuddyCircleMemberMapper buddyCircleMemberMapper = mock(BuddyCircleMemberMapper.class);
    private final DishMapper dishMapper = mock(DishMapper.class);
    private final DishIngredientMapper dishIngredientMapper = mock(DishIngredientMapper.class);
    private final UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
    private final MenuVisibilitySupport menuVisibilitySupport = mock(MenuVisibilitySupport.class);

    private final PlanServiceImpl planService = new PlanServiceImpl(
            circlePlanMapper,
            circlePlanRecipeMapper,
            circlePlanShoppingListMapper,
            circlePlanShoppingItemMapper,
            circlePlanShoppingItemSourceMapper,
            buddyCircleMapper,
            buddyCircleMemberMapper,
            dishMapper,
            dishIngredientMapper,
            userAccountMapper,
            menuVisibilitySupport);

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void createPlanAllowsMultiplePlansOnSameDay() {
        AuthContext.setUserId("viewer");
        AtomicInteger idCounter = new AtomicInteger(1);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(null);
        when(userAccountMapper.selectById("viewer")).thenReturn(user("viewer", "胖虎"));
        when(circlePlanMapper.insert(any(CirclePlan.class))).thenAnswer(invocation -> {
            CirclePlan plan = invocation.getArgument(0);
            plan.setId("plan-" + idCounter.getAndIncrement());
            return 1;
        });

        PlanCreateRequest request = new PlanCreateRequest();
        request.setCircleId("circle-1");
        request.setPlanDate(LocalDate.of(2026, 6, 5));
        request.setTitle("周五下班拼饭局");

        PlanDetailResponse first = planService.createPlan(request);
        request.setTitle("夜宵加班补给");
        PlanDetailResponse second = planService.createPlan(request);

        assertThat(first.getId()).isEqualTo("plan-1");
        assertThat(second.getId()).isEqualTo("plan-2");
        verify(circlePlanMapper, times(2)).insert(any(CirclePlan.class));
    }

    @Test
    void getPlanDetailRejectsUserOutsideCircle() {
        AuthContext.setUserId("viewer");
        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan("plan-1", "circle-1", "creator"));
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> planService.getPlanDetail("plan-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("你还不在这个搭子圈里");
    }

    @Test
    void addRecipesSkipsDuplicateDish() {
        AuthContext.setUserId("viewer");
        CirclePlan plan = plan("plan-1", "circle-1", "viewer");
        CirclePlanRecipe existing = recipe("plan-1", "dish-1");
        existing.setAddedByUserId("viewer");
        DishSummaryResponse dish = dishSummary("dish-1", "viewer", LocalDateTime.of(2026, 6, 1, 10, 0));

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of(existing));
        when(dishMapper.selectByIds(any())).thenReturn(List.of(dish));
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(null);
        when(userAccountMapper.selectById("viewer")).thenReturn(user("viewer", "胖虎"));
        when(userAccountMapper.selectBatchIds(any())).thenReturn(List.of(user("viewer", "胖虎")));
        doNothing().when(menuVisibilitySupport).hydrateSummaries(anyList());
        when(menuVisibilitySupport.canViewDish(any(DishSummaryResponse.class), anyString(), anyBoolean())).thenReturn(true);

        PlanRecipesUpdateRequest request = new PlanRecipesUpdateRequest();
        request.setDishIds(List.of("dish-1"));

        PlanDetailResponse response = planService.addRecipes("plan-1", request);

        assertThat(response.getRecipes()).hasSize(1);
        assertThat(response.getRecipes().get(0).getAddedByNickname()).isEqualTo("胖虎");
        verify(circlePlanRecipeMapper, never()).insert(any(CirclePlanRecipe.class));
    }

    @Test
    void startShoppingListAggregatesIngredientsByNameAndKeepsSources() {
        AuthContext.setUserId("viewer");
        CirclePlan plan = plan("plan-1", "circle-1", "viewer");
        List<CirclePlanRecipe> planRecipes = List.of(recipe("plan-1", "dish-1"), recipe("plan-1", "dish-2"));
        List<Dish> dishes = List.of(dishEntity("dish-1", "红烧肉"), dishEntity("dish-2", "炒土豆丝"));
        List<DishIngredient> ingredients = List.of(
                ingredient("dish-1", "蒜", "1头", 0),
                ingredient("dish-2", "土豆", "2个", 0),
                ingredient("dish-2", "蒜", "适量", 1)
        );
        CirclePlanShoppingList[] storedList = {null};
        List<CirclePlanShoppingItem> storedItems = new ArrayList<>();
        List<CirclePlanShoppingItemSource> storedSources = new ArrayList<>();
        AtomicInteger itemIds = new AtomicInteger(1);
        AtomicInteger sourceIds = new AtomicInteger(1);

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(planRecipes);
        when(dishMapper.selectBatchIds(any())).thenReturn(dishes);
        when(circlePlanShoppingListMapper.selectOne(any())).thenAnswer(invocation -> storedList[0]);
        when(circlePlanShoppingItemMapper.selectList(any())).thenAnswer(invocation -> new ArrayList<>(storedItems));
        when(circlePlanShoppingItemSourceMapper.selectList(any())).thenAnswer(invocation -> new ArrayList<>(storedSources));
        when(dishIngredientMapper.selectList(any())).thenReturn(ingredients);
        when(userAccountMapper.selectById("viewer")).thenReturn(user("viewer", "胖虎"));
        when(circlePlanShoppingListMapper.insert(any(CirclePlanShoppingList.class))).thenAnswer(invocation -> {
            CirclePlanShoppingList shoppingList = invocation.getArgument(0);
            shoppingList.setId("list-1");
            storedList[0] = shoppingList;
            return 1;
        });
        when(circlePlanShoppingItemMapper.insert(any(CirclePlanShoppingItem.class))).thenAnswer(invocation -> {
            CirclePlanShoppingItem item = invocation.getArgument(0);
            item.setId("item-" + itemIds.getAndIncrement());
            storedItems.add(item);
            return 1;
        });
        when(circlePlanShoppingItemSourceMapper.insert(any(CirclePlanShoppingItemSource.class))).thenAnswer(invocation -> {
            CirclePlanShoppingItemSource source = invocation.getArgument(0);
            source.setId("source-" + sourceIds.getAndIncrement());
            storedSources.add(source);
            return 1;
        });

        PlanShoppingListResponse response = planService.startShoppingList("plan-1");

        assertThat(response.isShoppingStarted()).isTrue();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getIngredientName()).isEqualTo("蒜");
        assertThat(response.getItems().get(0).getSources()).extracting("dishName")
                .containsExactly("红烧肉", "炒土豆丝");
    }

    @Test
    void toggleShoppingItemStoresBuyerNickname() {
        AuthContext.setUserId("viewer");
        CirclePlan plan = plan("plan-1", "circle-1", "viewer");
        CirclePlanShoppingList shoppingList = new CirclePlanShoppingList();
        shoppingList.setId("list-1");
        shoppingList.setPlanId("plan-1");
        shoppingList.setStartedByUserId("viewer");
        shoppingList.setStartedAt(LocalDateTime.of(2026, 6, 5, 12, 0));
        shoppingList.setRestartCount(0);
        CirclePlanShoppingItem item = new CirclePlanShoppingItem();
        item.setId("item-1");
        item.setShoppingListId("list-1");
        item.setIngredientName("蒜");
        item.setPurchased(false);
        item.setSort(0);

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(shoppingList);
        when(circlePlanShoppingItemMapper.selectById("item-1")).thenReturn(item);
        when(circlePlanShoppingItemMapper.selectList(any())).thenAnswer(invocation -> List.of(item));
        when(circlePlanShoppingItemSourceMapper.selectList(any())).thenReturn(List.of());
        when(userAccountMapper.selectById("viewer")).thenReturn(user("viewer", "胖虎"));

        PlanShoppingListResponse response = planService.toggleShoppingItem("plan-1", "item-1");

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).isPurchased()).isTrue();
        assertThat(response.getItems().get(0).getPurchasedByNickname()).isEqualTo("胖虎");
    }

    @Test
    void deletePlanOnlyAllowsCreator() {
        AuthContext.setUserId("viewer");
        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan("plan-1", "circle-1", "creator"));
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> planService.deletePlan("plan-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("只有创建者可以删除计划");

        verify(circlePlanMapper, never()).deleteById(anyString());
    }

    private CirclePlan plan(String id, String circleId, String creatorUserId) {
        CirclePlan plan = new CirclePlan();
        plan.setId(id);
        plan.setCircleId(circleId);
        plan.setCreatorUserId(creatorUserId);
        plan.setPlanDate(LocalDate.of(2026, 6, 5));
        plan.setTitle("周五下班拼饭局");
        return plan;
    }

    private CirclePlanRecipe recipe(String planId, String dishId) {
        CirclePlanRecipe recipe = new CirclePlanRecipe();
        recipe.setPlanId(planId);
        recipe.setDishId(dishId);
        recipe.setCreatedAt(LocalDateTime.of(2026, 6, 5, 10, 0));
        return recipe;
    }

    private BuddyCircle circle(String id) {
        BuddyCircle circle = new BuddyCircle();
        circle.setId(id);
        circle.setName("周末探店局");
        return circle;
    }

    private UserAccount user(String id, String nickname) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setNickname(nickname);
        return user;
    }

    private DishSummaryResponse dishSummary(String id, String ownerUserId, LocalDateTime createdAt) {
        DishSummaryResponse dish = new DishSummaryResponse();
        dish.setId(id);
        dish.setOwnerUserId(ownerUserId);
        dish.setName("椒麻手撕鸡");
        dish.setCreatedAt(createdAt);
        dish.setEffectiveVisibility("public");
        return dish;
    }

    private Dish dishEntity(String id, String name) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setName(name);
        return dish;
    }

    private DishIngredient ingredient(String dishId, String name, String amount, int sort) {
        DishIngredient ingredient = new DishIngredient();
        ingredient.setDishId(dishId);
        ingredient.setName(name);
        ingredient.setAmount(amount);
        ingredient.setSort(sort);
        return ingredient;
    }
}
