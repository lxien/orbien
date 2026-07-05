package io.github.lxien.orbien.client.statemachine.agent.action;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.statemachine.agent.AgentContext;
import io.github.lxien.orbien.client.statemachine.agent.AgentEvent;
import io.github.lxien.orbien.client.statemachine.agent.AgentState;
import io.github.lxien.orbien.client.utils.ProxyConfigAssembler;
import io.github.lxien.orbien.core.domain.*;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.TMSP;
import io.github.lxien.orbien.core.message.TMSPFrame;
import io.github.lxien.orbien.core.utils.ProtobufUtil;
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
        AppConfig config = context.getConfig();
        List<ProxyConfig> proxies = config.getProxies();
        Channel control = context.getControl();

        Message.BatchCreateProxiesRequest.Builder builder = Message.BatchCreateProxiesRequest.newBuilder();
        List<Message.Proxy> messages = proxies.stream().map(ProxyConfigAssembler::toProto).toList();
        builder.addAllProxies(messages);

        logger.info("推送本地代理配置到服务端，共 {} 条: {}",
                messages.size(),
                messages.stream().map(Message.Proxy::getName).toList());

        ByteBuf buf = ProtobufUtil.toByteBuf(builder.build(), control.alloc());
        TMSPFrame tmspFrame = new TMSPFrame(0, TMSP.MSG_PROXY_REPORT_REQ, buf);
        control.writeAndFlush(tmspFrame).addListener(future -> {
            if (future.isSuccess()) {
                logger.debug("成功推送 {} 条代理配置到服务端", messages.size());
            }
        });
    }
}
