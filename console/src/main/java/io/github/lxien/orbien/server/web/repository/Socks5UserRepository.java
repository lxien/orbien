package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.Socks5UserDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface Socks5UserRepository extends JpaRepository<Socks5UserDO, Long> {
    List<Socks5UserDO> findByProxyId(String proxyId);

    List<Socks5UserDO> findByProxyIdIn(Collection<String> proxyIds);

    boolean existsByProxyIdAndUsername(String proxyId, String username);

    boolean existsByProxyIdAndUsernameAndIdNot(String proxyId, String username, Long id);

    void deleteByProxyIdIn(Collection<String> proxyIds);
}
