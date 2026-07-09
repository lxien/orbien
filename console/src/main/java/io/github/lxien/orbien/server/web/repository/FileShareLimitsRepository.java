package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.FileShareLimitsDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FileShareLimitsRepository extends JpaRepository<FileShareLimitsDO, String> {
    List<FileShareLimitsDO> findByProxyIdIn(Collection<String> proxyIds);

    void deleteByProxyIdIn(Collection<String> proxyIds);
}
