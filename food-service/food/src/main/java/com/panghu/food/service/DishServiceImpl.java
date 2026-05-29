package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.DishDetailResponse;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.dto.DishUpsertRequest;
import com.panghu.food.dto.IngredientItem;
import com.panghu.food.dto.StepItem;
import com.panghu.food.entity.ActivityFeed;
import com.panghu.food.entity.Dish;
import com.panghu.food.entity.DishIngredient;
import com.panghu.food.entity.DishStep;
import com.panghu.food.entity.FriendRelation;
import com.panghu.food.entity.UserProfileSettings;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.ActivityFeedMapper;
import com.panghu.food.mapper.DishMapper;
import com.panghu.food.mapper.DishIngredientMapper;
import com.panghu.food.mapper.DishStepMapper;
import com.panghu.food.mapper.FriendRelationMapper;
import com.panghu.food.mapper.UserProfileSettingsMapper;
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
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl implements DishService {
    private static final int HOME_LIST_LIMIT = 3;

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
    private UserProfileSettingsMapper userProfileSettingsMapper;

    @Autowired
    private FriendRelationMapper friendRelationMapper;

    @Autowired
    private ActivityFeedMapper activityFeedMapper;

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
        return dishes.stream()
                .map(this::applyEffectiveVisibility)
                .filter(item -> categoryId == null || categoryId.equals(item.getCategoryId()))
                .filter(item -> canViewDish(item, currentUserId, "circle".equals(scope)))
                .collect(Collectors.toList());
    }

    @Override
    public List<DishSummaryResponse> getFeaturedDishes(String categoryId) {
        String currentUserId = AuthContext.getUserId();
        List<DishSummaryResponse> source = categoryId == null || categoryId.trim().isEmpty()
                ? dishMapper.selectAllActive()
                : dishMapper.selectByCategoryId(categoryId, null);
        return filterHomeDishes(source, currentUserId, item -> Boolean.TRUE.equals(item.getIsFeatured()));
    }

    @Override
    public List<DishSummaryResponse> getRecentDishes() {
        String currentUserId = AuthContext.getUserId();
        if (currentUserId == null) {
            return new ArrayList<>();
        }
        return dishMapper.selectByOwnerUserId(currentUserId).stream()
                .map(this::applyEffectiveVisibility)
                .limit(HOME_LIST_LIMIT)
                .collect(Collectors.toList());
    }

    @Override
    public DishDetailResponse getDishById(String id) {
        DishDetailResponse detail = dishMapper.selectDishDetailById(id);
        if (detail == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "菜品不存在");
        }
        applyEffectiveVisibility(detail);
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
        Dish dish = new Dish();
        applyDishRequest(dish, dishCreateDTO);
        LocalDateTime now = LocalDateTime.now();
        dish.setOwnerUserId(currentUserId);
        dish.setCreatedAt(now);
        dish.setUpdatedAt(now);
        dishMapper.insert(dish);
        replaceChildren(dish.getId(), dishCreateDTO);
        createActivity(currentUserId, dish.getId(), "dish_created", resolveActivityScope(dish.getVisibility(), currentUserId));
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
        applyDishRequest(dish, dishUpdateDTO);
        dish.setUpdatedAt(LocalDateTime.now());
        dishMapper.updateById(dish);
        replaceChildren(id, dishUpdateDTO);
        createActivity(currentUserId, dish.getId(), "dish_updated", resolveActivityScope(dish.getVisibility(), currentUserId));
        return getDishById(id);
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
        dishIngredientMapper.deleteByDishId(id);
        dishStepMapper.deleteByDishId(id);
        dishMapper.deleteById(id);
    }

    private void applyDishRequest(Dish dish, DishUpsertRequest request) {
        dish.setName(request.getName());
        dish.setImage(request.getImage());
        dish.setDescription(request.getDescription());
        dish.setCategoryId(request.getCategoryId());
        dish.setCookTimeMinutes(request.getCookTimeMinutes());
        dish.setDifficulty(request.getDifficulty());
        dish.setServings(request.getServings());
        dish.setVisibility(VisibilityUtils.normalizeDishVisibility(request.getVisibility()));
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
            if (item == null || isBlank(item.getName()) || isBlank(item.getAmount())) {
                continue;
            }
            DishIngredient ingredient = new DishIngredient();
            ingredient.setDishId(dishId);
            ingredient.setName(item.getName());
            ingredient.setAmount(item.getAmount());
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

    private String resolveDefaultVisibility(String ownerUserId) {
        UserProfileSettings settings = userProfileSettingsMapper.selectById(ownerUserId);
        return settings == null ? "friends" : VisibilityUtils.normalizeProfileVisibility(settings.getDefaultMenuVisibility());
    }

    private List<DishSummaryResponse> filterHomeDishes(
            List<DishSummaryResponse> source,
            String currentUserId,
            Predicate<DishSummaryResponse> extraFilter) {
        return source.stream()
                .map(this::applyEffectiveVisibility)
                .filter(extraFilter)
                .filter(item -> canViewDish(item, currentUserId, false))
                .limit(HOME_LIST_LIMIT)
                .collect(Collectors.toList());
    }

    private DishSummaryResponse applyEffectiveVisibility(DishSummaryResponse item) {
        if (item != null) {
            item.setEffectiveVisibility(VisibilityUtils.effectiveVisibility(item.getVisibility(), resolveDefaultVisibility(item.getOwnerUserId())));
        }
        return item;
    }

    private DishDetailResponse applyEffectiveVisibility(DishDetailResponse item) {
        if (item != null) {
            item.setEffectiveVisibility(VisibilityUtils.effectiveVisibility(item.getVisibility(), resolveDefaultVisibility(item.getOwnerUserId())));
        }
        return item;
    }

    private boolean isFriend(String currentUserId, String ownerUserId) {
        if (currentUserId == null) {
            return false;
        }
        return friendRelationMapper.selectCount(new QueryWrapper<FriendRelation>()
                .eq("user_id", currentUserId)
                .eq("friend_user_id", ownerUserId)) > 0;
    }

    private boolean canViewDish(DishSummaryResponse dish, String viewerUserId, boolean circleMode) {
        if (dish == null) {
            return false;
        }
        if (viewerUserId != null && Objects.equals(viewerUserId, dish.getOwnerUserId())) {
            return true;
        }
        String visibility = dish.getEffectiveVisibility();
        if ("public".equals(visibility)) {
            return true;
        }
        if ("private".equals(visibility)) {
            return false;
        }
        if ("friends".equals(visibility)) {
            return viewerUserId != null && (isFriend(viewerUserId, dish.getOwnerUserId()) || circleMode);
        }
        return false;
    }

    private boolean canViewDish(DishDetailResponse dish, String viewerUserId, boolean circleMode) {
        if (dish == null) {
            return false;
        }
        if (viewerUserId != null && Objects.equals(viewerUserId, dish.getOwnerUserId())) {
            return true;
        }
        String visibility = dish.getEffectiveVisibility();
        if ("public".equals(visibility)) {
            return true;
        }
        if ("private".equals(visibility)) {
            return false;
        }
        if ("friends".equals(visibility)) {
            return viewerUserId != null && (isFriend(viewerUserId, dish.getOwnerUserId()) || circleMode);
        }
        return false;
    }

    private void createActivity(String userId, String dishId, String type, String visibilityScope) {
        if ("private".equals(visibilityScope)) {
            return;
        }
        ActivityFeed feed = new ActivityFeed();
        feed.setActorUserId(userId);
        feed.setDishId(dishId);
        feed.setActivityType(type);
        feed.setVisibilityScope(visibilityScope);
        feed.setCreatedAt(LocalDateTime.now());
        activityFeedMapper.insert(feed);
    }

    private String resolveActivityScope(String dishVisibility, String ownerUserId) {
        String effectiveVisibility = VisibilityUtils.effectiveVisibility(dishVisibility, resolveDefaultVisibility(ownerUserId));
        if ("private".equals(effectiveVisibility)) {
            return "private";
        }
        if ("friends".equals(effectiveVisibility)) {
            return "friends";
        }
        return "public";
    }

}
