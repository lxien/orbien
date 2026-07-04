/*
 *
 *  *    Copyright 2026 lxien
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

package io.github.lxien.orbien.server.statemachine.agent.action;

import io.github.lxien.orbien.core.domain.DomainInfo;
import io.github.lxien.orbien.core.domain.HealthCheckConfig;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.enums.HealthCheckType;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.server.statemachine.agent.AgentState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

/**
 * 将代理配置信息同步给在线客户端
 */
@Component
public class ProxyConfigSyncAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyConfigSyncAction.class);
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Resource
    private AppConfig appConfig;

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        logger.debug("开始将代理配置运行信息同步到在线客户端");
        String agentId = context.getAgentId();
        List<ProxyConfigExt> proxies = proxyConfigService.findByAgentId(agentId);
        if (CollectionUtils.isEmpty(proxies)) {
            return;
        }
        List<Message.RuntimeInfo> list = proxies.stream().map(p -> {
            ProxyConfig config = p.getProxyConfig();
            Set<DomainInfo> domains = p.getDomains();

            List<Message.Target> targetList = config.getTargets().stream().map(t ->
                    Message.Target.newBuilder()
                            .setHost(t.getHost())
                            .setPort(t.getPort())
                            .setName(t.getName())
                            .setWeight(t.getWeight())
                            .build()).toList();

            Message.RuntimeInfo.Builder runtimeInfoBuilder = Message.RuntimeInfo.newBuilder()
                    .setProxyId(config.getProxyId())
                    .setName(config.getName())
                    .addAllTargets(targetList);

            List<String> remoteAdders = domains.stream()
                    .map(d -> buildAddress(d.getFullDomain(), config.getProtocol())).toList();
            runtimeInfoBuilder.addAllRemoteAddr(remoteAdders);

            HealthCheckConfig healthCheckConfig = config.getHealthCheck();
            if (healthCheckConfig != null && healthCheckConfig.isEnabled()) {
                Message.HealthCheck healthCheck = Message.HealthCheck.newBuilder()
                        .setType(convertHealthCheckType(healthCheckConfig.getType()))
                        .setInterval(healthCheckConfig.getInterval())
                        .setTimeout(healthCheckConfig.getTimeout())
                        .setMaxFailed(healthCheckConfig.getMaxFailed())
                        .setPath(healthCheckConfig.getPath())
                        .setEnabled(healthCheckConfig.isEnabled())
                        .build();
                runtimeInfoBuilder.setHealthCheck(healthCheck);
            }
            return runtimeInfoBuilder.build();
        }).toList();
        Message.ProxySyncResponse.Builder builder = Message.ProxySyncResponse.newBuilder();
        builder.setProxySyncType(Message.ProxySyncType.FULL);
        builder.addAllItems(list);

        Channel control = context.getControl();
        ByteBuf payload = ProtobufUtil.toByteBuf(builder.build(), control.alloc());
        TMSPFrame frame = new TMSPFrame(0, TMSP.MSG_CONFIG_SYNC, payload);
        control.writeAndFlush(frame);
    }

    private Message.HealthCheckType convertHealthCheckType(HealthCheckType type) {
        return switch (type) {
            case TCP -> Message.HealthCheckType.HEALTH_CHECK_TYPE_TCP;
            case HTTP -> Message.HealthCheckType.HEALTH_CHECK_TYPE_HTTP;
        };
    }

    private String buildAddress(String domain, ProtocolType protocolType) {
        String prefix;
        String port;

        switch (protocolType) {
            case HTTP:
                prefix = "http://";
                int httpPort = appConfig.getHttpProxyPort();
                port = httpPort == 80 ? "" : ":" + httpPort;
                break;
            case HTTPS:
                prefix = "https://";
                int httpsPort = appConfig.getHttpsProxyPort();
                port = httpsPort == 443 ? "" : ":" + httpsPort;
                break;
            default:
                prefix = "";
                port = "";
        }

        return prefix + domain + port;
    }
}
