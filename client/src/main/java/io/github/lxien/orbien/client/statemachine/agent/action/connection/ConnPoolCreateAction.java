package io.github.lxien.orbien.client.statemachine.agent.action.connection;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.domain.ConnectionConfig;
import io.github.lxien.orbien.client.config.domain.PoolConfig;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.client.statemachine.agent.action.AgentBaseAction;
import io.github.lxien.orbien.client.transport.TransportProtocolResolver;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Set;

/**
 * 预创建数据传输连接
 */
public class ConnPoolCreateAction extends AgentBaseAction {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnPoolCreateAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        AppConfig config = context.getConfig();
        ConnectionConfig connectionConfig = config.getConnectionConfig();
        boolean enabled = connectionConfig.getPoolConfig().isEnabled();
        boolean hasTls = context.getTlsContext() != null;
        if (!enabled) {
            return;
        }

        TransportProtocol controlProtocol = TransportProtocolResolver.globalDefault(config);
        Set<TransportProtocol> protocols = TransportProtocolResolver.collectDataProtocols(config);
        logger.debug("[传输] 连接池预创建，控制协议={}，数据协议集合={}",
                controlProtocol.getName(),
                protocols.stream().map(TransportProtocol::getName).toList());

        PoolConfig.MultiplexPoolConfig multiplexPoolConfig = connectionConfig.getPoolConfig().getMultiplex();
        PoolConfig.DirectPoolConfig directPoolConfig = connectionConfig.getPoolConfig().getDirect();

        for (TransportProtocol protocol : protocols) {
            if (multiplexPoolConfig.isPlain() && needsPool(context, protocol, false, true)) {
                ConnCreateHelper.createMultiplexTunnel(context, config, protocol, false);
            }
            if (hasTls && multiplexPoolConfig.isEncrypt() && needsPool(context, protocol, true, true)) {
                ConnCreateHelper.createMultiplexTunnel(context, config, protocol, true);
            }
            for (int i = 0; i < directPoolConfig.getPlainCount(); i++) {
                ConnCreateHelper.createDirectTunnel(context, config, protocol, false);
            }
            if (hasTls) {
                for (int i = 0; i < directPoolConfig.getEncryptCount(); i++) {
                    ConnCreateHelper.createDirectTunnel(context, config, protocol, true);
                }
            }
        }
    }

    private boolean needsPool(AgentContext context, TransportProtocol protocol, boolean encrypt, boolean multiplex) {
        return context.getPoolManager().acquire(protocol, encrypt, multiplex) == null;
    }
}
