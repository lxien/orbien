package io.github.lxien.orbien.cli.config;

import io.github.lxien.orbien.client.config.AppConfig;
import io.github.lxien.orbien.client.config.DefaultAppConfig;
import io.github.lxien.orbien.client.config.TomlConfigLoader;
import io.github.lxien.orbien.client.config.domain.AuthConfig;
import io.github.lxien.orbien.core.enums.AgentType;
import io.github.lxien.orbien.core.utils.StringUtils;
import io.github.lxien.orbien.credentials.Credentials;
import io.github.lxien.orbien.credentials.CredentialsStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class RunAppConfigFactory {

    private RunAppConfigFactory() {
    }

    public static AppConfig build(String configPath) {
        Path resolved = resolveConfigPath(configPath);
        TomlConfigLoader loader = new TomlConfigLoader(resolved.toString());
        DefaultAppConfig loaded = (DefaultAppConfig) loader.load();
        DefaultAppConfig.Builder builder = copyBuilder(loaded);
        builder.agentType(AgentType.SESSION);
        mergeAuth(builder, loaded.getAuthConfig());
        return builder.build();
    }

    private static Path resolveConfigPath(String configPath) {
        if (StringUtils.hasText(configPath)) {
            Path resolved = Paths.get(configPath).toAbsolutePath().normalize();
            if (!Files.exists(resolved)) {
                throw new IllegalArgumentException("配置文件不存在: " + resolved);
            }
            return resolved;
        }

        String configFileName = "orbienc.toml";
        String[] searchPaths = {"config/" + configFileName, configFileName};
        for (String path : searchPaths) {
            Path candidate = Paths.get(path);
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalArgumentException("未找到配置文件，例如: orbien run orbienc.toml\n" +
                "搜索路径: config/orbienc.toml, orbienc.toml");
    }

    private static void mergeAuth(DefaultAppConfig.Builder builder, AuthConfig fileAuth) {
        String fileToken = fileAuth != null ? fileAuth.getToken() : null;
        if (StringUtils.hasText(fileToken)) {
            AuthConfig authConfig = new AuthConfig();
            authConfig.setToken(fileToken.trim());
            builder.authConfig(authConfig);
            return;
        }

        Credentials credentials = CredentialsStore.loadOrThrow();
        AuthConfig authConfig = new AuthConfig();
        authConfig.setToken(credentials.getToken());
        builder.authConfig(authConfig);
    }

    private static DefaultAppConfig.Builder copyBuilder(DefaultAppConfig config) {
        DefaultAppConfig.Builder builder = DefaultAppConfig.builder()
                .serverAddr(config.getServerAddr())
                .serverPort(config.getServerPort())
                .authConfig(config.getAuthConfig())
                .transportConfig(config.getTransportConfig())
                .connectionConfig(config.getConnectionConfig())
                .logConfig(config.getLogConfig())
                .agentType(config.getAgentType());
        if (config.getProxies() != null) {
            builder.addProxies(config.getProxies());
        }
        return builder;
    }
}
