package io.github.lxien.orbien.server.web.oauth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Component
public class OAuthStateStore {

    private final Cache<String, OAuthState> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final SecureRandom secureRandom = new SecureRandom();

    public String put(OAuthState state) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        cache.put(key, state);
        return key;
    }

    public OAuthState consume(String state) {
        if (state == null || state.isBlank()) {
            throw new BizException("无效的 OAuth state");
        }
        OAuthState value = cache.getIfPresent(state);
        cache.invalidate(state);
        if (value == null) {
            throw new BizException("OAuth state 无效或已过期");
        }
        return value;
    }
}
