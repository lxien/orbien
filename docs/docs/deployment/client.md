---
sidebar_position: 3
title: 客户端部署
description: 客户端部署
---

# 客户端部署

## 配置文件

```toml
# orbien.toml
server_addr = "server_addr"
server_port = 9527

[auth]
token = "token"
```

## 使用 Docker 安装

```shell
mkdir -p /opt/orbien/{logs,.orbien}

docker run -d \
  --name orbien \
  --restart unless-stopped \
  -v /opt/orbien/orbien.toml:/app/orbien.toml:ro \
  -v /opt/orbien/logs:/app/logs \
  -v /opt/orbien/.orbien:/root/.orbien \
  lxien/orbien:0.29.0
```

### Docker Compose

```yaml
# docker-compose.yml
services:
  orbien:
    image: lxien/orbien:0.29.0
    container_name: orbien
    restart: unless-stopped
    volumes:
      - /opt/orbien/orbien.toml:/app/orbien.toml:ro
      - /opt/orbien/logs:/app/logs
      - /opt/orbien/.orbien:/root/.orbien
```

```shell
docker compose up -d
docker compose logs -f
docker compose restart
```

`.orbien` 必须挂载，否则容器重建会丢身份。

```shell
docker logs -f orbien
docker restart orbien
```

## 使用 systemd 安装

### 下载二进制文件

将二进制文件放到需要安装的路径

```shell
chmod +x /path/to/orbien/orbien
```

### 编写服务

```shell
cat >/etc/systemd/system/orbien.service <<'EOF'
[Unit]
Description=Orbien
After=network.target

[Service]
WorkingDirectory=/path/to/orbien
ExecStart=/path/to/orbien/orbien -c /path/to/orbien/orbien.toml
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

### 启动服务

```shell
systemctl daemon-reload
systemctl enable --now orbien
systemctl status orbien
```

改配置后执行：`systemctl restart orbien`

:::info
Linux 二进制需要 `glibc >= 2.34`，过旧系统请用 Docker。
:::
