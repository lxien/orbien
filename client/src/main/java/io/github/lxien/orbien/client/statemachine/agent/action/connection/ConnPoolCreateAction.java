package io.github.lxien.orbien.client.statemachine.agent.action.connection;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.domain.ConnectionConfig;
import io.github.lxien.orbien.client.config.domain.PoolConfig;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.client.statemachine.agent.action.AgentBaseAction;

/**
 * 预创建数据传输连接
 */
public class ConnPoolCreateAction extends AgentBaseAction {

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        AppConfig config = context.getConfig();
        ConnectionConfig connectionConfig = config.getConnectionConfig();
        boolean enabled = connectionConfig.getPoolConfig().isEnabled();
        boolean hasTls = context.getTlsContext() != null;
        if (!enabled) {
            return;
        }
        // 创建多路复用隧道
       PoolConfig.MultiplexPoolConfig multiplexPoolConfig = connectionConfig.getPoolConfig().getMultiplex();
        if (multiplexPoolConfig.isPlain()) {
            ConnCreateHelper.createMultiplexTunnel(context, config, false);
        }
        if (hasTls && multiplexPoolConfig.isEncrypt()) {
            ConnCreateHelper.createMultiplexTunnel(context, config, true);
        }

        // 创建独立隧道
       PoolConfig.DirectPoolConfig directPoolConfig = connectionConfig.getPoolConfig().getDirect();
        int plainCount = directPoolConfig.getPlainCount();
        int encryptCount = directPoolConfig.getEncryptCount();
        
        for (int i = 0; i < plainCount; i++) {
            ConnCreateHelper.createDirectTunnel(context, config, false);
        }
        if (hasTls) {
            for (int i = 0; i < encryptCount; i++) {
                ConnCreateHelper.createDirectTunnel(context, config, true);
            }
        }
    }
}
