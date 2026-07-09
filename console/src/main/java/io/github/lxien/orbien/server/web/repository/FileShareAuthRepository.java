package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.FileShareAuthDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FileShareAuthRepository extends JpaRepository<FileShareAuthDO, String> {
    List<FileShareAuthDO> findByProxyIdIn(Collection<String> proxyIds);

    void deleteByProxyIdIn(Collection<String> proxyIds);
}
