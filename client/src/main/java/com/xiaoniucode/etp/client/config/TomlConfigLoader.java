package com.xiaoniucode.etp.client.config;

import com.xiaoniucode.etp.client.config.domain.*;
import com.xiaoniucode.etp.common.utils.StringUtils;
import com.xiaoniucode.etp.common.utils.TomlUtils;
import com.moandjiezana.toml.Toml;
import com.xiaoniucode.etp.common.config.ConfigSource;
import com.xiaoniucode.etp.common.config.ConfigSourceType;
import com.xiaoniucode.etp.core.domain.*;
import com.xiaoniucode.etp.core.domain.TransportCustomConfig;
import com.xiaoniucode.etp.core.enums.*;
import lombok.Getter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TOML 客户端配置加载器
 *
 * @author xiaoniucode
 */
@Getter
public class TomlConfigLoader implements ConfigSource {
    private final String path;

    public TomlConfigLoader(String path) {
        this.path = path;
    }

    @Override
    public AppConfig load() {
        Toml root = TomlUtils.readToml(path);
        DefaultAppConfig.Builder builder = DefaultAppConfig.builder();

        String serverAddrValue = root.getString("server_addr");
        Long serverPortValue = root.getLong("server_port");

        if (StringUtils.hasText(serverAddrValue)) {
            builder.serverAddr(serverAddrValue.trim());
        }

        if (serverPortValue != null) {
            validatePort(serverPortValue.intValue());
            builder.serverPort(serverPortValue.intValue());
        }
        // 读取传输配置
        Toml transportTable = root.getTable("transport");
        TransportConfig globalTransportConfig = new TransportConfig();
        MultiplexConfig multiplexConfig = new MultiplexConfig(true);
        globalTransportConfig.setMultiplexConfig(multiplexConfig);
        TlsConfig tlsConfig = new TlsConfig(true);
        globalTransportConfig.setTlsConfig(tlsConfig);
        if (transportTable != null) {
            // 读取多路复用配置
            Toml multiplexTable = transportTable.getTable("multiplex");
            if (multiplexTable != null) {
                Boolean enabled = multiplexTable.getBoolean("enabled", true);
                multiplexConfig.setEnabled(enabled);
                globalTransportConfig.setMultiplexConfig(multiplexConfig);
            }

            // 读取 TLS 配置
            Toml tlsTable = transportTable.getTable("tls");
            if (tlsTable != null) {
                Boolean enabled = tlsTable.getBoolean("enabled", true);
                String certFile = tlsTable.getString("cert_file");
                String keyFile = tlsTable.getString("key_file");
                String caFile = tlsTable.getString("ca_file");
                String keyPass = tlsTable.getString("key_pass");
                tlsConfig = new TlsConfig(enabled, certFile, keyFile, caFile, keyPass);
                globalTransportConfig.setTlsConfig(tlsConfig);
            }

            builder.transportConfig(globalTransportConfig);
        }

        // 读取连接配置
        Toml connectionTable = root.getTable("connection");
        if (connectionTable != null) {
            ConnectionConfig connectionConfig = new ConnectionConfig();
            // 读取重试配置
            Toml retryTable = connectionTable.getTable("retry");
            if (retryTable != null) {
                RetryConfig retryConfig = new RetryConfig();
                Long initialDelaySecValue = retryTable.getLong("initial_delay", 0L);
                Long maxDelaySecValue = retryTable.getLong("max_delay", 0L);
                Long maxRetriesValue = retryTable.getLong("max_retries", 0L);
                if (initialDelaySecValue != null) {
                    retryConfig.setInitialDelay(initialDelaySecValue.intValue());
                }
                if (maxDelaySecValue != null) {
                    retryConfig.setMaxDelay(maxDelaySecValue.intValue());
                }
                if (maxRetriesValue != null) {
                    retryConfig.setMaxRetries(maxRetriesValue.intValue());
                }
                connectionConfig.setRetryConfig(retryConfig);
            }

            // 读取连接池配置
            Toml poolTable = connectionTable.getTable("pool");
            if (poolTable != null) {
                PoolConfig poolConfig = new PoolConfig();
                Boolean enabled = poolTable.getBoolean("enabled", false);
                poolConfig.setEnabled(enabled);
                // 读取多路复用连接池配置
                Toml multiplexPoolTable = poolTable.getTable("multiplex");
                if (multiplexPoolTable != null) {
                    PoolConfig.MultiplexPoolConfig multiplexPoolConfig = poolConfig.getMultiplex();
                    multiplexPoolConfig.setPlain(multiplexPoolTable.getBoolean("plain", false));
                    multiplexPoolConfig.setEncrypt(multiplexPoolTable.getBoolean("encrypt", false));
                }

                // 读取独立连接池配置
                Toml directPoolTable = poolTable.getTable("direct");
                if (directPoolTable != null) {
                    PoolConfig.DirectPoolConfig directPoolConfig = poolConfig.getDirect();
                    directPoolConfig.setPlainCount(directPoolTable.getLong("plain_count", 0L).intValue());
                    directPoolConfig.setEncryptCount(directPoolTable.getLong("encrypt_count", 0L).intValue());
                }

                connectionConfig.setPoolConfig(poolConfig);
            }

            builder.connectionConfig(connectionConfig);
        }

        // 读取认证配置
        Toml authTable = root.getTable("auth");
        if (authTable != null) {
            String token = authTable.getString("token", "");
            AuthConfig authConfig = new AuthConfig();
            authConfig.setToken(token.trim());
            builder.authConfig(authConfig);
        }

        // 读取代理配置
        List<Toml> proxiesTables = root.getTables("proxies");
        if (proxiesTables != null && !proxiesTables.isEmpty()) {
            List<ProxyConfig> proxies = new ArrayList<>();
            for (Toml proxyTable : proxiesTables) {
                ProxyConfig proxyConfig = new ProxyConfig();

                String name = proxyTable.getString("name");
                String protocol = proxyTable.getString("protocol");
                String localIp = proxyTable.getString("local_ip", "127.0.0.1");
                Long localPortValue = proxyTable.getLong("local_port");
                Long remotePortValue = proxyTable.getLong("remote_port");
                Boolean enableV = proxyTable.getBoolean("enabled", true);
                Boolean forceHttpsV = proxyTable.getBoolean("force_Https", false);
                String loadBalanceStrategy = proxyTable.getString("load_balance_strategy");

                if (!StringUtils.hasText(name)) {
                    throw new IllegalArgumentException("代理名称不能为空");
                }
                if (!StringUtils.hasText(protocol)) {
                    throw new IllegalArgumentException("协议类型不能为空");
                }
                ProtocolType protocolType = ProtocolType.fromName(protocol.trim());
                if (protocolType == null) {
                    throw new IllegalArgumentException("无效的协议类型: " + protocol);
                }
                if (StringUtils.hasText(loadBalanceStrategy)) {
                    LoadBalanceType loadBalanceType = LoadBalanceType.fromName(loadBalanceStrategy);
                    if (loadBalanceType == null) {
                        throw new IllegalArgumentException("负载均衡策略类型不支持：" + loadBalanceStrategy);
                    }
                    proxyConfig.setLoadBalanceType(loadBalanceType);
                }
                proxyConfig.setName(name.trim());
                proxyConfig.setProtocol(protocolType);
                proxyConfig.setForceHttps(forceHttpsV);
                proxyConfig.setStatus(enableV ? ProxyStatus.OPEN : ProxyStatus.CLOSED);
                if (remotePortValue != null) {
                    validatePort(remotePortValue.intValue());
                    proxyConfig.setRemotePort(remotePortValue.intValue());
                }

                //解析目标服务
                List<Target> targets = proxyTable.getList("targets", new ArrayList<>()).stream()
                        .map(item -> {
                            Map<String, Object> map = (Map) item;
                            String host = (String) map.getOrDefault("host", "127.0.0.1");
                            Long port = (Long) map.get("port");
                            Long weight = (Long) map.getOrDefault("weight", 1L);
                            Object nameV = map.get("name");
                            if (port == null) {
                                throw new IllegalArgumentException("目标端口不能为空");
                            }
                            return new Target(host, port.intValue(), weight.intValue(), nameV != null ? String.valueOf(nameV) : null);
                        }).collect(Collectors.toList());
                if (localPortValue != null) {
                    targets.add(new Target(localIp, localPortValue.intValue(), 1, name));
                }
                if (targets.isEmpty()) {
                    throw new IllegalArgumentException("至少配置一个目标内网服务");
                }
                proxyConfig.addTargets(targets);

                //解析HTTP(S)协议域名配置
                if (proxyConfig.isHttpOrHttps()) {
                    Boolean autoDomain = proxyTable.getBoolean("auto_domain", true);
                    List<String> customDomains = proxyTable.getList("custom_domains");
                    List<String> subDomains = proxyTable.getList("sub_domains");

                    RouteConfig routeConfig = new RouteConfig();
                    routeConfig.setAutoDomain(autoDomain);

                    if (customDomains != null) {
                        for (String domain : customDomains) {
                            if (StringUtils.hasText(domain)) {
                                routeConfig.getCustomDomains().add(domain.trim());
                            }
                        }
                    }
                    if (subDomains != null) {
                        for (String domain : subDomains) {
                            if (StringUtils.hasText(domain)) {
                                routeConfig.getSubDomains().add(domain.trim());
                            }
                        }
                    }
                    proxyConfig.setRouteConfig(routeConfig);
                }


                //访问控制
                Toml accessControl = proxyTable.getTable("access_control");
                if (accessControl != null) {
                    Boolean enabled = accessControl.getBoolean("enabled", false);
                    String mode = accessControl.getString("mode");
                    if (!StringUtils.hasText(mode)) {
                        throw new IllegalArgumentException("必须指定访问控制模式");
                    }
                    List<String> allow = accessControl.getList("allow", new ArrayList<>());
                    List<String> deny = accessControl.getList("deny", new ArrayList<>());
                    AccessControlConfig accessControlConfig = new AccessControlConfig(enabled,
                            AccessControl.fromValue(mode),
                            new HashSet<>(allow),
                            new HashSet<>(deny));
                    proxyConfig.setAccessControl(accessControlConfig);
                }

                proxies.add(proxyConfig);
                //HTTP(S) BASIC AUTH
                if (ProtocolType.isHttpOrHttps(protocol)) {
                    Toml basicAuth = proxyTable.getTable("basic_auth");
                    if (basicAuth != null) {
                        Boolean enabled = basicAuth.getBoolean("enabled", false);
                        HashSet<HttpUser> httpUsers = new HashSet<>();
                        List<HashMap> users = basicAuth.getList("users");
                        if (users != null && !users.isEmpty()) {
                            for (HashMap map : users) {
                                String user = (String) map.getOrDefault("user", "");
                                String pass = (String) map.getOrDefault("pass", "");
                                httpUsers.add(new HttpUser(user, pass));
                            }
                        }
                        BasicAuthConfig basicAuthConfig = new BasicAuthConfig();
                        basicAuthConfig.setEnabled(enabled);
                        basicAuthConfig.addUsers(httpUsers);
                        proxyConfig.setBasicAuth(basicAuthConfig);
                    }
                }
                //HTTPS SSL证书
                if (protocolType.isHttps()) {
                    Toml ssl = proxyTable.getTable("ssl");
                    if (ssl != null) {
                        String keyFile = ssl.getString("key_file");
                        String certFile = ssl.getString("cert_file");
                        if (!StringUtils.hasText(keyFile)) {
                            throw new IllegalArgumentException("请配置私钥路径：" + keyFile);
                        }
                        if (!StringUtils.hasText(certFile)) {
                            throw new IllegalArgumentException("请配置证书路径：" + certFile);
                        }
                        if (!new File(keyFile).exists()) {
                            throw new IllegalArgumentException("私钥不存在，请检查私钥路径");
                        }
                        if (!new File(certFile).exists()) {
                            throw new IllegalArgumentException("证书不存在，请检查证书路径");
                        }
                        proxyConfig.setSslConfig(new SslConfig(keyFile, certFile));
                    }
                }
                Toml healthCheck = proxyTable.getTable("health_check");
                if (healthCheck != null) {
                    HealthCheckConfig healthCheckConfig = new HealthCheckConfig();
                    Boolean enabled = healthCheck.getBoolean("enabled");
                    if (enabled != null) {
                        healthCheckConfig.setEnabled(enabled);
                    }
                    String type = healthCheck.getString("type");
                    if (type == null) {
                        throw new IllegalArgumentException("请指定健康检查类型：type");
                    }
                    HealthCheckType healthCheckType = HealthCheckType.fromName(type);
                    if (healthCheckType == null) {
                        throw new IllegalArgumentException("健康检查类型不可用：" + type);
                    }
                    healthCheckConfig.setType(healthCheckType);
                    Long interval = healthCheck.getLong("interval");
                    if (interval != null) {
                        healthCheckConfig.setInterval(interval.intValue());
                    }
                    Long timeout = healthCheck.getLong("timeout");
                    if (timeout != null) {
                        healthCheckConfig.setTimeout(timeout.intValue());
                    }
                    Long maxFailed = healthCheck.getLong("max_failed");
                    if (maxFailed != null) {
                        healthCheckConfig.setMaxFailed(maxFailed.intValue());
                    }
                    String path = healthCheck.getString("path");
                    if (StringUtils.hasText(path)) {
                        healthCheckConfig.setPath(path);
                    }
                    proxyConfig.setHealthCheck(healthCheckConfig);
                }

                //带宽限制
                Toml bandwidth = proxyTable.getTable("bandwidth");
                if (bandwidth != null) {
                    String limit = bandwidth.getString("limit_total");
                    String limitIn = bandwidth.getString("limit_in");
                    String limitOut = bandwidth.getString("limit_out");
                    if (StringUtils.hasText(limit) || StringUtils.hasText(limitIn) || StringUtils.hasText(limitOut)) {
                        BandwidthConfig bandwidthConfig = new BandwidthConfig(limit, limitIn, limitOut);
                        proxyConfig.setBandwidth(bandwidthConfig);
                    }
                }
                //自定义传输配置
                Toml transport = proxyTable.getTable("transport");
                TransportCustomConfig transportCustomConfig = new TransportCustomConfig();
                //默认使用全局配置
                transportCustomConfig.setMultiplex(globalTransportConfig.getMultiplexConfig().isEnabled());
                if (transport != null) {
                    Boolean multiplex = transport.getBoolean("multiplex");
                    Boolean compress = transport.getBoolean("compress", true);
                    Boolean encrypt = transport.getBoolean("encrypt", true);
                    if (multiplex != null) {
                        transportCustomConfig.setMultiplex(multiplex);
                    }
                    transportCustomConfig.setCompress(compress);
                    transportCustomConfig.setEncrypt(encrypt);
                }
                if (protocolType.isUdp()) {
                    transportCustomConfig.setMultiplex(true);
                }

                proxyConfig.setTransport(transportCustomConfig);
            }
            builder.addProxies(proxies);
        }

        //解析日志配置
        Toml logTable = root.getTable("log");
        LogConfig logConfig = new LogConfig();
        if (logTable != null) {
            String level = logTable.getString("level");
            String path = logTable.getString("path");
            String name = logTable.getString("name");
            String archivePattern = logTable.getString("archive_pattern");
            String logPattern = logTable.getString("log_pattern");
            Long maxHistoryValue = logTable.getLong("max_history");
            String totalSizeCap = logTable.getString("total_size_cap");

            if (StringUtils.hasText(level)) {
                logConfig.setLevel(level.trim());
            }
            if (StringUtils.hasText(path)) {
                logConfig.setPath(path.trim());
            }
            if (StringUtils.hasText(name)) {
                logConfig.setName(name.trim());
            }
            if (StringUtils.hasText(archivePattern)) {
                logConfig.setArchivePattern(archivePattern.trim());
            }
            if (StringUtils.hasText(logPattern)) {
                logConfig.setLogPattern(logPattern.trim());
            }
            if (maxHistoryValue != null && maxHistoryValue > 0) {
                logConfig.setMaxHistory(maxHistoryValue.intValue());
            }
            if (StringUtils.hasText(totalSizeCap)) {
                logConfig.setTotalSizeCap(totalSizeCap.trim());
            }
        }
        builder.logConfig(logConfig);

        return builder.build();
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
