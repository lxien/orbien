/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.web.service.support;

import io.github.lxien.orbien.server.metrics.Metrics;
import io.github.lxien.orbien.server.metrics.MetricsCollector;
import io.github.lxien.orbien.server.web.dto.proxy.ProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.ProxyTrafficSnippetDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class ProxyTrafficEnricher {

    @Autowired
    private MetricsCollector metricsCollector;

    public void enrich(List<? extends ProxyListDTO> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return;
        }
        for (ProxyListDTO row : rows) {
            ProxyTrafficSnippetDTO snippet = new ProxyTrafficSnippetDTO();
            Metrics metrics = metricsCollector.getProxyMetrics(row.getId());
            if (metrics != null) {
                snippet.setUpRate(metrics.getWriteRate());
                snippet.setDownRate(metrics.getReadRate());
            }
            row.setTraffic(snippet);
        }
    }
}
