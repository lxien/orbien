/*
 *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.server.web.controller;

import com.xiaoniucode.etp.server.web.common.message.Ajax;
import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.dto.portpool.PortPoolDTO;
import com.xiaoniucode.etp.server.web.param.portpool.PortPoolBatchDeleteParam;
import com.xiaoniucode.etp.server.web.param.portpool.PortPoolCreateParam;
import com.xiaoniucode.etp.server.web.param.portpool.PortPoolUpdateParam;
import com.xiaoniucode.etp.server.web.service.PortPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/port-pools")
public class PortPoolController {
    @Autowired
    private PortPoolService portPoolService;

    @GetMapping
    public Ajax findByPage(@ModelAttribute PageQuery pageQuery) {
        PageResult<PortPoolDTO> result = portPoolService.findByPage(pageQuery);
        return Ajax.success(result);
    }

    @GetMapping("/{id}")
    public Ajax getById(@PathVariable Long id) {
        PortPoolDTO dto = portPoolService.getById(id);
        return Ajax.success(dto);
    }

    @PostMapping
    public Ajax create(@RequestBody @Validated PortPoolCreateParam param) {
        PortPoolDTO dto = portPoolService.create(param);
        return Ajax.success(dto);
    }

    @PutMapping
    public Ajax update(@RequestBody @Validated PortPoolUpdateParam param) {
        portPoolService.update(param);
        return Ajax.success();
    }

    @DeleteMapping
    public Ajax batchDelete(@RequestBody @Validated PortPoolBatchDeleteParam param) {
        portPoolService.deleteBatch(param);
        return Ajax.success();
    }
}
