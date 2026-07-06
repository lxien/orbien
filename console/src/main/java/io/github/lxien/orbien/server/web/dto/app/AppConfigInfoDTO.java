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

package io.github.lxien.orbien.server.web.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * orbien 代理服务 配置信息
 */
@Data
public class AppConfigInfoDTO implements Serializable {
    private String serverAddr;
    private Integer serverPort;
    private Integer httpProxyPort;
    private Integer httpsProxyPort;
    private String rootDomain;
    /**
     * WebSocket 传输是否启用
     */
    private Boolean websocketEnabled;
    /**
     * WebSocket 监听端口（启用时有效）
     */
    private Integer websocketPort;
    /**
     * QUIC 传输是否启用
     */
    private Boolean quicEnabled;
    /**
     * QUIC 监听端口（启用时有效）
     */
    private Integer quicPort;
}