package com.wuyuhang.delivery.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具类：负责令牌的签发与解析。
 * <p>
 * 采用 HS256 对称加密，密钥来自配置文件；载荷中只放必要的身份与权限信息，
 * 不放密码等敏感数据。
 *
 * @author 吴宇航
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_REAL_NAME = "realName";
    private static final String CLAIM_STATION_ID = "stationId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.prefix:Bearer }")
    private String prefix;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT 初始化完成，令牌有效期 {} 秒", expiration);
    }

    /**
     * 令牌有效期（秒）。
     */
    public Long getExpiration() {
        return expiration;
    }

    /**
     * 请求头中令牌的前缀，例如 "Bearer "。
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 签发令牌。
     */
    public String generateToken(LoginUser loginUser) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expiration * 1000);
        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .claim(CLAIM_USER_ID, loginUser.getUserId())
                .claim(CLAIM_USERNAME, loginUser.getUsername())
                .claim(CLAIM_REAL_NAME, loginUser.getRealName())
                .claim(CLAIM_STATION_ID, loginUser.getStationId())
                .claim(CLAIM_ROLES, loginUser.getRoles())
                .claim(CLAIM_PERMISSIONS, loginUser.getPermissions())
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析令牌为登录用户对象。
     *
     * @return 解析成功返回用户对象；令牌非法或过期返回 null
     */
    @SuppressWarnings("unchecked")
    public LoginUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Object stationIdClaim = claims.get(CLAIM_STATION_ID);
            List<String> roles = claims.get(CLAIM_ROLES, List.class);
            List<String> permissions = claims.get(CLAIM_PERMISSIONS, List.class);

            return LoginUser.builder()
                    .userId(claims.get(CLAIM_USER_ID, Number.class) == null
                            ? null : claims.get(CLAIM_USER_ID, Number.class).longValue())
                    .username(claims.get(CLAIM_USERNAME, String.class))
                    .realName(claims.get(CLAIM_REAL_NAME, String.class))
                    .stationId(stationIdClaim == null ? null : ((Number) stationIdClaim).longValue())
                    .roles(roles == null ? List.of() : roles)
                    .permissions(permissions == null ? List.of() : permissions)
                    .build();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 解析失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 去掉令牌前缀（如 "Bearer "）。
     */
    public String resolveToken(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String trimmed = headerValue.trim();
        String actualPrefix = prefix == null ? "" : prefix.trim();
        if (!actualPrefix.isEmpty() && trimmed.startsWith(actualPrefix)) {
            return trimmed.substring(actualPrefix.length()).trim();
        }
        return trimmed;
    }
}
