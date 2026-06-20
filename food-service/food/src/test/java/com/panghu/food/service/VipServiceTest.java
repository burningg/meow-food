package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.dto.PlanAiUsageResponse;
import com.panghu.food.dto.VipInfoResponse;
import com.panghu.food.dto.VipUsageResponse;
import com.panghu.food.entity.UserVip;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserVipMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VipServiceTest {
    private final UserVipMapper userVipMapper = mock(UserVipMapper.class);
    private final VipService vipService = new VipService(userVipMapper);

    @Test
    void getVipInfoResetsDailyUsageOnNewDay() {
        UserVip vip = activeVip();
        vip.setDailyRecipeAnalysisDate(LocalDate.now().minusDays(1));
        vip.setDailyRecipeAnalysisUsed(3);
        when(userVipMapper.selectOne(any(QueryWrapper.class))).thenReturn(vip);

        VipInfoResponse info = vipService.getVipInfo("user-1");

        assertThat(info.getDailyRecipeAnalysisUsed()).isZero();
        assertThat(info.getDailyRecipeAnalysisRemaining()).isEqualTo(5);
        verify(userVipMapper).updateById(vip);
    }

    @Test
    void assertCanUseAiRejectsNonVipUser() {
        UserVip vip = activeVip();
        vip.setIsVip(false);
        when(userVipMapper.selectOne(any(QueryWrapper.class))).thenReturn(vip);

        assertThatThrownBy(() -> vipService.assertCanUseAi("user-1"))
                .isInstanceOf(ApiException.class)
                .hasMessage("仅 VIP 可使用 AI 菜谱识别");
    }

    @Test
    void consumeAiUsageIncrementsUsedCount() {
        UserVip vip = activeVip();
        when(userVipMapper.selectOne(any(QueryWrapper.class))).thenReturn(vip);

        VipUsageResponse usage = vipService.consumeAiUsage("user-1");

        assertThat(usage.getUsedToday()).isEqualTo(2);
        assertThat(usage.getRemainingToday()).isEqualTo(3);
        verify(userVipMapper).updateById(vip);
    }

    @Test
    void consumePlanAiUsageUsesNormalMonthlyLimit() {
        UserVip vip = activeVip();
        vip.setIsVip(false);
        vip.setExpiresAt(null);
        vip.setMonthlyPlanAiUsed(9);
        when(userVipMapper.selectOne(any(QueryWrapper.class))).thenReturn(vip);

        PlanAiUsageResponse usage = vipService.consumePlanAiUsage("user-1");

        assertThat(usage.getMonthlyLimit()).isEqualTo(10);
        assertThat(usage.getUsedThisMonth()).isEqualTo(10);
        assertThat(usage.getRemainingThisMonth()).isZero();
        verify(userVipMapper).updateById(vip);
    }

    @Test
    void consumePlanAiUsageUsesVipMonthlyLimit() {
        UserVip vip = activeVip();
        vip.setMonthlyPlanAiUsed(29);
        when(userVipMapper.selectOne(any(QueryWrapper.class))).thenReturn(vip);

        PlanAiUsageResponse usage = vipService.consumePlanAiUsage("user-1");

        assertThat(usage.getMonthlyLimit()).isEqualTo(30);
        assertThat(usage.getUsedThisMonth()).isEqualTo(30);
        assertThat(usage.getRemainingThisMonth()).isZero();
        verify(userVipMapper).updateById(vip);
    }

    @Test
    void assertCanUsePlanAiRejectsMonthlyLimitExceeded() {
        UserVip vip = activeVip();
        vip.setMonthlyPlanAiUsed(30);
        when(userVipMapper.selectOne(any(QueryWrapper.class))).thenReturn(vip);

        assertThatThrownBy(() -> vipService.assertCanUsePlanAi("user-1"))
                .isInstanceOf(ApiException.class)
                .hasMessage("本月 AI 排菜次数已用完");
    }

    @Test
    void claimFreeTrialActivatesInactiveVipForOneMonth() {
        UserVip vip = activeVip();
        vip.setIsVip(false);
        vip.setVipLevel(null);
        vip.setOpenedAt(null);
        vip.setExpiresAt(null);
        vip.setOpenAmount(BigDecimal.ZERO);
        vip.setDailyRecipeAnalysisLimit(0);
        vip.setDailyRecipeAnalysisUsed(2);
        when(userVipMapper.selectOne(any(QueryWrapper.class))).thenReturn(vip);

        VipInfoResponse info = vipService.claimFreeTrial("user-1");

        assertThat(info.getVip()).isTrue();
        assertThat(info.getVipLevel()).isEqualTo("VIP");
        assertThat(info.getOpenAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(info.getDailyRecipeAnalysisLimit()).isEqualTo(3);
        assertThat(info.getDailyRecipeAnalysisUsed()).isZero();
        assertThat(info.getDailyRecipeAnalysisRemaining()).isEqualTo(3);
        assertThat(info.getOpenedAt()).isNotNull();
        assertThat(info.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
        verify(userVipMapper).updateById(vip);
    }

    @Test
    void claimFreeTrialDoesNotResetActiveVip() {
        UserVip vip = activeVip();
        LocalDateTime originalExpiresAt = vip.getExpiresAt();
        when(userVipMapper.selectOne(any(QueryWrapper.class))).thenReturn(vip);

        VipInfoResponse info = vipService.claimFreeTrial("user-1");

        assertThat(info.getVip()).isTrue();
        assertThat(info.getVipLevel()).isEqualTo("VIP 3");
        assertThat(info.getExpiresAt()).isEqualTo(originalExpiresAt);
        verify(userVipMapper, never()).updateById(vip);
    }

    @Test
    void activatePaidYearActivatesVipForOneYear() {
        UserVip vip = activeVip();
        vip.setIsVip(false);
        vip.setVipLevel(null);
        vip.setExpiresAt(null);
        vip.setOpenAmount(BigDecimal.ZERO);
        vip.setDailyRecipeAnalysisLimit(0);
        vip.setDailyRecipeAnalysisUsed(2);
        when(userVipMapper.selectOne(any(QueryWrapper.class))).thenReturn(vip);

        VipInfoResponse info = vipService.activatePaidYear("user-1", BigDecimal.valueOf(2.90));

        assertThat(info.getVip()).isTrue();
        assertThat(info.getVipLevel()).isEqualTo("VIP");
        assertThat(info.getOpenAmount()).isEqualByComparingTo(BigDecimal.valueOf(2.90));
        assertThat(info.getDailyRecipeAnalysisLimit()).isEqualTo(3);
        assertThat(info.getDailyRecipeAnalysisUsed()).isZero();
        assertThat(info.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(364));
        verify(userVipMapper).updateById(vip);
    }

    @Test
    void normalUserLimitsAreLowerThanVipLimits() {
        UserVip vip = activeVip();
        vip.setIsVip(false);
        vip.setExpiresAt(null);
        when(userVipMapper.selectOne(any(QueryWrapper.class))).thenReturn(vip);

        assertThat(vipService.getCircleLimit("user-1")).isEqualTo(3);
        assertThat(vipService.getMenuLimit("user-1")).isEqualTo(50);
    }

    @Test
    void activeVipGetsExpandedLimits() {
        when(userVipMapper.selectOne(any(QueryWrapper.class))).thenReturn(activeVip());

        assertThat(vipService.getCircleLimit("user-1")).isEqualTo(10);
        assertThat(vipService.getMenuLimit("user-1")).isEqualTo(500);
    }

    private UserVip activeVip() {
        UserVip vip = new UserVip();
        vip.setId("vip-1");
        vip.setUserId("user-1");
        vip.setVipLevel("VIP 3");
        vip.setIsVip(true);
        vip.setOpenedAt(LocalDateTime.now().minusDays(1));
        vip.setExpiresAt(LocalDateTime.now().plusDays(10));
        vip.setOpenAmount(BigDecimal.valueOf(199));
        vip.setDailyRecipeAnalysisLimit(5);
        vip.setDailyRecipeAnalysisUsed(1);
        vip.setDailyRecipeAnalysisDate(LocalDate.now());
        vip.setMonthlyPlanAiUsed(1);
        vip.setMonthlyPlanAiMonth(YearMonth.now().toString());
        return vip;
    }
}
