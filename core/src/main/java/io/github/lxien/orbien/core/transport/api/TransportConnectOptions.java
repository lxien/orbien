package io.github.lxien.orbien.core.transport.api;

import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig;
import io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import lombok.Builder;
import lombok.Getter;

import java.util.function.Consumer;

@Getter
@Builder
public class TransportConnectOptions {
    private final TransportEndpoint endpoint;
    private final PipelineRole role;
    private final EventLoopGroup eventLoopGroup;
    private final SslContext sslContext;
    private final WebSocketProtocolConfig webSocketConfig;
    private final QuicProtocolConfig quicConfig;
    private final TlsConfig tlsConfig;
    private final Consumer<ChannelPipeline> pipelineTailConfigurer;
}
