package io.github.lxien.orbien.client.transport;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.domain.TransportConfig;
import io.github.lxien.orbien.client.manager.ProxyManager;
import io.github.lxien.orbien.client.manager.ProxyManagerHolder;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.support.RuntimeInfoSupport;
import io.github.lxien.orbien.core.enums.TransportProtocol;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 客户端数据隧道传输协议解析。
 * <p>
 * 运行时以服务端推送的 {@link Message.RuntimeInfo} 为准（{@link ProxyManager}），
 * 本地 {@link AppConfig#getProxies()} 仅用于启动时向服务端上报，不参与运行时解析。
 */
public final class TransportProtocolResolver {

    private TransportProtocolResolver() {
    }

    public static TransportProtocol globalDefault(AppConfig config) {
        if (config == null) {
            return TransportProtocol.TCP;
        }
        TransportConfig transportConfig = config.getTransportConfig();
        TransportProtocol globalDefault = transportConfig != null ? transportConfig.getProtocol() : null;
        return globalDefault != null ? globalDefault : TransportProtocol.TCP;
    }

    /**
     * 从服务端推送的运行时配置解析数据隧道协议。
     */
    public static TransportProtocol resolveFromRuntimeInfo(TransportProtocol globalDefault,
                                                           Message.RuntimeInfo runtimeInfo) {
        return RuntimeInfoSupport.resolveDataProtocol(globalDefault, runtimeInfo);
    }

    /**
     * 收集连接池预建所需的数据隧道协议集合（控制协议 + 各代理的服务端配置）。
     */
    public static Set<TransportProtocol> collectDataProtocols(AppConfig config) {
        TransportProtocol globalDefault = globalDefault(config);
        Set<TransportProtocol> protocols = new LinkedHashSet<>();
        protocols.add(globalDefault);
        Collection<Message.RuntimeInfo> runtimeInfos = ProxyManagerHolder.get().list();
        for (Message.RuntimeInfo runtimeInfo : runtimeInfos) {
            protocols.add(resolveFromRuntimeInfo(globalDefault, runtimeInfo));
        }
        return protocols;
    }
}
