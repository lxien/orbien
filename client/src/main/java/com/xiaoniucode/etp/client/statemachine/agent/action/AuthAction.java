package com.xiaoniucode.etp.client.statemachine.agent.action;

import com.xiaoniucode.etp.client.common.AgentConstants;
import com.xiaoniucode.etp.client.common.OSUtils;
import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.config.domain.AuthConfig;
import com.xiaoniucode.etp.client.identity.AgentIdentity;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.AgentState;
import com.xiaoniucode.etp.common.utils.StringUtils;
import com.xiaoniucode.etp.core.enums.AgentType;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.message.TMSP;
import com.xiaoniucode.etp.core.message.TMSPFrame;
import com.xiaoniucode.etp.core.utils.ProtobufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class AuthAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AuthAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext ctx) {
        try {
            logger.debug("开始认证");
            AppConfig config = ctx.getConfig();
            AuthConfig authConfig = config.getAuthConfig();
            Channel control = ctx.getControl();
            Message.AgentType agentType = toProto(config.getAgentType());

            Message.AuthInfo.Builder builder = Message.AuthInfo.newBuilder()
                    .setToken(authConfig.getToken())
                    .setVersion(AgentConstants.AGENT_VERSION)
                    .setAgentType(agentType)
                    .setOs(OSUtils.getOS())
                    .setArch(OSUtils.getOSArch())
                    .setName(OSUtils.getHostName());

            AgentIdentity agentIdentity = ctx.getAgentIdentity();
            String agentId = agentIdentity.getIdentity();
            if (StringUtils.hasText(agentId)) {
                builder.setAgentId(agentId);
            }

            ByteBuf buf = ProtobufUtil.toByteBuf(builder.build(), control.alloc());
            TMSPFrame frame = new TMSPFrame(0, TMSP.MSG_AUTH, buf);
            control.writeAndFlush(frame).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    logger.debug("认证消息发送成功");
                } else {
                    logger.error("认证失败", f.cause());
                    ctx.fireEvent(AgentEvent.LOCAL_GOAWAY);
                }
            });
        } catch (Exception e) {
            logger.error("认证失败", e);
            ctx.fireEvent(AgentEvent.LOCAL_GOAWAY);
        }
    }

    private Message.AgentType toProto(AgentType agentType) {
        if (agentType == null) {
            return Message.AgentType.UNRECOGNIZED;
        }
        switch (agentType) {
            case STANDALONE:
                return Message.AgentType.BINARY;
            case EMBEDDED:
                return Message.AgentType.SESSION;
            default:
                return Message.AgentType.UNRECOGNIZED;
        }
    }

}
