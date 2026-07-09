package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.FileShareUserDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FileShareUserRepository extends JpaRepository<FileShareUserDO, Long> {
    List<FileShareUserDO> findByProxyId(String proxyId);

    List<FileShareUserDO> findByProxyIdIn(Collection<String> proxyIds);

    boolean existsByProxyIdAndUsername(String proxyId, String username);

    boolean existsByProxyIdAndUsernameAndIdNot(String proxyId, String username, Long id);

    void deleteByProxyIdIn(Collection<String> proxyIds);
}
