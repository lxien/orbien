/*
 *
 *  *    Copyright 2026 lxien
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package io.github.lxien.orbien.server.web.repository;

import io.github.lxien.orbien.server.web.dto.metrics.DailyTrafficQueryResult;
import io.github.lxien.orbien.server.web.dto.metrics.HourlyTrafficQueryResult;
import io.github.lxien.orbien.server.web.dto.metrics.ProxyTrafficQueryResult;
import io.github.lxien.orbien.server.web.entity.MetricsDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MetricsRepository extends JpaRepository<MetricsDO, Long> {

    void deleteByProxyId(String proxyId);

    void deleteByProxyIdIn(Collection<String> proxyIds);

    @Query(value = """
            SELECT 
                DATE(m.created_at) AS dateStr,
                SUM(m.write_bytes) AS totalWrite,
                SUM(m.read_bytes) AS totalRead
            FROM metrics m
            WHERE m.proxy_id = :proxyId 
              AND m.created_at >= :startTime 
              AND m.created_at < :endTime
            GROUP BY DATE(m.created_at)
            ORDER BY dateStr ASC
            """,
            nativeQuery = true)
    @SuppressWarnings("all")
    List<DailyTrafficQueryResult> queryDailyTrafficByRange(@Param("proxyId") String proxyId,
                                                           @Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 按小时聚合。TIMESTAMPADD(HOUR, n, DATE(...)) 兼容 H2(MODE=MySQL) / MySQL 8
     * 用CAST AS TIMESTAMP会出现MySQL语法错误（应用 DATETIME 或不 CAST）
     */
    @Query(value = """
            SELECT
                TIMESTAMPADD(HOUR, HOUR(m.created_at), DATE(m.created_at)) AS statHour,
                SUM(m.read_bytes) AS readBytes,
                SUM(m.write_bytes) AS writeBytes,
                SUM(m.read_messages) AS readMessages,
                SUM(m.write_messages) AS writeMessages
            FROM metrics m
            WHERE m.proxy_id = :proxyId
              AND m.created_at >= :startTime
              AND m.created_at < :endTime
            GROUP BY TIMESTAMPADD(HOUR, HOUR(m.created_at), DATE(m.created_at))
            ORDER BY statHour ASC
            """, nativeQuery = true)
    @SuppressWarnings("all")
    List<HourlyTrafficQueryResult> queryHourlyTrafficByRange(
            @Param("proxyId") String proxyId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 按 proxy_id 聚合，对该代理所有快照记录求和，按总流量倒序
     */
    @Query(value = """
            SELECT
                m.proxy_id AS proxyId,
                SUM(m.read_bytes) AS readBytes,
                SUM(m.write_bytes) AS writeBytes,
                SUM(m.read_messages) AS readMessages,
                SUM(m.write_messages) AS writeMessages,
                SUM(m.read_bytes) + SUM(m.write_bytes) AS totalBytes
            FROM metrics m
            GROUP BY m.proxy_id
            ORDER BY totalBytes DESC, proxyId ASC
            """,
            countQuery = """
                    SELECT COUNT(*) FROM (
                        SELECT 1
                        FROM metrics m
                        GROUP BY m.proxy_id
                    ) grouped
                    """,
            nativeQuery = true)
    @SuppressWarnings("all")
    Page<ProxyTrafficQueryResult> pageTrafficByProxy(Pageable pageable);

    long countByCreatedAtBefore(LocalDateTime createdAtBefore);

    void deleteByCreatedAtBefore(LocalDateTime createdAtBefore);
}
