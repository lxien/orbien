/*
 *
 *  *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.server.web.controller;

import com.xiaoniucode.etp.server.web.common.message.Ajax;
import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertSaveAndDeployParam;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertSaveParam;
import com.xiaoniucode.etp.server.web.service.SslCertificateService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ssl-cert")
public class SslCertController {
    @Autowired
    private SslCertificateService sslCertificateService;

    @PostMapping("save-cert")
    public Ajax saveCert(@RequestBody SslCertSaveParam param) {
        return Ajax.success(sslCertificateService.saveCert(param));
    }
    @PostMapping("save-and-deploy")
    public Ajax saveAndDeploy(@RequestBody SslCertSaveAndDeployParam param) {
        sslCertificateService.saveAndDeployCert(param);
        return Ajax.success();
    }
    @GetMapping
    public Ajax findByPage(@ModelAttribute PageQuery pageQuery) {
        return Ajax.success(sslCertificateService.findByPage(pageQuery));
    }

    @DeleteMapping
    public Ajax deleteByIds(@RequestBody List<String> ids) {
        sslCertificateService.deleteByIds(ids);
        return Ajax.success();
    }

    @GetMapping("download-cert/{certId}")
    public void downloadCert(@PathVariable String certId, HttpServletResponse response) {
        sslCertificateService.downloadCert(certId, response);
    }
}
