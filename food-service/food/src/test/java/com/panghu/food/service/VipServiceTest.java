package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.dto.VipInfoResponse;
import com.panghu.food.dto.VipUsageResponse;
import com.panghu.food.entity.UserVip;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserVipMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        return vip;
    }
}
