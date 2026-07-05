package io.github.lxien.orbien.core.transport.api;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public interface TransportSession {

    String sessionId();

    TransportProtocol protocol();

    boolean isActive();

    boolean isWritable();

    Channel nettyChannel();

    void write(Object msg);

    void close();

    void setAutoRead(boolean autoRead);

    <T> Attribute<T> attr(AttributeKey<T> key);
}
