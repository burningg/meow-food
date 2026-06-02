package com.panghu.food.web;

import com.panghu.food.dto.AuthLoginRequest;
import com.panghu.food.dto.AuthLoginResponse;
import com.panghu.food.dto.AuthRegisterRequest;
import com.panghu.food.dto.AuthUserResponse;
import com.panghu.food.dto.WechatLoginRequest;
import com.panghu.food.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthLoginResponse> register(@RequestBody AuthRegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/wechat-login")
    public ResponseEntity<AuthLoginResponse> wechatLogin(@RequestBody WechatLoginRequest request) {
        return ResponseEntity.ok(authService.wechatLogin(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
