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

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProxyProperties implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 公网端口
     */
    private Integer remotePort;

    /**
     * 内网 IP
     */
    private String localIp = "127.0.0.1";

    /**
     * 代理协议，仅支持 HTTP / HTTPS（对应内嵌 Web 容器）
     */
    private WebProxyProtocol protocol = WebProxyProtocol.HTTP;
    private Boolean forceHttps = false;

    /**
     * 自定义域名列表
     */
    private List<String> customDomains = new ArrayList<>();

    /**
     * 是否自动生成域名，默认自动生成子域名
     */
    private Boolean autoDomain = true;

    /**
     * 子域名列表
     */
    private List<String> subDomains = new ArrayList<>();

    /**
     * 访问控制
     */
    @NestedConfigurationProperty
    private AccessControlProperties accessControl = new AccessControlProperties();

    /**
     * 基础认证
     */
    @NestedConfigurationProperty
    private BasicAuthProperties basicAuth = new BasicAuthProperties();

    /**
     * 带宽限制配置
     */
    @NestedConfigurationProperty
    private BandwidthProperties bandwidth = new BandwidthProperties();
    @NestedConfigurationProperty
    private TransportCustomProperties transport = new TransportCustomProperties();
}
