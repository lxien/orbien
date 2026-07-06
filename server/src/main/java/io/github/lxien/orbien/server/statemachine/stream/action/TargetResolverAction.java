package io.github.lxien.orbien.server.statemachine.stream.action;

import io.github.lxien.orbien.core.domain.*;
import io.github.lxien.orbien.core.enums.LoadBalanceType;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.transport.api.TransportEndpointResolver;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.loadbalance.LoadBalancer;
import io.github.lxien.orbien.server.loadbalance.LoadBalancerFactory;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.statemachine.agent.AgentManager;
import io.github.lxien.orbien.server.statemachine.stream.StreamContext;
import io.github.lxien.orbien.server.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.server.statemachine.stream.StreamManager;
import io.github.lxien.orbien.server.statemachine.stream.StreamState;
import io.github.lxien.orbien.server.loadbalance.HealthManager;
import io.github.lxien.orbien.server.transport.BandwidthLimiter;
import io.github.lxien.orbien.server.vhost.DomainRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.Resource;
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
        // UDP 代理复用同一 DatagramChannel，不能关闭 AUTO_READ，否则后续包永远进不了 UdpVisitorHandler
        if (!context.isDatagram()) {
            visitor.config().setOption(ChannelOption.AUTO_READ, false);
        }
        ProxyConfigExt ext = resolveProxyConfig(context);
        if (ext == null || ext.getProxyConfig().getStatus().isClosed()) {
            logger.debug("代理不可用，关闭流：streamId={}", context.getStreamId());
            context.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
            return;
        }
        ProxyConfig config = ext.getProxyConfig();
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
            //传输协议
            TransportProtocol transportProtocol = config.getTransportProtocol(TransportProtocol.TCP);
            context.setTransportProtocol(transportProtocol);
            context.setMultiplex(TransportEndpointResolver.normalizeMultiplex(
                    transportProtocol, config.isMuxTunnelFor(transportProtocol)));

            logger.debug("[传输] 流 {} 代理={} 数据隧道传输协议={} encrypt={} multiplex={}",
                    context.getStreamId(), config.getName(), transportProtocol.getName(),
                    config.isEncrypt(), context.isMultiplex());

            if (config.isUdp()) {
                context.setDatagram(true);
            }
            context.setTarget(selectedTarget);
            context.fireEvent(StreamEvent.TARGET_VALIDATED);
        } else {
            logger.debug("代理 {} 客户端不可用，关闭流: streamId={}", config.getProxyId(), context.getStreamId());
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
        if (healthCheck != null && healthCheck.isEnabled() && !config.isUdp()) {
            availableTargets = healthManager.getAvailableTargets(config.getProxyId(), targets);
        }
        if (CollectionUtils.isEmpty(availableTargets)) {
            logger.warn("没有可用的健康目标: proxy={}", config.getName());
            return null;
        }
        if (config.isLoadBalanceNeeded()) {
            LoadBalanceType loadBalanceType = config.getLoadBalanceType();
            LoadBalancer loadBalancer = loadBalancerFactory.getLoadBalancer(loadBalanceType);
            Target selected = loadBalancer.select(config.getProxyId(), availableTargets);
            if (selected != null) {
                logger.debug("负载均衡选择: {} -> {}:{}", config.getName(), selected.getHost(), selected.getPort());
            }
            return selected;
        } else {
            Target selected = availableTargets.getFirst();
            logger.debug("单个目标选择: {} -> {}:{}",
                    config.getName(), selected.getHost(), selected.getPort());
            return selected;
        }
    }

    private ProxyConfigExt resolveProxyConfig(StreamContext context) {
        if (context.getProtocol().isHttpOrHttps()) {
            String domain = context.getVisitorDomain();
            String proxyId = domainRegistry.getProxyIdByDomain(domain);
            return proxyConfigService.findById(proxyId);
        } else if (context.getProtocol().isTcp()) {
            int remotePort = context.getListenerPort();
            return proxyConfigService.findByListenPort(remotePort, ProtocolType.TCP);
        } else if (context.getProtocol().isUdp()) {
            int remotePort = context.getListenerPort();
            return proxyConfigService.findByListenPort(remotePort, ProtocolType.UDP);
        }
        return null;
    }
}