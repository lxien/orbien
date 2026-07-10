package io.github.lxien.orbien.core.filetransfer;

import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.domain.TransportCustomConfig;
import io.github.lxien.orbien.core.message.Message;
import io.github.lxien.orbien.core.message.support.RuntimeInfoSupport;
import io.github.lxien.orbien.core.transport.compress.CompressionType;

/**
 * 文件共享控制通道压缩配置解析
 */
public final class FileTransferCompressionSupport {

    private FileTransferCompressionSupport() {
    }

    public static CompressionType resolveFromProxy(ProxyConfig config) {
        if (config == null || !config.hasTransport()) {
            return CompressionType.NONE;
        }
        return resolveFromTransport(config.getTransport());
    }

    public static CompressionType resolveFromRuntime(Message.RuntimeInfo runtimeInfo) {
        if (runtimeInfo == null || !runtimeInfo.hasTransport()) {
            return CompressionType.NONE;
        }
        return resolveFromTransport(RuntimeInfoSupport.fromTransportProto(runtimeInfo.getTransport()));
    }

    public static CompressionType resolveFromTransport(TransportCustomConfig transport) {
        if (transport == null || !Boolean.TRUE.equals(transport.getCompress())) {
            return CompressionType.NONE;
        }
        CompressionType algorithm = transport.resolveCompressAlgorithm();
        return algorithm != null && algorithm.isCompressed() ? algorithm : CompressionType.SNAPPY;
    }
}
