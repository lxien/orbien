package io.github.lxien.orbien.core.transport.api;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import lombok.Builder;
import lombok.Getter;

import java.util.function.BiConsumer;

@Getter
@Builder
public class TransportBindOptions {
    private final TransportProtocol protocol;
    private final String addr;
    private final int port;
    private final SslContext sslContext;
    private final WebSocketProtocolConfig webSocketConfig;
    private final QuicProtocolConfig quicConfig;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final BiConsumer<Channel, ChannelPipeline> pipelineConfigurer;
}
