package io.github.lxien.orbien.server.transport.file;

import io.github.lxien.orbien.core.domain.FileShareAuthConfig;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.filetransfer.FileTransferConstants;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileAuthService {

    private static final long SESSION_TTL_SECONDS = FileTransferConstants.SESSION_TTL_MINUTES * 60L;

    private final PasswordEncoder passwordEncoder;
    private final Map<String, FileSession> sessions = new ConcurrentHashMap<>();

    public FileAuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String login(ProxyConfig config, String username, String password) {
        FileShareAuthConfig auth = config.getFileShareAuth();
        if (auth == null || !auth.isEnabled()) {
            throw new AuthException("认证未启用");
        }
        FileShareAuthConfig.FileShareUser user = auth.getUsers().stream()
                .filter(u -> username.equals(u.getUsername()))
                .findFirst()
                .orElseThrow(() -> new AuthException("用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPassword())
                && !password.equals(user.getPassword())) {
            throw new AuthException("用户名或密码错误");
        }
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        sessions.put(sessionId, new FileSession(config.getProxyId(), username, user.getPermission(),
                Instant.now().getEpochSecond() + SESSION_TTL_SECONDS));
        return sessionId;
    }

    public FileSession validate(String sessionId) {
        if (sessionId == null) {
            throw new AuthException("未登录");
        }
        FileSession session = sessions.get(sessionId);
        if (session == null || session.expireAt() < Instant.now().getEpochSecond()) {
            sessions.remove(sessionId);
            throw new AuthException("会话已过期");
        }
        return session;
    }

    /**
     * 校验会话并按当前代理配置同步权限，权限变更后立即生效。
     */
    public FileSession syncSession(String sessionId, ProxyConfig config) {
        FileSession session = validate(sessionId);
        if (!config.getProxyId().equals(session.proxyId())) {
            throw new AuthException("会话无效");
        }
        FileShareAuthConfig auth = config.getFileShareAuth();
        if (auth == null || !auth.isEnabled()) {
            logout(sessionId);
            throw new AuthException("认证未启用，请重新登录");
        }
        String currentPermission = resolveUserPermission(auth, session.username());
        if (currentPermission == null) {
            logout(sessionId);
            throw new AuthException("用户已被移除，请重新登录");
        }
        if (!currentPermission.equals(session.permission())) {
            FileSession updated = new FileSession(session.proxyId(), session.username(),
                    currentPermission, session.expireAt());
            sessions.put(sessionId, updated);
            return updated;
        }
        return session;
    }

    public void invalidateProxySessions(String proxyId) {
        sessions.entrySet().removeIf(entry -> proxyId.equals(entry.getValue().proxyId()));
    }

    private String resolveUserPermission(FileShareAuthConfig auth, String username) {
        return auth.getUsers().stream()
                .filter(u -> username.equals(u.getUsername()))
                .map(FileShareAuthConfig.FileShareUser::getPermission)
                .findFirst()
                .orElse(null);
    }

    public void logout(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    public record FileSession(String proxyId, String username, String permission, long expireAt) {
        public boolean canWrite() {
            return FileTransferConstants.PERMISSION_READ_WRITE.equals(permission);
        }

        public static FileSession guest(String proxyId) {
            return new FileSession(proxyId, "guest", FileTransferConstants.PERMISSION_READ_WRITE, Long.MAX_VALUE);
        }
    }

    public static class AuthException extends RuntimeException {
        public AuthException(String message) {
            super(message);
        }
    }
}
