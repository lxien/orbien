package io.github.lxien.orbien.server.statemachine.agent.action.config;

import io.github.lxien.orbien.core.domain.ProxyConfigExt;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.support.RuntimeInfoSupport;
import io.github.lxien.orbien.core.notify.EventBus;
import io.github.lxien.orbien.server.config.AppConfig;
import io.github.lxien.orbien.server.event.ProxyAddEvent;
import io.github.lxien.orbien.server.service.ProxyConfigService;
import io.github.lxien.orbien.server.statemachine.agent.AgentContext;
import io.github.lxien.orbien.server.uid.UidGenerator;
import io.github.lxien.orbien.server.manager.ProxyManager;
import io.github.lxien.orbien.core.enums.PortPoolType;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UdpProxyProcessor implements ProxyProcessor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(UdpProxyProcessor.class);
    @Autowired
    private ProxyManager proxyManager;
    @Autowired
    private UidGenerator uidGenerator;
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ListenPortResolver listenPortResolver;
    @Resource
    private AppConfig appConfig;

    @Override
    public Message.RuntimeInfo process(AgentContext context, Message.Proxy proxy) throws Exception {
        String agentId = context.getAgentId();
        int remotePort = proxy.getRemotePort();
        String name = proxy.getName();
        ProxyConfigExt ext = proxyConfigService.findByAgentAndName(agentId, name);
        String proxyId;
        if (ext != null) {
            proxyId = ext.getProxyConfig().getProxyId();
            proxyManager.deactivate(proxyId);
        } else {
            proxyId = uidGenerator.getUIDAsString();
        }
        Integer listenPort = listenPortResolver.resolve(remotePort, ext, PortPoolType.UDP);
        if (remotePort < 1 && (ext == null || ext.getProxyConfig().getListenPort() == null)) {
            logger.debug("UDP 代理 {} 自动分配端口: {}", proxy.getName(), listenPort);
        }

        proxyManager.registerUdp(agentId, proxyId, listenPort);
        eventBus.publishSync(new ProxyAddEvent(agentId, proxyId, proxy, listenPort));

        Message.RuntimeInfo.Builder builder = Message.RuntimeInfo.newBuilder();
        builder.setProxyId(proxyId);
        builder.setName(proxy.getName());
        builder.setHealthCheck(proxy.getHealthCheck());
        builder.addAllTargets(proxy.getTargetsList());
        RuntimeInfoSupport.applyTransport(builder, proxy);
        builder.addRemoteAddr(appConfig.getServerAddr() + ":" + listenPort);
        logger.debug("UDP 代理 {} 注册成功，监听端口: {}", proxy.getName(), listenPort);
        return builder.build();
    }

    @Override
    public boolean supports(Message.ProtocolType protocolType) {
        return Objects.requireNonNull(ProtocolType.fromName(protocolType.name())).isUdp();
    }
}
