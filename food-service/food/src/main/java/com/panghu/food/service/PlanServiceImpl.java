package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.dto.PlanAiArrangeRequest;
import com.panghu.food.dto.PlanAiArrangementConfirmRequest;
import com.panghu.food.dto.PlanAiArrangementRecipeResponse;
import com.panghu.food.dto.PlanAiArrangementResponse;
import com.panghu.food.dto.PlanCreateRequest;
import com.panghu.food.dto.PlanDayPlansResponse;
import com.panghu.food.dto.PlanDetailResponse;
import com.panghu.food.dto.PlanMonthResponse;
import com.panghu.food.dto.PlanRecipesUpdateRequest;
import com.panghu.food.dto.PlanShoppingItemResponse;
import com.panghu.food.dto.PlanShoppingItemSourceResponse;
import com.panghu.food.dto.PlanShoppingListResponse;
import com.panghu.food.dto.PlanSummaryResponse;
import com.panghu.food.entity.BuddyCircle;
import com.panghu.food.entity.BuddyCircleMember;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlanServiceImpl implements PlanService {
    private static final int MIN_AI_DISH_COUNT = 1;
    private static final int MAX_AI_DISH_COUNT = 8;
    private static final int RECENT_PLAN_HISTORY_LIMIT = 30;
    private static final String DEFAULT_HEALTH_ADVICE = "荤素搭配";
    private static final String MEAL_TYPE_LUNCH = "lunch";
    private static final String MEAL_TYPE_DINNER = "dinner";

    private final CirclePlanMapper circlePlanMapper;
    private final CirclePlanRecipeMapper circlePlanRecipeMapper;
    private final CirclePlanShoppingListMapper circlePlanShoppingListMapper;
    private final CirclePlanShoppingItemMapper circlePlanShoppingItemMapper;
    private final CirclePlanShoppingItemSourceMapper circlePlanShoppingItemSourceMapper;
    private final BuddyCircleMapper buddyCircleMapper;
    private final BuddyCircleMemberMapper buddyCircleMemberMapper;
    private final DishMapper dishMapper;
    private final DishIngredientMapper dishIngredientMapper;
    private final UserAccountMapper userAccountMapper;
    private final MenuVisibilitySupport menuVisibilitySupport;
    private final DishAiService dishAiService;
    private final VipService vipService;

    public PlanServiceImpl(CirclePlanMapper circlePlanMapper,
                           CirclePlanRecipeMapper circlePlanRecipeMapper,
                           CirclePlanShoppingListMapper circlePlanShoppingListMapper,
                           CirclePlanShoppingItemMapper circlePlanShoppingItemMapper,
                           CirclePlanShoppingItemSourceMapper circlePlanShoppingItemSourceMapper,
                           BuddyCircleMapper buddyCircleMapper,
                           BuddyCircleMemberMapper buddyCircleMemberMapper,
                           DishMapper dishMapper,
                           DishIngredientMapper dishIngredientMapper,
                           UserAccountMapper userAccountMapper,
                           MenuVisibilitySupport menuVisibilitySupport,
                           DishAiService dishAiService,
                           VipService vipService) {
        this.circlePlanMapper = circlePlanMapper;
        this.circlePlanRecipeMapper = circlePlanRecipeMapper;
        this.circlePlanShoppingListMapper = circlePlanShoppingListMapper;
        this.circlePlanShoppingItemMapper = circlePlanShoppingItemMapper;
        this.circlePlanShoppingItemSourceMapper = circlePlanShoppingItemSourceMapper;
        this.buddyCircleMapper = buddyCircleMapper;
        this.buddyCircleMemberMapper = buddyCircleMemberMapper;
        this.dishMapper = dishMapper;
        this.dishIngredientMapper = dishIngredientMapper;
        this.userAccountMapper = userAccountMapper;
        this.menuVisibilitySupport = menuVisibilitySupport;
        this.dishAiService = dishAiService;
        this.vipService = vipService;
    }

    @Override
    public PlanMonthResponse getPlans(String month) {
        String currentUserId = AuthContext.requireUserId();
        YearMonth targetMonth = parseMonth(month);
        List<String> circleIds = getMemberCircleIds(currentUserId);

        PlanMonthResponse response = new PlanMonthResponse();
        response.setMonth(targetMonth.toString());
        if (circleIds.isEmpty()) {
            return response;
        }

        List<CirclePlan> plans = circlePlanMapper.selectList(new QueryWrapper<CirclePlan>()
                .in("circle_id", circleIds)
                .between("plan_date", targetMonth.atDay(1), targetMonth.atEndOfMonth())
                .orderByAsc("plan_date")
                .orderByAsc("created_at")
                .orderByAsc("id"));

        PlanSummaryContext summaryContext = loadPlanSummaryContext(plans);
        Map<LocalDate, List<PlanSummaryResponse>> byDate = new LinkedHashMap<>();
        for (CirclePlan plan : plans) {
            byDate.computeIfAbsent(plan.getPlanDate(), key -> new ArrayList<>()).add(buildPlanSummary(plan, summaryContext));
        }
        response.setDays(byDate.entrySet().stream().map(entry -> {
            PlanDayPlansResponse day = new PlanDayPlansResponse();
            day.setDate(entry.getKey());
            day.setPlans(entry.getValue());
            return day;
        }).collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional
    public PlanDetailResponse createPlan(PlanCreateRequest request) {
        String currentUserId = AuthContext.requireUserId();
        String circleId = normalizeRequired(request.getCircleId(), "请选择圈子");
        LocalDate planDate = request.getPlanDate();
        if (planDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择计划日期");
        }

        BuddyCircle circle = requireCircleMember(circleId, currentUserId);
        CirclePlan plan = new CirclePlan();
        plan.setCircleId(circleId);
        plan.setPlanDate(planDate);
        plan.setTitle(normalizeTitle(request.getTitle()));
        plan.setCreatorUserId(currentUserId);
        plan.setShoppingStatus(CirclePlan.SHOPPING_STATUS_NOT_STARTED);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        circlePlanMapper.insert(plan);
        return buildPlanDetail(plan, circle);
    }

    @Override
    public PlanAiArrangementResponse arrangePlanByAi(PlanAiArrangeRequest request) {
        String currentUserId = AuthContext.requireUserId();
        String circleId = normalizeRequired(request == null ? null : request.getCircleId(), "请选择圈子");
        LocalDate planDate = request == null ? null : request.getPlanDate();
        if (planDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择计划日期");
        }
        int dishCount = normalizeAiDishCount(request == null ? null : request.getDishCount());
        String mealType = normalizeMealType(request == null ? null : request.getMealType());
        String mealTypeLabel = mealTypeLabel(mealType);
        String healthAdvice = normalizeOptional(request == null ? null : request.getHealthAdvice(), DEFAULT_HEALTH_ADVICE);

        requireCircleMember(circleId, currentUserId);
        List<DishSummaryResponse> visibleDishes = loadCircleVisibleDishes(circleId, currentUserId);
        if (visibleDishes.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "当前圈子暂无可安排的菜谱");
        }

        vipService.assertCanUsePlanAi(currentUserId);
        List<CirclePlan> recentPlans = loadRecentCirclePlans(circleId);
        PlanArrangementHistoryContext historyContext = buildPlanArrangementHistoryContext(recentPlans, visibleDishes);
        // 历史高频菜谱放前面并带上次数，让 AI 的首要依据更贴近用户过往口味。
        List<DishSummaryResponse> historyRankedDishes = rankDishesByHistoricalPreference(
                visibleDishes,
                historyContext.recipeCountByDishId());
        DishAiService.PlanArrangementAiResult aiResult = dishAiService.arrangePlan(
                mealTypeLabel,
                planDate,
                Math.min(dishCount, visibleDishes.size()),
                healthAdvice,
                historyRankedDishes,
                historyContext.histories(),
                historyContext.recipeCountByDishId());

        PlanAiArrangementResponse response = buildAiArrangementResponse(
                aiResult,
                visibleDishes,
                historyContext.recipeCountByDishId(),
                dishCount,
                mealTypeLabel,
                planDate,
                healthAdvice);
        response.setUsage(vipService.consumePlanAiUsage(currentUserId));
        return response;
    }

    @Override
    @Transactional
    public PlanDetailResponse confirmAiArrangement(PlanAiArrangementConfirmRequest request) {
        String currentUserId = AuthContext.requireUserId();
        String circleId = normalizeRequired(request == null ? null : request.getCircleId(), "请选择圈子");
        LocalDate planDate = request == null ? null : request.getPlanDate();
        if (planDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择计划日期");
        }
        List<String> dishIds = normalizeIds(request == null ? null : request.getDishIds());
        if (dishIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请至少选择一道菜谱");
        }

        BuddyCircle circle = requireCircleMember(circleId, currentUserId);
        Map<String, DishSummaryResponse> visibleDishById = loadCircleVisibleDishes(circleId, currentUserId).stream()
                .collect(Collectors.toMap(DishSummaryResponse::getId, item -> item, (left, right) -> left));
        for (String dishId : dishIds) {
            if (!visibleDishById.containsKey(dishId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "只能添加当前圈子可见的菜谱");
            }
        }

        CirclePlan plan = new CirclePlan();
        plan.setCircleId(circleId);
        plan.setPlanDate(planDate);
        plan.setTitle(normalizeTitle(request == null ? null : request.getTitle()));
        plan.setCreatorUserId(currentUserId);
        plan.setShoppingStatus(CirclePlan.SHOPPING_STATUS_NOT_STARTED);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        circlePlanMapper.insert(plan);
        insertPlanRecipes(plan.getId(), dishIds, currentUserId);
        return buildPlanDetail(plan, circle);
    }

    @Override
    public PlanDetailResponse getPlanDetail(String planId) {
        PlanContext context = requirePlanContext(planId, AuthContext.requireUserId());
        return buildPlanDetail(context.plan(), context.circle());
    }

    @Override
    @Transactional
    public void deletePlan(String planId) {
        String currentUserId = AuthContext.requireUserId();
        PlanContext context = requirePlanContext(planId, currentUserId);
        if (!Objects.equals(context.plan().getCreatorUserId(), currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有创建者可以删除计划");
        }
        deleteShoppingListData(context.plan().getId());
        circlePlanRecipeMapper.delete(new QueryWrapper<CirclePlanRecipe>().eq("plan_id", context.plan().getId()));
        circlePlanMapper.deleteById(context.plan().getId());
    }

    @Override
    @Transactional
    public PlanDetailResponse addRecipes(String planId, PlanRecipesUpdateRequest request) {
        String currentUserId = AuthContext.requireUserId();
        PlanContext context = requirePlanContext(planId, currentUserId);
        List<String> dishIds = normalizeIds(request == null ? null : request.getDishIds());
        if (dishIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请至少选择一道菜谱");
        }

        List<CirclePlanRecipe> existingRecipes = loadPlanRecipeRelations(context.plan().getId());
        Set<String> existingDishIds = existingRecipes
                .stream()
                .map(CirclePlanRecipe::getDishId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int nextSort = nextRecipeSort(existingRecipes);

        List<String> pendingDishIds = dishIds.stream()
                .filter(dishId -> !existingDishIds.contains(dishId))
                .collect(Collectors.toList());
        Map<String, DishSummaryResponse> candidateById = pendingDishIds.isEmpty()
                ? Collections.emptyMap()
                : loadCircleVisibleDishes(context.circle().getId(), currentUserId).stream()
                .collect(Collectors.toMap(DishSummaryResponse::getId, item -> item));

        for (String dishId : dishIds) {
            if (existingDishIds.contains(dishId)) {
                continue;
            }
            if (!candidateById.containsKey(dishId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "只能添加当前圈子可见的菜谱");
            }
            CirclePlanRecipe recipe = new CirclePlanRecipe();
            recipe.setPlanId(context.plan().getId());
            recipe.setDishId(dishId);
            recipe.setAddedByUserId(currentUserId);
            recipe.setCreatedAt(LocalDateTime.now());
            recipe.setSort(nextSort++);
            circlePlanRecipeMapper.insert(recipe);
        }

        synchronizeShoppingListIfExists(context.plan(), currentUserId);
        return buildPlanDetail(context.plan(), context.circle());
    }

    @Override
    @Transactional
    public PlanDetailResponse sortRecipes(String planId, PlanRecipesUpdateRequest request) {
        String currentUserId = AuthContext.requireUserId();
        PlanContext context = requirePlanContext(planId, currentUserId);
        List<String> dishIds = normalizeIds(request == null ? null : request.getDishIds());
        List<CirclePlanRecipe> recipes = loadPlanRecipeRelations(context.plan().getId());
        if (recipes.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "暂无可排序的菜谱");
        }
        if (dishIds.size() != recipes.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "菜谱排序数据不完整");
        }

        Map<String, CirclePlanRecipe> recipeByDishId = recipes.stream()
                .collect(Collectors.toMap(CirclePlanRecipe::getDishId, item -> item, (left, right) -> left));
        for (String dishId : dishIds) {
            if (!recipeByDishId.containsKey(dishId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "只能排序当前计划里的菜谱");
            }
        }

        for (int i = 0; i < dishIds.size(); i++) {
            CirclePlanRecipe recipe = recipeByDishId.get(dishIds.get(i));
            recipe.setSort(i + 1);
            circlePlanRecipeMapper.updateById(recipe);
        }
        return buildPlanDetail(context.plan(), context.circle());
    }

    @Override
    @Transactional
    public PlanDetailResponse removeRecipe(String planId, String dishId) {
        String currentUserId = AuthContext.requireUserId();
        PlanContext context = requirePlanContext(planId, currentUserId);
        String normalizedDishId = normalizeRequired(dishId, "菜谱不存在");

        circlePlanRecipeMapper.delete(new QueryWrapper<CirclePlanRecipe>()
                .eq("plan_id", context.plan().getId())
                .eq("dish_id", normalizedDishId));
        synchronizeShoppingListIfExists(context.plan(), currentUserId);
        return buildPlanDetail(context.plan(), context.circle());
    }

    @Override
    @Transactional
    public PlanShoppingListResponse startShoppingList(String planId) {
        String currentUserId = AuthContext.requireUserId();
        PlanContext context = requirePlanContext(planId, currentUserId);
        rebuildShoppingList(context.plan(), currentUserId, true);
        return buildShoppingListResponse(context.plan(), context.circle());
    }

    @Override
    public PlanShoppingListResponse getShoppingList(String planId) {
        PlanContext context = requirePlanContext(planId, AuthContext.requireUserId());
        return buildShoppingListResponse(context.plan(), context.circle());
    }

    @Override
    @Transactional
    public PlanShoppingListResponse toggleShoppingItem(String planId, String itemId) {
        String currentUserId = AuthContext.requireUserId();
        PlanContext context = requirePlanContext(planId, currentUserId);
        CirclePlanShoppingList shoppingList = circlePlanShoppingListMapper.selectOne(new QueryWrapper<CirclePlanShoppingList>()
                .eq("plan_id", context.plan().getId())
                .last("LIMIT 1"));
        if (shoppingList == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先开始采购");
        }

        CirclePlanShoppingItem item = circlePlanShoppingItemMapper.selectById(itemId);
        if (item == null || !Objects.equals(item.getShoppingListId(), shoppingList.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "采购项不存在");
        }

        boolean nextPurchased = !Boolean.TRUE.equals(item.getPurchased());
        item.setPurchased(nextPurchased);
        item.setPurchasedByUserId(nextPurchased ? currentUserId : null);
        item.setPurchasedAt(nextPurchased ? LocalDateTime.now() : null);
        circlePlanShoppingItemMapper.updateById(item);
        updatePlanShoppingStatus(context.plan(), summarizeShopping(context.plan().getId()));
        return buildShoppingListResponse(context.plan(), context.circle());
    }

    @Override
    @Transactional
    public PlanShoppingListResponse resetShoppingList(String planId) {
        String currentUserId = AuthContext.requireUserId();
        PlanContext context = requirePlanContext(planId, currentUserId);
        rebuildShoppingList(context.plan(), currentUserId, true);
        return buildShoppingListResponse(context.plan(), context.circle());
    }

    private PlanAiArrangementResponse buildAiArrangementResponse(DishAiService.PlanArrangementAiResult aiResult,
                                                                 List<DishSummaryResponse> visibleDishes,
                                                                 Map<String, Long> recipeCountByDishId,
                                                                 int requestedDishCount,
                                                                 String mealTypeLabel,
                                                                 LocalDate planDate,
                                                                 String healthAdvice) {
        Map<String, DishSummaryResponse> visibleDishById = visibleDishes.stream()
                .collect(Collectors.toMap(DishSummaryResponse::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<String> selectedDishIds = new ArrayList<>();
        Map<String, String> reasonByDishId = new HashMap<>();

        if (aiResult != null && aiResult.recipes() != null) {
            for (DishAiService.PlanArrangementRecipe recipe : aiResult.recipes()) {
                if (!visibleDishById.containsKey(recipe.dishId()) || selectedDishIds.contains(recipe.dishId())) {
                    continue;
                }
                selectedDishIds.add(recipe.dishId());
                reasonByDishId.put(recipe.dishId(), normalizeOptional(recipe.reason(), "适合这顿饭。"));
                if (selectedDishIds.size() >= requestedDishCount) {
                    break;
                }
            }
        }

        // AI 可能少返回、重复返回或返回不可见 ID，这里用后端可见菜谱按历史偏好补齐。
        int targetCount = Math.min(requestedDishCount, visibleDishes.size());
        List<DishSummaryResponse> fallbackDishes = rankDishesByHistoricalPreference(visibleDishes, recipeCountByDishId);
        for (DishSummaryResponse dish : fallbackDishes) {
            if (selectedDishIds.size() >= targetCount) {
                break;
            }
            if (selectedDishIds.contains(dish.getId())) {
                continue;
            }
            selectedDishIds.add(dish.getId());
            reasonByDishId.put(dish.getId(), "结合最近计划和圈内菜谱热度补上这一道。");
        }

        PlanAiArrangementResponse response = new PlanAiArrangementResponse();
        response.setTitle(normalizeAiTitle(aiResult == null ? null : aiResult.title(), mealTypeLabel, planDate));
        response.setPetText(normalizeOptional(aiResult == null ? null : aiResult.petText(), "饭团把菜谱叼来啦，先看看这桌合不合胃口。"));
        response.setSuggestionText(normalizeOptional(aiResult == null ? null : aiResult.suggestionText(), "按最近计划和圈内菜谱搭了一餐。"));
        response.setHealthText(normalizeOptional(aiResult == null ? null : aiResult.healthText(), healthAdvice));
        response.setRecipes(selectedDishIds.stream().map(dishId -> {
            PlanAiArrangementRecipeResponse item = new PlanAiArrangementRecipeResponse();
            item.setDish(visibleDishById.get(dishId));
            item.setReason(reasonByDishId.getOrDefault(dishId, "适合这顿饭。"));
            return item;
        }).collect(Collectors.toList()));
        return response;
    }

    private List<DishSummaryResponse> rankDishesByHistoricalPreference(List<DishSummaryResponse> visibleDishes,
                                                                       Map<String, Long> recipeCountByDishId) {
        return visibleDishes.stream()
                .sorted(Comparator
                        .comparing((DishSummaryResponse item) -> recipeCountByDishId.getOrDefault(item.getId(), 0L)).reversed()
                        .thenComparing(DishSummaryResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private List<CirclePlan> loadRecentCirclePlans(String circleId) {
        return circlePlanMapper.selectList(new QueryWrapper<CirclePlan>()
                .eq("circle_id", circleId)
                .orderByDesc("plan_date")
                .orderByDesc("created_at")
                .orderByDesc("id")
                .last("LIMIT " + RECENT_PLAN_HISTORY_LIMIT));
    }

    private PlanArrangementHistoryContext buildPlanArrangementHistoryContext(List<CirclePlan> recentPlans,
                                                                             List<DishSummaryResponse> visibleDishes) {
        if (recentPlans.isEmpty()) {
            return new PlanArrangementHistoryContext(Collections.emptyList(), Collections.emptyMap());
        }

        Map<String, DishSummaryResponse> visibleDishById = visibleDishes.stream()
                .collect(Collectors.toMap(DishSummaryResponse::getId, item -> item, (left, right) -> left));
        List<String> planIds = recentPlans.stream().map(CirclePlan::getId).collect(Collectors.toList());
        List<CirclePlanRecipe> planRecipes = circlePlanRecipeMapper.selectList(new QueryWrapper<CirclePlanRecipe>()
                .in("plan_id", planIds)
                .orderByAsc("plan_id")
                .orderByAsc("sort")
                .orderByAsc("created_at")
                .orderByAsc("id"));
        Map<String, List<CirclePlanRecipe>> recipesByPlanId = planRecipes.stream()
                .collect(Collectors.groupingBy(CirclePlanRecipe::getPlanId));
        Map<String, Long> recipeCountByDishId = new LinkedHashMap<>();
        List<DishAiService.PlanArrangementHistory> histories = new ArrayList<>();

        for (CirclePlan plan : recentPlans) {
            List<String> dishNames = new ArrayList<>();
            for (CirclePlanRecipe recipe : recipesByPlanId.getOrDefault(plan.getId(), Collections.emptyList())) {
                DishSummaryResponse dish = visibleDishById.get(recipe.getDishId());
                if (dish == null) {
                    continue;
                }
                recipeCountByDishId.merge(recipe.getDishId(), 1L, Long::sum);
                dishNames.add(dish.getName());
            }
            if (!dishNames.isEmpty()) {
                histories.add(new DishAiService.PlanArrangementHistory(
                        plan.getTitle(),
                        plan.getPlanDate() == null ? "" : plan.getPlanDate().toString(),
                        dishNames));
            }
        }
        return new PlanArrangementHistoryContext(histories, recipeCountByDishId);
    }

    private void insertPlanRecipes(String planId, List<String> dishIds, String currentUserId) {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < dishIds.size(); i++) {
            CirclePlanRecipe recipe = new CirclePlanRecipe();
            recipe.setPlanId(planId);
            recipe.setDishId(dishIds.get(i));
            recipe.setAddedByUserId(currentUserId);
            recipe.setCreatedAt(now);
            recipe.setSort(i + 1);
            circlePlanRecipeMapper.insert(recipe);
        }
    }

    private PlanDetailResponse buildPlanDetail(CirclePlan plan, BuddyCircle circle) {
        PlanDetailResponse response = new PlanDetailResponse();
        response.setId(plan.getId());
        response.setTitle(plan.getTitle());
        response.setPlanDate(plan.getPlanDate());
        response.setCircleId(circle.getId());
        response.setCircleName(circle.getName());
        UserAccount creator = userAccountMapper.selectById(plan.getCreatorUserId());
        response.setCreatorUserId(plan.getCreatorUserId());
        response.setCreatorNickname(creator == null ? "" : creator.getNickname());
        List<DishSummaryResponse> recipes = loadPlanRecipes(plan.getId());
        response.setRecipes(recipes);

        ShoppingSummary shoppingSummary = summarizeShopping(plan.getId());
        response.setShoppingStatus(resolveShoppingStatus(shoppingSummary));
        response.setShoppingStarted(shoppingSummary.started());
        response.setShoppingRestartCount(shoppingSummary.restartCount());
        response.setShoppingTotalItemCount(shoppingSummary.totalCount());
        response.setShoppingPurchasedItemCount(shoppingSummary.purchasedCount());
        return response;
    }

    private PlanSummaryResponse buildPlanSummary(CirclePlan plan) {
        return buildPlanSummary(plan, loadPlanSummaryContext(List.of(plan)));
    }

    private PlanSummaryResponse buildPlanSummary(CirclePlan plan, PlanSummaryContext context) {
        PlanSummaryResponse response = new PlanSummaryResponse();
        response.setId(plan.getId());
        response.setTitle(plan.getTitle());
        response.setPlanDate(plan.getPlanDate());
        response.setCircleId(plan.getCircleId());
        BuddyCircle circle = context.circlesById().get(plan.getCircleId());
        response.setCircleName(circle == null ? "" : circle.getName());
        response.setCreatorUserId(plan.getCreatorUserId());
        UserAccount creator = context.usersById().get(plan.getCreatorUserId());
        response.setCreatorNickname(creator == null ? "" : creator.getNickname());
        response.setRecipeCount(context.recipeCountByPlanId().getOrDefault(plan.getId(), 0L).intValue());

        ShoppingSummary shoppingSummary = context.shoppingSummaryByPlanId()
                .getOrDefault(plan.getId(), new ShoppingSummary(false, 0, 0, 0));
        response.setShoppingStatus(resolveShoppingStatus(shoppingSummary));
        response.setShoppingStarted(shoppingSummary.started());
        response.setShoppingTotalItemCount(shoppingSummary.totalCount());
        response.setShoppingPurchasedItemCount(shoppingSummary.purchasedCount());
        return response;
    }

    private PlanShoppingListResponse buildShoppingListResponse(CirclePlan plan, BuddyCircle circle) {
        PlanShoppingListResponse response = new PlanShoppingListResponse();
        response.setPlanId(plan.getId());
        response.setPlanTitle(plan.getTitle());
        response.setPlanDate(plan.getPlanDate());
        response.setCircleId(circle.getId());
        response.setCircleName(circle.getName());

        CirclePlanShoppingList shoppingList = circlePlanShoppingListMapper.selectOne(new QueryWrapper<CirclePlanShoppingList>()
                .eq("plan_id", plan.getId())
                .last("LIMIT 1"));
        if (shoppingList == null) {
            response.setShoppingStarted(false);
            return response;
        }

        response.setId(shoppingList.getId());
        response.setShoppingStarted(true);
        response.setStartedByUserId(shoppingList.getStartedByUserId());
        response.setStartedAt(shoppingList.getStartedAt());
        response.setRestartCount(shoppingList.getRestartCount() == null ? 0 : shoppingList.getRestartCount());
        UserAccount startedBy = userAccountMapper.selectById(shoppingList.getStartedByUserId());
        response.setStartedByNickname(startedBy == null ? "" : startedBy.getNickname());

        List<CirclePlanShoppingItem> items = circlePlanShoppingItemMapper.selectList(new QueryWrapper<CirclePlanShoppingItem>()
                .eq("shopping_list_id", shoppingList.getId())
                .orderByAsc("sort")
                .orderByAsc("id"));
        Map<String, UserAccount> buyersById = selectUsersByIds(items.stream()
                .map(CirclePlanShoppingItem::getPurchasedByUserId)
                .collect(Collectors.toList()));
        Map<String, List<CirclePlanShoppingItemSource>> sourcesByItemId = items.isEmpty()
                ? Collections.emptyMap()
                : circlePlanShoppingItemSourceMapper.selectList(
                                new QueryWrapper<CirclePlanShoppingItemSource>()
                                        .in("shopping_item_id", items.stream().map(CirclePlanShoppingItem::getId).collect(Collectors.toList()))
                                        .orderByAsc("sort")
                                        .orderByAsc("id"))
                        .stream()
                        .collect(Collectors.groupingBy(CirclePlanShoppingItemSource::getShoppingItemId));

        int purchasedCount = 0;
        List<PlanShoppingItemResponse> itemResponses = new ArrayList<>();
        for (CirclePlanShoppingItem item : items) {
            if (Boolean.TRUE.equals(item.getPurchased())) {
                purchasedCount++;
            }
            PlanShoppingItemResponse itemResponse = new PlanShoppingItemResponse();
            itemResponse.setId(item.getId());
            itemResponse.setIngredientName(item.getIngredientName());
            itemResponse.setPurchased(Boolean.TRUE.equals(item.getPurchased()));
            itemResponse.setPurchasedByUserId(item.getPurchasedByUserId());
            itemResponse.setPurchasedAt(item.getPurchasedAt());
            if (item.getPurchasedByUserId() != null) {
                UserAccount buyer = buyersById.get(item.getPurchasedByUserId());
                itemResponse.setPurchasedByNickname(buyer == null ? "" : buyer.getNickname());
            }
            itemResponse.setSources(sourcesByItemId.getOrDefault(item.getId(), Collections.emptyList()).stream().map(source -> {
                PlanShoppingItemSourceResponse sourceResponse = new PlanShoppingItemSourceResponse();
                sourceResponse.setDishId(source.getDishId());
                sourceResponse.setDishName(source.getDishName());
                sourceResponse.setAmount(source.getAmount());
                return sourceResponse;
            }).collect(Collectors.toList()));
            itemResponses.add(itemResponse);
        }

        response.setTotalItemCount(items.size());
        response.setPurchasedItemCount(purchasedCount);
        response.setItems(itemResponses);
        return response;
    }

    private PlanSummaryContext loadPlanSummaryContext(List<CirclePlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return new PlanSummaryContext(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        }

        List<String> planIds = plans.stream()
                .map(CirclePlan::getId)
                .collect(Collectors.toList());
        Map<String, BuddyCircle> circlesById = selectCirclesByIds(plans.stream()
                .map(CirclePlan::getCircleId)
                .collect(Collectors.toList()));
        Map<String, UserAccount> usersById = selectUsersByIds(plans.stream()
                .map(CirclePlan::getCreatorUserId)
                .collect(Collectors.toList()));

        Map<String, Long> recipeCountByPlanId = circlePlanRecipeMapper.selectList(new QueryWrapper<CirclePlanRecipe>()
                        .in("plan_id", planIds))
                .stream()
                .collect(Collectors.groupingBy(CirclePlanRecipe::getPlanId, Collectors.counting()));

        // 月视图只需要汇总状态，批量加载后按 planId 回填，避免每个计划单独查清单和采购项。
        Map<String, CirclePlanShoppingList> shoppingListByPlanId = circlePlanShoppingListMapper.selectList(new QueryWrapper<CirclePlanShoppingList>()
                        .in("plan_id", planIds))
                .stream()
                .collect(Collectors.toMap(CirclePlanShoppingList::getPlanId, item -> item, (left, right) -> left));
        Map<String, ShoppingSummary> shoppingSummaryByPlanId = buildShoppingSummaries(shoppingListByPlanId);

        return new PlanSummaryContext(circlesById, usersById, recipeCountByPlanId, shoppingSummaryByPlanId);
    }

    private Map<String, ShoppingSummary> buildShoppingSummaries(Map<String, CirclePlanShoppingList> shoppingListByPlanId) {
        if (shoppingListByPlanId.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> planIdByShoppingListId = shoppingListByPlanId.values().stream()
                .collect(Collectors.toMap(CirclePlanShoppingList::getId, CirclePlanShoppingList::getPlanId, (left, right) -> left));
        List<CirclePlanShoppingItem> items = circlePlanShoppingItemMapper.selectList(new QueryWrapper<CirclePlanShoppingItem>()
                .in("shopping_list_id", planIdByShoppingListId.keySet()));
        Map<String, List<CirclePlanShoppingItem>> itemsByShoppingListId = items.stream()
                .collect(Collectors.groupingBy(CirclePlanShoppingItem::getShoppingListId));

        Map<String, ShoppingSummary> result = new LinkedHashMap<>();
        for (CirclePlanShoppingList shoppingList : shoppingListByPlanId.values()) {
            List<CirclePlanShoppingItem> shoppingItems = itemsByShoppingListId.getOrDefault(shoppingList.getId(), Collections.emptyList());
            int purchasedCount = (int) shoppingItems.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getPurchased()))
                    .count();
            result.put(shoppingList.getPlanId(), new ShoppingSummary(
                    true,
                    shoppingList.getRestartCount() == null ? 0 : shoppingList.getRestartCount(),
                    shoppingItems.size(),
                    purchasedCount));
        }
        return result;
    }

    private void rebuildShoppingList(CirclePlan plan, String operatorUserId, boolean countAsRestart) {
        List<PlanRecipeIngredientSource> sources = loadPlanRecipeIngredientSources(plan.getId());
        if (sources.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "暂无可采购的食材");
        }

        CirclePlanShoppingList shoppingList = circlePlanShoppingListMapper.selectOne(new QueryWrapper<CirclePlanShoppingList>()
                .eq("plan_id", plan.getId())
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (shoppingList == null) {
            shoppingList = new CirclePlanShoppingList();
            shoppingList.setPlanId(plan.getId());
            shoppingList.setStartedByUserId(operatorUserId);
            shoppingList.setStartedAt(now);
            shoppingList.setRestartCount(0);
            shoppingList.setUpdatedAt(now);
            circlePlanShoppingListMapper.insert(shoppingList);
        } else {
            shoppingList.setUpdatedAt(now);
            if (countAsRestart) {
                shoppingList.setStartedByUserId(operatorUserId);
                shoppingList.setStartedAt(now);
                shoppingList.setRestartCount((shoppingList.getRestartCount() == null ? 0 : shoppingList.getRestartCount()) + 1);
            }
            circlePlanShoppingListMapper.updateById(shoppingList);
        }

        deleteShoppingItems(shoppingList.getId());

        Map<String, List<PlanRecipeIngredientSource>> grouped = new LinkedHashMap<>();
        for (PlanRecipeIngredientSource source : sources) {
            grouped.computeIfAbsent(source.ingredientName(), key -> new ArrayList<>()).add(source);
        }

        int itemSort = 0;
        for (Map.Entry<String, List<PlanRecipeIngredientSource>> entry : grouped.entrySet()) {
            CirclePlanShoppingItem item = new CirclePlanShoppingItem();
            item.setShoppingListId(shoppingList.getId());
            item.setIngredientName(entry.getKey());
            item.setPurchased(false);
            item.setSort(itemSort++);
            circlePlanShoppingItemMapper.insert(item);

            int sourceSort = 0;
            for (PlanRecipeIngredientSource source : entry.getValue()) {
                CirclePlanShoppingItemSource itemSource = new CirclePlanShoppingItemSource();
                itemSource.setShoppingItemId(item.getId());
                itemSource.setDishId(source.dishId());
                itemSource.setDishName(source.dishName());
                itemSource.setAmount(source.amount());
                itemSource.setSort(sourceSort++);
                circlePlanShoppingItemSourceMapper.insert(itemSource);
            }
        }
        updatePlanShoppingStatus(plan, grouped.size(), 0);
    }

    private void synchronizeShoppingListIfExists(CirclePlan plan, String operatorUserId) {
        CirclePlanShoppingList shoppingList = circlePlanShoppingListMapper.selectOne(new QueryWrapper<CirclePlanShoppingList>()
                .eq("plan_id", plan.getId())
                .last("LIMIT 1"));
        if (shoppingList != null) {
            if (circlePlanRecipeMapper.selectCount(new QueryWrapper<CirclePlanRecipe>().eq("plan_id", plan.getId())) == 0) {
                deleteShoppingListData(plan.getId());
                return;
            }
            rebuildShoppingList(plan, operatorUserId, false);
        }
    }

    private void deleteShoppingListData(String planId) {
        CirclePlanShoppingList shoppingList = circlePlanShoppingListMapper.selectOne(new QueryWrapper<CirclePlanShoppingList>()
                .eq("plan_id", planId)
                .last("LIMIT 1"));
        if (shoppingList == null) {
            return;
        }
        deleteShoppingItems(shoppingList.getId());
        circlePlanShoppingListMapper.deleteById(shoppingList.getId());
        CirclePlan plan = circlePlanMapper.selectById(planId);
        if (plan != null) {
            updatePlanShoppingStatus(plan, CirclePlan.SHOPPING_STATUS_NOT_STARTED);
        }
    }

    private void deleteShoppingItems(String shoppingListId) {
        List<CirclePlanShoppingItem> items = circlePlanShoppingItemMapper.selectList(new QueryWrapper<CirclePlanShoppingItem>()
                .eq("shopping_list_id", shoppingListId));
        if (!items.isEmpty()) {
            circlePlanShoppingItemSourceMapper.delete(new QueryWrapper<CirclePlanShoppingItemSource>()
                    .in("shopping_item_id", items.stream().map(CirclePlanShoppingItem::getId).collect(Collectors.toList())));
        }
        circlePlanShoppingItemMapper.delete(new QueryWrapper<CirclePlanShoppingItem>().eq("shopping_list_id", shoppingListId));
    }

    private List<PlanRecipeIngredientSource> loadPlanRecipeIngredientSources(String planId) {
        List<CirclePlanRecipe> planRecipes = loadPlanRecipeRelations(planId);
        if (planRecipes.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> dishIds = planRecipes.stream().map(CirclePlanRecipe::getDishId).collect(Collectors.toList());
        Map<String, Dish> dishById = dishMapper.selectBatchIds(dishIds).stream()
                .collect(Collectors.toMap(Dish::getId, item -> item));
        Map<String, List<DishIngredient>> ingredientsByDishId = dishIngredientMapper.selectList(new QueryWrapper<DishIngredient>()
                        .in("dish_id", dishIds)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(DishIngredient::getDishId));

        List<PlanRecipeIngredientSource> sources = new ArrayList<>();
        for (CirclePlanRecipe planRecipe : planRecipes) {
            Dish dish = dishById.get(planRecipe.getDishId());
            if (dish == null) {
                continue;
            }
            for (DishIngredient ingredient : ingredientsByDishId.getOrDefault(planRecipe.getDishId(), Collections.emptyList())) {
                String ingredientName = ingredient.getName() == null ? "" : ingredient.getName().trim();
                if (ingredientName.isEmpty()) {
                    continue;
                }
                sources.add(new PlanRecipeIngredientSource(
                        planRecipe.getDishId(),
                        dish.getName(),
                        ingredientName,
                        ingredient.getAmount() == null ? "" : ingredient.getAmount().trim()));
            }
        }
        return sources;
    }

    private List<DishSummaryResponse> loadPlanRecipes(String planId) {
        List<CirclePlanRecipe> planRecipes = loadPlanRecipeRelations(planId);
        if (planRecipes.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> dishIds = planRecipes.stream().map(CirclePlanRecipe::getDishId).collect(Collectors.toList());
        List<DishSummaryResponse> ownerOrdered = dishMapper.selectByIds(dishIds);
        menuVisibilitySupport.hydrateSummaries(ownerOrdered);
        hydrateIngredientNames(ownerOrdered);
        Map<String, DishSummaryResponse> byId = ownerOrdered.stream()
                .collect(Collectors.toMap(DishSummaryResponse::getId, item -> item, (left, right) -> left));
        Map<String, UserAccount> addedByUserMap = userAccountMapper.selectBatchIds(planRecipes.stream()
                        .map(CirclePlanRecipe::getAddedByUserId)
                        .filter(Objects::nonNull)
                        .filter(userId -> !userId.isBlank())
                        .distinct()
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(UserAccount::getId, item -> item, (left, right) -> left));

        List<DishSummaryResponse> ordered = new ArrayList<>();
        for (CirclePlanRecipe planRecipe : planRecipes) {
            DishSummaryResponse dish = byId.get(planRecipe.getDishId());
            if (dish != null) {
                dish.setAddedByUserId(planRecipe.getAddedByUserId());
                UserAccount addedBy = addedByUserMap.get(planRecipe.getAddedByUserId());
                dish.setAddedByNickname(addedBy == null ? "" : addedBy.getNickname());
                ordered.add(dish);
            }
        }
        return ordered;
    }

    private List<CirclePlanRecipe> loadPlanRecipeRelations(String planId) {
        return circlePlanRecipeMapper.selectList(new QueryWrapper<CirclePlanRecipe>()
                .eq("plan_id", planId)
                .orderByAsc("sort")
                .orderByAsc("created_at")
                .orderByAsc("id"));
    }

    private int nextRecipeSort(List<CirclePlanRecipe> recipes) {
        int maxSort = recipes.stream()
                .map(CirclePlanRecipe::getSort)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        return Math.max(maxSort, recipes.size()) + 1;
    }

    private List<DishSummaryResponse> loadCircleVisibleDishes(String circleId, String viewerUserId) {
        List<String> memberIds = buddyCircleMemberMapper.selectList(new QueryWrapper<BuddyCircleMember>()
                        .eq("circle_id", circleId)
                        .orderByAsc("joined_at")
                        .orderByAsc("id"))
                .stream()
                .map(BuddyCircleMember::getUserId)
                .collect(Collectors.toList());
        if (memberIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<DishSummaryResponse> dishes = dishMapper.selectAllActive();
        menuVisibilitySupport.hydrateSummaries(dishes);
        hydrateIngredientNames(dishes);
        return dishes.stream()
                .filter(item -> memberIds.contains(item.getOwnerUserId()))
                .filter(item -> isVisibleInCircleContext(item, viewerUserId, circleId))
                .sorted(Comparator.comparing(DishSummaryResponse::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private void hydrateIngredientNames(List<DishSummaryResponse> dishes) {
        List<String> dishIds = dishes.stream()
                .map(DishSummaryResponse::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (dishIds.isEmpty()) {
            return;
        }

        Map<String, List<String>> ingredientNamesByDishId = dishIngredientMapper.selectList(new QueryWrapper<DishIngredient>()
                        .in("dish_id", dishIds)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .filter(item -> item.getDishId() != null && item.getName() != null && !item.getName().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        DishIngredient::getDishId,
                        Collectors.mapping(DishIngredient::getName, Collectors.toList())));

        for (DishSummaryResponse dish : dishes) {
            dish.setIngredientNames(new ArrayList<>(ingredientNamesByDishId.getOrDefault(dish.getId(), Collections.emptyList())));
        }
    }

    private ShoppingSummary summarizeShopping(String planId) {
        CirclePlanShoppingList shoppingList = circlePlanShoppingListMapper.selectOne(new QueryWrapper<CirclePlanShoppingList>()
                .eq("plan_id", planId)
                .last("LIMIT 1"));
        if (shoppingList == null) {
            return new ShoppingSummary(false, 0, 0, 0);
        }
        List<CirclePlanShoppingItem> items = circlePlanShoppingItemMapper.selectList(new QueryWrapper<CirclePlanShoppingItem>()
                .eq("shopping_list_id", shoppingList.getId()));
        int purchasedCount = (int) items.stream().filter(item -> Boolean.TRUE.equals(item.getPurchased())).count();
        return new ShoppingSummary(true, shoppingList.getRestartCount() == null ? 0 : shoppingList.getRestartCount(), items.size(), purchasedCount);
    }

    private void updatePlanShoppingStatus(CirclePlan plan, ShoppingSummary shoppingSummary) {
        updatePlanShoppingStatus(plan, resolveShoppingStatus(shoppingSummary));
    }

    private void updatePlanShoppingStatus(CirclePlan plan, int totalCount, int purchasedCount) {
        updatePlanShoppingStatus(plan, resolveShoppingStatus(totalCount, purchasedCount));
    }

    private void updatePlanShoppingStatus(CirclePlan plan, String nextStatus) {
        if (Objects.equals(plan.getShoppingStatus(), nextStatus)) {
            return;
        }
        plan.setShoppingStatus(nextStatus);
        plan.setUpdatedAt(LocalDateTime.now());
        circlePlanMapper.updateById(plan);
    }

    private String resolveShoppingStatus(ShoppingSummary shoppingSummary) {
        if (!shoppingSummary.started()) {
            return CirclePlan.SHOPPING_STATUS_NOT_STARTED;
        }
        return resolveShoppingStatus(shoppingSummary.totalCount(), shoppingSummary.purchasedCount());
    }

    private String resolveShoppingStatus(int totalCount, int purchasedCount) {
        if (totalCount <= 0) {
            return CirclePlan.SHOPPING_STATUS_NOT_PURCHASED;
        }
        if (purchasedCount <= 0) {
            return CirclePlan.SHOPPING_STATUS_NOT_PURCHASED;
        }
        if (purchasedCount >= totalCount) {
            return CirclePlan.SHOPPING_STATUS_PURCHASED;
        }
        return CirclePlan.SHOPPING_STATUS_PARTIALLY_PURCHASED;
    }

    private PlanContext requirePlanContext(String planId, String userId) {
        CirclePlan plan = circlePlanMapper.selectById(planId);
        if (plan == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "计划不存在");
        }
        BuddyCircle circle = requireCircleMember(plan.getCircleId(), userId);
        return new PlanContext(plan, circle);
    }

    private BuddyCircle requireCircleMember(String circleId, String userId) {
        BuddyCircle circle = buddyCircleMapper.selectById(circleId);
        if (circle == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "搭子圈不存在");
        }
        long count = buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                .eq("circle_id", circleId)
                .eq("user_id", userId));
        if (count == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "你还不在这个搭子圈里");
        }
        return circle;
    }

    private List<String> getMemberCircleIds(String userId) {
        return buddyCircleMemberMapper.selectList(new QueryWrapper<BuddyCircleMember>()
                        .eq("user_id", userId))
                .stream()
                .map(BuddyCircleMember::getCircleId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean isVisibleInCircleContext(DishSummaryResponse dish, String viewerUserId, String circleId) {
        if (dish == null) {
            return false;
        }
        if (VisibilityUtils.VISIBILITY_PUBLIC.equals(dish.getEffectiveVisibility())) {
            return true;
        }
        if (VisibilityUtils.VISIBILITY_CIRCLE.equals(dish.getEffectiveVisibility())) {
            return dish.getEffectiveCircleIds().contains(circleId);
        }
        return menuVisibilitySupport.canViewDish(dish, viewerUserId, true);
    }

    private YearMonth parseMonth(String month) {
        try {
            return month == null || month.trim().isEmpty() ? YearMonth.now() : YearMonth.parse(month.trim());
        } catch (DateTimeParseException error) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "月份格式应为 YYYY-MM");
        }
    }

    private String normalizeTitle(String title) {
        String normalized = normalizeRequired(title, "计划标题不能为空");
        if (normalized.length() > 40) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "计划标题不能超过 40 个字符");
        }
        return normalized;
    }

    private int normalizeAiDishCount(Integer dishCount) {
        int normalized = dishCount == null ? 3 : dishCount;
        if (normalized < MIN_AI_DISH_COUNT || normalized > MAX_AI_DISH_COUNT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "菜数范围应为 1-8 道");
        }
        return normalized;
    }

    private String normalizeMealType(String mealType) {
        String normalized = normalizeRequired(mealType, "请选择午餐或晚餐");
        if (!MEAL_TYPE_LUNCH.equals(normalized) && !MEAL_TYPE_DINNER.equals(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择午餐或晚餐");
        }
        return normalized;
    }

    private String mealTypeLabel(String mealType) {
        return MEAL_TYPE_DINNER.equals(mealType) ? "晚餐" : "午餐";
    }

    private String normalizeOptional(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private String normalizeAiTitle(String title, String mealTypeLabel, LocalDate planDate) {
        String normalized = normalizeOptional(title, formatShortDate(planDate) + " " + mealTypeLabel + "排菜");
        if (normalized.length() <= 40) {
            return normalized;
        }
        return normalized.substring(0, 40);
    }

    private String formatShortDate(LocalDate date) {
        if (date == null) {
            return "今天";
        }
        return String.format("%02d.%02d", date.getMonthValue(), date.getDayOfMonth());
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private List<String> normalizeIds(Collection<String> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<String, UserAccount> selectUsersByIds(Collection<String> userIds) {
        List<String> normalized = normalizeIds(userIds);
        if (normalized.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserAccount> users = userAccountMapper.selectBatchIds(normalized);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        return users.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UserAccount::getId, item -> item, (left, right) -> left));
    }

    private Map<String, BuddyCircle> selectCirclesByIds(Collection<String> circleIds) {
        List<String> normalized = normalizeIds(circleIds);
        if (normalized.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BuddyCircle> circles = buddyCircleMapper.selectBatchIds(normalized);
        if (circles == null || circles.isEmpty()) {
            return Collections.emptyMap();
        }
        return circles.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(BuddyCircle::getId, item -> item, (left, right) -> left));
    }

    private record PlanContext(CirclePlan plan, BuddyCircle circle) {
    }

    private record PlanSummaryContext(
            Map<String, BuddyCircle> circlesById,
            Map<String, UserAccount> usersById,
            Map<String, Long> recipeCountByPlanId,
            Map<String, ShoppingSummary> shoppingSummaryByPlanId) {
    }

    private record PlanArrangementHistoryContext(
            List<DishAiService.PlanArrangementHistory> histories,
            Map<String, Long> recipeCountByDishId) {
    }

    private record ShoppingSummary(boolean started, int restartCount, int totalCount, int purchasedCount) {
    }

    private record PlanRecipeIngredientSource(String dishId, String dishName, String ingredientName, String amount) {
    }
}
