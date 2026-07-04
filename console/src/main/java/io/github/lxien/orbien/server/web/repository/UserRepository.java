package io.github.lxien.orbien.server.web.repository;
import io.github.lxien.orbien.server.web.entity.SysUserDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<SysUserDO, Integer> {
    /**
     * 根据用户名查询用户
     */
    Optional<SysUserDO> findByUsername(String username);
}
