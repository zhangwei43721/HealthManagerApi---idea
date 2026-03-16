package com.rabbiter.healthsys.common;

import com.rabbiter.healthsys.config.JwtConfig;
import com.rabbiter.healthsys.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserTokenResolver {

    private final JwtConfig jwtConfig;

    @Nullable
    public User parseUser(String token) {
        return jwtConfig.parseToken(token, User.class);
    }

    @Nullable
    public Integer parseUserId(String token) {
        User user = parseUser(token);
        return user != null ? user.getId() : null;
    }

    public String createToken(User user) {
        return jwtConfig.createToken(user);
    }
}
