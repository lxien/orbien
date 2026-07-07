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
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.param.tls.TlsCertAutoRenewParam;
import io.github.lxien.orbien.server.web.param.tls.TlsCertSaveAndDeployParam;
import io.github.lxien.orbien.server.web.param.tls.TlsCertSaveParam;
import io.github.lxien.orbien.server.web.service.TlsCertificateService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tls-cert")
public class TlsCertController {
    @Autowired
    private TlsCertificateService tlsCertificateService;

    @PostMapping("save-cert")
    public Ajax saveCert(@RequestBody TlsCertSaveParam param) {
        return Ajax.success(tlsCertificateService.saveCert(param));
    }
    @PostMapping("save-and-deploy")
    public Ajax saveAndDeploy(@RequestBody TlsCertSaveAndDeployParam param) {
        return Ajax.success(tlsCertificateService.saveAndDeployCert(param));
    }
    @GetMapping
    public Ajax findByPage(@ModelAttribute PageQuery pageQuery) {
        return Ajax.success(tlsCertificateService.findByPage(pageQuery));
    }

    @DeleteMapping
    public Ajax deleteByIds(@RequestBody List<String> ids) {
        tlsCertificateService.deleteByIds(ids);
        return Ajax.success();
    }

    @GetMapping("download-cert/{certId}")
    public void downloadCert(@PathVariable String certId, HttpServletResponse response) {
        tlsCertificateService.downloadCert(certId, response);
    }

    @PutMapping("{certId}/auto-renew")
    public Ajax updateAutoRenew(@PathVariable String certId, @RequestBody TlsCertAutoRenewParam param) {
        return Ajax.success(tlsCertificateService.updateAutoRenew(certId, Boolean.TRUE.equals(param.getAutoRenew())));
    }
}
