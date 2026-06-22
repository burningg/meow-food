package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.PetClaimRequest;
import com.panghu.food.dto.PetRenameRequest;
import com.panghu.food.dto.PetResponse;
import com.panghu.food.entity.UserPet;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.CirclePlanMapper;
import com.panghu.food.mapper.DishMapper;
import com.panghu.food.mapper.UserPetMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PetServiceImpl implements PetService {
    private static final int ACTIVITY_WINDOW_DAYS = 15;
    private static final int FULLNESS_PER_RECIPE = 20;
    private static final String MOOD_HAPPY = "happy";
    private static final String MOOD_BORED = "bored";

    private static final Map<String, PetDefinition> PET_DEFINITIONS = Map.of(
            "tabby_cat", new PetDefinition("狸花猫"),
            "corgi", new PetDefinition("柯基")
    );

    private static final List<String> TODAY_STORIES = List.of(
            "%s把小鱼干藏在菜单夹里，又悄悄巡视了一遍你的晚餐菜谱。",
            "%s今天认真闻了闻锅铲，像是在确认晚饭有没有按时出现。",
            "%s趴在冰箱旁边发呆，最后决定把期待写进小尾巴里。",
            "%s把计划表当成餐垫铺好，坐得端端正正等开饭。",
            "%s路过菜谱卡片时停了三秒，像是在挑今晚最想吃哪道。",
            "%s把饭碗推到显眼的位置，暗示已经非常委婉。",
            "%s今天学会了守着菜单，谁翻页都要先经过 TA 同意。",
            "%s在厨房门口打了个滚，假装自己只是路过。",
            "%s把收藏夹翻得哗啦响，好像发现了不得了的夜宵灵感。",
            "%s靠着计划本睡着了，梦里大概有热汤和米饭。",
            "%s认真盯着菜谱图片，尾巴尖开心地敲了两下地板。",
            "%s今天把饭点记得很牢，比闹钟还准一点点。",
            "%s闻到空气里的葱姜味，立刻精神了起来。",
            "%s把小爪子搭在菜单边上，像是在帮你占座。",
            "%s今天巡视了厨房三圈，宣布一切适合开饭。",
            "%s偷偷练习了饭前期待的表情，看起来很专业。",
            "%s把空碗擦得亮亮的，只差一道热菜登场。",
            "%s对着计划页点了点头，仿佛已经批准这顿安排。",
            "%s在菜谱旁边伸了个懒腰，顺便把幸福感拉满。",
            "%s今天对冰箱门很有礼貌，只看了五次。",
            "%s把菜单夹压在身下，表示这份灵感暂时归 TA 保管。",
            "%s听见锅盖响了一下，立刻坐直等后续剧情。",
            "%s把饭桌边的位置占好，留给你一个很小的空位。",
            "%s今天发现了一个新目标：把每个计划都等成晚饭。",
            "%s在厨房光影里晃了晃脑袋，看起来心情不错。",
            "%s悄悄把最喜欢的菜谱挪到最上面，动作非常自然。",
            "%s趴在计划旁边眯眼，像是在给明天的饭加油。",
            "%s把期待藏进耳朵里，但还是被你看出来了。",
            "%s今天没有捣乱，只是认真监督每一个饭点。",
            "%s围着菜单转了一圈，最后决定继续陪你想吃什么。"
    );

    private final UserPetMapper userPetMapper;
    private final DishMapper dishMapper;
    private final CirclePlanMapper circlePlanMapper;

    private Clock clock = Clock.systemDefaultZone();

    public PetServiceImpl(UserPetMapper userPetMapper,
                          DishMapper dishMapper,
                          CirclePlanMapper circlePlanMapper) {
        this.userPetMapper = userPetMapper;
        this.dishMapper = dishMapper;
        this.circlePlanMapper = circlePlanMapper;
    }

    @Override
    public PetResponse getMyPet() {
        String userId = AuthContext.requireUserId();
        UserPet pet = findUserPet(userId);
        if (pet == null) {
            PetResponse response = new PetResponse();
            response.setClaimed(false);
            return response;
        }
        return buildPetResponse(userId, pet);
    }

    @Override
    @Transactional
    public PetResponse claimPet(PetClaimRequest request) {
        String userId = AuthContext.requireUserId();
        if (findUserPet(userId) != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "你已经领取宠物了");
        }

        String petType = normalizePetType(request == null ? null : request.getPetType());
        PetDefinition definition = PET_DEFINITIONS.get(petType);
        if (definition == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "暂不支持这个宠物");
        }

        String name = validateName(request == null ? null : request.getName());

        LocalDateTime now = LocalDateTime.now(clock);
        UserPet pet = new UserPet();
        pet.setUserId(userId);
        pet.setPetType(petType);
        pet.setName(name);
        pet.setExperience(0);
        pet.setCreatedAt(now);
        pet.setUpdatedAt(now);
        userPetMapper.insert(pet);
        return buildPetResponse(userId, pet);
    }

    @Override
    @Transactional
    public PetResponse renamePet(PetRenameRequest request) {
        String userId = AuthContext.requireUserId();
        UserPet pet = findUserPet(userId);
        if (pet == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "你还没有领取宠物");
        }

        pet.setName(validateName(request == null ? null : request.getName()));
        pet.setUpdatedAt(LocalDateTime.now(clock));
        userPetMapper.updateById(pet);
        return buildPetResponse(userId, pet);
    }

    private PetResponse buildPetResponse(String userId, UserPet pet) {
        LocalDate today = LocalDate.now(clock);
        LocalDate windowStart = today.minusDays(ACTIVITY_WINDOW_DAYS - 1L);

        LocalDateTime latestDishAt = dishMapper.selectLatestCreatedAtByOwnerUserId(userId);
        LocalDateTime latestPlanAt = circlePlanMapper.selectLatestCreatedAtByCreatorUserId(userId);
        Mood mood = resolveMood(max(latestDishAt, latestPlanAt), windowStart);

        Long recipeCount = circlePlanMapper.countRecipesCreatedByUserInPlanDateRange(userId, windowStart, today);
        long safeRecipeCount = recipeCount == null ? 0L : recipeCount;
        int fullnessPercent = (int) Math.min(100L, safeRecipeCount * FULLNESS_PER_RECIPE);

        PetResponse response = new PetResponse();
        response.setClaimed(true);
        response.setId(pet.getId());
        response.setPetType(pet.getPetType());
        response.setPetTypeName(PET_DEFINITIONS.getOrDefault(pet.getPetType(), PET_DEFINITIONS.get("tabby_cat")).name());
        response.setName(pet.getName());
        response.setExperience(pet.getExperience() == null ? 0 : pet.getExperience());
        response.setCompanionDays(companionDays(pet.getCreatedAt(), today));
        response.setMoodCode(mood.code());
        response.setMoodLabel(mood.label());
        response.setFullnessPercent(fullnessPercent);
        response.setTodayStory(resolveTodayStory(userId, pet, today));
        response.setClaimedAt(pet.getCreatedAt());
        return response;
    }

    private UserPet findUserPet(String userId) {
        return userPetMapper.selectOne(new QueryWrapper<UserPet>()
                .eq("user_id", userId)
                .last("LIMIT 1"));
    }

    private Mood resolveMood(LocalDateTime latestActivityAt, LocalDate windowStart) {
        // 菜谱和计划都没有时默认开心；只要最近 15 天内有一次创建行为，就保持开心。
        if (latestActivityAt == null || !latestActivityAt.toLocalDate().isBefore(windowStart)) {
            return new Mood(MOOD_HAPPY, "开心");
        }
        return new Mood(MOOD_BORED, "无聊");
    }

    private int companionDays(LocalDateTime claimedAt, LocalDate today) {
        if (claimedAt == null) {
            return 0;
        }
        return Math.max(1, (int) ChronoUnit.DAYS.between(claimedAt.toLocalDate(), today) + 1);
    }

    private String resolveTodayStory(String userId, UserPet pet, LocalDate today) {
        // 今日小事按用户、宠物和日期生成稳定索引，避免同一天反复刷新时文案跳动。
        String seed = userId + ":" + pet.getId() + ":" + today;
        int index = Math.floorMod(seed.hashCode(), TODAY_STORIES.size());
        return String.format(TODAY_STORIES.get(index), pet.getName());
    }

    private LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isAfter(right) ? left : right;
    }

    private String normalizePetType(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim();
    }

    private String validateName(String value) {
        String name = normalizeName(value);
        if (name.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请给宠物起个名字");
        }
        if (name.codePointCount(0, name.length()) > 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "宠物名字最多 8 个字");
        }
        return name;
    }

    private record PetDefinition(String name) {
    }

    private record Mood(String code, String label) {
    }
}
