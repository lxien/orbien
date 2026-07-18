package io.github.lxien.orbien.server.web.service;
import io.github.lxien.orbien.server.web.dto.auth.LoginDTO;
import io.github.lxien.orbien.server.web.param.auth.LoginParam;
import jakarta.validation.Valid;
public interface AuthService {
    LoginDTO login(@Valid LoginParam request);

    /**
     * 直接为已确认身份的用户签发 JWT
     */
    LoginDTO issueToken(String username);
}
