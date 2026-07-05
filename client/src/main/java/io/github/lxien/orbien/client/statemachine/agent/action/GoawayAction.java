package io.github.lxien.orbien.client.statemachine.agent.action;

import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.client.statemachine.stream.StreamEvent;
import io.github.lxien.orbien.client.statemachine.stream.StreamManager;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;


public class GoawayAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(GoawayAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext agentContext) {
        logger.debug("开始停止客户端，清理资源");
        //关闭所有流
        StreamManager.getStreamContexts().forEach(streamContext -> {
            streamContext.fireEvent(StreamEvent.STREAM_LOCAL_CLOSE);
        });
        //通知服务端清理资源
        Channel control = agentContext.getControl();
        if (event == AgentEvent.LOCAL_GOAWAY && from == AgentState.CONNECTED) {
            control.writeAndFlush(new TMSPFrame(agentContext.getConnectionId(), TMSP.MSG_GOAWAY))
                    .addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            logger.debug("通知服务端断开连接失败（可能连接已断）");
                        }
                    });
        }
        //关闭连接池所有数据连接
        agentContext.getPoolManager().closeAll();
        //关闭控制连接
        ChannelUtils.closeOnFlush(control);
        //停止客户端进程
        agentContext.getTunnelClient().stop();
        logger.debug("orbien 内网穿透客户端已停止");
    }
}