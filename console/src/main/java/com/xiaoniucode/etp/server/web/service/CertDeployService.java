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

package com.xiaoniucode.etp.server.web.service;

import com.xiaoniucode.etp.server.web.dto.deploy.SslDeployDTO;
import com.xiaoniucode.etp.server.web.dto.deploy.SslDeployInfoDTO;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertDeployParam;

import java.util.List;

public interface CertDeployService {
    void closeSsl(String proxyId);

    void deleteDeploy(Long deployId);
    void batchDeleteDeploy(List<Long> deployId);

    SslDeployInfoDTO getSslDeployInfo(String proxyId);

    /**
     * 部署并覆盖，如果已经部署过则覆盖当前部署
     *
     * @param param 部署信息
     * @return 部署结果
     */
    SslDeployDTO deployAndOverride(SslCertDeployParam param);

}
