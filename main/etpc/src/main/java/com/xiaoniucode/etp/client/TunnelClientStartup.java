package com.xiaoniucode.etp.client;

import ch.qos.logback.classic.Level;
import com.xiaoniucode.etp.client.config.AppConfig;
import com.xiaoniucode.etp.client.config.TomlConfigLoader;
import com.xiaoniucode.etp.client.config.domain.LogConfig;
import com.xiaoniucode.etp.client.logging.LogbackConfigurator;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

public class TunnelClientStartup {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TunnelClientStartup.class);
    private static TunnelClient tunnelClient;
    public static void main(String[] args) {
        try {
            AppConfig config = buildConfig(args);
            initLogback(config);
            registerShutdownHook();
            tunnelClient = new TunnelClient(config);
            tunnelClient.start();
        } catch (IllegalArgumentException e) {
            logger.error("参数错误: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("启动失败", e);
            System.exit(1);
        }
    }

    private static AppConfig buildConfig(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-c".equals(args[i])) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("-c 选项需要指定配置文件路径");
                }
                return loadConfigFromFile(args[i + 1]);
            }
        }
        return loadConfigFromDefaultLocations();
    }

    private static AppConfig loadConfigFromFile(String configPath) {
        if (!Files.exists(Paths.get(configPath))) {
            throw new IllegalArgumentException("配置文件不存在: " + configPath);
        }
        TomlConfigLoader configSource = new TomlConfigLoader(configPath);
        return configSource.load();
    }

    private static AppConfig loadConfigFromDefaultLocations() {
        String configFileName = "etpc.toml";
        String[] searchPaths = {"config/" + configFileName, configFileName};
        for (String path : searchPaths) {
            if (Files.exists(Paths.get(path))) {
                logger.info("使用配置文件: {}", path);
                TomlConfigLoader configSource = new TomlConfigLoader(path);
                return configSource.load();
            }
        }

        throw new IllegalArgumentException("未找到配置文件，请使用 -c 选项指定配置文件路径。\n" +
            "搜索路径: config/etpc.toml, etpc.toml");
    }

    private static void initLogback(AppConfig config) {
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
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (tunnelClient != null) {
                try {
                    tunnelClient.stop();
                } catch (Exception e) {
                    logger.error("停止客户端时发生错误", e);
                }
            }
        }));
    }
}
