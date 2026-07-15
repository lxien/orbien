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
import io.github.lxien.orbien.server.web.dto.headerrewrite.HeaderRewriteDetailDTO;
import io.github.lxien.orbien.server.web.param.headerrewrite.HeaderRewriteRuleAddParam;
import io.github.lxien.orbien.server.web.param.headerrewrite.HeaderRewriteRuleUpdateParam;
import io.github.lxien.orbien.server.web.param.headerrewrite.HeaderRewriteUpdateParam;
import io.github.lxien.orbien.server.web.service.HeaderRewriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/header-rewrite")
public class HeaderRewriteController {
    @Autowired
    private HeaderRewriteService headerRewriteService;

    @GetMapping("/{proxyId}")
    public Ajax get(@PathVariable String proxyId) {
        HeaderRewriteDetailDTO detail = headerRewriteService.getByProxyId(proxyId);
        return Ajax.success(detail);
    }

    @PutMapping
    public Ajax update(@RequestBody @Validated HeaderRewriteUpdateParam param) {
        headerRewriteService.update(param);
        return Ajax.success();
    }

    @PostMapping("/rule")
    public Ajax addRule(@RequestBody @Validated HeaderRewriteRuleAddParam param) {
        headerRewriteService.addRule(param);
        return Ajax.success();
    }

    @PutMapping("/rule")
    public Ajax updateRule(@RequestBody @Validated HeaderRewriteRuleUpdateParam param) {
        headerRewriteService.updateRule(param);
        return Ajax.success();
    }

    @DeleteMapping("/rule/{id}")
    public Ajax deleteRule(@PathVariable Long id) {
        headerRewriteService.deleteRule(id);
        return Ajax.success();
    }
}
