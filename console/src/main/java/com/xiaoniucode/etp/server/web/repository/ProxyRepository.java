package com.xiaoniucode.etp.server.web.repository;

import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.core.enums.ProxyStatus;
import com.xiaoniucode.etp.server.web.dto.proxy.ProxyDetailQueryResult;
import com.xiaoniucode.etp.server.web.dto.proxy.ProxyListQueryResult;
import com.xiaoniucode.etp.server.web.dto.stats.DashboardSummaryDTO;
import com.xiaoniucode.etp.server.web.dto.stats.ProxyProtocolCountDTO;
import com.xiaoniucode.etp.server.web.entity.ProxyDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 代理 Repository
 */
@Repository
public interface ProxyRepository extends JpaRepository<ProxyDO, String>, JpaSpecificationExecutor<ProxyDO> {
    boolean existsByAgentIdAndName(String agentId, String name);

    boolean existsByAgentIdAndNameAndIdNot(String agentId, String name, String id);

    @Query("""
            SELECT new com.xiaoniucode.etp.server.web.dto.proxy.ProxyListQueryResult(a, p)
            FROM ProxyDO p
            LEFT JOIN AgentDO a ON a.id = p.agentId
            WHERE p.protocol = :protocol
            ORDER BY p.updatedAt DESC
            """)
    Page<ProxyListQueryResult> findProxiesWithAssociations(@Param("protocol") ProtocolType protocol, Pageable pageable);

    Page<ProxyDO> findByProtocolOrderByUpdatedAtDesc(ProtocolType protocol, Pageable pageable);

    @Query("""
            SELECT new com.xiaoniucode.etp.server.web.dto.proxy.ProxyDetailQueryResult(a, p,ba,ac)
            FROM ProxyDO p
            LEFT JOIN AgentDO a ON p.agentId = a.id 
            LEFT JOIN BasicAuthDO ba ON ba.proxyId = p.id
            LEFT JOIN AccessControlDO ac ON ac.proxyId = p.id
            WHERE p.id = :id
            """)
    ProxyDetailQueryResult findDetailByProxyId(@Param("id") String id);

    @Query("""
            SELECT new com.xiaoniucode.etp.server.web.dto.proxy.ProxyDetailQueryResult(a, p,ba,ac)
            FROM ProxyDO p
            LEFT JOIN AgentDO a ON p.agentId = a.id
            LEFT JOIN BasicAuthDO ba ON ba.proxyId = p.id
            LEFT JOIN AccessControlDO ac ON ac.proxyId = p.id
            WHERE p.listenPort = :listenPort
            """)
    ProxyDetailQueryResult findDetailByListenPort(@Param("listenPort") Integer listenPort);

    @Query("""
            SELECT new com.xiaoniucode.etp.server.web.dto.proxy.ProxyDetailQueryResult(a, p,ba,ac)
            FROM ProxyDO p
            LEFT JOIN AgentDO a ON p.agentId = a.id
            LEFT JOIN BasicAuthDO ba ON ba.proxyId = p.id
            LEFT JOIN AccessControlDO ac ON ac.proxyId = p.id
            WHERE a.id=:agentId AND p.name =:name
            """)
    ProxyDetailQueryResult findDetailByAgentIdAndProxyName(@Param("agentId") String agentId, @Param("name") String name);

    @Query("""
            SELECT NEW com.xiaoniucode.etp.server.web.dto.stats.DashboardSummaryDTO(
                null ,
                null ,
                COUNT(p),
                SUM(CASE WHEN p.status = :status THEN 1 ELSE 0 END)
            )
            FROM ProxyDO p
            """)
    DashboardSummaryDTO countTotalAndEnabledCount(@Param("status") ProxyStatus status);

    @Query("""
            SELECT NEW com.xiaoniucode.etp.server.web.dto.stats.ProxyProtocolCountDTO(
                SUM(CASE WHEN p.protocol = :http THEN 1 ELSE 0 END),
                SUM(CASE WHEN p.protocol = :https THEN 1 ELSE 0 END),
                SUM(CASE WHEN p.protocol = :tcp THEN 1 ELSE 0 END)
            )
            FROM ProxyDO p
            """)
    ProxyProtocolCountDTO countHttpAndTcp(
            @Param("http") ProtocolType http,
            @Param("https") ProtocolType https,
            @Param("tcp") ProtocolType tcp
    );

    void deleteByIdIn(List<String> ids);

    @Query("SELECT p.listenPort FROM ProxyDO p WHERE p.listenPort IS NOT NULL")
    List<Integer> findAllListenPorts();

    List<ProxyDO> findByAgentId(String agentId);

    List<ProxyDO> findByProtocol(ProtocolType protocol);

    @Query("""
            SELECT new com.xiaoniucode.etp.server.web.dto.proxy.ProxyListQueryResult(a, p)
            FROM ProxyDO p
            LEFT JOIN AgentDO a ON a.id = p.agentId
            WHERE p.id IN :proxyIds
            """)
    List<ProxyListQueryResult> findWithAgentByIdIn(@Param("proxyIds") List<String> proxyIds);
}