package com.xiaoniucode.etp.client.statemachine.agent.action;

import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.config.ConfigUtils;
import com.xiaoniucode.etp.client.statemachine.agent.AgentContext;
import com.xiaoniucode.etp.client.statemachine.agent.AgentEvent;
import com.xiaoniucode.etp.client.statemachine.agent.AgentState;
import com.xiaoniucode.etp.client.utils.ProxyConfigAssembler;
import com.xiaoniucode.etp.core.domain.*;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.message.TMSP;
import com.xiaoniucode.etp.core.message.TMSPFrame;
import com.xiaoniucode.etp.core.utils.ProtobufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

public class AuthSuccessAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AuthSuccessAction.class);

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        handleLocalProxyPush(context);
        context.fireEvent(AgentEvent.CREATE_CONN_POOL);
    }

    private void handleLocalProxyPush(AgentContext context) {
        logger.debug("推送本地配置到服务端");
        AppConfig config = ConfigUtils.getConfig();
        List<ProxyConfig> proxies = config.getProxies();
        Channel control = context.getControl();

        Message.BatchCreateProxiesRequest.Builder builder = Message.BatchCreateProxiesRequest.newBuilder();
        List<Message.Proxy> messages = proxies.stream().map(ProxyConfigAssembler::toProto).toList();
        builder.addAllProxies(messages);

        ByteBuf buf = ProtobufUtil.toByteBuf(builder.build(), control.alloc());
        TMSPFrame tmspFrame = new TMSPFrame(0, TMSP.MSG_PROXY_REPORT_REQ, buf);
        control.writeAndFlush(tmspFrame).addListener(future -> {
            if (future.isSuccess()) {
                logger.debug("成功推送 {} 条代理配置到服务端", messages.size());
            }
        });
    }
}
