/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http:
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.web.service;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.dto.metrics.TrafficChartVO;
import io.github.lxien.orbien.server.web.dto.metrics.TrafficCountDTO;
import io.github.lxien.orbien.server.web.param.metrics.ProxyQueryParam;

public interface MetricsService {
    /**
     * 获取所有指标数据
     * @return 指标数据列表
     */
    TrafficChartVO getTotal24hTraffic();

    TrafficChartVO getProxy24hTraffic(ProxyQueryParam param);
    void deleteByProxyId(String proxyId);

    PageResult<TrafficCountDTO> queryPage(PageQuery pageQuery);

    /**
     * 删除超过指定天数的流量统计记录
     * @param days 保留天数
     * @return 删除条数
     */
    int deleteOldMetrics(int days);
}
