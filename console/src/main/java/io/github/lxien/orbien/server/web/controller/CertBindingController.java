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

package io.github.lxien.orbien.server.web.controller;

import io.github.lxien.orbien.server.web.common.message.Ajax;
import io.github.lxien.orbien.server.web.param.binding.CertBindParam;
import io.github.lxien.orbien.server.web.param.binding.CertBindPreviewParam;
import io.github.lxien.orbien.server.web.param.binding.CertRebindParam;
import io.github.lxien.orbien.server.web.service.CertBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cert-binding")
@RequiredArgsConstructor
public class CertBindingController {

    private final CertBindingService certBindingService;

    @PostMapping("/bind")
    public Ajax bind(@RequestBody @Validated CertBindParam param) {
        return Ajax.success(certBindingService.bind(param));
    }

    @PostMapping("/preview")
    public Ajax preview(@RequestBody @Validated CertBindPreviewParam param) {
        return Ajax.success(certBindingService.preview(param));
    }

    @GetMapping("/bindable-domains")
    public Ajax listBindableDomains(@RequestParam String certId) {
        return Ajax.success(certBindingService.listBindableDomains(certId));
    }

    @GetMapping("/proxy/{proxyId}")
    public Ajax getProxyCertMatrix(@PathVariable String proxyId) {
        return Ajax.success(certBindingService.getProxyCertMatrix(proxyId));
    }

    @GetMapping("/cert/{certId}")
    public Ajax getCertUsage(@PathVariable String certId) {
        return Ajax.success(certBindingService.getCertUsage(certId));
    }

    @PutMapping("/{bindingId}/disable")
    public Ajax disable(@PathVariable Long bindingId) {
        certBindingService.disable(bindingId);
        return Ajax.success();
    }

    @PutMapping("/{bindingId}/enable")
    public Ajax enable(@PathVariable Long bindingId) {
        certBindingService.enable(bindingId);
        return Ajax.success();
    }

    @DeleteMapping("/{bindingId}")
    public Ajax unbind(@PathVariable Long bindingId) {
        certBindingService.unbind(bindingId);
        return Ajax.success();
    }

    @PutMapping("/{bindingId}/rebind")
    public Ajax rebind(@PathVariable Long bindingId, @RequestBody @Validated CertRebindParam param) {
        certBindingService.rebind(bindingId, param);
        return Ajax.success();
    }

    @PostMapping("/{bindingId}/redeploy")
    public Ajax redeploy(@PathVariable Long bindingId) {
        certBindingService.redeploy(bindingId);
        return Ajax.success();
    }

    @PutMapping("/proxy/{proxyId}/disable-all")
    public Ajax disableAllByProxy(@PathVariable String proxyId) {
        certBindingService.disableAllByProxy(proxyId);
        return Ajax.success();
    }
}
