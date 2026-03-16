package com.rabbiter.healthsys.config;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtConfig {
    // 有效期
    private static final long JWT_EXPIRE = 60 * 180 * 1000L;  //1小时
    // 令牌秘钥 (应该使用足够长的密钥，HS256需要至少256位)
    private static final String JWT_KEY = "12345678901234567890123456789012"; // 32字节密钥

    private final SecretKey secretKey;

    public JwtConfig() {
        // 使用Keys类生成安全的密钥
        this.secretKey = Keys.hmacShaKeyFor(JWT_KEY.getBytes());
    }

    public String createToken(Object data) {
        // 当前时间
        long currentTime = System.currentTimeMillis();
        // 过期时间
        long expTime = currentTime + JWT_EXPIRE;
        // 构建jwt
        JwtBuilder builder = Jwts.builder()
                .id(UUID.randomUUID() + "")
                .subject(JSON.toJSONString(data))
                .issuer("system")
                .issuedAt(new Date(currentTime))
                .signWith(secretKey)
                .expiration(new Date(expTime));
        return builder.compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T parseToken(String token, Class<T> clazz) {
        Claims body = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return JSON.parseObject(body.getSubject(), clazz);
    }

}
