package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.panghu.food.auth.AuthContext;
import com.panghu.food.auth.JwtTokenUtil;
import com.panghu.food.auth.PasswordUtils;
import com.panghu.food.component.WechatComponent;
import com.panghu.food.dto.AuthLoginRequest;
import com.panghu.food.dto.AuthLoginResponse;
import com.panghu.food.dto.AuthRegisterRequest;
import com.panghu.food.dto.AuthUserResponse;
import com.panghu.food.dto.WechatLoginRequest;
import com.panghu.food.entity.UserAccount;
import com.panghu.food.entity.UserProfileSettings;
import com.panghu.food.entity.WechatAuthVO;
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
    private final WechatComponent wechatComponent;

    public AuthServiceImpl(UserAccountMapper userAccountMapper,
                           UserProfileSettingsMapper userProfileSettingsMapper,
                           JwtTokenUtil jwtTokenUtil,
                           WechatComponent wechatComponent) {
        this.userAccountMapper = userAccountMapper;
        this.userProfileSettingsMapper = userProfileSettingsMapper;
        this.jwtTokenUtil = jwtTokenUtil;
        this.wechatComponent = wechatComponent;
    }

    @Override
    public AuthLoginResponse login(AuthLoginRequest request) {
        if (request == null || isBlank(request.getAccount()) || isBlank(request.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号和密码不能为空");
        }
        UserAccount user = userAccountMapper.selectOne(new QueryWrapper<UserAccount>()
                .eq("account", request.getAccount().trim()).last("LIMIT 1"));
        if (user == null || !PasswordUtils.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号或密码错误");
        }
        return issueSession(user);
    }

    @Override
    public AuthLoginResponse register(AuthRegisterRequest request) {
        if (request == null || isBlank(request.getAccount()) || isBlank(request.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号和密码不能为空");
        }
        String account = request.getAccount().trim();
        String password = request.getPassword();
        if (account.length() < 3 || account.length() > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号长度需为 3-20 位");
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码长度需为 6-20 位");
        }
        UserAccount exists = userAccountMapper.selectOne(new QueryWrapper<UserAccount>()
                .eq("account", account).last("LIMIT 1"));
        if (exists != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号已存在");
        }

        UserAccount user = new UserAccount();
        user.setAccount(account);
        user.setPasswordHash(PasswordUtils.hash(password));
        user.setUsername(account);
        user.setNickname(account);
        userAccountMapper.insert(user);

        return issueSession(user);
    }

    @Override
    public AuthLoginResponse wechatLogin(WechatLoginRequest request) {
        if (request == null || isBlank(request.getCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "微信登录凭证不能为空");
        }

        WechatAuthVO auth = wechatComponent.getWechatAuth(request.getCode().trim());
        if (auth == null || !isBlank(auth.getErrcode()) || isBlank(auth.getOpenid())) {
            String message = auth != null && !isBlank(auth.getErrmsg()) ? auth.getErrmsg() : "微信登录失败";
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }

        String account = "wx_" + auth.getOpenid();
        UserAccount user = userAccountMapper.selectOne(new QueryWrapper<UserAccount>()
                .eq("account", account).last("LIMIT 1"));
        if (user == null) {
            if (isBlank(request.getNickname()) || isBlank(request.getAvatar())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "WECHAT_PROFILE_REQUIRED",
                        "首次微信登录请先选择头像并填写昵称");
            }

            user = new UserAccount();
            user.setAccount(account);
            user.setPasswordHash(PasswordUtils.hash("wechat:" + auth.getOpenid()));
            user.setUsername(account);
            user.setNickname(normalizeNickname(request.getNickname()));
            user.setAvatar(normalizeOptional(request.getAvatar()));
            userAccountMapper.insert(user);
        }

        return issueSession(user);
    }

    private AuthLoginResponse issueSession(UserAccount user) {
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeNickname(String nickname) {
        String value = isBlank(nickname) ? "微信用户" : nickname.trim();
        return value.length() > 20 ? value.substring(0, 20) : value;
    }

    private String normalizeOptional(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
