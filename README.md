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
 <a href="https://github.com/lxien/orbien/releases/v0.20.0">
    <img src="https://img.shields.io/badge/orbien-0.20.0-blue?style=for-the-badge" alt="orbien:0.20.0"/>
  </a>
<a href="https://somsubhra.github.io/github-release-stats/?username=lxien&repository=orbien">
  <img src="https://img.shields.io/github/downloads/lxien/orbien/total?style=for-the-badge" alt="Downloads"/>
</a>

</div>

<div align="center">
  <a href="README.md"><strong>README</strong></a> &nbsp;|&nbsp;
  <a href="README_ZH.md"><strong>简体中文</strong></a>
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

On a cloud server with a public IP and a `Docker` environment, run the script to install the `orbien` server in one
step. H2 lightweight database is used by default.

```shell
curl -fsSL https://raw.githubusercontent.com/lxien/orbien/main/scripts/install.sh -o install.sh && chmod +x install.sh && sudo sh install.sh
```

| Item           | Description                                                               |
|----------------|---------------------------------------------------------------------------|
| Console URL    | `http://<host>:8020` (`admin` / `123456`)                                 |
| Data directory | Linux `/opt/orbien`, macOS `~/.orbien`                                    |
| Default ports  | TCP tunnel `9527` · HTTP `8080` · HTTPS `8443` · TCP/UDP pool `9050-9099` |

### 2.3 Client

Download the binary for your platform from [Releases](https://github.com/lxien/orbien/releases).

#### 2.3.1 Quick tunnel

```shell
orbien login --server <server-host>:9527 --token <access-token>
orbien http 8080
orbien tcp 3306
```

#### 2.3.2 Spring Boot Starter

Can be embedded into a Spring Boot project to quickly expose Web applications or microservices to the public network.

```xml

<dependency>
    <groupId>io.github.lxien</groupId>
    <artifactId>orbien-spring-boot-starter</artifactId>
    <version>0.3.0</version>
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