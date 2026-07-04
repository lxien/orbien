package io.github.lxien.orbien.server.web.repository;
import io.github.lxien.orbien.server.web.entity.LoginTokenDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/**
 * 认证令牌 Repository
 */
@Repository
public interface LoginTokenRepository extends JpaRepository<LoginTokenDO, String> {
}
