<div align="center">
  <img src="doc/logo.png" alt="Logo" width="180" height="180" style="border-radius:24px;margin-bottom:20px;"/>
</div>
<p align="center" style="font-size:18px;color:#555;margin-top:-10px;margin-bottom:24px;">
  A high-performance intranet penetration platform
</p>
<div align="center">
  <a href="https://github.com/xiaoniucode/etp/stargazers">
    <img src="https://img.shields.io/github/stars/xiaoniucode/etp?style=for-the-badge&logo=github" alt="GitHub Stars"/>
  </a>
  <a href="https://github.com/xiaoniucode/etp/forks">
    <img src="https://img.shields.io/github/forks/xiaoniucode/etp?style=for-the-badge&logo=github" alt="GitHub Forks"/>
  </a>
  <a href="https://github.com/xiaoniucode/etp/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/xiaoniucode/etp?style=for-the-badge" alt="License"/>
  </a>
 <a href="https://github.com/xiaoniucode/etp/releases/v0.15.0">
    <img src="https://img.shields.io/badge/etp-0.15.0-blue?style=for-the-badge" alt="etp:0.15.0"/>
  </a>
<a href="https://somsubhra.github.io/github-release-stats/?username=xiaoniucode&repository=etp">
  <img src="https://img.shields.io/github/downloads/xiaoniucode/etp/total?style=for-the-badge" alt="Downloads"/>
</a>

</div>

<div align="center">
  <a href="README.md"><strong>README</strong></a> &nbsp;|&nbsp;
  <a href="README_ZH.md"><strong>简体中文</strong></a> &nbsp;|&nbsp;
  <a href="https://xiaoniucode.github.io/etp"><strong>Documentation</strong></a>
</div>

![dashboard.png](doc/image/dashboard.png)

## Introduction
**etp (Easy Tunnel Proxy)** is a high-performance **intranet penetration platform**.
- Support for TCP and HTTP protocol proxying
- Data compression transmission, reducing bandwidth consumption
- TCP multiplexing stream transmission, multiple requests over single physical connection
- mTLS mutual authentication for secure data transmission
- IP CIDR access control (whitelist/blacklist)
- HTTP BasicAuth authentication, Token-based identity verification
- Fine-grained bandwidth rate limiting and traffic management
- Load balancing and cluster proxy support, enhancing system availability
- Custom domain and subdomain routing support
- Built-in modern Web UI dashboard for visual management and operational monitoring
- Spring Boot integration, reducing development and testing costs
- Compatible with Windows, Linux, and macOS for cross-platform deployment
- Client-autonomous + server-centralized management configuration rules for simplified administration

## Quick Start

### Server Installation

Requirements:

- Docker 20+
- Linux x86_64

One-command Docker startup for `etps` server:

```shell
curl -fsSL https://raw.githubusercontent.com/xiaoniucode/etp/main/scripts/install.sh -o install.sh && chmod +x install.sh && sudo sh install.sh
```

Management dashboard access: `http://server_ip:8020` (admin: 123456)

### Client Installation

Download the latest version from the [GitHub Releases](https://github.com/xiaoniucode/etp/tags) page and select the binary file for your operating system.

After extracting locally, edit the configuration file `etpc.toml`:

```toml
server_addr = "etps server IP or domain"
[auth]
token = "authentication token"
```

Run the client:

```shell
./etpc -c etpc.toml # Linux / MacOS

etpc.exe -c etpc.toml # Windows
```

For more usage details, please refer to the [documentation website](https://xiaoniucode.github.io/etp/).

## Feedback

Report issues: [issues](https://github.com/xiaoniucode/etp/issues)

## Project Trends

<p align="center">
  <a href="https://github.com/xiaoniucode/etp/stargazers">
    <img src="https://api.star-history.com/svg?repos=xiaoniucode/etp&type=Date" alt="Star History">
  </a>
</p>