package com.xiaoniucode.etp.server.statemachine.agent.action;

import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.server.uid.UidGenerator;
import com.xiaoniucode.etp.core.enums.AgentType;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.message.TMSP;
import com.xiaoniucode.etp.core.message.TMSPFrame;
import com.xiaoniucode.etp.core.notify.EventBus;
import com.xiaoniucode.etp.core.utils.ProtobufUtil;
import com.xiaoniucode.etp.server.event.AgentAuthEvent;
import com.xiaoniucode.etp.server.service.AgentConfigService;
import com.xiaoniucode.etp.server.service.TokenConfigService;
import com.xiaoniucode.etp.server.statemachine.agent.AgentInfo;
import com.xiaoniucode.etp.server.statemachine.agent.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Component
public class AuthAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(AuthAction.class);
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private TokenConfigService tokenConfigService;
    @Autowired
    private UidGenerator uuidGenerator;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private AgentConfigService agentConfigService;
    @Resource
    private AppConfig appConfig;

    /**
     * 检查 Token 是否存在
     * 检查Token 并发限制
     * 检查AgentId是否已经认证在线，避免重复登录 检查是否是断线重连
     * 检查agentId是否存在，不存在则创建 --> 检查该Token下已注册Agent数是否超过限制
     *
     */
    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        Channel control = context.getControl();
        Message.AuthInfo authInfo = context.getAndRemoveAs(AgentConstants.AGENT_AUTH_INFO, Message.AuthInfo.class);
        //是否是重连
        boolean isReconnect = (event == AgentEvent.RETRY_CONNECT);

        if (isReconnect) {
            AgentInfo existAgentInfo = context.getAgentInfo();
            String token = authInfo.getToken();
            if (!Objects.equals(token, existAgentInfo.getToken())) {
                logger.warn("断线重连认证失败，令牌不匹配，当前令牌：{}，历史令牌：{}", token, context.getAgentInfo().getToken());
                Message.AuthResponse authResponse = Message.AuthResponse.newBuilder()
                        .setStatus(Message.Status.newBuilder().setCode(1).setMessage("重连认证失败，令牌不匹配"))
                        .build();
                sendAuthError(control, authResponse);
                context.fireEvent(AgentEvent.AUTH_FAILURE);
                return;
            }
            String agentId = authInfo.getAgentId();
            if (!StringUtils.hasText(agentId) || !Objects.equals(agentId, existAgentInfo.getAgentId())) {
                logger.warn("断线重连认证失败，设备ID不匹配，当前设备ID：{}，历史设备ID：{}", agentId, context.getAgentInfo().getAgentId());
                Message.AuthResponse authResponse = Message.AuthResponse.newBuilder()
                        .setStatus(Message.Status.newBuilder().setCode(1).setMessage("重连认证失败，设备ID不匹配"))
                        .build();
                sendAuthError(control, authResponse);
                context.fireEvent(AgentEvent.AUTH_FAILURE);
                return;
            }
        }
        String token = authInfo.getToken();
        if (!tokenConfigService.existsByToken(token) && !isReconnect) {
            logger.error("客户端认证失败，无效令牌：{}", token);
            Message.AuthResponse authResponse = Message.AuthResponse.newBuilder()
                    .setStatus(Message.Status.newBuilder().setCode(100).setMessage("认证失败，无效令牌:" + token))
                    .build();
            sendAuthError(control, authResponse);
            context.fireEvent(AgentEvent.AUTH_FAILURE);
            return;
        }

        //如果没有 agentId 则生成
        String agentId = authInfo.getAgentId();
        AgentInfo oldAgentInfo = null;
        if (!StringUtils.hasText(agentId)) {
            agentId = uuidGenerator.getUIDAsString();
        } else {
            //如果携带了 agentId 需要检查是否存在
            Optional<AgentInfo> agentInfoOpt = agentConfigService.findById(agentId);
            if (agentInfoOpt.isEmpty()) {
                logger.warn("设备ID {} 不存在", agentId);
                Message.AuthResponse authResponse = Message.AuthResponse.newBuilder()
                        .setStatus(Message.Status.newBuilder().setCode(100).setMessage("AgentId " + agentId + " 不存在"))
                        .build();
                sendAuthError(control, authResponse);
                context.fireEvent(AgentEvent.AUTH_FAILURE);
                return;
            } else {
                oldAgentInfo = agentInfoOpt.get();
            }
        }
        AgentInfo agentInfo = createOrUpdateAgentInfo(agentId, oldAgentInfo, authInfo);
        context.setControl(control);
        context.setAgentInfo(agentInfo);

        agentManager.addAgentContextIndex(agentId, context);


        Message.AuthResponse authResponse = Message.AuthResponse.newBuilder()
                .setStatus(Message.Status.newBuilder().setCode(0).setMessage("认证成功"))
                .setConnectionId(context.getConnectionId())
                .setAgentId(agentId)
                .build();
        TMSPFrame authFrame = new TMSPFrame(0, TMSP.MSG_AUTH_RESP);
        ByteBuf payload = ProtobufUtil.toByteBuf(authResponse, control.alloc());
        authFrame.setPayload(payload);

        control.writeAndFlush(authFrame).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                logger.error("发送认证成功消息失败", future.cause());
            }
        });
        //发布客户端认证异步事件
        eventBus.publishAsync(new AgentAuthEvent(agentInfo, isReconnect));
        //发布状态机 认证成功事件
        context.fireEvent(AgentEvent.AUTH_SUCCESS);
        logger.debug("设备认证成功：[设备ID={}，设备类型={}，版本号={}]", agentId, agentInfo.getAgentType(), agentInfo.getVersion());
    }

    private AgentInfo createOrUpdateAgentInfo(String agentId, AgentInfo oldAgentInfo, Message.AuthInfo authInfo) {
        AgentInfo agentInfo = oldAgentInfo == null ? new AgentInfo() : oldAgentInfo;
        agentInfo.setName(authInfo.getName());
        agentInfo.setToken(authInfo.getToken());
        agentInfo.setAgentId(agentId);
        agentInfo.setAgentType(getAgentType(authInfo));
        agentInfo.setVersion(authInfo.getVersion());
        agentInfo.setOs(authInfo.getOs());
        agentInfo.setArch(authInfo.getArch());
        agentInfo.setToken(authInfo.getToken());

        if (oldAgentInfo == null) {
            agentInfo.setCreatedAt(LocalDateTime.now());
        }
        agentInfo.setLastActiveTime(LocalDateTime.now());
        return agentInfo;
    }

    private AgentType getAgentType(Message.AuthInfo authInfo) {
        switch (authInfo.getAgentType()) {
            case BINARY -> {
                return AgentType.STANDALONE;
            }
            case SESSION -> {
                return AgentType.EMBEDDED;
            }
        }
        return null;
    }

    public void sendAuthError(Channel control, Message.AuthResponse authResponse) {
        ByteBuf payload = ProtobufUtil.toByteBuf(authResponse, control.alloc());
        TMSPFrame authFrame = new TMSPFrame(0, TMSP.MSG_AUTH_RESP);
        authFrame.setPayload(payload);
        control.writeAndFlush(authFrame);
    }
}
