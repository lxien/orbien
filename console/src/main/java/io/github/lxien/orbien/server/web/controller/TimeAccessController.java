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
import io.github.lxien.orbien.server.web.dto.timeaccess.TimeAccessDetailDTO;
import io.github.lxien.orbien.server.web.param.timeaccess.TimeAccessUpdateParam;
import io.github.lxien.orbien.server.web.param.timeaccess.TimeAccessWindowAddParam;
import io.github.lxien.orbien.server.web.param.timeaccess.TimeAccessWindowUpdateParam;
import io.github.lxien.orbien.server.web.service.TimeAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/time-access")
public class TimeAccessController {
    @Autowired
    private TimeAccessService timeAccessService;

    @GetMapping("/{proxyId}")
    public Ajax get(@PathVariable String proxyId) {
        TimeAccessDetailDTO detail = timeAccessService.getByProxyId(proxyId);
        return Ajax.success(detail);
    }

    @PutMapping
    public Ajax update(@RequestBody @Validated TimeAccessUpdateParam param) {
        timeAccessService.update(param);
        return Ajax.success();
    }

    @PostMapping("/window")
    public Ajax addWindow(@RequestBody @Validated TimeAccessWindowAddParam param) {
        timeAccessService.addWindow(param);
        return Ajax.success();
    }

    @PutMapping("/window")
    public Ajax updateWindow(@RequestBody @Validated TimeAccessWindowUpdateParam param) {
        timeAccessService.updateWindow(param);
        return Ajax.success();
    }

    @DeleteMapping("/window/{id}")
    public Ajax deleteWindow(@PathVariable Long id) {
        timeAccessService.deleteWindow(id);
        return Ajax.success();
    }
}
