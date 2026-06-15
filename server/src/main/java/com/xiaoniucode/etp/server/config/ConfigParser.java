package com.xiaoniucode.etp.server.config;

import com.xiaoniucode.etp.common.PortChecker;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigParser {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConfigParser.class);

    public static AppConfig parse(String[] args) {
        try {
            AppConfig appConfig = buildConfig(args);
            int bindPort = appConfig.getServerPort();
            if (PortChecker.isPortOccupied(bindPort)) {
                logger.error("{} 端口已经被占用", bindPort);
                System.exit(0);
            }
            return appConfig;
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            System.err.println("错误: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("启动失败", e);
            System.err.println("启动失败: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    private static AppConfig buildConfig(String[] args) {
        String configPath = parseConfigPath(args);
        if (configPath != null) {
            return loadConfigFromFile(configPath);
        }
        return loadConfigFromDefaultLocations();
    }

    private static String parseConfigPath(String[] args) {
        String configPath = null;
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if ("-c".equals(arg)) {
                if (configPath != null) {
                    throw new IllegalArgumentException("-c 选项只能指定一次");
                }
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("-c 选项需要指定配置文件路径");
                }
                configPath = args[++i];
            } else {
                throw new IllegalArgumentException("未知选项: " + arg);
            }
            i++;
        }
        return configPath;
    }

    private static AppConfig loadConfigFromDefaultLocations() {
        String configFileName = "etps.toml";
        String[] searchPaths = {"config/" + configFileName, configFileName};
        for (String path : searchPaths) {
            if (Files.exists(Paths.get(path))) {
                logger.info("找到配置文件: {}", path);
                return loadConfigFromFile(path);
            }
        }
        throw new IllegalArgumentException("未找到配置文件，请使用 -c 选项指定配置文件路径。\n" +
            "搜索路径: config/etps.toml, etps.toml");
    }

    private static AppConfig loadConfigFromFile(String configPath) {
        if (!Files.exists(Paths.get(configPath))) {
            throw new IllegalArgumentException("配置文件不存在: " + configPath);
        }
        TomlConfigSource configSource = new TomlConfigSource(configPath);
        return configSource.load();
    }
}
