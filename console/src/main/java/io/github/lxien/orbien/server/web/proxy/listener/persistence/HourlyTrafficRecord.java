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

package io.github.lxien.orbien.server.web.proxy.listener.persistence;

import io.github.lxien.orbien.server.notify.EventBus;
import io.github.lxien.orbien.server.notify.EventListener;
import io.github.lxien.orbien.server.event.HourlyTrafficEvent;
import io.github.lxien.orbien.server.metrics.HourlyTraffic;
import io.github.lxien.orbien.server.web.entity.MetricsDO;
import io.github.lxien.orbien.server.web.repository.MetricsRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HourlyTrafficRecord  implements EventListener<HourlyTrafficEvent> {
    private final Logger logger = LoggerFactory.getLogger(HourlyTrafficRecord.class);
    @Autowired
    private EventBus eventBus;
    @Autowired
    private MetricsRepository metricsRepository;
    @PostConstruct
    public void init() {
        eventBus.register(this);
    }
    @Override
    public void onEvent(HourlyTrafficEvent event) {
        if (event.getAgentType().isSession()){
            logger.debug("忽略来自内嵌客户端代理数据指标信息");
            return;
        }
        String proxyId = event.getProxyId();
        HourlyTraffic hourlyTraffic = event.getHourlyTraffic();

        MetricsDO metricsDO = new MetricsDO();
        metricsDO.setProxyId(proxyId);
        metricsDO.setReadBytes(hourlyTraffic.getReadBytes());
        metricsDO.setWriteBytes(hourlyTraffic.getWriteBytes());
        metricsDO.setReadMessages(hourlyTraffic.getReadMessages());
        metricsDO.setWriteMessages(hourlyTraffic.getWriteMessages());
        metricsDO.setCreatedAt(hourlyTraffic.getHour());

        metricsRepository.save(metricsDO);
        logger.debug("保存代理流量数据成功，代理ID: {}, 时间: {}", proxyId, hourlyTraffic.getHour());
    }
}
