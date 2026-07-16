package io.github.lxien.orbien.client.console;

import io.github.lxien.orbien.client.manager.ProxyManagerHolder;
import io.github.lxien.orbien.core.message.Message;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SessionConsole {

    private static final int POLL_INTERVAL_MS = 500;
    private static final int READY_TIMEOUT_MS = 60_000;

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String BRIGHT_CYAN = "\u001B[96m";

    private final String serverAddr;
    private final int serverPort;
    private final AtomicBoolean printed = new AtomicBoolean(false);
    private volatile Thread watcherThread;

    private SessionConsole(String serverAddr, int serverPort) {
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
    }

    public static SessionConsole start(String serverAddr, int serverPort) {
        SessionConsole console = new SessionConsole(serverAddr, serverPort);
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
        watcherThread = new Thread(this::watchProxies, "orbien-session-console");
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
        PrintStream out = System.out;
        out.println();
        out.println(color(CYAN, "═══════════════════════════════════════════════════════"));
        out.println(color(BOLD + GREEN, "  隧道已建立"));
        out.println(color(CYAN, "═══════════════════════════════════════════════════════"));
        out.printf("  服务端: %s:%d%n", serverAddr, serverPort);
        out.println("  状态:   " + color(GREEN, "已连接"));
        out.println();
        out.println("  转发规则:");
        for (Message.RuntimeInfo runtimeInfo : runtimeInfos) {
            out.printf("    %s%n", formatForwarding(runtimeInfo));
        }
        out.println();
        out.println("  按 Ctrl+C 退出");
        out.println(color(CYAN, "═══════════════════════════════════════════════════════"));
        out.println();
    }

    private void printWaitingHint() {
        PrintStream out = System.out;
        out.println();
        out.println(color(CYAN, "═══════════════════════════════════════════════════════"));
        out.println(color(BOLD, "  已连接服务端，等待代理注册..."));
        out.printf("  服务端: %s:%d%n", serverAddr, serverPort);
        out.println("  访问地址请查看管理面板");
        out.println("  按 Ctrl+C 退出");
        out.println(color(CYAN, "═══════════════════════════════════════════════════════"));
        out.println();
    }

    private static String formatForwarding(Message.RuntimeInfo runtimeInfo) {
        String local = formatLocalTarget(runtimeInfo);
        List<String> remoteAddrs = runtimeInfo.getRemoteAddrList();
        String protocolName = inferProtocol(runtimeInfo);
        String remote = !remoteAddrs.isEmpty() ? highlight(remoteAddrs.get(0)) : "(地址待分配)";

        if (protocolName != null) {
            return String.format("%-5s %s -> %s", protocolName, remote, local);
        }
        return String.format("%s -> %s", remote, local);
    }

    private static String inferProtocol(Message.RuntimeInfo runtimeInfo) {
        if (runtimeInfo.hasFileLimits()) {
            return "file";
        }
        List<String> remotes = runtimeInfo.getRemoteAddrList();
        if (!remotes.isEmpty()) {
            String remote = remotes.get(0);
            if (remote.startsWith("https://")) {
                return "https";
            }
            if (remote.startsWith("http://")) {
                return "http";
            }
        }
        return null;
    }

    private static String formatLocalTarget(Message.RuntimeInfo runtimeInfo) {
        if (runtimeInfo.getTargetsCount() == 0) {
            return "local";
        }
        Message.Target target = runtimeInfo.getTargets(0);
        String hostPort = target.getHost() + ":" + target.getPort();
        String protocol = inferProtocol(runtimeInfo);
        if ("http".equals(protocol) || "https".equals(protocol) || "file".equals(protocol)) {
            return "http://" + hostPort;
        }
        return hostPort;
    }

    private static String highlight(String text) {
        return color(BOLD + BRIGHT_CYAN, text);
    }

    private static String color(String code, String text) {
        return code + text + RESET;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
