package com.wu.aiagent.controller;

import com.wu.aiagent.auth.AuthInterceptor;
import com.wu.aiagent.auth.JwtUtil;
import com.wu.aiagent.dto.AuthResponse;
import com.wu.aiagent.dto.LoginRequest;
import com.wu.aiagent.dto.RegisterRequest;
import com.wu.aiagent.entity.SysUser;
import com.wu.aiagent.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户认证接口。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册。
     */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        SysUser user = userService.register(request);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername());
    }

    /**
     * 用户登录。
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        SysUser user = userService.login(request.getUsername(), request.getPassword());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername());
    }

    /**
     * 当前登录用户信息。
     */
    @GetMapping("/me")
    public Map<String, Object> me(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
                                  @RequestAttribute(AuthInterceptor.ATTR_USERNAME) String username) {
        return Map.of("userId", userId, "username", username);
    }
}
