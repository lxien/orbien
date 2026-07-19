package io.github.lxien.orbien.cli.launcher;

import ch.qos.logback.classic.Level;
import io.github.lxien.orbien.client.TunnelClient;
import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.TomlConfigLoader;
import io.github.lxien.orbien.client.config.domain.LogConfig;
import io.github.lxien.orbien.client.console.SessionConsole;
import io.github.lxien.orbien.client.logging.LogbackConfigurator;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

public final class TunnelClientLauncher {

    private static final CountDownLatch SHUTDOWN_LATCH = new CountDownLatch(1);

    private static TunnelClient tunnelClient;
    private static SessionConsole sessionConsole;

    private TunnelClientLauncher() {
    }

    public static int launchLegacy(String configPath) {
        try {
            AppConfig config = loadLegacyConfig(configPath);
            initFileLogging(config);
            startTunnel(config, false);
            awaitShutdown();
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("参数错误: " + e.getMessage());
            return 1;
        } catch (Throwable e) {
            System.err.println("启动失败: " + e);
            e.printStackTrace(System.err);
            return 1;
        }
    }

    public static int launchSession(AppConfig config) {
        try {
            startTunnel(config, true);
            sessionConsole = SessionConsole.start(config.getServerAddr(), config.getServerPort());
            awaitShutdown();
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (Throwable e) {
            System.err.println("启动失败: " + e);
            e.printStackTrace(System.err);
            return 1;
        }
    }

    private static void startTunnel(AppConfig config, boolean sessionMode) {
        registerShutdownHook();
        tunnelClient = new TunnelClient(config);
        tunnelClient.setExitJvmOnStop(true);
        tunnelClient.start();
        if (sessionMode) {
            System.out.printf("连接 %s:%d ...%n", config.getServerAddr(), config.getServerPort());
        }
    }

    private static AppConfig loadLegacyConfig(String configPath) {
        if (configPath != null && !configPath.isBlank()) {
            return loadConfigFromFile(configPath);
        }
        return loadConfigFromDefaultLocations();
    }

    private static AppConfig loadConfigFromFile(String configPath) {
        Path resolved = Paths.get(configPath).toAbsolutePath().normalize();
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException("配置文件不存在: " + resolved);
        }
        System.err.println("使用配置文件: " + resolved);
        TomlConfigLoader configSource = new TomlConfigLoader(resolved.toString());
        return configSource.load();
    }

    private static AppConfig loadConfigFromDefaultLocations() {
        String[] searchPaths = {"config/orbien.toml", "orbien.toml"};
        for (String path : searchPaths) {
            if (Files.exists(Paths.get(path))) {
                System.err.println("使用配置文件: " + path);
                TomlConfigLoader configSource = new TomlConfigLoader(path);
                return configSource.load();
            }
        }
        throw new IllegalArgumentException("未找到配置文件，例如: orbien run orbien.toml 或 orbien -c orbien.toml\n" +
                "搜索路径: config/orbien.toml, orbien.toml");
    }

    private static void initFileLogging(AppConfig config) {
        LogConfig log = config.getLogConfig();
        if (log == null) {
            return;
        }
        new LogbackConfigurator.Builder()
                .setPath(log.getPath())
                .setLogPattern(log.getLogPattern())
                .setArchivePattern(log.getArchivePattern())
                .setLogLevel(Level.toLevel(log.getLevel(), Level.INFO))
                .setLogName(log.getName())
                .setMaxHistory(log.getMaxHistory())
                .setTotalSizeCap(log.getTotalSizeCap())
                .addLogger("io.netty.channel.ChannelHandlerMask", Level.INFO)
                .addLogger("io.netty.handler.ssl.util.BouncyCastleUtil", Level.ERROR)
                .addLogger("io.netty.handler.ssl", Level.WARN)
                .build()
                .configure();
        if (LoggerFactory.getILoggerFactory() instanceof ch.qos.logback.classic.LoggerContext) {
            InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        }
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (sessionConsole != null) {
                sessionConsole.stop();
            }
            if (tunnelClient != null) {
                try {
                    tunnelClient.shutdownGracefully();
                } catch (Exception e) {
                    System.err.println("停止客户端失败: " + e.getMessage());
                }
            }
            SHUTDOWN_LATCH.countDown();
        }, "orbien-shutdown-hook"));
    }

    private static void awaitShutdown() {
        try {
            SHUTDOWN_LATCH.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
