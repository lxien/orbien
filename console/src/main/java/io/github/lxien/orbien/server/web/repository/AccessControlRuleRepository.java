package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.entity.AccessControlRuleDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 访问控制规则 Repository
 */
@Repository
public interface AccessControlRuleRepository extends JpaRepository<AccessControlRuleDO, Long> {

    void deleteByProxyIdIn(List<String> ids);

    List<AccessControlRuleDO> findByProxyId(String proxyId);

    List<AccessControlRuleDO> findByProxyIdIn(List<String> proxyIds);

    boolean existsByProxyIdAndCidr(String proxyId, String cidr);

    boolean existsByProxyIdAndCidrAndIdNot(String proxyId, String cidr,  Long id);

}
