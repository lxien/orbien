---
sidebar_position: 2
title: 服务端部署
description: 数据库选型、Docker 与 JAR 部署
---

# 服务端部署

## 选择数据库

服务端内置两套数据库 Profile，通过环境变量 `SPRING_PROFILES_ACTIVE` 切换：

| Profile  | 类型       |  适合场景   |
|:---------|:---------|:-------:|
| `h2`（默认） | 嵌入式轻量文件库 | 服务器资源有限 |
| `mysql`  | 外部 MySQL | 服务器资源充足 |

### H2 相关变量

| 变量                       | 默认值                    | 含义      |
|:-------------------------|:-----------------------|:--------|
| `SPRING_PROFILES_ACTIVE` | `h2`                   | 启用 H2   |
| `H2_DATA_DIR`            | `./data/orbien-server` | 数据库文件目录 |
| `H2_USERNAME`            | `sa`                   | 用户名     |
| `H2_PASSWORD`            | 空                      | 密码      |

### MySQL 相关变量

| 变量                       | 默认值         | 含义         |
|:-------------------------|:------------|:-----------|
| `SPRING_PROFILES_ACTIVE` | —           | 设为 `mysql` |
| `MYSQL_HOST`             | `localhost` | MySQL 主机   |
| `MYSQL_PORT`             | `3306`      | 端口         |
| `MYSQL_DATABASE`         | `orbien`    | 库名         |
| `MYSQL_USERNAME`         | `root`      | 用户名        |
| `MYSQL_PASSWORD`         | `123456`    | 密码         |

## 准备目录

```shell
mkdir -p /opt/orbien/{data,logs,cert,config}
```

## 编写 orbien-server.toml

```toml
# orbien-server.toml
server_addr = "0.0.0.0"
server_port = 9527
http_proxy_port = 8080
https_proxy_port = 8443

[dashboard]
enabled = true
addr = "0.0.0.0"
port = 8020
username = "admin"
password = "改成强密码"
```

## Docker

### 数据卷

| 容器路径                      | 说明             |
|:--------------------------|:---------------|
| `/app/orbien-server.toml` | 服务端应用配置        |
| `/app/data`               | 持久化数据          |
| `/app/cert`               | 域名证书以及系统默认自签证书 |
| `/app/config`             | 配置文件           |

### 使用 H2 安装

```shell
docker run -d \
  --name orbien-server \
  --restart unless-stopped \
  -p 9527:9527 \
  -p 8020:8020 \
  -p 8080:8080 \
  -p 8443:8443 \
  -p 9050-9060:9050-9060 \
  -p 9050-9060:9050-9060/udp \
  -e TZ=Asia/Shanghai \
  -e SPRING_PROFILES_ACTIVE=h2 \
  -e H2_DATA_DIR=/app/data/orbien-server \
  -e JAVA_OPTS="-Xms512m -Xmx512m -XX:MaxDirectMemorySize=512m -XX:+UseG1GC --enable-native-access=ALL-UNNAMED" \
  -v /opt/orbien/orbien-server.toml:/app/orbien-server.toml:ro \
  -v /opt/orbien/data:/app/data \
  -v /opt/orbien/cert:/app/cert \
  -v /opt/orbien/config:/app/config \
  lxien/orbien-server:0.29.0
```

#### Docker Compose

```yaml
# docker-compose.yml
services:
  orbien-server:
    image: lxien/orbien-server:0.29.0
    container_name: orbien-server
    restart: unless-stopped
    ports:
      - "9527:9527"
      - "8020:8020"
      - "8080:8080"
      - "8443:8443"
      - "9050-9060:9050-9060"
      - "9050-9060:9050-9060/udp"
    environment:
      TZ: Asia/Shanghai
      SPRING_PROFILES_ACTIVE: h2
      H2_DATA_DIR: /app/data/orbien-server
      JAVA_OPTS: "-Xms512m -Xmx512m -XX:MaxDirectMemorySize=512m -XX:+UseG1GC --enable-native-access=ALL-UNNAMED"
    volumes:
      - /opt/orbien/orbien-server.toml:/app/orbien-server.toml:ro
      - /opt/orbien/data:/app/data
      - /opt/orbien/cert:/app/cert
      - /opt/orbien/config:/app/config
```

```shell
docker compose up -d
docker compose logs -f
```

### 使用 MySQL

先确保 MySQL 已启动，且服务端容器能访问到 `MYSQL_HOST`，然后：

```shell
docker run -d \
  --name orbien-server \
  --restart unless-stopped \
  -p 9527:9527 \
  -p 8020:8020 \
  -p 8080:8080 \
  -p 8443:8443 \
  -p 9050-9060:9050-9060 \
  -p 9050-9060:9050-9060/udp \
  -e TZ=Asia/Shanghai \
  -e SPRING_PROFILES_ACTIVE=mysql \
  -e MYSQL_HOST=10.0.0.10 \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DATABASE=orbien \
  -e MYSQL_USERNAME=orbien \
  -e MYSQL_PASSWORD='你的密码' \
  -e JAVA_OPTS="-Xms512m -Xmx512m -XX:MaxDirectMemorySize=512m -XX:+UseG1GC --enable-native-access=ALL-UNNAMED" \
  -v /opt/orbien/orbien-server.toml:/app/orbien-server.toml:ro \
  -v /opt/orbien/data:/app/data \
  -v /opt/orbien/cert:/app/cert \
  -v /opt/orbien/config:/app/config \
  lxien/orbien-server:0.29.0
```

#### Docker Compose

```yaml
# docker-compose.yml
services:
  orbien-server:
    image: lxien/orbien-server:0.29.0
    container_name: orbien-server
    restart: unless-stopped
    ports:
      - "9527:9527"
      - "8020:8020"
      - "8080:8080"
      - "8443:8443"
      - "9050-9060:9050-9060"
      - "9050-9060:9050-9060/udp"
    environment:
      TZ: Asia/Shanghai
      SPRING_PROFILES_ACTIVE: mysql
      MYSQL_HOST: 10.0.0.10
      MYSQL_PORT: 3306
      MYSQL_DATABASE: orbien
      MYSQL_USERNAME: orbien
      MYSQL_PASSWORD: 你的密码
      JAVA_OPTS: "-Xms512m -Xmx512m -XX:MaxDirectMemorySize=512m -XX:+UseG1GC --enable-native-access=ALL-UNNAMED"
    volumes:
      - /opt/orbien/orbien-server.toml:/app/orbien-server.toml:ro
      - /opt/orbien/data:/app/data
      - /opt/orbien/cert:/app/cert
      - /opt/orbien/config:/app/config
```

```shell
docker compose up -d
docker compose logs -f
```

看日志确认启动成功：

```shell
docker logs -f orbien-server
```

浏览器打开 `http://<公网IP>:8020`
