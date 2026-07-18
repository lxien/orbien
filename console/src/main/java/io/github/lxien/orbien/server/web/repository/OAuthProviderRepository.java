package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.OAuthProviderDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthProviderRepository extends JpaRepository<OAuthProviderDO, Long> {
    Optional<OAuthProviderDO> findByProvider(String provider);
}
