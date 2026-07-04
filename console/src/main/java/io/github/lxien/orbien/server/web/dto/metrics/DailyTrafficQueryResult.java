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

import java.time.LocalDate;

/**
 * 范围查询天级流量结果 Projection 强类型映射接口
 *
 * @author lxien
 */
public interface DailyTrafficQueryResult {

    /**
     * 获取统计日期
     * 对应 SQL 中的: DATE(m.created_at) AS dateStr
     *
     * @return 统计日期
     */
    LocalDate getDateStr();

    /**
     * 获取当天总写字节（上行总流量）
     *
     * @return 上行总字节数
     */
    Long getTotalWrite();

    /**
     * 获取当天总读字节（下行总流量）
     *
     * @return 下行总字节数
     */
    Long getTotalRead();
}
