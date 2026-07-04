package io.github.lxien.orbien.server.web.repository;
import io.github.lxien.orbien.server.web.entity.BasicAuthDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BasicAuth Repository
 */
@Repository
public interface BasicAuthRepository extends JpaRepository<BasicAuthDO, String> {
    void deleteByProxyIdIn(List<String> ids);
}
