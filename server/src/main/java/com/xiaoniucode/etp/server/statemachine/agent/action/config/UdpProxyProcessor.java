package com.xiaoniucode.etp.server.statemachine.agent.action.config;

import com.xiaoniucode.etp.core.domain.ProxyConfig;
import com.xiaoniucode.etp.core.domain.ProxyConfigExt;
import com.xiaoniucode.etp.core.enums.ProtocolType;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.notify.EventBus;
import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.server.event.ProxyAddEvent;
import com.xiaoniucode.etp.server.event.ProxyDeleteEvent;
import com.xiaoniucode.etp.server.service.ProxyConfigService;
import com.xiaoniucode.etp.server.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.server.uid.UidGenerator;
import com.xiaoniucode.etp.server.exceptions.EtpException;
import com.xiaoniucode.etp.server.exceptions.PortConflictException;
import com.xiaoniucode.etp.server.manager.ProxyManager;
import com.xiaoniucode.etp.core.enums.PortPoolType;
import com.xiaoniucode.etp.server.port.PortPoolManager;
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
    private PortPoolManager portPoolManager;
    @Autowired
    private ProxyManager proxyManager;
    @Autowired
    private UidGenerator uidGenerator;
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private EventBus eventBus;
    @Resource
    private AppConfig appConfig;

    @Override
    public Message.RuntimeInfo process(AgentContext context, Message.Proxy proxy) throws Exception {
        String agentId = context.getAgentId();
        int remotePort = proxy.getRemotePort();
        String name = proxy.getName();
        ProxyConfigExt ext = proxyConfigService.findByAgentAndName(agentId, name);
        if (ext != null) {
            ProxyConfig config = ext.getProxyConfig();
            String proxyId = config.getProxyId();
            proxyManager.deactivate(proxyId);
            eventBus.publishSync(new ProxyDeleteEvent(agentId, proxyId));
        }
        Integer listenPort;
        if (remotePort < 1) {
            listenPort = portPoolManager.acquire(PortPoolType.UDP);
            if (listenPort == null) {
                throw new EtpException("没有可用的 UDP 端口");
            }
            logger.debug("UDP 代理 {} 自动分配端口: {}", proxy.getName(), listenPort);
        } else {
            if (!portPoolManager.isAvailable(PortPoolType.UDP, remotePort)) {
                throw new PortConflictException(remotePort);
            }
            portPoolManager.reserve(PortPoolType.UDP, remotePort);
            listenPort = remotePort;
        }
        String proxyId = uidGenerator.getUIDAsString();

        proxyManager.registerUdp(agentId, proxyId, listenPort);
        eventBus.publishAsync(new ProxyAddEvent(agentId, proxyId, proxy, listenPort));

        Message.RuntimeInfo.Builder builder = Message.RuntimeInfo.newBuilder();
        builder.setProxyId(proxyId);
        builder.setName(proxy.getName());
        builder.setHealthCheck(proxy.getHealthCheck());
        builder.addAllTargets(proxy.getTargetsList());
        builder.addRemoteAddr(appConfig.getServerAddr() + ":" + listenPort);
        logger.debug("UDP 代理 {} 注册成功，监听端口: {}", proxy.getName(), listenPort);
        return builder.build();
    }

    @Override
    public boolean supports(Message.ProtocolType protocolType) {
        return Objects.requireNonNull(ProtocolType.fromName(protocolType.name())).isUdp();
    }
}
