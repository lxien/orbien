package com.xiaoniucode.etp.server.web.repository;

import com.xiaoniucode.etp.server.web.entity.AccessTokenDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 访问令牌 Repository
 */
@Repository
public interface AccessTokenRepository extends JpaRepository<AccessTokenDO, Integer> {
    /**
     * 检查是否存在指定名称的访问令牌
     */
    boolean existsByName(String name);

    /**
     * 检查是否存在指定名称但排除指定 ID 的访问令牌
     */
    boolean existsByNameAndIdNot(String name, Integer id);

    /**
     * 查询访问令牌是否已经存在
     *
     * @param name  名称
     * @param token 令牌值
     * @return 只要name 或token有一个就算存在
     */
    boolean existsByNameOrToken(String name, String token);
    /**
     * 检查是否存在指定 token 的访问令牌
     */
    boolean existsByToken(String token);
}
