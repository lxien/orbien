package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.OAuthBindingDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OAuthBindingRepository extends JpaRepository<OAuthBindingDO, Long> {
    Optional<OAuthBindingDO> findByProviderAndExternalId(String provider, String externalId);

    Optional<OAuthBindingDO> findByUsernameAndProvider(String username, String provider);

    List<OAuthBindingDO> findByUsername(String username);

    void deleteByUsernameAndProvider(String username, String provider);
}
