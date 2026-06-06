package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.DishSummaryResponse;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlanServiceImpl implements PlanService {
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
                           MenuVisibilitySupport menuVisibilitySupport) {
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

        Map<LocalDate, List<PlanSummaryResponse>> byDate = new LinkedHashMap<>();
        for (CirclePlan plan : plans) {
            byDate.computeIfAbsent(plan.getPlanDate(), key -> new ArrayList<>()).add(buildPlanSummary(plan));
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
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        circlePlanMapper.insert(plan);
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

        Set<String> existingDishIds = circlePlanRecipeMapper.selectList(new QueryWrapper<CirclePlanRecipe>()
                        .eq("plan_id", context.plan().getId()))
                .stream()
                .map(CirclePlanRecipe::getDishId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<DishSummaryResponse> candidateDishes = loadCircleVisibleDishes(context.circle().getId(), currentUserId);
        Map<String, DishSummaryResponse> candidateById = candidateDishes.stream()
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
            circlePlanRecipeMapper.insert(recipe);
        }

        synchronizeShoppingListIfExists(context.plan(), currentUserId);
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
        response.setShoppingStarted(shoppingSummary.started());
        response.setShoppingRestartCount(shoppingSummary.restartCount());
        response.setShoppingTotalItemCount(shoppingSummary.totalCount());
        response.setShoppingPurchasedItemCount(shoppingSummary.purchasedCount());
        return response;
    }

    private PlanSummaryResponse buildPlanSummary(CirclePlan plan) {
        PlanSummaryResponse response = new PlanSummaryResponse();
        response.setId(plan.getId());
        response.setTitle(plan.getTitle());
        response.setPlanDate(plan.getPlanDate());
        response.setCircleId(plan.getCircleId());
        BuddyCircle circle = buddyCircleMapper.selectById(plan.getCircleId());
        response.setCircleName(circle == null ? "" : circle.getName());
        response.setCreatorUserId(plan.getCreatorUserId());
        UserAccount creator = userAccountMapper.selectById(plan.getCreatorUserId());
        response.setCreatorNickname(creator == null ? "" : creator.getNickname());
        response.setRecipeCount(circlePlanRecipeMapper.selectCount(new QueryWrapper<CirclePlanRecipe>()
                .eq("plan_id", plan.getId())).intValue());

        ShoppingSummary shoppingSummary = summarizeShopping(plan.getId());
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
                UserAccount buyer = userAccountMapper.selectById(item.getPurchasedByUserId());
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

    private void rebuildShoppingList(CirclePlan plan, String operatorUserId, boolean countAsRestart) {
        List<PlanRecipeIngredientSource> sources = loadPlanRecipeIngredientSources(plan.getId());
        if (sources.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先添加菜谱再开始采购");
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
        List<CirclePlanRecipe> planRecipes = circlePlanRecipeMapper.selectList(new QueryWrapper<CirclePlanRecipe>()
                .eq("plan_id", planId)
                .orderByAsc("created_at")
                .orderByAsc("id"));
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
        List<CirclePlanRecipe> planRecipes = circlePlanRecipeMapper.selectList(new QueryWrapper<CirclePlanRecipe>()
                .eq("plan_id", planId)
                .orderByAsc("created_at")
                .orderByAsc("id"));
        if (planRecipes.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> dishIds = planRecipes.stream().map(CirclePlanRecipe::getDishId).collect(Collectors.toList());
        List<DishSummaryResponse> ownerOrdered = dishMapper.selectAllActive();
        menuVisibilitySupport.hydrateSummaries(ownerOrdered);
        hydrateIngredientNames(ownerOrdered);
        Map<String, DishSummaryResponse> byId = ownerOrdered.stream()
                .filter(item -> dishIds.contains(item.getId()))
                .collect(Collectors.toMap(DishSummaryResponse::getId, item -> item, (left, right) -> left));

        List<DishSummaryResponse> ordered = new ArrayList<>();
        for (CirclePlanRecipe planRecipe : planRecipes) {
            DishSummaryResponse dish = byId.get(planRecipe.getDishId());
            if (dish != null) {
                ordered.add(dish);
            }
        }
        return ordered;
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

    private record PlanContext(CirclePlan plan, BuddyCircle circle) {
    }

    private record ShoppingSummary(boolean started, int restartCount, int totalCount, int purchasedCount) {
    }

    private record PlanRecipeIngredientSource(String dishId, String dishName, String ingredientName, String amount) {
    }
}
