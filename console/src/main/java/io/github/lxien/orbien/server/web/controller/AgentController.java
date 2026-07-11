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
import io.github.lxien.orbien.server.web.dto.agent.AgentDTO;
import io.github.lxien.orbien.server.web.param.agent.AgentBatchDeleteParam;
import io.github.lxien.orbien.server.web.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@Validated
public class AgentController {
    @Autowired
    private AgentService agentService;

    @GetMapping("list-by-page")
    public Ajax findByPage(@ModelAttribute PageQuery pageQuery) {
        PageResult<AgentDTO> clients = agentService.findByPage(pageQuery);
        return Ajax.success(clients);
    }

    @GetMapping("list")
    public Ajax listAll() {
        List<AgentDTO> clients = agentService.findAll();
        return Ajax.success(clients);
    }

    @GetMapping("list-for-proxy")
    public Ajax listForProxy(@RequestParam(required = false) String includeId) {
        List<AgentDTO> clients = agentService.findForProxySelection(includeId);
        return Ajax.success(clients);
    }

    @GetMapping("/{id}")
    public Ajax getById(@PathVariable String id) {
        AgentDTO client = agentService.findById(id);
        return Ajax.success(client);
    }

    @PutMapping("/kickout/{id}")
    public Ajax kickout(@PathVariable String id) {
        agentService.kickout(id);
        return Ajax.success();
    }

    @DeleteMapping
    public Ajax batchDelete(@RequestBody @Validated AgentBatchDeleteParam param) {
        agentService.deleteBatch(param);
        return Ajax.success();
    }
}
