package io.github.lxien.orbien.core.message.support;

import io.github.lxien.orbien.core.domain.DomainInfo;
import io.github.lxien.orbien.core.domain.FileShareLimitsConfig;
import io.github.lxien.orbien.core.domain.HealthCheckConfig;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.Target;
import io.github.lxien.orbien.core.domain.TransportCustomConfig;
import io.github.lxien.orbien.core.enums.HealthCheckType;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.enums.TransportProtocol;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.transport.api.TransportEndpointResolver;
import io.github.lxien.orbien.core.transport.compress.CompressionType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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
        if (transport.getCompressAlgorithm() != null && transport.getCompressAlgorithm().isCompressed()) {
            builder.setCompressAlgorithm(transport.getCompressAlgorithm().toConfigValue());
        }
        if (transport.getProtocol() != null) {
            builder.setProtocol(transport.getProtocol().getName());
        }
        return builder.build();
    }

    public static TransportCustomConfig fromTransportProto(Message.Transport transport) {
        if (transport == null) {
            return null;
        }
        TransportCustomConfig config = new TransportCustomConfig();
        if (transport.hasMultiplex()) {
            config.setMultiplex(transport.getMultiplex());
        }
        if (transport.hasEncrypt()) {
            config.setEncrypt(transport.getEncrypt());
        }
        if (transport.hasCompress()) {
            config.setCompress(transport.getCompress());
        }
        if (transport.hasCompressAlgorithm()) {
            config.setCompressAlgorithm(CompressionType.of(transport.getCompressAlgorithm()));
        }
        if (transport.hasProtocol()) {
            TransportProtocol protocol = TransportProtocol.fromName(transport.getProtocol());
            config.setProtocol(protocol);
        }
        return config;
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

    public static Message.HealthCheck toHealthCheckProto(HealthCheckConfig config) {
        if (config == null) {
            return null;
        }
        Message.HealthCheck.Builder builder = Message.HealthCheck.newBuilder()
                .setEnabled(config.isEnabled());
        if (config.getType() != null) {
            builder.setType(toProtoHealthCheckType(config.getType()));
        }
        if (config.getInterval() != null) {
            builder.setInterval(config.getInterval());
        }
        if (config.getTimeout() != null) {
            builder.setTimeout(config.getTimeout());
        }
        if (config.getMaxFailed() != null) {
            builder.setMaxFailed(config.getMaxFailed());
        }
        if (config.getPath() != null) {
            builder.setPath(config.getPath());
        }
        return builder.build();
    }

    public static Message.HealthCheckType toProtoHealthCheckType(HealthCheckType type) {
        return switch (type) {
            case TCP -> Message.HealthCheckType.HEALTH_CHECK_TYPE_TCP;
            case HTTP -> Message.HealthCheckType.HEALTH_CHECK_TYPE_HTTP;
        };
    }

    public static Message.RuntimeInfo buildRuntimeInfo(ProxyConfig config, Collection<String> remoteAddrs) {
        List<Message.Target> targetList = config.getTargets().stream()
                .map(RuntimeInfoSupport::toTargetProto)
                .toList();

        Message.RuntimeInfo.Builder builder = Message.RuntimeInfo.newBuilder()
                .setProxyId(config.getProxyId())
                .setName(config.getName())
                .addAllTargets(targetList);

        if (remoteAddrs != null && !remoteAddrs.isEmpty()) {
            builder.addAllRemoteAddr(remoteAddrs);
        }

        if (!config.isUdp() && !config.isSocks5() && !config.isFile()) {
            Message.HealthCheck healthCheck = toHealthCheckProto(config.getHealthCheck());
            if (healthCheck != null) {
                builder.setHealthCheck(healthCheck);
            }
        }

        applyTransport(builder, config);
        applyFileShareLimits(builder, config);
        return builder.build();
    }

    public static void applyFileShareLimits(Message.RuntimeInfo.Builder builder, ProxyConfig config) {
        if (config == null || !config.isFile() || !config.hasFileShareLimits()) {
            return;
        }
        FileShareLimitsConfig limits = config.getFileShareLimits();
        Message.FileShareLimits.Builder lb = Message.FileShareLimits.newBuilder();
        if (limits.getRootPath() != null) {
            lb.setRootPath(limits.getRootPath());
        }
        if (limits.getMaxUploadSize() != null) {
            lb.setMaxUploadSize(limits.getMaxUploadSize());
        }
        lb.setAllowUpload(limits.isAllowUpload());
        lb.setAllowDelete(limits.isAllowDelete());
        lb.setAllowMkdir(limits.isAllowMkdir());
        lb.setAllowMove(limits.isAllowMove());
        lb.setAllowRename(limits.isAllowRename());
        builder.setFileLimits(lb.build());
    }

    public static Message.FileShareLimits toFileShareLimitsProto(FileShareLimitsConfig config) {
        if (config == null) {
            return null;
        }
        Message.FileShareLimits.Builder builder = Message.FileShareLimits.newBuilder();
        if (config.getRootPath() != null) {
            builder.setRootPath(config.getRootPath());
        }
        if (config.getMaxUploadSize() != null) {
            builder.setMaxUploadSize(config.getMaxUploadSize());
        }
        builder.setAllowUpload(config.isAllowUpload());
        builder.setAllowDelete(config.isAllowDelete());
        builder.setAllowMkdir(config.isAllowMkdir());
        builder.setAllowMove(config.isAllowMove());
        builder.setAllowRename(config.isAllowRename());
        return builder.build();
    }

    public static List<String> buildRemoteAddrs(Set<DomainInfo> domains,
                                                ProtocolType protocolType,
                                                int httpProxyPort,
                                                int httpsProxyPort) {
        if (domains == null || domains.isEmpty()) {
            return List.of();
        }
        return domains.stream()
                .map(domain -> buildRemoteAddr(domain.getFullDomain(), protocolType, httpProxyPort, httpsProxyPort))
                .toList();
    }

    public static String buildRemoteAddr(String domain,
                                         ProtocolType protocolType,
                                         int httpProxyPort,
                                         int httpsProxyPort) {
        String prefix;
        String port;
        switch (protocolType) {
            case HTTP -> {
                prefix = "http://";
                port = httpProxyPort == 80 ? "" : ":" + httpProxyPort;
            }
            case HTTPS -> {
                prefix = "https://";
                port = httpsProxyPort == 443 ? "" : ":" + httpsProxyPort;
            }
            case FILE -> {
                prefix = "https://";
                port = httpsProxyPort == 443 ? "" : ":" + httpsProxyPort;
            }
            default -> {
                prefix = "";
                port = "";
            }
        }
        return prefix + domain + port;
    }

    private static Message.Target toTargetProto(Target target) {
        Message.Target.Builder builder = Message.Target.newBuilder()
                .setHost(target.getHost())
                .setPort(target.getPort());
        if (target.getName() != null) {
            builder.setName(target.getName());
        }
        if (target.getWeight() != null) {
            builder.setWeight(target.getWeight());
        }
        return builder.build();
    }
}
