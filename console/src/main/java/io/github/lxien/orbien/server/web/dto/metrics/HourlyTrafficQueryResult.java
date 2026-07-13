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

package io.github.lxien.orbien.server.web.dto.metrics;

import java.time.LocalDateTime;

/**
 * 范围查询小时级流量结果 Projection（H2 / MySQL 共用 native SQL）
 */
public interface HourlyTrafficQueryResult {

    /**
     * 小时桶起始整点时间
     * 对应 SQL: TIMESTAMPADD(...) AS statHour
     */
    LocalDateTime getStatHour();

    Long getReadBytes();

    Long getWriteBytes();

    Long getReadMessages();

    Long getWriteMessages();
}
