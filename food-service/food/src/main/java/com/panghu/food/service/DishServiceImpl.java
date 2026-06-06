package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.DishDetailResponse;
import com.panghu.food.dto.DishAiAnalysisRequest;
import com.panghu.food.dto.DishAiAnalysisResponse;
import com.panghu.food.dto.DishAiImportRequest;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.dto.DishUpsertRequest;
import com.panghu.food.dto.IngredientItem;
import com.panghu.food.dto.StepItem;
import com.panghu.food.entity.BuddyCircleMember;
import com.panghu.food.entity.Dish;
import com.panghu.food.entity.DishIngredient;
import com.panghu.food.entity.DishStep;
import com.panghu.food.entity.DishVisibilityCircle;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.BuddyCircleMemberMapper;
import com.panghu.food.mapper.DishMapper;
import com.panghu.food.mapper.DishIngredientMapper;
import com.panghu.food.mapper.DishVisibilityCircleMapper;
import com.panghu.food.mapper.DishStepMapper;
import com.panghu.food.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl implements DishService {
    private static final int HOME_FEATURED_LIST_LIMIT = 3;
    private static final int MAX_AI_IMPORT_IMAGE_COUNT = 3;

    @Value("${upload.base-path:./uploads}")
    private String basePath;

    @Value("${upload.access-url:http://localhost:8080/uploads/}")
    private String accessUrl;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishIngredientMapper dishIngredientMapper;

    @Autowired
    private DishStepMapper dishStepMapper;

    @Autowired
    private BuddyCircleMemberMapper buddyCircleMemberMapper;

    @Autowired
    private DishVisibilityCircleMapper dishVisibilityCircleMapper;

    @Autowired
    private VipService vipService;

    @Autowired
    private DishAiService dishAiService;

    @Autowired
    private MenuVisibilitySupport menuVisibilitySupport;

    @Override
    public List<DishSummaryResponse> getDishes(String categoryId, String ownerUserId, String scope, String circleId) {
        String currentUserId = AuthContext.getUserId();
        List<DishSummaryResponse> dishes;
        if ("my".equals(scope)) {
            String userId = currentUserId == null ? ownerUserId : currentUserId;
            dishes = dishMapper.selectByOwnerUserId(userId);
        } else if (ownerUserId != null) {
            dishes = dishMapper.selectByOwnerUserId(ownerUserId);
        } else if (categoryId != null && !categoryId.trim().isEmpty()) {
            dishes = dishMapper.selectByCategoryId(categoryId, null);
        } else {
            dishes = dishMapper.selectAllActive();
        }
        menuVisibilitySupport.hydrateSummaries(dishes);
        List<DishSummaryResponse> visibleDishes = dishes.stream()
                .filter(item -> categoryId == null || categoryId.equals(item.getCategoryId()))
                .filter(item -> canViewDish(item, currentUserId, "circle".equals(scope)))
                .collect(Collectors.toList());
        hydrateIngredientNames(visibleDishes);
        return visibleDishes;
    }

    @Override
    public List<DishSummaryResponse> getFeaturedDishes(String categoryId) {
        String currentUserId = AuthContext.getUserId();
        List<DishSummaryResponse> source = categoryId == null || categoryId.trim().isEmpty()
                ? dishMapper.selectAllActive()
                : dishMapper.selectByCategoryId(categoryId, null);
        menuVisibilitySupport.hydrateSummaries(source);
        return filterHomeDishes(source, currentUserId, item -> Boolean.TRUE.equals(item.getIsFeatured()));
    }

    @Override
    public List<DishSummaryResponse> getRecentDishes() {
        String currentUserId = AuthContext.getUserId();
        if (currentUserId == null) {
            return new ArrayList<>();
        }
        List<DishSummaryResponse> recentDishes = dishMapper.selectByOwnerUserId(currentUserId);
        menuVisibilitySupport.hydrateSummaries(recentDishes);
        hydrateIngredientNames(recentDishes);
        return recentDishes;
    }

    @Override
    public DishDetailResponse getDishById(String id) {
        DishDetailResponse detail = dishMapper.selectDishDetailById(id);
        if (detail == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "菜品不存在");
        }
        menuVisibilitySupport.hydrateDetail(detail);
        if (!canViewDish(detail, AuthContext.getUserId(), false)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "暂无访问权限");
        }
        detail.setIngredients(toIngredientItems(dishIngredientMapper.selectByDishId(id)));
        detail.setSteps(toStepItems(dishStepMapper.selectByDishId(id)));
        return detail;
    }

    @Override
    public String uploadImage(MultipartFile file) {
        try {
            // 验证文件
            FileUtils.validateImageFile(file);

            // 生成文件名
            String fileName = FileUtils.generateFileName(file);

            // 保存文件
            FileUtils.saveFile(file, basePath, fileName);

            // 返回访问URL
            return accessUrl + fileName;
        } catch (IOException e) {
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DishDetailResponse createDish(DishUpsertRequest dishCreateDTO) {
        String currentUserId = AuthContext.requireUserId();
        assertMenuLimit(currentUserId);
        validateDishRequest(currentUserId, dishCreateDTO);
        Dish dish = new Dish();
        applyDishRequest(currentUserId, dish, dishCreateDTO);
        LocalDateTime now = LocalDateTime.now();
        dish.setOwnerUserId(currentUserId);
        dish.setCreatedAt(now);
        dish.setUpdatedAt(now);
        dishMapper.insert(dish);
        replaceVisibilityCircles(dish.getId(), dishCreateDTO);
        replaceChildren(dish.getId(), dishCreateDTO);
        return getDishById(dish.getId());
    }

    @Override
    @Transactional
    public DishDetailResponse updateDish(String id, DishUpsertRequest dishUpdateDTO) {
        String currentUserId = AuthContext.requireUserId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "菜品不存在");
        }
        if (!Objects.equals(dish.getOwnerUserId(), currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只能编辑自己的菜谱");
        }
        validateDishRequest(currentUserId, dishUpdateDTO);
        applyDishRequest(currentUserId, dish, dishUpdateDTO);
        dish.setUpdatedAt(LocalDateTime.now());
        dishMapper.updateById(dish);
        replaceVisibilityCircles(id, dishUpdateDTO);
        replaceChildren(id, dishUpdateDTO);
        return getDishById(id);
    }

    @Override
    public DishAiAnalysisResponse analyzeDishByAi(DishAiAnalysisRequest request) {
        String currentUserId = AuthContext.requireUserId();
        if (request == null || isBlank(request.getImage())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先上传菜品图片");
        }
        vipService.assertCanUseAi(currentUserId);
        DishAiAnalysisResponse response = dishAiService.analyzeDish(request.getImage(), request.getName());
        response.setUsage(vipService.consumeAiUsage(currentUserId));
        return response;
    }

    @Override
    public DishAiAnalysisResponse importDishByAi(DishAiImportRequest request) {
        String currentUserId = AuthContext.requireUserId();
        String text = request == null ? "" : trimToEmpty(request.getText());
        List<String> images = request == null ? new ArrayList<>() : normalizeAiImportImages(request.getImages());
        if (isBlank(text) && images.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先粘贴菜谱文字或上传截图");
        }
        if (images.size() > MAX_AI_IMPORT_IMAGE_COUNT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "最多上传3张图片");
        }
        vipService.assertCanUseAi(currentUserId);
        DishAiAnalysisResponse response = dishAiService.importDish(text, images);
        response.setUsage(vipService.consumeAiUsage(currentUserId));
        return response;
    }

    @Override
    @Transactional
    public void deleteDish(String id) {
        String currentUserId = AuthContext.requireUserId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "菜品不存在");
        }
        if (!Objects.equals(dish.getOwnerUserId(), currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只能删除自己的菜谱");
        }
        dishVisibilityCircleMapper.delete(new QueryWrapper<DishVisibilityCircle>().eq("dish_id", id));
        dishIngredientMapper.deleteByDishId(id);
        dishStepMapper.deleteByDishId(id);
        dishMapper.deleteById(id);
    }

    private void applyDishRequest(String userId, Dish dish, DishUpsertRequest request) {
        dish.setName(request.getName());
        dish.setImage(request.getImage());
        dish.setDescription(request.getDescription());
        dish.setCategoryId(request.getCategoryId());
        dish.setCookTimeMinutes(request.getCookTimeMinutes());
        dish.setDifficulty(request.getDifficulty());
        dish.setServings(request.getServings());
        dish.setVisibility(resolveRequestedVisibility(userId, request));
        dish.setStatus(1);
    }

    private void replaceChildren(String dishId, DishUpsertRequest request) {
        dishIngredientMapper.deleteByDishId(dishId);
        dishStepMapper.deleteByDishId(dishId);

        List<IngredientItem> ingredients = request.getIngredients() == null
                ? new ArrayList<>()
                : request.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            IngredientItem item = ingredients.get(i);
            if (item == null || isBlank(item.getName())) {
                continue;
            }
            DishIngredient ingredient = new DishIngredient();
            ingredient.setDishId(dishId);
            ingredient.setName(item.getName());
            ingredient.setAmount(isBlank(item.getAmount()) ? "" : item.getAmount().trim());
            ingredient.setSort(item.getSort() == null ? i + 1 : item.getSort());
            dishIngredientMapper.insert(ingredient);
        }

        List<StepItem> steps = request.getSteps() == null ? new ArrayList<>() : request.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            StepItem item = steps.get(i);
            if (item == null || isBlank(item.getContent())) {
                continue;
            }
            DishStep step = new DishStep();
            step.setDishId(dishId);
            step.setStepNo(item.getStepNo() == null ? i + 1 : item.getStepNo());
            step.setContent(item.getContent());
            dishStepMapper.insert(step);
        }
    }

    private void replaceVisibilityCircles(String dishId, DishUpsertRequest request) {
        dishVisibilityCircleMapper.delete(new QueryWrapper<DishVisibilityCircle>().eq("dish_id", dishId));
        String userId = AuthContext.requireUserId();
        String visibility = resolveRequestedVisibility(userId, request);
        if (!VisibilityUtils.VISIBILITY_CIRCLE.equals(visibility)) {
            return;
        }
        for (String circleId : resolveRequestedCircleIds(userId, request, visibility)) {
            DishVisibilityCircle relation = new DishVisibilityCircle();
            relation.setDishId(dishId);
            relation.setCircleId(circleId);
            relation.setCreatedAt(LocalDateTime.now());
            dishVisibilityCircleMapper.insert(relation);
        }
    }

    private List<IngredientItem> toIngredientItems(List<DishIngredient> ingredients) {
        List<IngredientItem> result = new ArrayList<>();
        for (DishIngredient ingredient : ingredients) {
            IngredientItem item = new IngredientItem();
            item.setName(ingredient.getName());
            item.setAmount(ingredient.getAmount());
            item.setSort(ingredient.getSort());
            result.add(item);
        }
        return result;
    }

    private List<StepItem> toStepItems(List<DishStep> steps) {
        List<StepItem> result = new ArrayList<>();
        for (DishStep step : steps) {
            StepItem item = new StepItem();
            item.setStepNo(step.getStepNo());
            item.setContent(step.getContent());
            result.add(item);
        }
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> normalizeAiImportImages(List<String> images) {
        if (images == null) {
            return new ArrayList<>();
        }
        return images.stream()
                .map(this::trimToEmpty)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    private List<DishSummaryResponse> filterHomeDishes(
            List<DishSummaryResponse> source,
            String currentUserId,
            Predicate<DishSummaryResponse> extraFilter) {
        List<DishSummaryResponse> dishes = source.stream()
                .filter(extraFilter)
                .filter(item -> canViewDish(item, currentUserId, false))
                .limit(HOME_FEATURED_LIST_LIMIT)
                .collect(Collectors.toList());
        hydrateIngredientNames(dishes);
        return dishes;
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

    private boolean canViewDish(DishSummaryResponse dish, String viewerUserId, boolean circleMode) {
        return menuVisibilitySupport.canViewDish(dish, viewerUserId, circleMode);
    }

    private boolean canViewDish(DishDetailResponse dish, String viewerUserId, boolean circleMode) {
        return menuVisibilitySupport.canViewDish(dish, viewerUserId, circleMode);
    }

    private void validateDishRequest(String userId, DishUpsertRequest request) {
        String visibility = resolveRequestedVisibility(userId, request);
        if (!VisibilityUtils.isSupportedDishVisibility(visibility)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "无效的菜单权限");
        }
        if (VisibilityUtils.DISH_VISIBILITY_INHERIT.equals(visibility)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "菜单权限必须是具体值");
        }
        List<String> circleIds = resolveRequestedCircleIds(userId, request, visibility);
        if (VisibilityUtils.VISIBILITY_CIRCLE.equals(visibility) && circleIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "指定圈子权限至少选择一个圈子");
        }
    }

    private void assertMenuLimit(String userId) {
        int limit = vipService.getMenuLimit(userId);
        Long count = dishMapper.selectCount(new QueryWrapper<Dish>()
                .eq("owner_user_id", userId)
                .eq("status", 1));
        if (count != null && count >= limit) {
            String message = limit <= 50
                    ? "普通用户最多创建 50 道菜谱，开通 VIP 可提升至 500 道"
                    : "VIP 用户最多创建 500 道菜谱";
            throw new ApiException(HttpStatus.FORBIDDEN, message);
        }
    }

    private String resolveRequestedVisibility(String userId, DishUpsertRequest request) {
        String requestedVisibility = VisibilityUtils.normalizeDishVisibility(request == null ? null : request.getVisibility());
        if (VisibilityUtils.DISH_VISIBILITY_INHERIT.equals(requestedVisibility)) {
            return menuVisibilitySupport.resolveDefaultVisibility(userId);
        }
        return requestedVisibility;
    }

    private List<String> resolveRequestedCircleIds(String userId, DishUpsertRequest request, String resolvedVisibility) {
        if (!VisibilityUtils.VISIBILITY_CIRCLE.equals(resolvedVisibility)) {
            return Collections.emptyList();
        }
        String requestedVisibility = VisibilityUtils.normalizeDishVisibility(request == null ? null : request.getVisibility());
        if (VisibilityUtils.DISH_VISIBILITY_INHERIT.equals(requestedVisibility)) {
            return menuVisibilitySupport.getDefaultMenuCircleIds(userId);
        }
        return normalizedOwnedCircleIds(userId, request == null ? null : request.getVisibilityCircleIds());
    }

    private List<String> normalizedOwnedCircleIds(String userId, List<String> circleIds) {
        List<String> normalized = circleIds == null
                ? Collections.emptyList()
                : circleIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (normalized.isEmpty()) {
            return normalized;
        }
        long count = buddyCircleMemberMapper.selectCount(new QueryWrapper<BuddyCircleMember>()
                .eq("user_id", userId)
                .in("circle_id", normalized));
        if (count != normalized.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "指定圈子里包含你未加入的圈子");
        }
        return normalized;
    }

}
