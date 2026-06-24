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
import com.xiaoniucode.etp.server.web.dto.deploy.SslDeployDTO;
import com.xiaoniucode.etp.server.web.dto.deploy.SslDeployInfoDTO;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertDeployParam;
import com.xiaoniucode.etp.server.web.service.CertDeployService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cert-deploy")
@RequiredArgsConstructor
public class CertDeployController {

    private final CertDeployService certificateDeploymentService;

    @PostMapping("deploy")
    public Ajax deploy(@RequestBody SslCertDeployParam param) {
        SslDeployDTO sslDeployDTO = certificateDeploymentService.deploy(param);
        return Ajax.success(sslDeployDTO);
    }

    @DeleteMapping("delete/{deployId}")
    public Ajax deleteDeploy(@PathVariable Long deployId) {
        certificateDeploymentService.deleteDeploy(deployId);
        return Ajax.success();
    }

    @PutMapping("close-ssl/{proxyId}")
    public Ajax closeSsl(@PathVariable String proxyId) {
        certificateDeploymentService.closeSsl(proxyId);
        return Ajax.success();
    }

    @GetMapping("get-ssl/{proxyId}")
    public Ajax getSslInfo(@PathVariable String proxyId) {
        SslDeployInfoDTO sslDeployInfoDTO = certificateDeploymentService.getSslDeployInfo(proxyId);
        return Ajax.success(sslDeployInfoDTO);
    }
}
