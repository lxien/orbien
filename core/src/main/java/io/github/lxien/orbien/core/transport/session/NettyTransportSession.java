package io.github.lxien.orbien.core.transport.session;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.transport.api.TransportSession;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public final class NettyTransportSession implements TransportSession {

    private final Channel channel;
    private final TransportProtocol protocol;
    private final String sessionId;

    public NettyTransportSession(Channel channel, TransportProtocol protocol) {
        this(channel, protocol, channel.id().asShortText());
    }

    public NettyTransportSession(Channel channel, TransportProtocol protocol, String sessionId) {
        this.channel = channel;
        this.protocol = protocol;
        this.sessionId = sessionId;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public TransportProtocol protocol() {
        return protocol;
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public boolean isWritable() {
        return channel.isWritable();
    }

    @Override
    public Channel nettyChannel() {
        return channel;
    }

    @Override
    public void write(Object msg) {
        channel.writeAndFlush(msg);
    }

    @Override
    public void close() {
        channel.close();
    }

    @Override
    public void setAutoRead(boolean autoRead) {
        channel.config().setAutoRead(autoRead);
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return channel.attr(key);
    }
}
