<div align="center">
  <img src="doc/image/logo.png" alt="Logo" width="180" height="180" style="border-radius:24px;margin-bottom:20px;"/>
</div>
<p align="center" style="font-size:18px;color:#555;margin-top:-10px;margin-bottom:24px;">
  A high-performance intranet penetration platform
</p>
<div align="center">
  <a href="https://github.com/lxien/orbien/stargazers">
    <img src="https://img.shields.io/github/stars/lxien/orbien?style=for-the-badge&logo=github" alt="GitHub Stars"/>
  </a>
  <a href="https://github.com/lxien/orbien/forks">
    <img src="https://img.shields.io/github/forks/lxien/orbien?style=for-the-badge&logo=github" alt="GitHub Forks"/>
  </a>
  <a href="https://github.com/lxien/orbien/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/lxien/orbien?style=for-the-badge" alt="License"/>
  </a>
 <a href="https://github.com/lxien/orbien/releases/v0.21.0">
    <img src="https://img.shields.io/badge/orbien-0.21.0-blue?style=for-the-badge" alt="orbien:0.21.0"/>
  </a>
<a href="https://somsubhra.github.io/github-release-stats/?username=lxien&repository=orbien">
  <img src="https://img.shields.io/github/downloads/lxien/orbien/total?style=for-the-badge" alt="Downloads"/>
</a>
  <a href="https://discord.gg/4dgQjCS3k">
    <img src="https://img.shields.io/badge/Discord-Join-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Discord"/>
  </a>
  <a href="https://stackoak.com/">
    <img src="https://img.shields.io/badge/Demo-Online-0E8A16?style=for-the-badge" alt="Online Demo"/>
  </a>

</div>

<div align="center">
  <a href="README.md"><strong>README</strong></a> &nbsp;|&nbsp;
  <a href="README_ZH.md"><strong>简体中文</strong></a>
  &nbsp;|&nbsp;
  <a href="https://stackoak.com/"><strong>Live Demo</strong></a>
</div>

![dashboard.png](doc/image/dashboard.png)

## 1. Introduction

**Orbien** is a high-performance **intranet penetration platform** built on Netty, supporting multi-protocol proxying,
multiple transport channels, secure authentication, and visual operations management.

### 1.1 Features

- **Proxy protocols**: Supports TCP / UDP / HTTP / HTTPS / SOCKS5 / file sharing and more, with a built-in file
  management UI panel
- **Data transport**: TCP, WebSocket, QUIC; supports multiplexing and independent connections, with optional Snappy /
  LZ4 / ZSTD compression
- **Security & authentication**: mTLS mutual authentication, Token-based identity authentication, IP CIDR access
  control, HTTP BasicAuth, and time-window access
- **Traffic control**: Fine-grained bandwidth rate limiting, network backpressure, large-file chunking and streaming
  transfer
- **High availability**: Round-robin / weighted / random / least-connections load balancing strategies, and service
  health checks
- **Development & testing**: Supports HTTP/HTTPS traffic capture, header rewriting, HAProxy real IP retrieval, and more
- **Domain routing**: Subdomains and custom domains, multi-domain proxying; ACME certificate issuance, auto-renewal, and
  one-click deployment
- **Operations management**: Built-in modern Web console with metrics monitoring, memory monitoring, centralized
  configuration management, and OAuth third-party login integration
- **Configuration modes**: Client autonomy + server-side centralized configuration management, with bidirectional rule
  sync for both public and private network scenarios
- **Developer integration**: Binary client and Spring Boot Starter for embedded access
- **Cross-platform**: Compatible with Windows, Linux, and macOS (amd64 / arm64)

## 2. Quick Start

### 2.2 Server

Requires Linux, Docker, and a public IP. Uses H2 database by default.

```shell
mkdir -p /opt/orbien/data /opt/orbien/logs

cat > /opt/orbien/orbien-server.toml <<'EOF'
server_addr = "0.0.0.0"
server_port = 9527
http_proxy_port = 8080
https_proxy_port = 8443

[dashboard]
enabled = true
addr = "0.0.0.0"
port = 8020
username = "admin"
password = "123456"

[[port_pool.tcp]]
start = 9050
end = 9060

[[port_pool.udp]]
start = 9050
end = 9060
EOF

docker run -d \
  --name orbien-server \
  --restart unless-stopped \
  -p 8080:8080 \
  -p 8443:8443 \
  -p 8020:8020 \
  -p 9527:9527 \
  -p 9050-9060:9050-9060 \
  -p 9050-9060:9050-9060/udp \
  -e SPRING_PROFILES_ACTIVE=h2 \
  -e H2_DATA_DIR=/app/data/orbien-server \
  -e JAVA_OPTS="-Xms512m -Xmx512m -XX:MaxDirectMemorySize=512m -XX:+UseG1GC --enable-native-access=ALL-UNNAMED" \
  -e TZ=Asia/Shanghai \
  -v /opt/orbien/orbien-server.toml:/app/orbien-server.toml:ro \
  -v /opt/orbien/data:/app/data \
  -v /opt/orbien/cert:/app/cert \
  -v /opt/orbien/config:/app/config \
  -v /opt/orbien/logs:/app/logs \
  lxien/orbien-server:0.21.0
```

| Item      | Value                                                                 |
|-----------|-----------------------------------------------------------------------|
| Dashboard | `http://<host>:8020` (`admin` / `123456`)                             |
| Data dir  | `/opt/orbien`                                                         |
| Ports     | Tunnel `9527` · HTTP `8080` · HTTPS `8443` · TCP/UDP pool `9050-9060` |

### 2.3 Client

#### 2.3.1 Binary

Download from [Releases](https://github.com/lxien/orbien/releases).

```shell
Usage: orbien [-hV] [-c=<configFile>] [COMMAND]
Orbien intranet penetration client
  -c=<configFile>    Path to the config file
  -h, --help         Show this help message and exit.
  -V, --version      Print version information and exit.
Commands:
  login   Save server credentials
  logout  Clear local credentials
  run     Start the client from a config file
  http    Start an HTTP proxy
  tcp     Start a TCP proxy
  udp     Start a UDP proxy
```

案例：

```shell
orbien login --server <server-host>:9527 --token <access-token>
orbien http 8080
orbien tcp 3306
```

#### 2.3.2 Docker

```shell
mkdir -p /path/to/orbien/logs /path/to/orbien/.orbien

cat > /path/to/orbien/orbien.toml <<'EOF'
server_addr = "<server-host>"
server_port = 9527

[auth]
token = "<access-token>"

EOF

docker run -d \
  --name orbien \
  --restart unless-stopped \
  -e TZ=Asia/Shanghai \
  -v /path/to/orbien/orbien.toml:/app/orbien.toml:ro \
  -v /path/to/orbien/logs:/app/logs \
  -v /path/to/orbien/.orbien:/root/.orbien \
  lxien/orbien:0.21.0
```
#### 2.3.3 Spring Boot Starter

```xml

<dependency>
    <groupId>io.github.lxien</groupId>
    <artifactId>orbien-spring-boot-starter</artifactId>
    <version>0.4.0</version>
</dependency>
```

```yaml
orbien:
  client:
    enabled: true
    server-addr: <server-host>
    auth:
      token: <access-token>
    proxy:
      protocol: http
```

## Feedback

- Issues: [github.com/lxien/orbien/issues](https://github.com/lxien/orbien/issues)
- Online discussion： [Discord](https://discord.com/invite/4dgQjCS3k)
- QQ Group：1061026558

<img src="doc/image/qq-group.png" width="200"/>