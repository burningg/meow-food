package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.auth.JwtTokenUtil;
import com.panghu.food.auth.PasswordUtils;
import com.panghu.food.dto.AuthLoginRequest;
import com.panghu.food.dto.AuthLoginResponse;
import com.panghu.food.dto.AuthUserResponse;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.entity.UserProfileSettings;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.UserAccountMapper;
import com.panghu.food.mapper.UserProfileSettingsMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserAccountMapper userAccountMapper;
    private final UserProfileSettingsMapper userProfileSettingsMapper;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthServiceImpl(UserAccountMapper userAccountMapper,
                           UserProfileSettingsMapper userProfileSettingsMapper,
                           JwtTokenUtil jwtTokenUtil) {
        this.userAccountMapper = userAccountMapper;
        this.userProfileSettingsMapper = userProfileSettingsMapper;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public AuthLoginResponse login(AuthLoginRequest request) {
        UserAccount user = userAccountMapper.selectOne(new QueryWrapper<UserAccount>()
                .eq("account", request.getAccount()).last("LIMIT 1"));
        if (user == null || !PasswordUtils.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号或密码错误");
        }
        AuthLoginResponse response = new AuthLoginResponse();
        response.setToken(jwtTokenUtil.createToken(user.getId()));
        response.setUser(toAuthUser(user, getSettings(user.getId())));
        return response;
    }

    @Override
    public AuthUserResponse getCurrentUser() {
        String userId = AuthContext.requireUserId();
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        return toAuthUser(user, getSettings(userId));
    }

    private UserProfileSettings getSettings(String userId) {
        UserProfileSettings settings = userProfileSettingsMapper.selectById(userId);
        if (settings == null) {
            settings = new UserProfileSettings();
            settings.setUserId(userId);
            settings.setDefaultMenuVisibility("friends");
            settings.setAllowFriendFeed(true);
            userProfileSettingsMapper.insert(settings);
        }
        return settings;
    }

    private AuthUserResponse toAuthUser(UserAccount user, UserProfileSettings settings) {
        AuthUserResponse response = new AuthUserResponse();
        response.setId(user.getId());
        response.setAccount(user.getAccount());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setBio(user.getBio());
        response.setDefaultMenuVisibility(VisibilityUtils.normalizeProfileVisibility(settings.getDefaultMenuVisibility()));
        return response;
    }
}
