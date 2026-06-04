package com.wu.aiagent.auth;

import com.wu.aiagent.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * JWT 工具类。
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final AuthProperties authProperties;

    /**
     * 签发登录令牌。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(authProperties.getExpireHours(), ChronoUnit.HOURS);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(signingKey())
                .compact();
    }

    /**
     * 解析并校验令牌。
     *
     * @param token JWT
     * @return 载荷
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从令牌读取用户 ID。
     *
     * @param token JWT
     * @return 用户 ID
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    private SecretKey signingKey() {
        byte[] keyBytes = authProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
