package com.panghu.food.service;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.dto.PlanAiArrangeRequest;
import com.panghu.food.dto.PlanAiArrangementConfirmRequest;
import com.panghu.food.dto.PlanAiArrangementResponse;
import com.panghu.food.dto.PlanCreateRequest;
import com.panghu.food.dto.PlanDetailResponse;
import com.panghu.food.dto.PlanMonthResponse;
import com.panghu.food.dto.PlanRecipesUpdateRequest;
import com.panghu.food.dto.PlanRecipeCandidatesResponse;
import com.panghu.food.dto.PlanShoppingListResponse;
import com.panghu.food.dto.PlanAiUsageResponse;
import com.panghu.food.entity.BuddyCircle;
import com.panghu.food.entity.BuddyCircleMember;
import com.panghu.food.entity.CirclePlan;
import com.panghu.food.entity.CirclePlanRecipe;
import com.panghu.food.entity.CirclePlanShoppingItem;
import com.panghu.food.entity.CirclePlanShoppingItemSource;
import com.panghu.food.entity.CirclePlanShoppingList;
import com.panghu.food.entity.CirclePlanVisibleUser;
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
import com.panghu.food.mapper.CirclePlanVisibleUserMapper;
import com.panghu.food.mapper.DishIngredientMapper;
import com.panghu.food.mapper.DishMapper;
import com.panghu.food.mapper.UserAccountMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private final CirclePlanVisibleUserMapper circlePlanVisibleUserMapper = mock(CirclePlanVisibleUserMapper.class);
    private final BuddyCircleMapper buddyCircleMapper = mock(BuddyCircleMapper.class);
    private final BuddyCircleMemberMapper buddyCircleMemberMapper = mock(BuddyCircleMemberMapper.class);
    private final DishMapper dishMapper = mock(DishMapper.class);
    private final DishIngredientMapper dishIngredientMapper = mock(DishIngredientMapper.class);
    private final UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
    private final MenuVisibilitySupport menuVisibilitySupport = mock(MenuVisibilitySupport.class);
    private final DishAiService dishAiService = mock(DishAiService.class);
    private final VipService vipService = mock(VipService.class);

    private final PlanServiceImpl planService = new PlanServiceImpl(
            circlePlanMapper,
            circlePlanRecipeMapper,
            circlePlanShoppingListMapper,
            circlePlanShoppingItemMapper,
            circlePlanShoppingItemSourceMapper,
            circlePlanVisibleUserMapper,
            buddyCircleMapper,
            buddyCircleMemberMapper,
            dishMapper,
            dishIngredientMapper,
            userAccountMapper,
            menuVisibilitySupport,
            dishAiService,
            vipService);

    @BeforeEach
    void setUp() {
        when(circlePlanVisibleUserMapper.canUserViewPlan(anyString(), anyString())).thenReturn(1);
    }

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
        assertThat(first.getShoppingStatus()).isEqualTo(CirclePlan.SHOPPING_STATUS_NOT_STARTED);
        assertThat(second.getId()).isEqualTo("plan-2");
        verify(circlePlanMapper, times(2)).insert(any(CirclePlan.class));
    }

    @Test
    void createPlanStoresRestrictedVisibleUsersAndIncludesCreator() {
        AuthContext.setUserId("viewer");
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(
                member("circle-1", "member-1"),
                member("circle-1", "viewer")));
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(null);
        when(userAccountMapper.selectById("viewer")).thenReturn(user("viewer", "胖虎"));
        when(circlePlanMapper.insert(any(CirclePlan.class))).thenAnswer(invocation -> {
            CirclePlan plan = invocation.getArgument(0);
            plan.setId("plan-1");
            return 1;
        });

        PlanCreateRequest request = new PlanCreateRequest();
        request.setCircleId("circle-1");
        request.setPlanDate(LocalDate.of(2026, 6, 5));
        request.setTitle("周五下班拼饭局");
        request.setVisibleUserIds(List.of("member-1"));

        planService.createPlan(request);

        ArgumentCaptor<CirclePlanVisibleUser> captor = ArgumentCaptor.forClass(CirclePlanVisibleUser.class);
        verify(circlePlanVisibleUserMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(CirclePlanVisibleUser::getUserId)
                .containsExactly("member-1", "viewer");
        assertThat(captor.getAllValues()).extracting(CirclePlanVisibleUser::getPlanId)
                .containsOnly("plan-1");
    }

    @Test
    void createPlanRejectsVisibleUserOutsideCircle() {
        AuthContext.setUserId("viewer");
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(member("circle-1", "viewer")));

        PlanCreateRequest request = new PlanCreateRequest();
        request.setCircleId("circle-1");
        request.setPlanDate(LocalDate.of(2026, 6, 5));
        request.setTitle("周五下班拼饭局");
        request.setVisibleUserIds(List.of("outside-user"));

        assertThatThrownBy(() -> planService.createPlan(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("只能选择当前圈子里的用户");

        verify(circlePlanMapper, never()).insert(any(CirclePlan.class));
        verify(circlePlanVisibleUserMapper, never()).insert(any(CirclePlanVisibleUser.class));
    }

    @Test
    void arrangePlanByAiRejectsDishCountOutOfRange() {
        AuthContext.setUserId("viewer");
        PlanAiArrangeRequest request = aiArrangeRequest();
        request.setDishCount(9);

        assertThatThrownBy(() -> planService.arrangePlanByAi(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("菜数范围应为 1-8 道");
    }

    @Test
    void arrangePlanByAiRejectsEmptyVisibleDishes() {
        AuthContext.setUserId("viewer");
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(member("circle-1", "viewer")));
        when(dishMapper.selectByOwnerUserIds(anyList())).thenReturn(List.of());

        assertThatThrownBy(() -> planService.arrangePlanByAi(aiArrangeRequest()))
                .isInstanceOf(ApiException.class)
                .hasMessage("当前圈子暂无可安排的菜谱");
    }

    @Test
    void arrangePlanByAiFiltersInvisibleRecipeAndFallbacks() {
        AuthContext.setUserId("viewer");
        CirclePlan recentPlan = plan("plan-recent", "circle-1", "viewer");
        DishSummaryResponse first = dishSummary("dish-1", "viewer", LocalDateTime.of(2026, 6, 1, 10, 0));
        first.setName("番茄牛腩");
        DishSummaryResponse second = dishSummary("dish-2", "viewer", LocalDateTime.of(2026, 6, 2, 10, 0));
        second.setName("清炒藕带");
        CirclePlanRecipe historicalRecipe = recipe("plan-recent", "dish-1");
        PlanAiUsageResponse usage = new PlanAiUsageResponse();
        usage.setMonthlyLimit(30);
        usage.setUsedThisMonth(1);
        usage.setRemainingThisMonth(29);

        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(member("circle-1", "viewer")));
        when(dishMapper.selectByOwnerUserIds(anyList())).thenReturn(List.of(first, second));
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanMapper.selectRecentVisiblePlansByUserInCircle(eq("circle-1"), eq("viewer"), anyInt()))
                .thenReturn(List.of(recentPlan));
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of(historicalRecipe));
        when(dishAiService.arrangePlan(anyString(), any(), anyInt(), anyString(), anyList(), anyList(), anyMap()))
                .thenReturn(new DishAiService.PlanArrangementAiResult(
                        "今天午餐这样吃",
                        "饭团叼来一份清爽菜单。",
                        "热菜配蔬菜。",
                        "少油一点。",
                        List.of(
                                new DishAiService.PlanArrangementRecipe("dish-2", "清爽解腻"),
                                new DishAiService.PlanArrangementRecipe("dish-x", "不可见"))));
        doNothing().when(vipService).assertCanUsePlanAi("viewer");
        when(vipService.consumePlanAiUsage("viewer")).thenReturn(usage);

        PlanAiArrangeRequest request = aiArrangeRequest();
        request.setHealthAdvice(null);
        PlanAiArrangementResponse response = planService.arrangePlanByAi(request);

        assertThat(response.getRecipes()).hasSize(2);
        assertThat(response.getRecipes()).extracting(item -> item.getDish().getId())
                .containsExactly("dish-2", "dish-1");
        assertThat(response.getRecipes().get(0).getReason()).isEqualTo("清爽解腻");
        assertThat(response.getUsage().getRemainingThisMonth()).isEqualTo(29);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DishSummaryResponse>> candidatesCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Long>> historyCountCaptor = ArgumentCaptor.forClass(Map.class);
        verify(dishAiService).arrangePlan(
                anyString(),
                any(),
                anyInt(),
                eq("荤素搭配"),
                candidatesCaptor.capture(),
                anyList(),
                historyCountCaptor.capture());
        assertThat(candidatesCaptor.getValue()).extracting(DishSummaryResponse::getId)
                .containsExactly("dish-1", "dish-2");
        assertThat(historyCountCaptor.getValue()).containsEntry("dish-1", 1L);
        verify(vipService).consumePlanAiUsage("viewer");
    }

    @Test
    void confirmAiArrangementCreatesPlanWithVisibleRecipes() {
        AuthContext.setUserId("viewer");
        List<CirclePlanRecipe> storedRecipes = new ArrayList<>();
        DishSummaryResponse first = dishSummary("dish-1", "viewer", LocalDateTime.of(2026, 6, 1, 10, 0));
        DishSummaryResponse second = dishSummary("dish-2", "viewer", LocalDateTime.of(2026, 6, 2, 10, 0));

        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(member("circle-1", "viewer")));
        when(dishMapper.selectByOwnerUserIds(anyList())).thenReturn(List.of(first, second));
        when(dishMapper.selectByIds(any())).thenReturn(List.of(first, second));
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanRecipeMapper.selectList(any())).thenAnswer(invocation -> storedRecipes);
        when(circlePlanRecipeMapper.insert(any(CirclePlanRecipe.class))).thenAnswer(invocation -> {
            storedRecipes.add(invocation.getArgument(0));
            return 1;
        });
        when(circlePlanMapper.insert(any(CirclePlan.class))).thenAnswer(invocation -> {
            CirclePlan plan = invocation.getArgument(0);
            plan.setId("plan-ai");
            return 1;
        });
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(null);
        when(userAccountMapper.selectById("viewer")).thenReturn(user("viewer", "胖虎"));
        when(userAccountMapper.selectBatchIds(any())).thenReturn(List.of(user("viewer", "胖虎")));

        PlanAiArrangementConfirmRequest request = new PlanAiArrangementConfirmRequest();
        request.setCircleId("circle-1");
        request.setPlanDate(LocalDate.of(2026, 6, 21));
        request.setTitle("今天午餐这样吃");
        request.setDishIds(List.of("dish-1", "dish-2"));
        request.setVisibleUserIds(List.of("viewer"));

        PlanDetailResponse response = planService.confirmAiArrangement(request);

        assertThat(response.getId()).isEqualTo("plan-ai");
        assertThat(response.getRecipes()).extracting("id").containsExactly("dish-1", "dish-2");
        assertThat(storedRecipes).extracting(CirclePlanRecipe::getSort).containsExactly(1, 2);
        verify(circlePlanMapper).insert(any(CirclePlan.class));
        verify(circlePlanVisibleUserMapper).insert(any(CirclePlanVisibleUser.class));
    }

    @Test
    void getPlanDetailRejectsUserOutsideCircle() {
        AuthContext.setUserId("viewer");
        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan("plan-1", "circle-1", "creator"));
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> planService.getPlanDetail("plan-1", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("暂无访问权限");
    }

    @Test
    void getPlanDetailRejectsCircleMemberOutsidePlanVisibleUsers() {
        AuthContext.setUserId("viewer");
        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan("plan-1", "circle-1", "creator"));
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(circlePlanVisibleUserMapper.canUserViewPlan("plan-1", "viewer")).thenReturn(0);
        when(circlePlanRecipeMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> planService.getPlanDetail("plan-1", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("暂无访问权限");
    }

    @Test
    void getPlanDetailAllowsCircleMemberOutsideVisibleUsersWithShareToken() {
        AuthContext.setUserId("viewer");
        CirclePlan plan = plan("plan-1", "circle-1", "creator");
        plan.setShareToken("share-1");

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(circlePlanVisibleUserMapper.canUserViewPlan("plan-1", "viewer")).thenReturn(0);
        when(circlePlanRecipeMapper.selectCount(any())).thenReturn(0L);
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(null);
        when(userAccountMapper.selectById("creator")).thenReturn(user("creator", "创建者"));

        PlanDetailResponse response = planService.getPlanDetail("plan-1", "share-1");

        assertThat(response.isSharedView()).isTrue();
        assertThat(response.isViewerCanAddRecipes()).isTrue();
        assertThat(response.isViewerCanManageRecipes()).isFalse();
    }

    @Test
    void getPlanDetailAllowsAnonymousViewerWithShareToken() {
        CirclePlan plan = plan("plan-1", "circle-1", "creator");
        plan.setShareToken("share-1");
        DishSummaryResponse dish = dishSummary("dish-1", "creator", LocalDateTime.of(2026, 6, 1, 10, 0));

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of(recipe("plan-1", "dish-1")));
        when(dishMapper.selectByIds(any())).thenReturn(List.of(dish));
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(null);
        when(userAccountMapper.selectById("creator")).thenReturn(user("creator", "创建者"));
        when(userAccountMapper.selectBatchIds(any())).thenReturn(List.of());

        PlanDetailResponse response = planService.getPlanDetail("plan-1", "share-1");

        assertThat(response.isSharedView()).isTrue();
        assertThat(response.isViewerCanAddRecipes()).isFalse();
        assertThat(response.isViewerCanDelete()).isFalse();
    }

    @Test
    void getPlansMergesSharedPlanWithViewerPlansAfterLogin() {
        AuthContext.setUserId("guest");
        CirclePlan ownPlan = plan("own-plan", "circle-own", "guest");
        CirclePlan sharedPlan = plan("shared-plan", "circle-shared", "creator");
        sharedPlan.setShareToken("share-1");

        when(circlePlanMapper.selectById("shared-plan")).thenReturn(sharedPlan);
        when(buddyCircleMapper.selectById("circle-shared")).thenReturn(circle("circle-shared"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(0L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(member("circle-own", "guest")));
        when(circlePlanMapper.selectVisiblePlansByUserInCircleDateRange(eq("guest"), anyList(), any(), any()))
                .thenReturn(List.of(ownPlan));
        when(buddyCircleMapper.selectBatchIds(any())).thenReturn(List.of(circle("circle-own"), circle("circle-shared")));
        when(userAccountMapper.selectBatchIds(any())).thenReturn(List.of(user("guest", "访客"), user("creator", "创建者")));
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectList(any())).thenReturn(List.of());

        PlanMonthResponse response = planService.getPlans("2026-06", "shared-plan", "share-1");

        assertThat(response.getDays()).hasSize(1);
        assertThat(response.getDays().get(0).getPlans()).extracting("id")
                .containsExactly("own-plan", "shared-plan");
        assertThat(response.getDays().get(0).getPlans().get(0).isSharedView()).isFalse();
        assertThat(response.getDays().get(0).getPlans().get(1).isSharedView()).isTrue();
    }

    @Test
    void getPlansOnlyReturnsPlansVisibleToCircleMember() {
        AuthContext.setUserId("viewer");
        CirclePlan visiblePlan = plan("visible-plan", "circle-1", "creator");

        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(member("circle-1", "viewer")));
        when(circlePlanMapper.selectVisiblePlansByUserInCircleDateRange(eq("viewer"), anyList(), any(), any()))
                .thenReturn(List.of(visiblePlan));
        when(circlePlanMapper.selectPlansAddedByUserInPlanDateRange(eq("viewer"), any(), any()))
                .thenReturn(List.of());
        when(buddyCircleMapper.selectBatchIds(any())).thenReturn(List.of(circle("circle-1")));
        when(userAccountMapper.selectBatchIds(any())).thenReturn(List.of(user("creator", "创建者")));
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectList(any())).thenReturn(List.of());

        PlanMonthResponse response = planService.getPlans("2026-06", null, null);

        assertThat(response.getDays()).hasSize(1);
        assertThat(response.getDays().get(0).getPlans()).extracting("id")
                .containsExactly("visible-plan");
        assertThat(response.getDays().get(0).getPlans().get(0).isSharedView()).isFalse();
    }

    @Test
    void getPlansOnlyReturnsSharedPlanForAnonymousShareViewer() {
        CirclePlan sharedPlan = plan("shared-plan", "circle-shared", "creator");
        sharedPlan.setShareToken("share-1");

        when(circlePlanMapper.selectById("shared-plan")).thenReturn(sharedPlan);
        when(buddyCircleMapper.selectById("circle-shared")).thenReturn(circle("circle-shared"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(0L);
        when(buddyCircleMapper.selectBatchIds(any())).thenReturn(List.of(circle("circle-shared")));
        when(userAccountMapper.selectBatchIds(any())).thenReturn(List.of(user("creator", "创建者")));
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectList(any())).thenReturn(List.of());

        PlanMonthResponse response = planService.getPlans("2026-06", "shared-plan", "share-1");

        assertThat(response.getDays()).hasSize(1);
        assertThat(response.getDays().get(0).getPlans()).extracting("id")
                .containsExactly("shared-plan");
        assertThat(response.getDays().get(0).getPlans().get(0).isSharedView()).isTrue();
        assertThat(response.getDays().get(0).getPlans().get(0).isViewerCanAddRecipes()).isFalse();
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

        PlanDetailResponse response = planService.addRecipes("plan-1", request, null);

        assertThat(response.getRecipes()).hasSize(1);
        assertThat(response.getRecipes().get(0).getAddedByNickname()).isEqualTo("胖虎");
        verify(circlePlanRecipeMapper, never()).insert(any(CirclePlanRecipe.class));
    }

    @Test
    void addRecipesAppendsSortAfterExistingRecipes() {
        AuthContext.setUserId("viewer");
        CirclePlan plan = plan("plan-1", "circle-1", "viewer");
        CirclePlanRecipe existing = recipe("plan-1", "dish-1");
        existing.setSort(2);
        existing.setAddedByUserId("viewer");
        List<CirclePlanRecipe> storedRecipes = new ArrayList<>(List.of(existing));
        DishSummaryResponse existingDish = dishSummary("dish-1", "viewer", LocalDateTime.of(2026, 6, 1, 10, 0));
        DishSummaryResponse newDish = dishSummary("dish-2", "viewer", LocalDateTime.of(2026, 6, 2, 10, 0));

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(member("circle-1", "viewer")));
        when(circlePlanRecipeMapper.selectList(any())).thenAnswer(invocation -> storedRecipes.stream()
                .sorted((left, right) -> Integer.compare(left.getSort() == null ? 0 : left.getSort(),
                        right.getSort() == null ? 0 : right.getSort()))
                .collect(java.util.stream.Collectors.toList()));
        when(dishMapper.selectByOwnerUserIds(anyList())).thenReturn(List.of(existingDish, newDish));
        when(dishMapper.selectByIds(any())).thenReturn(List.of(existingDish, newDish));
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(null);
        when(userAccountMapper.selectById("viewer")).thenReturn(user("viewer", "胖虎"));
        when(userAccountMapper.selectBatchIds(any())).thenReturn(List.of(user("viewer", "胖虎")));
        doNothing().when(menuVisibilitySupport).hydrateSummaries(anyList());
        when(menuVisibilitySupport.canViewDish(any(DishSummaryResponse.class), anyString(), anyBoolean())).thenReturn(true);
        when(circlePlanRecipeMapper.insert(any(CirclePlanRecipe.class))).thenAnswer(invocation -> {
            CirclePlanRecipe recipe = invocation.getArgument(0);
            storedRecipes.add(recipe);
            return 1;
        });

        PlanRecipesUpdateRequest request = new PlanRecipesUpdateRequest();
        request.setDishIds(List.of("dish-2"));

        planService.addRecipes("plan-1", request, null);

        assertThat(storedRecipes).hasSize(2);
        assertThat(storedRecipes.get(1).getDishId()).isEqualTo("dish-2");
        assertThat(storedRecipes.get(1).getSort()).isEqualTo(3);
        verify(circlePlanRecipeMapper).insert(any(CirclePlanRecipe.class));
    }

    @Test
    void sharedViewerCanBrowseCircleRecipeCandidatesAfterLogin() {
        AuthContext.setUserId("guest");
        CirclePlan plan = plan("plan-1", "circle-1", "creator");
        plan.setShareToken("share-1");
        DishSummaryResponse sharedDish = dishSummary("dish-3", "creator", LocalDateTime.of(2026, 6, 3, 10, 0));
        sharedDish.setEffectiveVisibility("circle");
        sharedDish.setEffectiveCircleIds(List.of("circle-1"));

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(0L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(member("circle-1", "creator")));
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of());
        when(dishMapper.selectByOwnerUserIds(anyList())).thenReturn(List.of(sharedDish));
        doNothing().when(menuVisibilitySupport).hydrateSummaries(anyList());
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());

        PlanRecipeCandidatesResponse response = planService.getRecipeCandidates("plan-1", "share-1");

        assertThat(response.isViewerCanAddRecipes()).isTrue();
        assertThat(response.isViewerIsCircleMember()).isFalse();
        assertThat(response.getSourceLabel()).isEqualTo("圈内共享菜谱");
        assertThat(response.getRecipes()).extracting(DishSummaryResponse::getId).containsExactly("dish-3");
    }

    @Test
    void sharedViewerCanAddCircleRecipeCandidateAfterLogin() {
        AuthContext.setUserId("guest");
        CirclePlan plan = plan("plan-1", "circle-1", "creator");
        plan.setShareToken("share-1");
        List<CirclePlanRecipe> storedRecipes = new ArrayList<>();
        DishSummaryResponse sharedDish = dishSummary("dish-3", "creator", LocalDateTime.of(2026, 6, 3, 10, 0));
        sharedDish.setEffectiveVisibility("circle");
        sharedDish.setEffectiveCircleIds(List.of("circle-1"));

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(0L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(member("circle-1", "creator")));
        when(circlePlanRecipeMapper.selectList(any())).thenAnswer(invocation -> new ArrayList<>(storedRecipes));
        when(dishMapper.selectByOwnerUserIds(anyList())).thenReturn(List.of(sharedDish));
        when(dishMapper.selectByIds(any())).thenReturn(List.of(sharedDish));
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(null);
        when(userAccountMapper.selectById("creator")).thenReturn(user("creator", "创建者"));
        when(userAccountMapper.selectBatchIds(any())).thenReturn(List.of(user("guest", "访客")));
        doNothing().when(menuVisibilitySupport).hydrateSummaries(anyList());
        when(circlePlanRecipeMapper.insert(any(CirclePlanRecipe.class))).thenAnswer(invocation -> {
            storedRecipes.add(invocation.getArgument(0));
            return 1;
        });

        PlanRecipesUpdateRequest request = new PlanRecipesUpdateRequest();
        request.setDishIds(List.of("dish-3"));

        PlanDetailResponse response = planService.addRecipes("plan-1", request, "share-1");

        assertThat(storedRecipes).hasSize(1);
        assertThat(storedRecipes.get(0).getDishId()).isEqualTo("dish-3");
        assertThat(storedRecipes.get(0).getAddedByUserId()).isEqualTo("guest");
        assertThat(response.getRecipes()).extracting("id").containsExactly("dish-3");
    }

    @Test
    void sharedViewerCanSeePlanAfterAddingRecipeWithoutShareToken() {
        AuthContext.setUserId("guest");
        CirclePlan plan = plan("plan-1", "circle-1", "creator");
        CirclePlanRecipe addedRecipe = recipe("plan-1", "dish-3");
        addedRecipe.setAddedByUserId("guest");
        DishSummaryResponse sharedDish = dishSummary("dish-3", "creator", LocalDateTime.of(2026, 6, 3, 10, 0));

        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanMapper.selectPlansAddedByUserInPlanDateRange(eq("guest"), any(), any()))
                .thenReturn(List.of(plan));
        when(buddyCircleMapper.selectBatchIds(any())).thenReturn(List.of(circle("circle-1")));
        when(userAccountMapper.selectBatchIds(any())).thenReturn(List.of(user("creator", "创建者"), user("guest", "访客")));
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of(addedRecipe));
        when(circlePlanShoppingListMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(null);
        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(0L);
        when(circlePlanRecipeMapper.selectCount(any())).thenReturn(1L);
        when(dishMapper.selectByIds(any())).thenReturn(List.of(sharedDish));
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());
        when(userAccountMapper.selectById("creator")).thenReturn(user("creator", "创建者"));
        doNothing().when(menuVisibilitySupport).hydrateSummaries(anyList());

        PlanMonthResponse monthResponse = planService.getPlans("2026-06", null, null);
        PlanDetailResponse detailResponse = planService.getPlanDetail("plan-1", null);

        assertThat(monthResponse.getDays()).hasSize(1);
        assertThat(monthResponse.getDays().get(0).getPlans()).extracting("id").containsExactly("plan-1");
        assertThat(monthResponse.getDays().get(0).getPlans().get(0).isSharedView()).isTrue();
        assertThat(monthResponse.getDays().get(0).getPlans().get(0).isViewerCanAddRecipes()).isTrue();
        assertThat(monthResponse.getDays().get(0).getPlans().get(0).isViewerCanManageRecipes()).isFalse();
        assertThat(detailResponse.getRecipes()).extracting("id").containsExactly("dish-3");
        assertThat(detailResponse.isSharedView()).isTrue();
        assertThat(detailResponse.isViewerCanAddRecipes()).isTrue();
    }

    @Test
    void sharedGuestCanBrowseRecipeCandidatesBeforeLogin() {
        CirclePlan plan = plan("plan-1", "circle-1", "creator");
        plan.setShareToken("share-1");
        DishSummaryResponse sharedDish = dishSummary("dish-3", "creator", LocalDateTime.of(2026, 6, 3, 10, 0));
        sharedDish.setEffectiveVisibility("circle");
        sharedDish.setEffectiveCircleIds(List.of("circle-1"));

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(0L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(member("circle-1", "creator")));
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of());
        when(dishMapper.selectByOwnerUserIds(anyList())).thenReturn(List.of(sharedDish));
        doNothing().when(menuVisibilitySupport).hydrateSummaries(anyList());
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());

        PlanRecipeCandidatesResponse response = planService.getRecipeCandidates("plan-1", "share-1");

        assertThat(response.isViewerCanAddRecipes()).isFalse();
        assertThat(response.isViewerIsCircleMember()).isFalse();
        assertThat(response.getSourceLabel()).isEqualTo("圈内共享菜谱");
        assertThat(response.getRecipes()).extracting(DishSummaryResponse::getId).containsExactly("dish-3");
    }

    @Test
    void sortRecipesUpdatesRecipeSortByDishOrder() {
        AuthContext.setUserId("viewer");
        CirclePlan plan = plan("plan-1", "circle-1", "viewer");
        CirclePlanRecipe first = recipe("plan-1", "dish-1");
        CirclePlanRecipe second = recipe("plan-1", "dish-2");
        CirclePlanRecipe third = recipe("plan-1", "dish-3");
        List<CirclePlanRecipe> storedRecipes = new ArrayList<>(List.of(first, second, third));
        List<DishSummaryResponse> dishes = List.of(
                dishSummary("dish-1", "viewer", LocalDateTime.of(2026, 6, 1, 10, 0)),
                dishSummary("dish-2", "viewer", LocalDateTime.of(2026, 6, 2, 10, 0)),
                dishSummary("dish-3", "viewer", LocalDateTime.of(2026, 6, 3, 10, 0))
        );

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(circlePlanRecipeMapper.selectList(any())).thenAnswer(invocation -> storedRecipes.stream()
                .sorted((left, right) -> Integer.compare(left.getSort() == null ? 0 : left.getSort(),
                        right.getSort() == null ? 0 : right.getSort()))
                .collect(java.util.stream.Collectors.toList()));
        when(dishMapper.selectByIds(any())).thenReturn(dishes);
        when(dishIngredientMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(null);
        when(userAccountMapper.selectById("viewer")).thenReturn(user("viewer", "胖虎"));
        when(userAccountMapper.selectBatchIds(any())).thenReturn(List.of());

        PlanRecipesUpdateRequest request = new PlanRecipesUpdateRequest();
        request.setDishIds(List.of("dish-3", "dish-1", "dish-2"));

        PlanDetailResponse response = planService.sortRecipes("plan-1", request);

        assertThat(response.getRecipes()).extracting("id").containsExactly("dish-3", "dish-1", "dish-2");
        assertThat(third.getSort()).isEqualTo(1);
        assertThat(first.getSort()).isEqualTo(2);
        assertThat(second.getSort()).isEqualTo(3);
        verify(circlePlanRecipeMapper, times(3)).updateById(any(CirclePlanRecipe.class));
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
        assertThat(plan.getShoppingStatus()).isEqualTo(CirclePlan.SHOPPING_STATUS_NOT_PURCHASED);
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
        when(userAccountMapper.selectBatchIds(List.of("viewer"))).thenReturn(List.of(user("viewer", "胖虎")));

        PlanShoppingListResponse response = planService.toggleShoppingItem("plan-1", "item-1");

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).isPurchased()).isTrue();
        assertThat(response.getItems().get(0).getPurchasedByNickname()).isEqualTo("胖虎");
        assertThat(plan.getShoppingStatus()).isEqualTo(CirclePlan.SHOPPING_STATUS_PURCHASED);
    }

    @Test
    void removeRecipeAllowsSharedParticipantToRemoveOwnRecipeWithShareToken() {
        AuthContext.setUserId("viewer");
        CirclePlan plan = plan("plan-1", "circle-1", "creator");
        plan.setShareToken("share-1");
        CirclePlanRecipe recipe = recipe("plan-1", "dish-1");
        recipe.setAddedByUserId("viewer");

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(0L);
        when(circlePlanRecipeMapper.selectCount(any())).thenReturn(0L);
        when(circlePlanRecipeMapper.selectOne(any())).thenReturn(recipe);
        when(circlePlanRecipeMapper.selectList(any())).thenReturn(List.of());
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(null);

        PlanDetailResponse response = planService.removeRecipe("plan-1", "dish-1", "share-1");

        verify(circlePlanRecipeMapper).delete(any());
        assertThat(response.isSharedView()).isTrue();
        assertThat(response.isViewerCanAddRecipes()).isTrue();
        assertThat(response.isViewerCanManageRecipes()).isFalse();
    }

    @Test
    void removeRecipeRejectsSharedParticipantRemovingOtherUsersRecipe() {
        AuthContext.setUserId("viewer");
        CirclePlan plan = plan("plan-1", "circle-1", "creator");
        plan.setShareToken("share-1");
        CirclePlanRecipe recipe = recipe("plan-1", "dish-1");
        recipe.setAddedByUserId("other-user");

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(0L);
        when(circlePlanRecipeMapper.selectCount(any())).thenReturn(0L);
        when(circlePlanRecipeMapper.selectOne(any())).thenReturn(recipe);

        assertThatThrownBy(() -> planService.removeRecipe("plan-1", "dish-1", "share-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("只能移除自己加入的菜谱");

        verify(circlePlanRecipeMapper, never()).delete(any());
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

    @Test
    void deletePlanRemovesRecipesAndShoppingListData() {
        AuthContext.setUserId("creator");
        CirclePlan plan = plan("plan-1", "circle-1", "creator");
        CirclePlanShoppingList shoppingList = new CirclePlanShoppingList();
        shoppingList.setId("list-1");
        shoppingList.setPlanId("plan-1");
        CirclePlanShoppingItem item = new CirclePlanShoppingItem();
        item.setId("item-1");
        item.setShoppingListId("list-1");

        when(circlePlanMapper.selectById("plan-1")).thenReturn(plan);
        when(buddyCircleMapper.selectById("circle-1")).thenReturn(circle("circle-1"));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(circlePlanShoppingListMapper.selectOne(any())).thenReturn(shoppingList);
        when(circlePlanShoppingItemMapper.selectList(any())).thenReturn(List.of(item));

        planService.deletePlan("plan-1");

        verify(circlePlanShoppingItemSourceMapper).delete(any());
        verify(circlePlanShoppingItemMapper).delete(any());
        verify(circlePlanShoppingListMapper).deleteById("list-1");
        verify(circlePlanVisibleUserMapper).delete(any());
        verify(circlePlanRecipeMapper).delete(any());
        verify(circlePlanMapper).deleteById("plan-1");
    }

    private CirclePlan plan(String id, String circleId, String creatorUserId) {
        CirclePlan plan = new CirclePlan();
        plan.setId(id);
        plan.setCircleId(circleId);
        plan.setCreatorUserId(creatorUserId);
        plan.setPlanDate(LocalDate.of(2026, 6, 5));
        plan.setTitle("周五下班拼饭局");
        plan.setShoppingStatus(CirclePlan.SHOPPING_STATUS_NOT_STARTED);
        return plan;
    }

    private PlanAiArrangeRequest aiArrangeRequest() {
        PlanAiArrangeRequest request = new PlanAiArrangeRequest();
        request.setCircleId("circle-1");
        request.setMealType("lunch");
        request.setPlanDate(LocalDate.of(2026, 6, 21));
        request.setDishCount(2);
        request.setHealthAdvice("少油、清爽");
        return request;
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

    private BuddyCircleMember member(String circleId, String userId) {
        BuddyCircleMember member = new BuddyCircleMember();
        member.setCircleId(circleId);
        member.setUserId(userId);
        return member;
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
