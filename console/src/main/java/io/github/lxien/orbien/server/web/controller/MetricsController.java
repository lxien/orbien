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
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.param.metrics.MetricsBatchDeleteParam;
import io.github.lxien.orbien.server.web.param.metrics.ProxyQueryParam;
import io.github.lxien.orbien.server.web.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    @Autowired
    private MetricsService metricsService;

    @GetMapping("list")
    public Ajax list(@ModelAttribute PageQuery pageQuery) {
        return Ajax.success(metricsService.queryPage(pageQuery));
    }

    @GetMapping("global/24h")
    public Ajax getGlobal24hMetrics() {
        return Ajax.success(metricsService.getTotal24hTraffic());
    }

    @PostMapping("proxy/24h")
    public Ajax getProxy24hMetrics(@RequestBody @Validated ProxyQueryParam param) {
        return Ajax.success(metricsService.getProxy24hTraffic(param));
    }

    @DeleteMapping
    public Ajax batchDelete(@RequestBody @Validated MetricsBatchDeleteParam param) {
        metricsService.deleteBatch(param);
        return Ajax.success();
    }
}
