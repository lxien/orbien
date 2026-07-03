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

import com.xiaoniucode.etp.server.web.dto.binding.*;
import com.xiaoniucode.etp.server.web.param.binding.CertBindParam;
import com.xiaoniucode.etp.server.web.param.binding.CertBindPreviewParam;
import com.xiaoniucode.etp.server.web.param.binding.CertRebindParam;

import java.util.List;

public interface CertBindingService {

    CertBindResultDTO bind(CertBindParam param);

    List<CertBindPreviewItemDTO> preview(CertBindPreviewParam param);

    List<CertBindPreviewItemDTO> listBindableDomains(String certId);

    ProxyCertMatrixDTO getProxyCertMatrix(String proxyId);

    CertUsageDTO getCertUsage(String certId);

    void disable(Long bindingId);

    void enable(Long bindingId);

    void unbind(Long bindingId);

    void rebind(Long bindingId, CertRebindParam param);

    void redeploy(Long bindingId);

    void disableAllByProxy(String proxyId);

    CertBindResultDTO bindMatchingDomainsForProxy(String certId, String proxyId, boolean override);
}
