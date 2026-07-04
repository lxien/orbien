package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.AcmeDnsChallengeDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcmeDnsChallengeRepository extends JpaRepository<AcmeDnsChallengeDO, Long> {

    List<AcmeDnsChallengeDO> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}
