package com.xiaoniucode.etp.client.statemachine.agent.action;

import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.config.ConfigUtils;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.AgentState;
import com.xiaoniucode.etp.common.utils.StringUtils;
import com.xiaoniucode.etp.core.domain.*;
import com.xiaoniucode.etp.core.enums.LoadBalanceType;
import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.message.TMSP;
import com.xiaoniucode.etp.core.message.TMSPFrame;
import com.xiaoniucode.etp.core.utils.ProtobufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AuthSuccessAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AuthSuccessAction.class);
    private final AtomicBoolean init = new AtomicBoolean(false);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        if (init.get()) {
            return;
        }
        init.set(true);
        AppConfig configs = ConfigUtils.getConfig();
        List<ProxyConfig> proxies = configs.getProxies();
        Channel control = context.getControl();
        for (ProxyConfig config : proxies) {
            Message.NewProxy newProxy = buildNewProxy(config);
            ByteBuf payload = ProtobufUtil.toByteBuf(newProxy, control.alloc());
            TMSPFrame frame = new TMSPFrame(0, TMSP.MSG_PROXY_CREATE, payload);
            control.write(frame);
        }
        control.flush();
        context.fireEvent(AgentEvent.CREATE_TUNNEL_POOL);
    }

    private Message.NewProxy buildNewProxy(ProxyConfig config) {
        ProtocolType protocol = config.getProtocol();
        Message.NewProxy.Builder newProxyBuilder = Message.NewProxy.newBuilder();

        List<Message.Target> targets = config.getTargets().stream().map(t -> {
                    Message.Target.Builder target = Message.Target.newBuilder()
                            .setHost(t.getHost())
                            .setPort(t.getPort())
                            .setWeight(t.getWeight());
                    if (StringUtils.hasText(t.getName())) {
                        target.setName(t.getName());
                    }
                    if (t.getWeight() != null) {
                        target.setWeight(t.getWeight());
                    }
                    return target.build();
                }
        ).collect(Collectors.toList());
        newProxyBuilder.setName(config.getName())
                .addAllTargets(targets)
                .setForceHttps(config.getForceHttps())
                .setProtocol(Message.ProtocolType.valueOf(config.getProtocol().name()));

        if (config.getStatus().isOpen()) {
            newProxyBuilder.setEnable(true);
        }

        switch (protocol) {
            case TCP:
                if (config.getRemotePort() != null) {
                    newProxyBuilder.setRemotePort(config.getRemotePort());
                }
                break;
            case HTTP:
            case HTTPS:
                //域名配置
                RouteConfig domainInfo = config.getRouteConfig();
                if (domainInfo != null) {
                    Set<String> customDomains = domainInfo.getCustomDomains();
                    Boolean autoDomain = domainInfo.getAutoDomain();
                    Set<String> subDomains = domainInfo.getSubDomains();
                    Message.DomainInfo domainReq = Message.DomainInfo.newBuilder()
                            .setAutoDomain(autoDomain)
                            .addAllCustomDomains(customDomains)
                            .addAllSubDomains(subDomains).build();
                    newProxyBuilder.setDomain(domainReq);
                }

                //Basic Auth 认证
                if (config.hasBasicAuth()) {
                    BasicAuthConfig basicAuth = config.getBasicAuth();
                    Message.BasicAuth.Builder basicAuthBuilder = Message.BasicAuth.newBuilder()
                            .setEnable(basicAuth.isEnabled());
                    Set<HttpUser> users = basicAuth.getUsers();
                    if (users != null && !users.isEmpty()) {
                        for (HttpUser user : users) {
                            Message.HttpUser httpUser = Message.HttpUser.newBuilder()
                                    .setUser(user.getUsername())
                                    .setPass(user.getPassword())
                                    .build();
                            basicAuthBuilder.addHttpUsers(httpUser);
                        }
                    }
                    newProxyBuilder.setBasicAuth(basicAuthBuilder);
                }
                //HTTPS SSL证书
                if (config.isHttps()) {
                    SslConfig sslConfig = config.getSslConfig();
                    if (sslConfig != null) {
                        try {
                            String keyPem = Files.readString(new File(sslConfig.getKeyFile()).toPath(), StandardCharsets.UTF_8);
                            String certChainPem = Files.readString(new File(sslConfig.getCertFile()).toPath(), StandardCharsets.UTF_8);
                            Message.SslInfo.Builder sslInfoBuilder = Message.SslInfo.newBuilder()
                                    .setPrivateKeyPem(keyPem)
                                    .setCertChainPem(certChainPem);
                            newProxyBuilder.setSslInfo(sslInfoBuilder);
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                }
                break;
        }
        //传输
        if (config.hasTransport()) {
            Message.Transport.Builder builder = Message.Transport.newBuilder();
            TransportCustomConfig transport = config.getTransport();
            Boolean encrypt = transport.getEncrypt();
            Boolean compress = transport.getCompress();
            Boolean mux = transport.getMultiplex();
            if (encrypt != null) {
                builder.setEncrypt(encrypt);
            }
            if (compress != null) {
                builder.setCompress(compress);
            }
            if (mux != null) {
                builder.setMux(mux);
            }
            newProxyBuilder.setTransport(builder.build());
        }

        //访问控制
        if (config.hasAccessControl()) {
            AccessControlConfig access = config.getAccessControl();
            Message.AccessControl.Builder accessControlbuilder = Message.AccessControl
                    .newBuilder()
                    .setEnable(access.isEnabled())
                    .setMode(Message.AccessMode.valueOf(access.getMode().name()));
            if (access.hasAllow()) {
                Set<String> allow = access.getAllow();
                accessControlbuilder.addAllAllow(allow);
            }
            if (access.hasDeny()) {
                Set<String> deny = access.getDeny();
                accessControlbuilder.addAllDeny(deny);
            }
            newProxyBuilder.setAccessControl(accessControlbuilder.build());
        }
        //带宽限制
        if (config.hasBandwidthLimit()) {
            BandwidthConfig bandwidth = config.getBandwidth();
            Message.Bandwidth.Builder bw = Message.Bandwidth.newBuilder();
            if (bandwidth.hasLimitConfigured()) {
                bw.setLimit(bandwidth.getLimitTotal());
            }
            if (bandwidth.hasLimitInConfigured()) {
                bw.setLimitIn(bandwidth.getLimitIn());
            }
            if (bandwidth.hasLimitOutConfigured()) {
                bw.setLimitOut(bandwidth.getLimitOut());
            }
            newProxyBuilder.setBandwidth(bw.build());
        }
        //负载均衡
        if (config.hasLoadBalance()) {
            Message.LoadBalance.Builder loadBalanceBuilder = Message.LoadBalance.newBuilder();
            if (config.getLoadBalance().hasStrategy()) {
                Message.LoadBalanceStrategy strategy = toProtoType(config.getLoadBalance().getStrategy());
                loadBalanceBuilder.setStrategy(strategy);
            }
            newProxyBuilder.setLoadBalance(loadBalanceBuilder.build());
        }

        return newProxyBuilder.build();
    }

    private static Message.LoadBalanceStrategy toProtoType(LoadBalanceType strategy) {
        switch (strategy) {
            case ROUND_ROBIN:
                return Message.LoadBalanceStrategy.ROUND_ROBIN;
            case WEIGHT:
                return Message.LoadBalanceStrategy.WEIGHT;
            case RANDOM:
                return Message.LoadBalanceStrategy.RANDOM;
            case LEAST_CONN:
                return Message.LoadBalanceStrategy.LEAST_CONN;
            default:
                throw new IllegalArgumentException("未知负载均衡策略: " + strategy);
        }
    }
}
