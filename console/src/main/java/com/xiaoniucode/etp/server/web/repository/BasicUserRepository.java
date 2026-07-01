package com.xiaoniucode.etp.server.web.repository;

import com.xiaoniucode.etp.server.web.entity.BasicUserDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * HttpUser Repository
 */
@Repository
public interface BasicUserRepository extends JpaRepository<BasicUserDO, Long> {
    void deleteByProxyIdIn(List<String> ids);

    List<BasicUserDO> findByProxyId(String proxyId);

    List<BasicUserDO> findByProxyIdIn(List<String> proxyIds);

    boolean existsByProxyIdAndUsername(String proxyId,String username);

    /**
     * 判断当前代理配置下是否存在相同的用户名，但是排除自己
     *
     * @param proxyId  代理ID
     * @param username 用户名
     * @param id       排除的ID
     * @return 是否存在
     */
    boolean existsByProxyIdAndUsernameAndIdNot(String proxyId, String username, Long id);
}
