package com.xiaoniucode.etp.server.statemachine.stream.action;

import com.xiaoniucode.etp.core.domain.*;
import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.server.loadbalance.LoadBalancer;
import com.xiaoniucode.etp.server.loadbalance.LoadBalancerFactory;
import com.xiaoniucode.etp.server.service.ProxyConfigService;
import com.xiaoniucode.etp.server.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.server.statemachine.agent.AgentManager;
import com.xiaoniucode.etp.server.statemachine.stream.StreamContext;
import com.xiaoniucode.etp.server.statemachine.stream.StreamEvent;
import com.xiaoniucode.etp.server.statemachine.stream.StreamManager;
import com.xiaoniucode.etp.server.statemachine.stream.StreamState;
import com.xiaoniucode.etp.server.loadbalance.HealthManager;
import com.xiaoniucode.etp.server.transport.BandwidthLimiter;
import com.xiaoniucode.etp.server.vhost.DomainRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

@Component
public class TargetResolverAction extends StreamBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(TargetResolverAction.class);
    @Autowired
    private LoadBalancerFactory loadBalancerFactory;
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private HealthManager healthManager;
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private DomainRegistry domainRegistry;
    @Override
    protected void doExecute(StreamState from, StreamState to, StreamEvent event, StreamContext context) {
        Channel visitor = context.getVisitor();
        visitor.config().setOption(ChannelOption.AUTO_READ, false);
        ProxyConfig config = resolveProxyConfig(context);
        if (config == null || config.getStatus().isClosed()) {
            logger.debug("代理不可用，关闭流：streamId={}", context.getStreamId());
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        Optional<AgentContext> gentContextOpt = agentManager.getAgentContext(config.getAgentId());
        if (gentContextOpt.isPresent()) {
            context.setAgentContext(gentContextOpt.get());
            context.setProxyConfig(config);
            context.setAgentContext(gentContextOpt.get());
            Target selectedTarget = selectTarget(config);
            if (selectedTarget == null) {
                logger.warn("无可用 proxyId={} 后端目标，关闭流: streamId={}", config.getProxyId(), context.getStreamId());
                context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
                return;
            }
            BandwidthConfig bandwidth = config.getBandwidth();
            if (bandwidth != null) {
                StreamManager streamManager = context.getStreamManager();
                BandwidthLimiter bandwidthLimiter = streamManager.getOrCreateProxyLimiter(config.getProxyId(), bandwidth);
                context.setBandwidthLimiter(bandwidthLimiter);
                streamManager.incrementStreamCount(config.getProxyId());
            }
            context.setCompress(config.isCompress());
            context.setEncrypt(config.isEncrypt());
            context.setTarget(selectedTarget);
            context.fireEvent(StreamEvent.TARGET_VALIDATED);
        } else {
            logger.debug("代理 {} 客户端不可用，关闭流: streamId={}", config.getProxyId(), context.getStreamId());
            //客户端不可用
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
        }
    }

    private Target selectTarget(ProxyConfig config) {
        List<Target> targets = config.getTargets();
        if (CollectionUtils.isEmpty(targets)) {
            return null;
        }
        List<Target> availableTargets = targets;
        HealthCheckConfig healthCheck = config.getHealthCheck();
        //如果配置了健康检查 则获取健康目标服务列表
        if (healthCheck != null && healthCheck.isEnabled()) {
            availableTargets = healthManager.getAvailableTargets(config.getProxyId(), targets);
        }
        if (CollectionUtils.isEmpty(availableTargets)) {
            logger.warn("没有可用的健康目标: proxy={}", config.getName());
            return null;
        }
        if (config.isLoadBalanceNeeded()) {
            LoadBalanceConfig loadBalanceConfig = config.getLoadBalance();
            LoadBalancer loadBalancer = loadBalancerFactory.getLoadBalancer(loadBalanceConfig);
            Target selected = loadBalancer.select(config.getProxyId(), availableTargets);
            if (selected != null) {
                logger.debug("负载均衡选择: {} -> {}:{}",
                        config.getName(), selected.getHost(), selected.getPort());
            }
            return selected;
        } else {
            Target selected = availableTargets.getFirst();
            logger.debug("单个目标选择: {} -> {}:{}",
                    config.getName(), selected.getHost(), selected.getPort());
            return selected;
        }
    }

    private ProxyConfig resolveProxyConfig(StreamContext context) {
        if (context.getProtocol() == ProtocolType.HTTP) {
            String domain = context.getVisitorDomain();
            String proxyId = domainRegistry.getProxyIdByDomain(domain);
            return proxyConfigService.findById(proxyId).orElse(null);
        } else if (context.getProtocol() == ProtocolType.TCP) {
            int remotePort = context.getListenerPort();
            return proxyConfigService.findByListenPort(remotePort).orElse(null);
        }
        return null;
    }
}