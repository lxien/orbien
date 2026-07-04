package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.AgentDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<AgentDO, String> {

    List<AgentDO> findByLastActiveTimeBefore(LocalDateTime cutoff);
}
