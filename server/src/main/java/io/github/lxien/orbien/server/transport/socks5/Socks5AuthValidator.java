/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.server.transport.socks5;

import io.github.lxien.orbien.core.utils.StringUtils;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.Socks5AuthConfig;
import io.github.lxien.orbien.core.socks5.Socks5Constants;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class Socks5AuthValidator {

    private final PasswordEncoder passwordEncoder;

    public Socks5AuthValidator(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public boolean authenticate(ProxyConfig config, String username, String password) {
        if (config == null || !config.hasSocks5Auth()) {
            return false;
        }
        Socks5AuthConfig auth = config.getSocks5Auth();
        if (!auth.isEnabled()) {
            return true;
        }
        if (!StringUtils.hasText(username) || password == null || !auth.hasUsers()) {
            return false;
        }
        for (Socks5AuthConfig.Socks5User user : auth.getUsers()) {
            if (username.equals(user.getUsername()) && passwordMatches(password, user.getPassword())) {
                return true;
            }
        }
        return false;
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return Objects.equals(rawPassword, storedPassword);
    }

    public byte selectAuthMethod(ProxyConfig config) {
        if (config != null && config.hasSocks5Auth() && config.getSocks5Auth().isEnabled()) {
            return Socks5Constants.METHOD_USERNAME_PASSWORD;
        }
        return Socks5Constants.METHOD_NO_AUTH;
    }
}
