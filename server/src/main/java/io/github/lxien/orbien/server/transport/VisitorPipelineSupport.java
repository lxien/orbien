package io.github.lxien.orbien.server.transport;

import io.github.lxien.orbien.core.transport.NettyConstants;
import io.github.lxien.orbien.server.config.domain.ProxyProtocolConfig;
import io.github.lxien.orbien.server.transport.haproxy.ProxyProtocolDetectHandler;
import io.netty.channel.ChannelPipeline;

/**
 * 访客入口 pipeline 公共装配：HA PROXY 解码器必须在 TLS/业务 handler 之前。
 * @author lxien
 */
public final class VisitorPipelineSupport {

    private VisitorPipelineSupport() {
    }

    public static void prependProxyProtocol(ChannelPipeline pipeline, ProxyProtocolConfig config) {
        if (config == null || !config.isEnabled()) {
            return;
        }
        pipeline.addFirst(NettyConstants.HAPROXY_DETECT_HANDLER, new ProxyProtocolDetectHandler(config));
    }
}
