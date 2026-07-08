package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.Socks5AuthDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface Socks5AuthRepository extends JpaRepository<Socks5AuthDO, String> {
    List<Socks5AuthDO> findByProxyIdIn(Collection<String> proxyIds);
    void deleteByProxyIdIn(Collection<String> proxyIds);
}
