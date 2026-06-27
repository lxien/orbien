package com.xiaoniucode.etp.server.statemachine.agent.action;

import com.xiaoniucode.etp.core.domain.*;
import com.xiaoniucode.etp.core.message.Message;
import com.xiaoniucode.etp.core.message.TMSP;
import com.xiaoniucode.etp.core.message.TMSPFrame;
import com.xiaoniucode.etp.core.notify.EventBus;
import com.xiaoniucode.etp.core.utils.ProtobufUtil;
import com.xiaoniucode.etp.server.config.AppConfig;
import com.xiaoniucode.etp.server.event.ProxyReportEvent;
import com.xiaoniucode.etp.server.service.ProxyConfigService;
import com.xiaoniucode.etp.server.service.ConfigChangeDetector;
import com.xiaoniucode.etp.server.statemachine.agent.*;
import com.xiaoniucode.etp.server.statemachine.agent.action.config.*;
import com.xiaoniucode.etp.server.vhost.DomainInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 处理客户端首次登陆上报的本地代理配置信息
 */
@Component
public class ProxyReportAction extends AgentBaseAction {
    private final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyReportAction.class);
    @Resource
    private AppConfig appConfig;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ConfigChangeDetector configChangeDetector;
    @Autowired
    private ProxyConfigService proxyConfigService;
    @Autowired
    private ProxyOperationStrategyFactory strategyFactory;
    @Autowired
    private ProxyConfigResponseBuilder responseBuilder;
    @Autowired
    private EventBus eventBus;

    @Override
    protected void doExecute(AgentState from, AgentState to, AgentEvent event, AgentContext context) {
        Channel control = context.getControl();
        try {
            Message.BatchCreateProxiesRequest proxy = context.getAndRemoveAs(
                    AgentConstants.BATCH_CREATE_PROXIES_REQUEST,
                    Message.BatchCreateProxiesRequest.class);

            ProxyConfig newConfig = buildProxyConfig(proxy, context);

            Optional<ProxyConfig> existsConfigOpt = proxyConfigService
                    .findByAgentAndName(newConfig.getAgentId(), newConfig.getName());

            boolean isUpdate = existsConfigOpt.isPresent();

            if (isUpdate) {
                handleProxyUpdate(newConfig, existsConfigOpt.get(), context);
            } else {
                handleProxyCreate(newConfig, context);
            }
            boolean hasChange = operationResult.isHasChange();

            notifyProxyReport(isUpdate, newConfig, operationResult.getDomains(), hasChange);
            //将注册成功后的记录返回给客户端 重点 proxyId
            context.fireEvent(AgentEvent.AGENT_INIT);
            logger.info("代理{}成功: {}", isUpdate ? "更新" : "创建", newConfig.getName());
        } catch (Exception e) {
            logger.error("代理配置处理失败", e);
            sendErrorResponse(e.getMessage(), control);
        }
    }

    private void notifyProxyReport(boolean isUpdate, ProxyConfig config, Set<DomainInfo> domains, boolean hasChange) {
        String baseDomain = appConfig.getBaseDomain();
        if (!isUpdate) {
            if (config.isTcp()) {
                eventBus.publishAsync(new ProxyReportEvent(false, config, hasChange));
            } else {
                eventBus.publishAsync(new ProxyReportEvent(false, baseDomain, domains, config, hasChange));
            }
        } else {
            if (config.isTcp()) {
                eventBus.publishAsync(new ProxyReportEvent(true, config, hasChange));
            } else {
                eventBus.publishAsync(new ProxyReportEvent(true, baseDomain, domains, config, hasChange));
            }
        }
    }

    /**
     * 处理代理创建
     */
    private ProxyOperationResult handleProxyCreate(ProxyConfig newConfig, AgentContext context) throws Exception {
        logger.debug("准备创建新代理: {}", newConfig.getName());
        ProxyConfigOperationStrategy strategy = strategyFactory.getStrategy(newConfig);
        return strategy.create(newConfig, context.getAgentInfo());
    }

    /**
     * 处理代理更新
     */
    private ProxyOperationResult handleProxyUpdate(ProxyConfig newConfig, ProxyConfig oldConfig, AgentContext context) throws Exception {
        logger.debug("准备更新代理: {}", newConfig.getName());
        newConfig.setProxyId(oldConfig.getProxyId());
        if (!configChangeDetector.hasChanges(oldConfig, newConfig)) {
            logger.debug("代理配置 {} 没有发生变更，无需更新", newConfig.getName());
            Set<DomainInfo> domains = proxyConfigService.findDomainsByProxyId(oldConfig.getProxyId());

            return new ProxyOperationResult(domains, oldConfig.getListenPort(), false);
        }
        ProxyConfigOperationStrategy strategy = strategyFactory.getStrategy(newConfig);
        return strategy.update(newConfig, oldConfig, context.getAgentInfo());
    }

    /**
     * 构建代理配置
     */
    private ProxyConfig buildProxyConfig(Message.NewProxy proxy, AgentContext context) {
        AgentInfo agentInfo = context.getAgentInfo();
        ProxyConfig config = ProxyConfigBuilderUtil.buildProxyConfig(proxy, passwordEncoder);
        config.setAgentId(agentInfo.getAgentId());
        config.setAgentType(agentInfo.getAgentType());
        return config;
    }

    /**
     * 发送成功响应
     */
    private void sendSuccessResponse(ProxyConfig config, Set<DomainInfo> domains, Channel control) {
        Message.NewProxyResp response = responseBuilder.buildNewProxyResponse(config, domains);
        ByteBuf payload = ProtobufUtil.toByteBuf(response, control.alloc());
        TMSPFrame frame = new TMSPFrame(0, TMSP.MSG_PROXY_CREATE_RESP, payload);

        control.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.debug("代理响应发送成功: {}", config.getName());
            } else {
                logger.error("代理响应发送失败: {}", config.getName(), future.cause());
            }
        });
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(String errorMessage, Channel control) {
        if (!StringUtils.hasText(errorMessage)) {
            errorMessage = "error";
        }
        Message.Error errorMsg = responseBuilder.buildErrorResponse(errorMessage);
        ByteBuf payload = ProtobufUtil.toByteBuf(errorMsg, control.alloc());
        TMSPFrame frame = new TMSPFrame(TMSP.MSG_ERROR, payload);
        control.writeAndFlush(frame);
        logger.error("发送错误响应: {}", errorMessage);
    }
}
