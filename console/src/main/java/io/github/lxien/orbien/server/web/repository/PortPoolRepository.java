package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.PortPoolDO;
import io.github.lxien.orbien.core.enums.PortPoolType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 端口池 Repository
 */
@Repository
public interface PortPoolRepository extends JpaRepository<PortPoolDO, Long>, JpaSpecificationExecutor<PortPoolDO> {

    boolean existsByTypeAndStartPortAndEndPort(PortPoolType type, Integer startPort, Integer endPort);

    boolean existsByTypeAndStartPortAndEndPortAndIdNot(PortPoolType type, Integer startPort, Integer endPort, Long id);

    List<PortPoolDO> findByType(PortPoolType type);
}
