package io.github.lxien.orbien.client.statemachine.agent.action;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.domain.TransportConfig;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.transport.tls.TlsHelper;
import io.github.lxien.orbien.core.transport.TlsContextHolder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class TlslnitAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(TlslnitAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        logger.debug("检查是否有必要初始化 TLS 证书");
        try {
            AppConfig config = context.getConfig();
            TransportConfig transportConfig = config.getTransportConfig();
            TlsConfig tlsConfig = transportConfig.getTlsConfig();
            if (tlsConfig == null || tlsConfig.isEnabled()) {
                logger.debug("初始化 TLS 上下文");
                SslContext sslContext = TlsHelper.buildSslContext(true, tlsConfig, tlsConfig == null);
                TlsContextHolder.initialize(sslContext);
                context.setTlsContext(sslContext);
            }
            context.getStateMachine().fireEvent(context.getState(), AgentEvent.SSL_INITIALIZED, context);
        } catch (Exception e) {
            logger.error("TLS 初始化失败", e);
            context.fireEvent(AgentEvent.LOCAL_GOAWAY);
        }
    }
}
