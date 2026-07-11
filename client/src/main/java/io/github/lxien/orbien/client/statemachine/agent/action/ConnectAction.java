package io.github.lxien.orbien.client.statemachine.agent.action;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.client.transport.TransportClientBootstrap;
import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.client.transport.ControlIdleCheckHandler;
import io.github.lxien.orbien.client.transport.HeartbeatHandler;
import io.github.lxien.orbien.client.health.HealthCheckHolder;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 与服务端建立控制连接
 */
public class ConnectAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext ctx) {
        AppConfig appConfig = ctx.getConfig();
        TransportClientBootstrap.connectControl(
                appConfig,
                ctx.getControlWorkerGroup(),
                ctx.getTlsContext(),
                pipeline -> pipeline
                        .addLast(NettyConstants.CONTROL_IDLE_CHECK_HANDLER,
                                new ControlIdleCheckHandler(ctx, 90, 0, 0, TimeUnit.SECONDS))
                        .addLast(new HeartbeatHandler(30))
                        .addLast(NettyConstants.CONTROL_FRAME_HANDLER, ctx.getControlFrameHandler())
        ).whenComplete((session, error) -> {
            if (ctx.isShuttingDown()) {
                if (session != null && session.nettyChannel() != null) {
                    ChannelUtils.closeOnFlush(session.nettyChannel());
                }
                return;
            }
            if (error != null) {
                logger.error("无法连接到代理服务器：{}:{}", appConfig.getServerAddr(), appConfig.getServerPort(), error);
                ctx.fireEvent(AgentEvent.CONNECT_FAILURE);
                return;
            }
            logger.debug("[传输] 控制连接已建立 protocol={} endpoint={}:{}",
                    appConfig.getTransportConfig().getProtocol().getName(),
                    appConfig.getServerAddr(), appConfig.getServerPort());
            Channel control = session.nettyChannel();
            ctx.setControl(control);
            HealthCheckHolder.init(control);
            ctx.fireEvent(AgentEvent.CONNECT_SUCCESS);
        });
    }
}
