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

package io.github.lxien.orbien.server.web.service.assembler;

import io.github.lxien.orbien.core.domain.BandwidthConfig;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.server.web.proxy.converter.ProxyModelConvert;
import io.github.lxien.orbien.server.web.entity.ProxyDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProxyAssembler {
    @Autowired
    private ProxyModelConvert proxyModelConvert;

    public ProxyConfig toProxyConfig(ProxyDO proxyDO) {
        ProxyConfig proxyConfig = proxyModelConvert.toProxyConfig(proxyDO);
        BandwidthConfig bc = new BandwidthConfig(proxyDO.getLimitTotal(), proxyDO.getLimitIn(), proxyDO.getLimitOut());
        proxyConfig.setBandwidth(bc);
        return proxyConfig;
    }
}
