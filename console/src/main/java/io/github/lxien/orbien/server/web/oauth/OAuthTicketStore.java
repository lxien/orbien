package io.github.lxien.orbien.server.web.oauth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Component
public class OAuthTicketStore {

    private final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    private final SecureRandom secureRandom = new SecureRandom();

    public String put(String username) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        cache.put(ticket, username);
        return ticket;
    }

    public String consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new BizException("无效的登录凭证");
        }
        String username = cache.getIfPresent(ticket);
        cache.invalidate(ticket);
        if (username == null) {
            throw new BizException("登录凭证无效或已过期");
        }
        return username;
    }
}
