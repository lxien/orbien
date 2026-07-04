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

package io.github.lxien.orbien.server.web.controller;

import io.github.lxien.orbien.server.web.common.message.Ajax;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.dto.domain.DomainDTO;
import io.github.lxien.orbien.server.web.dto.domain.UsedDomainDTO;
import io.github.lxien.orbien.server.web.param.domain.DomainBatchDeleteParam;
import io.github.lxien.orbien.server.web.param.domain.DomainCreateParam;
import io.github.lxien.orbien.server.web.param.domain.DomainUpdateParam;
import io.github.lxien.orbien.server.web.service.DomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/domains")
public class DomainController {
    @Autowired
    private DomainService domainService;

    @GetMapping
    public Ajax findByPage(@ModelAttribute PageQuery pageQuery) {
        PageResult<DomainDTO> domains = domainService.findByPage(pageQuery);
        return Ajax.success(domains);
    }

    @GetMapping("/list")
    public Ajax listAll() {
        return Ajax.success(domainService.findAll());
    }

    @GetMapping("/used")
    public Ajax findUsedByPage(@ModelAttribute PageQuery pageQuery) {
        PageResult<UsedDomainDTO> domains = domainService.findUsedByPage(pageQuery);
        return Ajax.success(domains);
    }

    @GetMapping("/{id}")
    public Ajax getById(@PathVariable Integer id) {
        DomainDTO domain = domainService.getById(id);
        return Ajax.success(domain);
    }

    @PostMapping
    public Ajax create(@RequestBody @Validated DomainCreateParam param) {
        DomainDTO domain = domainService.create(param);
        return Ajax.success(domain);
    }

    @PutMapping
    public Ajax update(@RequestBody @Validated DomainUpdateParam param) {
        domainService.update(param);
        return Ajax.success();
    }

    @DeleteMapping
    public Ajax batchDelete(@RequestBody @Validated DomainBatchDeleteParam param) {
        domainService.deleteBatch(param);
        return Ajax.success();
    }
}
