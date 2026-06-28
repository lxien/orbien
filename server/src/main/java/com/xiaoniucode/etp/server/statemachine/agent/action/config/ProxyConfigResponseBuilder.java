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

package com.xiaoniucode.etp.server.statemachine.agent.action.config;

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.core.domain.DomainInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 响应构建器
 */
@Component
public class ProxyConfigResponseBuilder {
    @Resource
    private AppConfig appConfig;

//    /**
//     * 构建新代理响应
//     */
//    public Message.CreateProxyResponse buildNewProxyResponse(ProxyConfig config, Set<DomainInfo> domains) {
//        String remoteAddr = buildRemoteAddress(config, domains);
//
//        return Message.CreateProxyResponse.newBuilder()
//                .setProxyId(config.getProxyId())
//               //todo .setProxyName(config.getName())
//               //todo .setRemoteAddr(remoteAddr)
//                .build();
//    }
//
    /**
     * 构建远程地址信息
     */
    private String buildRemoteAddress(ProxyConfig config, Set<DomainInfo> domains) {
        ProtocolType protocol = config.getProtocol();
        if (protocol.isHttp()) {
            return buildHttpAddress(domains);
        } else if (protocol.isTcp()) {
            return buildTcpAddress(config);
        }
        return "";
    }

    /**
     * 构建HTTP地址
     */
    private String buildHttpAddress(Set<DomainInfo> domains) {
        StringBuilder remoteAddr = new StringBuilder();
        int httpProxyPort = appConfig.getHttpProxyPort();
        for (DomainInfo domain : domains) {
            remoteAddr.append("http://").append(domain.getFullDomain());
            if (httpProxyPort != 80) {
                remoteAddr.append(":").append(httpProxyPort);
            }
            remoteAddr.append("\n");
        }
        return remoteAddr.toString().trim();
    }

    /**
     * 构建TCP地址
     */
    private String buildTcpAddress(ProxyConfig config) {
        String serverAddr = appConfig.getServerAddr();
        Integer listenPort = config.getListenPort();
        return serverAddr + ":" + listenPort;
    }

    /**
     * 构建错误响应
     */
    public Message.Error buildErrorResponse(String message) {
        return Message.Error.newBuilder()
                .setStatus(Message.Status.newBuilder().setCode(1).setMessage(message))
                .build();
    }
}
