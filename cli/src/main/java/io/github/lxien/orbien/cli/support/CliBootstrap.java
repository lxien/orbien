package io.github.lxien.orbien.cli.support;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class CliBootstrap {

    private static final Set<String> SESSION_COMMANDS = Set.of(
            "login", "logout", "run", "http", "tcp", "udp"
    );

    private CliBootstrap() {
    }

    public static boolean isLegacyDebugMode(String[] args) {
        if (args == null || args.length == 0) {
            return false;
        }
        boolean hasRootConfig = false;
        boolean hasSubcommand = false;
        for (String arg : args) {
            if ("-c".equals(arg)) {
                hasRootConfig = true;
            }
            if (SESSION_COMMANDS.contains(arg)) {
                hasSubcommand = true;
            }
        }
        return hasRootConfig && !hasSubcommand;
    }

    public static void prepareQuietSession() {
        silenceJavaUtilLogging();
        CliLogbackSupport.configureQuietSession();
    }

    private static void silenceJavaUtilLogging() {
        LogManager logManager = LogManager.getLogManager();
        Logger root = logManager.getLogger("");
        if (root != null) {
            root.setLevel(Level.OFF);
        }
        Logger nettyLogger = logManager.getLogger("io.netty.util.internal.logging.InternalLoggerFactory");
        if (nettyLogger != null) {
            nettyLogger.setLevel(Level.OFF);
        }
    }
}
