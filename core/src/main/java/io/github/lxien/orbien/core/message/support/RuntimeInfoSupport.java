package io.github.lxien.orbien.core.message.support;

import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.TransportCustomConfig;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.transport.api.TransportEndpointResolver;

/**
 * 服务端向客户端推送的运行时配置（{@link Message.RuntimeInfo}）构建与解析辅助类。
 */
public final class RuntimeInfoSupport {

    private RuntimeInfoSupport() {
    }

    public static Message.Transport toTransportProto(TransportCustomConfig transport) {
        if (transport == null) {
            return null;
        }
        Message.Transport.Builder builder = Message.Transport.newBuilder();
        if (transport.getMultiplex() != null) {
            builder.setMultiplex(transport.getMultiplex());
        }
        if (transport.getEncrypt() != null) {
            builder.setEncrypt(transport.getEncrypt());
        }
        if (transport.getCompress() != null) {
            builder.setCompress(transport.getCompress());
        }
        if (transport.getProtocol() != null) {
            builder.setProtocol(transport.getProtocol().getName());
        }
        return builder.build();
    }

    public static Message.Transport toTransportProto(Message.Proxy proxy) {
        if (proxy == null || !proxy.hasTransport()) {
            return null;
        }
        return proxy.getTransport();
    }

    public static void applyTransport(Message.RuntimeInfo.Builder builder, ProxyConfig config) {
        if (config != null && config.hasTransport()) {
            Message.Transport transport = toTransportProto(config.getTransport());
            if (transport != null) {
                builder.setTransport(transport);
            }
        }
    }

    public static void applyTransport(Message.RuntimeInfo.Builder builder, Message.Proxy proxy) {
        Message.Transport transport = toTransportProto(proxy);
        if (transport != null) {
            builder.setTransport(transport);
        }
    }

    public static TransportProtocol resolveDataProtocol(TransportProtocol globalDefault,
                                                          Message.Transport transport) {
        if (transport != null && transport.hasProtocol()) {
            TransportProtocol resolved = TransportProtocol.fromName(transport.getProtocol());
            if (resolved != null) {
                return resolved;
            }
        }
        return TransportEndpointResolver.resolveDataProtocol(globalDefault, null);
    }

    public static TransportProtocol resolveDataProtocol(TransportProtocol globalDefault,
                                                          Message.RuntimeInfo runtimeInfo) {
        if (runtimeInfo != null && runtimeInfo.hasTransport()) {
            return resolveDataProtocol(globalDefault, runtimeInfo.getTransport());
        }
        return globalDefault != null ? globalDefault : TransportProtocol.TCP;
    }
}
