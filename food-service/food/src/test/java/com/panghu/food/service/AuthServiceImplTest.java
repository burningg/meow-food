package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.auth.JwtTokenUtil;
import com.panghu.food.component.WechatComponent;
import com.panghu.food.dto.AuthLoginResponse;
import com.panghu.food.dto.AuthRegisterRequest;
import com.panghu.food.dto.VipInfoResponse;
import com.panghu.food.entity.BuddyCircle;
import com.panghu.food.entity.BuddyCircleMember;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.entity.UserProfileSettings;
import com.panghu.food.mapper.BuddyCircleMapper;
import com.panghu.food.mapper.BuddyCircleMemberMapper;
import com.panghu.food.mapper.UserAccountMapper;
import com.panghu.food.mapper.UserProfileSettingsMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {
    private final UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
    private final UserProfileSettingsMapper userProfileSettingsMapper = mock(UserProfileSettingsMapper.class);
    private final JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
    private final WechatComponent wechatComponent = mock(WechatComponent.class);
    private final VipService vipService = mock(VipService.class);
    private final MenuVisibilitySupport menuVisibilitySupport = mock(MenuVisibilitySupport.class);
    private final BuddyCircleMapper buddyCircleMapper = mock(BuddyCircleMapper.class);
    private final BuddyCircleMemberMapper buddyCircleMemberMapper = mock(BuddyCircleMemberMapper.class);

    private final AuthServiceImpl authService = new AuthServiceImpl(
            userAccountMapper,
            userProfileSettingsMapper,
            jwtTokenUtil,
            wechatComponent,
            vipService,
            menuVisibilitySupport,
            buddyCircleMapper,
            buddyCircleMemberMapper);

    @Test
    void registerCreatesDefaultCircleForNewUser() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setAccount("alice");
        request.setPassword("secret123");

        when(userAccountMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId("user-1");
            return 1;
        }).when(userAccountMapper).insert(any(UserAccount.class));
        doAnswer(invocation -> {
            BuddyCircle circle = invocation.getArgument(0);
            circle.setId("circle-1");
            return 1;
        }).when(buddyCircleMapper).insert(any(BuddyCircle.class));
        when(jwtTokenUtil.createToken("user-1")).thenReturn("token-1");
        when(userProfileSettingsMapper.selectById("user-1")).thenReturn(existingSettings("user-1"));
        when(vipService.getVipInfo("user-1")).thenReturn(vipInfo());
        when(menuVisibilitySupport.getDefaultMenuCircleIds("user-1")).thenReturn(java.util.List.of());

        AuthLoginResponse response = authService.register(request);

        ArgumentCaptor<BuddyCircle> circleCaptor = ArgumentCaptor.forClass(BuddyCircle.class);
        verify(buddyCircleMapper).insert(circleCaptor.capture());
        assertThat(circleCaptor.getValue().getName()).isEqualTo("alice的美食圈");
        assertThat(circleCaptor.getValue().getOwnerUserId()).isEqualTo("user-1");

        ArgumentCaptor<BuddyCircleMember> memberCaptor = ArgumentCaptor.forClass(BuddyCircleMember.class);
        verify(buddyCircleMemberMapper).insert(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getCircleId()).isEqualTo("circle-1");
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(memberCaptor.getValue().getRole()).isEqualTo("owner");

        assertThat(response.getToken()).isEqualTo("token-1");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getAccount()).isEqualTo("alice");
        assertThat(response.getUser().getNickname()).isEqualTo("alice");
    }

    private UserProfileSettings existingSettings(String userId) {
        UserProfileSettings settings = new UserProfileSettings();
        settings.setUserId(userId);
        settings.setDefaultMenuVisibility(VisibilityUtils.DEFAULT_PROFILE_VISIBILITY);
        settings.setAllowFriendFeed(true);
        return settings;
    }

    private VipInfoResponse vipInfo() {
        VipInfoResponse response = new VipInfoResponse();
        response.setVip(false);
        response.setVipLevel(null);
        return response;
    }
}
