/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.lxien.orbien.server.config;

import io.github.lxien.orbien.common.config.ConfigSource;
import io.github.lxien.orbien.common.config.ConfigSourceType;
import io.github.lxien.orbien.common.utils.StringUtils;
import io.github.lxien.orbien.common.utils.TomlUtils;
import com.moandjiezana.toml.Toml;
import io.github.lxien.orbien.core.domain.PortInterval;
import io.github.lxien.orbien.core.domain.TlsConfig;
import io.github.lxien.orbien.server.config.domain.*;
import io.github.lxien.orbien.server.config.domain.*;
import io.github.lxien.orbien.core.transport.tls.TlsConfigSupport;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author lxien
 */
@Getter
public class TomlConfigSource implements ConfigSource {
    private final String path;
    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_BIND_PORT = 9527;
    private static final String DEFAULT_DASHBOARD_HOST = "0.0.0.0";
    private static final int DEFAULT_DASHBOARD_PORT = 8020;

    public TomlConfigSource(String path) {
        this.path = path;
    }

    @Override
    public AppConfig load() {
        Toml root = TomlUtils.readToml(path);
        AppConfig.Builder builder = AppConfig.builder();

        parseRoot(builder, root);
        parseDashboard(builder, root);
        parseTransport(builder, root);
        parsePortPool(builder, root);
        parseAuth(builder, root);
        parseProxyProtocol(builder, root);

        return builder.build();
    }

    private void parseRoot(AppConfig.Builder builder, Toml root) {
        String serverAddrValue = root.getString("server_addr", DEFAULT_HOST);
        if (StringUtils.hasText(serverAddrValue)) {
            builder.serverAddr(serverAddrValue.trim());
        }

        Long serverPortValue = root.getLong("server_port", (long) DEFAULT_BIND_PORT);
        validatePort(serverPortValue.intValue());
        builder.serverPort(serverPortValue.intValue());

        List<String> rootDomains = root.getList("root_domains");
        if (!CollectionUtils.isEmpty(rootDomains)) {
            builder.rootDomain(new HashSet<>(rootDomains));
        }
        Long httpProxyPort = root.getLong("http_proxy_port", 80L);
        int httpPort = httpProxyPort.intValue();
        validatePort(httpPort);
        builder.httpProxyPort(httpPort);

        Long httpsProxyPort = root.getLong("https_proxy_port", 443L);
        int httpsPort = httpsProxyPort.intValue();
        validatePort(httpsPort);
        builder.httpsProxyPort(httpsPort);
    }

    private void parseDashboard(AppConfig.Builder builder, Toml root) {
        Toml dash = root.getTable("dashboard");
        if (dash != null) {
            Boolean enabled = dash.getBoolean("enabled", false);
            if (enabled != null && enabled) {
                String addr = dash.getString("addr", DEFAULT_DASHBOARD_HOST);
                Long port = dash.getLong("port", (long) DEFAULT_DASHBOARD_PORT);
                String username = dash.getString("username");
                String password = dash.getString("password");
                String certFile = dash.getString("cert_file");
                String keyFile = dash.getString("key_file");
                String keyPass = dash.getString("key_pass");
                if (!StringUtils.hasText(username)) {
                    throw new IllegalArgumentException("请配置 Dashboard 用户名");
                }
                if (!StringUtils.hasText(password)) {
                    throw new IllegalArgumentException("请配置 Dashboard 密码");
                }
                DashboardConfig dashboard = new DashboardConfig(enabled, username, password, addr, port.intValue());
                dashboard.setCertFile(trimToNull(certFile));
                dashboard.setKeyFile(trimToNull(keyFile));
                dashboard.setKeyPassword(trimToNull(keyPass));
                validateDashboardSsl(dashboard);
                resolveDashboardCertPaths(dashboard, Paths.get(path).toAbsolutePath().normalize().getParent());
                builder.dashboard(dashboard);
            }
        }
    }

    private void parseTransport(AppConfig.Builder builder, Toml root) {
        Toml transport = root.getTable("transport");
        TransportConfig transportConfig = new TransportConfig();
        if (transport != null) {
            parseTls(transportConfig, transport);
            parseServerProtocol(transport.getTable("websocket"), transportConfig.getWebsocket(), 9528, false);
            parseServerProtocol(transport.getTable("quic"), transportConfig.getQuic(), 9529, false);
        }
        transportConfig.getTcp().setEnabled(true);
        resolveTransportCertPaths(transportConfig, Paths.get(path).toAbsolutePath().normalize().getParent());
        builder.transport(transportConfig);
    }

    private void parseServerProtocol(Toml table,
                                     io.github.lxien.orbien.core.domain.transport.ProtocolListenerConfig target,
                                     int defaultPort,
                                     boolean defaultEnabled) {
        target.setPort(defaultPort);
        target.setEnabled(defaultEnabled);
        if (table == null) {
            return;
        }
        Boolean enabled = table.getBoolean("enabled");
        if (enabled != null) {
            target.setEnabled(enabled);
        }
        int port = readServerPort(table, defaultPort);
        target.setPort(port);
        if (target instanceof io.github.lxien.orbien.core.domain.transport.WebSocketProtocolConfig ws) {
            String wsPath = table.getString("path");
            if (StringUtils.hasText(wsPath)) {
                ws.setPath(wsPath.trim());
            }
            Long maxFrame = table.getLong("max_frame_size");
            if (maxFrame != null) {
                ws.setMaxFrameSize(maxFrame.intValue());
            }
        }
        if (target instanceof io.github.lxien.orbien.core.domain.transport.QuicProtocolConfig quic) {
            Long maxIdle = table.getLong("max_idle_timeout_ms");
            if (maxIdle != null) {
                quic.setMaxIdleTimeoutMs(maxIdle);
            }
            Long maxStreams = table.getLong("initial_max_streams_bidi");
            if (maxStreams != null) {
                quic.setInitialMaxStreamsBidi(maxStreams.intValue());
            }
        }
    }

    private int readServerPort(Toml table, int defaultPort) {
        Long serverPort = table.getLong("server_port");
        if (serverPort != null) {
            validatePort(serverPort.intValue());
            return serverPort.intValue();
        }
        Long legacyPort = table.getLong("port");
        if (legacyPort != null) {
            validatePort(legacyPort.intValue());
            return legacyPort.intValue();
        }
        return defaultPort;
    }

    private void parseTls(TransportConfig transportConfig, Toml transport) {
        Toml tlsTable = transport.getTable("tls");
        if (tlsTable != null) {
            Boolean enabled = tlsTable.getBoolean("enabled", true);
            String certFile = tlsTable.getString("cert_file");
            String keyFile = tlsTable.getString("key_file");
            String caFile = tlsTable.getString("ca_file");
            String keyPass = tlsTable.getString("key_pass");
            TlsConfig tlsConfig = new TlsConfig(enabled, certFile, keyFile, caFile, keyPass);
            transportConfig.setTlsConfig(tlsConfig);
        }
    }

    private void resolveTransportCertPaths(TransportConfig transportConfig, Path configDir) {
        if (transportConfig == null || configDir == null) {
            return;
        }
        transportConfig.setTlsConfig(
                TlsConfigSupport.resolveAbsolutePaths(transportConfig.getTlsConfig(), configDir));
    }

    private void validateDashboardSsl(DashboardConfig dashboard) {
        boolean hasCert = StringUtils.hasText(dashboard.getCertFile());
        boolean hasKey = StringUtils.hasText(dashboard.getKeyFile());
        if (hasCert != hasKey) {
            throw new IllegalArgumentException("[dashboard] cert_file 与 key_file 需同时配置");
        }
    }

    private void resolveDashboardCertPaths(DashboardConfig dashboard, Path configDir) {
        if (dashboard == null || configDir == null || !dashboard.isSslEnabled()) {
            return;
        }
        dashboard.setCertFile(TlsConfigSupport.resolveAbsolutePath(configDir, dashboard.getCertFile()));
        dashboard.setKeyFile(TlsConfigSupport.resolveAbsolutePath(configDir, dashboard.getKeyFile()));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void parsePortPool(AppConfig.Builder builder, Toml root) {
        List<PortInterval> tcp = parsePortPoolEntries(root, "tcp");
        List<PortInterval> udp = parsePortPoolEntries(root, "udp");
        builder.portPool(new PortPoolConfig(tcp, udp));
    }

    private List<PortInterval> parsePortPoolEntries(Toml root, String protocol) {
        List<Toml> tables = resolvePortPoolTables(root, protocol);
        if (CollectionUtils.isEmpty(tables)) {
            return List.of();
        }

        List<PortInterval> intervals = new ArrayList<>(tables.size());
        for (int i = 0; i < tables.size(); i++) {
            intervals.add(parsePortPoolEntry(tables.get(i), protocol, i + 1));
        }
        return intervals;
    }

    private List<Toml> resolvePortPoolTables(Toml root, String protocol) {
        List<Toml> direct = root.getTables("port_pool." + protocol);
        if (!CollectionUtils.isEmpty(direct)) {
            return direct;
        }
        Toml portPool = root.getTable("port_pool");
        if (portPool == null) {
            return List.of();
        }
        List<Toml> nested = portPool.getTables(protocol);
        return nested == null ? List.of() : nested;
    }

    private PortInterval parsePortPoolEntry(Toml table, String protocol, int index) {
        String location = "[[port_pool." + protocol + "]] 第 " + index + " 条";

        Long single = table.getLong("single");
        Long start = table.getLong("start");
        Long end = table.getLong("end");

        boolean hasSingle = single != null;
        boolean hasStart = start != null;
        boolean hasEnd = end != null;
        boolean hasRange = hasStart || hasEnd;

        if (!hasSingle && !hasRange) {
            throw new IllegalArgumentException(location + " 必须配置 single 或 start/end");
        }
        if (hasSingle && hasRange) {
            throw new IllegalArgumentException(location + " single 与 start/end 不能同时配置");
        }
        if (hasRange && (!hasStart || !hasEnd)) {
            throw new IllegalArgumentException(location + " start 与 end 必须成对配置");
        }

        if (hasSingle) {
            validatePort(single.intValue());
            return PortInterval.ofPort(single.intValue());
        }

        validatePort(start.intValue());
        validatePort(end.intValue());
        return new PortInterval(start.intValue(), end.intValue());
    }

    private void parseAuth(AppConfig.Builder builder, Toml root) {
        Toml authNode = root.getTable("auth");
        if (authNode != null) {
            AuthConfig authConfig = new AuthConfig();
            parseAuthTokens(authConfig, authNode);
            builder.authConfig(authConfig);
        }
    }

    private void parseProxyProtocol(AppConfig.Builder builder, Toml root) {
        Toml table = root.getTable("proxy_protocol");
        if (table == null) {
            return;
        }
        ProxyProtocolConfig config = new ProxyProtocolConfig();
        Boolean enabled = table.getBoolean("enabled");
        if (enabled != null) {
            config.setEnabled(enabled);
        }
        Boolean strict = table.getBoolean("strict");
        if (strict != null) {
            config.setStrict(strict);
        }
        List<String> trusted = table.getList("trusted_proxies");
        if (trusted != null && !trusted.isEmpty()) {
            config.setTrustedProxies(new HashSet<>(trusted));
        }
        builder.proxyProtocol(config);
    }

    private void parseAuthTokens(AuthConfig authConfig, Toml authNode) {
        List<Toml> tokenNodes = authNode.getTables("tokens");
        if (tokenNodes == null) {
            return;
        }

        List<TokenConfig> tokenConfigs = new CopyOnWriteArrayList<>();
        Set<String> tokenTemp = new HashSet<>();
        for (Toml tokenTable : tokenNodes) {
            String name = tokenTable.getString("name");
            String token = tokenTable.getString("token");
            if (tokenTemp.contains(token)) {
                throw new IllegalArgumentException("Token令牌冲突，不能存在重复的令牌！ " + token);
            }
            TokenConfig accessToken = new TokenConfig(
                    name,
                    token
            );
            tokenConfigs.add(accessToken);
            tokenTemp.add(token);
        }
        authConfig.setTokens(tokenConfigs);
    }

    @Override
    public ConfigSourceType getSourceType() {
        return ConfigSourceType.TOML;
    }

    private void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("端口号必须在1-65535范围内: " + port);
        }
    }
}
