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

package io.github.lxien.orbien.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Getter
@Setter
@ConfigurationProperties(prefix = "orbien.client")
public class OrbienClientProperties {
    /**
     * 是否启用 orbien 内网穿透客户端
     */
    private Boolean enabled = false;

    /**
     * 代理服务地址
     */
    private String serverAddr = "127.0.0.1";

    /**
     * 代理服务端口
     */
    private Integer serverPort = 9527;


    @NestedConfigurationProperty
    private AuthProperties auth = new AuthProperties();

    @NestedConfigurationProperty
    private ProxyProperties proxy = new ProxyProperties();

    @NestedConfigurationProperty
    private TransportProperties transport = new TransportProperties();
    @NestedConfigurationProperty
    private ConnectionProperties connection = new ConnectionProperties();
}
