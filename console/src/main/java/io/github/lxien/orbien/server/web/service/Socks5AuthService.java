package io.github.lxien.orbien.server.web.service;

import io.github.lxien.orbien.server.web.dto.socks5auth.Socks5AuthDetailDTO;
import io.github.lxien.orbien.server.web.param.socks5auth.Socks5AuthUpdateParam;
import io.github.lxien.orbien.server.web.param.socks5auth.Socks5UserAddParam;
import io.github.lxien.orbien.server.web.param.socks5auth.Socks5UserUpdateParam;

public interface Socks5AuthService {
    Socks5AuthDetailDTO getByProxyId(String proxyId);

    void update(Socks5AuthUpdateParam param);

    void addUser(Socks5UserAddParam param);

    void updateUser(Socks5UserUpdateParam param);

    void deleteUser(Long id);
}
