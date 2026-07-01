/*
 *    Copyright 2026 xiaoniucode
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http:
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.xiaoniucode.etp.server.web.param.proxy;

import com.xiaoniucode.etp.core.enums.DeploymentMode;
import com.xiaoniucode.etp.core.enums.DomainType;
import com.xiaoniucode.etp.core.enums.ProxyStatus;
import com.xiaoniucode.etp.server.web.param.bandwidth.BandwidthSaveParam;
import com.xiaoniucode.etp.server.web.param.loadbalance.LoadBalanceParam;
import com.xiaoniucode.etp.server.web.param.transport.TransportSaveParam;
import com.xiaoniucode.etp.server.web.support.validation.EnumValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * HTTP 代理请求参数类
 */
@Getter
@Setter
public class HttpProxyCreateParam {
    @NotEmpty(message = "agentId 不能为空")
    private String agentId;
    @NotEmpty(message = "name 不能为空")
    private String name;
    @NotNull(message = "status 不能为空")
    @EnumValue(enumClass = ProxyStatus.class)
    private Integer status;
    @NotNull(message = "domainType 不能为空")
    @EnumValue(enumClass = DomainType.class)
    private Integer domainType;
    private String baseDomain;
    private Set<String> domains;
    @NotNull(message = "部署模式不能为空")
    @EnumValue(enumClass = DeploymentMode.class)
    private Integer deploymentMode;
    @NotNull(message = "targets 不能为空")
    @Size(min = 1, max = 100, message = "targets 必须包含至少一个服务")
    @Valid
    private List<ProxyTargetSaveParam> targets;
    @NotNull(message = "transport 不能为空")
    @Valid
    private TransportSaveParam transport;
    @Valid
    private BandwidthSaveParam bandwidth;
    @Valid
    private LoadBalanceParam loadBalance;

}