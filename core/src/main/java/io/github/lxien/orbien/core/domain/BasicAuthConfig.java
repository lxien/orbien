package io.github.lxien.orbien.core.domain;

import io.github.lxien.orbien.common.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Basic Auth 配置领域对象
 */
public class BasicAuthConfig {

    @Getter
    @Setter
    private boolean enabled = false;

    private final Set<HttpUser> users = new CopyOnWriteArraySet<>();
    private final Map<String, HttpUser> cache = new ConcurrentHashMap<>();

    public HttpUser getUser(String username) {
        return cache.get(username);
    }

    /**
     * 添加单个用户
     */
    public boolean addUser(HttpUser user) {
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            return false;
        }

        String username = user.getUsername();

        if (cache.putIfAbsent(username, user) != null) {
            return false;
        }

        users.add(user);
        return true;
    }

    /**
     * 批量添加用户
     */
    public void addUsers(Set<HttpUser> newUsers) {
        if (newUsers == null || newUsers.isEmpty()) {
            return;
        }
        for (HttpUser user : newUsers) {
            addUser(user);
        }
    }

    /**
     * 删除用户
     */
    public boolean removeUser(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }

        HttpUser user = cache.remove(username);
        if (user != null) {
            users.remove(user);
            return true;
        }
        return false;
    }

    public void clearUsers() {
        users.clear();
        cache.clear();
    }

    public boolean containsUser(String username) {
        return StringUtils.hasText(username) && cache.containsKey(username);
    }

    public int getUserCount() {
        return users.size();
    }

    public Set<HttpUser> getUsers() {
        return Collections.unmodifiableSet(users);
    }
}