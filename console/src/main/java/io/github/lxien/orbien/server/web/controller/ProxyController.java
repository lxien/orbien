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
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.dto.proxy.HttpProxyDetailDTO;
import io.github.lxien.orbien.server.web.dto.proxy.HttpsProxyDetailDTO;
import io.github.lxien.orbien.server.web.dto.proxy.HttpsProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.HttpProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.TcpProxyDetailDTO;
import io.github.lxien.orbien.server.web.dto.proxy.TcpProxyListDTO;
import io.github.lxien.orbien.server.web.dto.proxy.UdpProxyDetailDTO;
import io.github.lxien.orbien.server.web.dto.proxy.UdpProxyListDTO;
import io.github.lxien.orbien.server.web.param.proxy.*;
import io.github.lxien.orbien.server.web.param.proxy.*;
import io.github.lxien.orbien.server.web.service.ProxyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Validated
@RestController
@RequestMapping("/api/proxies")
public class ProxyController {
    @Autowired
    private ProxyService proxyService;

    @DeleteMapping
    public Ajax batchDelete(@RequestBody @Validated ProxyBatchDeleteParam param) {
        proxyService.batchDeleteProxies(param);
        return Ajax.success();
    }

    @PostMapping("tcp")
    public Ajax createTcpProxy(@RequestBody @Validated TcpProxyCreateParam param) {
        proxyService.createTcpProxy(param);
        return Ajax.success();
    }

    @PostMapping("udp")
    public Ajax createUdpProxy(@RequestBody @Validated UdpProxyCreateParam param) {
        proxyService.createUdpProxy(param);
        return Ajax.success();
    }

    @PostMapping("http")
    public Ajax createHttpProxy(@RequestBody @Validated HttpProxyCreateParam param) {
        proxyService.createHttpProxy(param);
        return Ajax.success();
    }
    @PostMapping("https")
    public Ajax createHttpsProxy(@RequestBody @Validated HttpsProxyCreateParam param) {
        proxyService.createHttpsProxy(param);
        return Ajax.success();
    }
    @PutMapping("tcp")
    public Ajax updateTcpProxy(@RequestBody @Validated TcpProxyUpdateParam param) {
        proxyService.updateTcpProxy(param);
        return Ajax.success();
    }

    @PutMapping("udp")
    public Ajax updateUdpProxy(@RequestBody @Validated UdpProxyUpdateParam param) {
        proxyService.updateUdpProxy(param);
        return Ajax.success();
    }

    @PutMapping("http")
    public Ajax updateHttpProxy(@RequestBody @Validated HttpProxyUpdateParam param) {
        proxyService.updateHttpProxy(param);
        return Ajax.success();
    }
    @PutMapping("https")
    public Ajax updateHttpsProxy(@RequestBody @Validated HttpsProxyUpdateParam param) {
        proxyService.updateHttpsProxy(param);
        return Ajax.success();
    }
    @GetMapping("tcp/{id}")
    public Ajax getTcpProxyDetailById(@PathVariable String id) {
        TcpProxyDetailDTO proxy = proxyService.getTcpProxyById(id);
        return Ajax.success(proxy);
    }

    @GetMapping("udp/{id}")
    public Ajax getUdpProxyDetailById(@PathVariable String id) {
        UdpProxyDetailDTO proxy = proxyService.getUdpProxyById(id);
        return Ajax.success(proxy);
    }

    @GetMapping("http/{id}")
    public Ajax getHttpProxyDetailById(@PathVariable String id) {
        HttpProxyDetailDTO proxy = proxyService.getHttpProxyById(id);
        return Ajax.success(proxy);
    }
    @GetMapping("https/{id}")
    public Ajax getHttpsProxyDetailById(@PathVariable String id) {
        HttpsProxyDetailDTO proxy = proxyService.getHttpsProxyById(id);
        return Ajax.success(proxy);
    }
    @GetMapping("tcp")
    public Ajax findTcpProxies(@ModelAttribute PageQuery pageQuery) {
        PageResult<TcpProxyListDTO> proxies = proxyService.findTcpProxies(pageQuery);
        return Ajax.success(proxies);
    }

    @GetMapping("udp")
    public Ajax findUdpProxies(@ModelAttribute PageQuery pageQuery) {
        PageResult<UdpProxyListDTO> proxies = proxyService.findUdpProxies(pageQuery);
        return Ajax.success(proxies);
    }

    @GetMapping("http")
    public Ajax getHttpProxies(@ModelAttribute PageQuery pageQuery) {
        PageResult<HttpProxyListDTO> proxies = proxyService.findHttpProxies(pageQuery);
        return Ajax.success(proxies);
    }
    @GetMapping("https")
    public Ajax getHttpsProxies(@ModelAttribute PageQuery pageQuery) {
        PageResult<HttpsProxyListDTO> proxies = proxyService.findHttpsProxies(pageQuery);
        return Ajax.success(proxies);
    }
    @PutMapping("status/{id}")
    public Ajax setStatus(@PathVariable String id, @Valid @RequestBody ProxyStatusUpdateParam param) {
        proxyService.setProxyStatus(id, param.getStatus());
        return Ajax.success();
    }

    @PutMapping("{id}/cluster")
    public Ajax saveClusterConfig(@PathVariable String id, @RequestBody @Validated ProxyClusterSaveParam param) {
        proxyService.saveClusterConfig(id, param);
        return Ajax.success();
    }
}
