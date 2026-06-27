---
sidebar_position: 2
---

# 客户端

## 一、下载安装

### 方式一：从 GitHub Releases 下载（推荐）

前往 [GitHub Releases](https://github.com/xiaoniucode/etp/releases) 页面，根据您的操作系统下载对应的二进制文件：

| 平台               | 文件名称                                                                                                                              |
|:-----------------|:----------------------------------------------------------------------------------------------------------------------------------|
| Linux (x86_64)   | [etpc_v0.15.0_linux_amd64.tar.gz](https://github.com/xiaoniucode/etp/releases/download/v0.15.0/etpc_v0.15.0_linux_amd64.tar.gz)   |
| Linux (arm64)    | [etpc_v0.15.0_linux_arm64.tar.gz](https://github.com/xiaoniucode/etp/releases/download/v0.15.0/etpc_v0.15.0_linux_arm64.tar.gz)   |
| macOS (x86_64)   | [etpc_v0.15.0_darwin_amd64.tar.gz](https://github.com/xiaoniucode/etp/releases/download/v0.15.0/etpc_v0.15.0_darwin_amd64.tar.gz) |
| macOS (arm64)    | [etpc_v0.15.0_darwin_arm64.tar.gz](https://github.com/xiaoniucode/etp/releases/download/v0.15.0/etpc_v0.15.0_darwin_arm64.tar.gz) |
| Windows (x86_64) | [etpc_v0.15.0_windows_amd64.zip](https://github.com/xiaoniucode/etp/releases/download/v0.15.0/etpc_v0.15.0_windows_amd64.zip)     |
| Windows (arm64)  | [etpc_v0.15.0_windows_arm64.zip](https://github.com/xiaoniucode/etp/releases/download/v0.15.0/etpc_v0.15.0_windows_arm64.zip)     |

#### 1. 编辑配置文件

```toml
#etpc.toml
server_addr = "your-server-ip-or-domain"
server_port = 9527
[auth]
token = "your-access-token"
```

#### 2. 运行客户端

```bash
./etpc -c etpc.toml
```

### 方式二：源码编译

#### 环境要求

* JDK 25+
* Maven 3.9+
* Git
* Oracle GraalVM

> 基于 GraalVM Native Image 构建原生可执行程序，可显著降低启动时间和内存占用。

#### 1. 克隆项目

```shell
git clone https://github.com/xiaoniucode/etp.git
cd etp/main
```

#### 2. 构建客户端原生程序

```shell
mvn clean install \
  native:compile \
  -Pnative \
  -am \
  -DskipTests \
  --no-transfer-progress \
  -pl etp-client
```

编译产物在`target/`目录