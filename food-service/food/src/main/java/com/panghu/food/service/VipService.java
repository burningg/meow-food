package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

@Service
public class VipService {
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
            LocalDateTime now = LocalDateTime.now();
            vip.setCreatedAt(now);
            vip.setUpdatedAt(now);
            userVipMapper.insert(vip);
        }
        return refreshDailyUsageIfNeeded(vip);
    }

    @Transactional
    public VipInfoResponse getVipInfo(String userId) {
        return toVipInfo(getOrCreateByUserId(userId));
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

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
