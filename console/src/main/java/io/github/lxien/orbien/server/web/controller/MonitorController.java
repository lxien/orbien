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
package io.github.lxien.orbien.server.web.controller;

import io.github.lxien.orbien.server.web.common.message.Ajax;
import io.github.lxien.orbien.server.web.common.monitor.ServerMonitor;
import io.github.lxien.orbien.server.web.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {
    @Autowired
    private StatsService statsService;

    @GetMapping("server-info")
    public Ajax getServerInfo() {
        return Ajax.success(ServerMonitor.getServerInfo());
    }

    @GetMapping("get-dashboard-summary")
    public Ajax getDashboardSummary() {
        return Ajax.success(statsService.getDashboardSummary());
    }

    @GetMapping("get-proxy-protocol-stats")
    public Ajax getProxyProtocolStats() {
        return Ajax.success(statsService.getProxyProtocolStats());
    }
}
