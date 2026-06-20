<div align="center">
  <img src="doc/logo.png" alt="Logo" width="180" height="180" style="border-radius:24px;margin-bottom:20px;"/>
</div>
<p align="center" style="font-size:18px;color:#555;margin-top:-10px;margin-bottom:24px;">
  一个高性能的内网穿透平台
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
 <a href="https://github.com/xiaoniucode/etp/releases/v0.10.0">
    <img src="https://img.shields.io/badge/etp-0.10.0-blue?style=for-the-badge" alt="etp:0.10.0"/>
  </a>
<a href="https://somsubhra.github.io/github-release-stats/?username=xiaoniucode&repository=etp">
  <img src="https://img.shields.io/github/downloads/xiaoniucode/etp/total?style=for-the-badge" alt="Downloads"/>
</a>

</div>

<div align="center">
  <a href="README.md"><strong>README</strong></a> &nbsp;|&nbsp;
  <a href="README_ZH.md"><strong>简体中文</strong></a> &nbsp;|&nbsp;
  <a href="https://xiaoniucode.github.io/etp"><strong>文档网站</strong></a>
</div>

![dashboard.png](doc/image/dashboard.png)

## 介绍
**etp (Easy Tunnel Proxy)** 是一个高性能的**内网穿透平台**。
- 支持 TCP、HTTP 协议代理
- 数据压缩传输，降低带宽消耗
- TCP 多路复用流传输，单物理连接承载多个请求
- mTLS 双向认证实现数据安全传输
- IP CIDR 访问控制（白名单/黑名单）
- HTTP BasicAuth 鉴权认证、Token 身份认证
- 精细化带宽限流与流量管控
- 负载均衡与集群代理支持，提升系统可用性
- 支持自定义域名、子域名路由
- 内置现代化 Web UI 面板，方便可视化管理和运维监控
- Spring Boot 集成，降低开发测试成本
- 兼容 Windows、Linux、macOS，跨平台部署
- 客户端自治 + 服务端集中化管理配置规则，便于管理

## 快速开始

### 安装服务端

环境要求：

- Docker 20+
- Linux x86_64

Docker一键启动`etps`服务端:

```shell
curl -fsSL https://raw.githubusercontent.com/xiaoniucode/etp/main/scripts/install.sh -o install.sh && chmod +x install.sh && sudo sh install.sh
```

管理面板访问地址：`http://服务器IP:8020` (admin: 123456)
### 安装客户端

从 [GitHub Releases](https://github.com/xiaoniucode/etp/tags)页面下载最新版本，根据您的操作系统下载对应的二进制文件。

下载到本地解压后编辑配置文件`etpc.toml`，

```toml
server_addr = "etps所在服务IP或域名"
[auth]
token = "身份认证令牌"
```

运行客户端:

```shell
./etpc -c etpc.toml # Linux / MacOS

etpc.exe -c etpc.toml # Windows
```

更多使用细节请查阅[文档网站](https://xiaoniucode.github.io/etp/)。

## 问题反馈

反馈问题:[issues](https://github.com/xiaoniucode/etp/issues)

## 项目趋势

<p align="center">
  <a href="https://github.com/xiaoniucode/etp/stargazers">
    <img src="https://api.star-history.com/svg?repos=xiaoniucode/etp&type=Date" alt="Star History">
  </a>
</p>
