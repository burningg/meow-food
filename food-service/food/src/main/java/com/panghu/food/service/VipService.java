package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.dto.PlanAiUsageResponse;
import com.panghu.food.dto.VipInfoResponse;
import com.panghu.food.dto.VipUsageResponse;
import com.panghu.food.entity.UserVip;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserVipMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
public class VipService {
    private static final String DEFAULT_VIP_LEVEL = "VIP";
    private static final int FREE_TRIAL_DAILY_RECIPE_ANALYSIS_LIMIT = 3;
    private static final int NORMAL_CIRCLE_LIMIT = 3;
    private static final int VIP_CIRCLE_LIMIT = 10;
    private static final int NORMAL_MENU_LIMIT = 50;
    private static final int VIP_MENU_LIMIT = 500;
    private static final int NORMAL_MONTHLY_PLAN_AI_LIMIT = 30;
    private static final int VIP_MONTHLY_PLAN_AI_LIMIT = 90;

    private final UserVipMapper userVipMapper;

    public VipService(UserVipMapper userVipMapper) {
        this.userVipMapper = userVipMapper;
    }

    @Transactional
    public UserVip getOrCreateByUserId(String userId) {
        UserVip vip = userVipMapper.selectOne(new QueryWrapper<UserVip>()
                .eq("user_id", userId)
                .last("LIMIT 1"));
        if (vip == null) {
            vip = new UserVip();
            vip.setUserId(userId);
            vip.setVipLevel(null);
            vip.setIsVip(false);
            vip.setOpenAmount(BigDecimal.ZERO);
            vip.setDailyRecipeAnalysisLimit(0);
            vip.setDailyRecipeAnalysisUsed(0);
            vip.setDailyRecipeAnalysisDate(LocalDate.now());
            vip.setMonthlyPlanAiUsed(0);
            vip.setMonthlyPlanAiMonth(YearMonth.now().toString());
            LocalDateTime now = LocalDateTime.now();
            vip.setCreatedAt(now);
            vip.setUpdatedAt(now);
            userVipMapper.insert(vip);
        }
        return refreshPlanAiUsageIfNeeded(refreshDailyUsageIfNeeded(vip));
    }

    @Transactional
    public VipInfoResponse getVipInfo(String userId) {
        return toVipInfo(getOrCreateByUserId(userId));
    }

    @Transactional
    public int getCircleLimit(String userId) {
        return isVipActive(getOrCreateByUserId(userId)) ? VIP_CIRCLE_LIMIT : NORMAL_CIRCLE_LIMIT;
    }

    @Transactional
    public int getMenuLimit(String userId) {
        return isVipActive(getOrCreateByUserId(userId)) ? VIP_MENU_LIMIT : NORMAL_MENU_LIMIT;
    }

    @Transactional
    public void assertCanUsePlanAi(String userId) {
        UserVip vip = getOrCreateByUserId(userId);
        if (remainingPlanAi(vip) <= 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "本月 AI 排菜次数已用完");
        }
    }

    @Transactional
    public PlanAiUsageResponse consumePlanAiUsage(String userId) {
        UserVip vip = getOrCreateByUserId(userId);
        if (remainingPlanAi(vip) <= 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "本月 AI 排菜次数已用完");
        }
        vip.setMonthlyPlanAiUsed(safeInt(vip.getMonthlyPlanAiUsed()) + 1);
        vip.setUpdatedAt(LocalDateTime.now());
        userVipMapper.updateById(vip);
        return toPlanAiUsage(vip);
    }

    @Transactional
    public VipInfoResponse claimFreeTrial(String userId) {
        UserVip vip = getOrCreateByUserId(userId);
        if (isVipActive(vip)) {
            return toVipInfo(vip);
        }

        LocalDateTime now = LocalDateTime.now();
        vip.setVipLevel(DEFAULT_VIP_LEVEL);
        vip.setIsVip(true);
        vip.setOpenedAt(now);
        vip.setExpiresAt(now.plusMonths(1));
        vip.setOpenAmount(BigDecimal.ZERO);
        vip.setDailyRecipeAnalysisLimit(FREE_TRIAL_DAILY_RECIPE_ANALYSIS_LIMIT);
        vip.setDailyRecipeAnalysisUsed(0);
        vip.setDailyRecipeAnalysisDate(LocalDate.now());
        vip.setUpdatedAt(now);
        userVipMapper.updateById(vip);
        return toVipInfo(vip);
    }

    @Transactional
    public VipInfoResponse activatePaidYear(String userId, BigDecimal amount) {
        UserVip vip = getOrCreateByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        vip.setVipLevel(DEFAULT_VIP_LEVEL);
        vip.setIsVip(true);
        vip.setOpenedAt(now);
        vip.setExpiresAt(now.plusYears(1));
        vip.setOpenAmount(amount);
        vip.setDailyRecipeAnalysisLimit(FREE_TRIAL_DAILY_RECIPE_ANALYSIS_LIMIT);
        vip.setDailyRecipeAnalysisUsed(0);
        vip.setDailyRecipeAnalysisDate(LocalDate.now());
        vip.setUpdatedAt(now);
        userVipMapper.updateById(vip);
        return toVipInfo(vip);
    }

    public boolean isVipActive(UserVip vip) {
        return vip != null
                && Boolean.TRUE.equals(vip.getIsVip())
                && vip.getExpiresAt() != null
                && !vip.getExpiresAt().isBefore(LocalDateTime.now());
    }

    @Transactional
    public void assertCanUseAi(String userId) {
        UserVip vip = getOrCreateByUserId(userId);
        if (!Boolean.TRUE.equals(vip.getIsVip())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "仅 VIP 可使用 AI 菜谱识别");
        }
        if (vip.getExpiresAt() == null || vip.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "VIP 已过期");
        }
        if (remaining(vip) <= 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "今日 AI 菜谱分析次数已用完");
        }
    }

    @Transactional
    public VipUsageResponse consumeAiUsage(String userId) {
        UserVip vip = getOrCreateByUserId(userId);
        if (remaining(vip) <= 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "今日 AI 菜谱分析次数已用完");
        }
        vip.setDailyRecipeAnalysisUsed(safeInt(vip.getDailyRecipeAnalysisUsed()) + 1);
        vip.setUpdatedAt(LocalDateTime.now());
        userVipMapper.updateById(vip);
        return toUsage(vip);
    }

    private UserVip refreshDailyUsageIfNeeded(UserVip vip) {
        LocalDate today = LocalDate.now();
        if (vip.getDailyRecipeAnalysisDate() == null || !today.equals(vip.getDailyRecipeAnalysisDate())) {
            vip.setDailyRecipeAnalysisDate(today);
            vip.setDailyRecipeAnalysisUsed(0);
            vip.setUpdatedAt(LocalDateTime.now());
            userVipMapper.updateById(vip);
        }
        if (vip.getDailyRecipeAnalysisLimit() == null) {
            vip.setDailyRecipeAnalysisLimit(0);
        }
        if (vip.getDailyRecipeAnalysisUsed() == null) {
            vip.setDailyRecipeAnalysisUsed(0);
        }
        return vip;
    }

    private UserVip refreshPlanAiUsageIfNeeded(UserVip vip) {
        String currentMonth = YearMonth.now().toString();
        if (vip.getMonthlyPlanAiMonth() == null || !currentMonth.equals(vip.getMonthlyPlanAiMonth())) {
            vip.setMonthlyPlanAiMonth(currentMonth);
            vip.setMonthlyPlanAiUsed(0);
            vip.setUpdatedAt(LocalDateTime.now());
            userVipMapper.updateById(vip);
        }
        if (vip.getMonthlyPlanAiUsed() == null) {
            vip.setMonthlyPlanAiUsed(0);
        }
        return vip;
    }

    private VipInfoResponse toVipInfo(UserVip vip) {
        VipInfoResponse response = new VipInfoResponse();
        response.setVip(isVipActive(vip));
        response.setVipLevel(vip.getVipLevel());
        response.setOpenedAt(vip.getOpenedAt());
        response.setExpiresAt(vip.getExpiresAt());
        response.setOpenAmount(vip.getOpenAmount());
        response.setDailyRecipeAnalysisLimit(safeInt(vip.getDailyRecipeAnalysisLimit()));
        response.setDailyRecipeAnalysisUsed(safeInt(vip.getDailyRecipeAnalysisUsed()));
        response.setDailyRecipeAnalysisRemaining(Math.max(remaining(vip), 0));
        response.setMonthlyPlanAiLimit(planAiLimit(vip));
        response.setMonthlyPlanAiUsed(safeInt(vip.getMonthlyPlanAiUsed()));
        response.setMonthlyPlanAiRemaining(Math.max(remainingPlanAi(vip), 0));
        return response;
    }

    private VipUsageResponse toUsage(UserVip vip) {
        VipUsageResponse response = new VipUsageResponse();
        response.setDailyLimit(safeInt(vip.getDailyRecipeAnalysisLimit()));
        response.setUsedToday(safeInt(vip.getDailyRecipeAnalysisUsed()));
        response.setRemainingToday(Math.max(remaining(vip), 0));
        return response;
    }

    private int remaining(UserVip vip) {
        return safeInt(vip.getDailyRecipeAnalysisLimit()) - safeInt(vip.getDailyRecipeAnalysisUsed());
    }

    private PlanAiUsageResponse toPlanAiUsage(UserVip vip) {
        PlanAiUsageResponse response = new PlanAiUsageResponse();
        response.setMonthlyLimit(planAiLimit(vip));
        response.setUsedThisMonth(safeInt(vip.getMonthlyPlanAiUsed()));
        response.setRemainingThisMonth(Math.max(remainingPlanAi(vip), 0));
        return response;
    }

    private int remainingPlanAi(UserVip vip) {
        return planAiLimit(vip) - safeInt(vip.getMonthlyPlanAiUsed());
    }

    private int planAiLimit(UserVip vip) {
        return isVipActive(vip) ? VIP_MONTHLY_PLAN_AI_LIMIT : NORMAL_MONTHLY_PLAN_AI_LIMIT;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
