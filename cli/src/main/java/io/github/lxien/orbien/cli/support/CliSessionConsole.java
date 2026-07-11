package io.github.lxien.orbien.cli.support;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.manager.ProxyManagerHolder;
import io.github.lxien.orbien.core.domain.ProxyConfig;
import io.github.lxien.orbien.core.enums.ProtocolType;
import io.github.lxien.orbien.core.message.Message;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CliSessionConsole {

    private static final int POLL_INTERVAL_MS = 500;
    private static final int READY_TIMEOUT_MS = 60_000;

    private final AppConfig config;
    private final AtomicBoolean printed = new AtomicBoolean(false);
    private volatile Thread watcherThread;

    private CliSessionConsole(AppConfig config) {
        this.config = config;
    }

    public static CliSessionConsole start(AppConfig config) {
        CliSessionConsole console = new CliSessionConsole(config);
        console.startWatcher();
        return console;
    }

    public void stop() {
        Thread thread = watcherThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void startWatcher() {
        watcherThread = new Thread(this::watchProxies, "orbien-cli-console");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private void watchProxies() {
        long deadline = System.currentTimeMillis() + READY_TIMEOUT_MS;
        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < deadline) {
            Collection<Message.RuntimeInfo> runtimeInfos = ProxyManagerHolder.get().list();
            if (!runtimeInfos.isEmpty() && printed.compareAndSet(false, true)) {
                printSession(runtimeInfos);
                return;
            }
            sleepQuietly(POLL_INTERVAL_MS);
        }

        if (printed.compareAndSet(false, true)) {
            printWaitingHint();
        }
    }

    private void printSession(Collection<Message.RuntimeInfo> runtimeInfos) {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  隧道已建立");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.printf("  服务端: %s:%d%n", config.getServerAddr(), config.getServerPort());
        System.out.println("  状态:   已连接");
        System.out.println();
        System.out.println("  转发规则:");
        for (Message.RuntimeInfo runtimeInfo : runtimeInfos) {
            System.out.printf("    %s%n", formatForwarding(runtimeInfo));
        }
        System.out.println();
        System.out.println("  按 Ctrl+C 退出");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();
    }

    private void printWaitingHint() {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  已连接服务端，等待代理注册...");
        System.out.printf("  服务端: %s:%d%n", config.getServerAddr(), config.getServerPort());
        System.out.println("  访问地址请查看管理面板");
        System.out.println("  按 Ctrl+C 退出");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();
    }

    private String formatForwarding(Message.RuntimeInfo runtimeInfo) {
        ProtocolType protocol = resolveProtocol(runtimeInfo.getName());
        String protocolName = protocol != null ? protocol.getDesc() : "proxy";
        String local = formatLocalTarget(runtimeInfo, protocol);
        List<String> remoteAddrs = runtimeInfo.getRemoteAddrList();

        if (protocol != null && protocol.isHttpOrHttps()) {
            if (!remoteAddrs.isEmpty()) {
                return String.format("%-5s %s -> %s", protocolName, remoteAddrs.get(0), local);
            }
            return String.format("%-5s (域名待分配) -> %s", protocolName, local);
        }

        if (protocol != null && (protocol.isTcp() || protocol.isUdp())) {
            String remote = !remoteAddrs.isEmpty()
                    ? remoteAddrs.get(0)
                    : formatTcpRemoteFallback(runtimeInfo.getName());
            return String.format("%-5s %s -> %s", protocolName, remote, local);
        }

        if (!remoteAddrs.isEmpty()) {
            return String.format("%-5s %s -> %s", protocolName, remoteAddrs.get(0), local);
        }
        return String.format("%-5s %s", protocolName, local);
    }

    private String formatTcpRemoteFallback(String proxyName) {
        for (ProxyConfig proxy : config.getProxies()) {
            if (proxy.getName().equals(proxyName) && proxy.getRemotePort() != null) {
                return ":" + proxy.getRemotePort();
            }
        }
        return "(由端口池分配)";
    }

    private String formatLocalTarget(Message.RuntimeInfo runtimeInfo, ProtocolType protocol) {
        if (runtimeInfo.getTargetsCount() > 0) {
            Message.Target target = runtimeInfo.getTargets(0);
            String scheme = protocol != null && protocol.isHttpOrHttps() ? "http://" : "";
            return scheme + target.getHost() + ":" + target.getPort();
        }
        return "local";
    }

    private ProtocolType resolveProtocol(String proxyName) {
        for (ProxyConfig proxy : config.getProxies()) {
            if (proxy.getName().equals(proxyName)) {
                return proxy.getProtocol();
            }
        }
        return null;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
