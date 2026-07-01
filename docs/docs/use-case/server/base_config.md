---
sidebar_position: 1
---

# 基础配置

服务端基础配置包含服务监听地址、端口、HTTP/HTTPS代理端口、根域名等核心参数。

## 配置示例

```toml
# 传输隧道监听地址
server_addr = "0.0.0.0"
# 传输隧道端口
server_port = 9527
# HTTP 代理端口
http_proxy_port = 80
# 根域名（用于自动分配子域名）
root_domain = "domain.com"
```

## 参数说明

| 参数名             | 类型      | 默认值     | 描述                             | 必填 |
|:----------------|:--------|:--------|:-------------------------------|:---|
| server_addr     | String  | 0.0.0.0 | 服务端传输隧道监听地址，通常为 0.0.0.0 或特定 IP | 否  |
| server_port     | Integer | 9527    | 服务端传输隧道端口，端口范围 1-65535         | 否  |
| http_proxy_port | Integer | 80      | HTTP 代理端口，用于 HTTP 协议穿透         | 否  |
| root_domain     | String  | -       | 根域名，配置后可自动分配子域名给客户端           | 否  |

## 说明

- **server_addr**：监听地址建议配置为 0.0.0.0，允许所有网络接口访问
- **root_domain**：配置根域名后，客户端可使用子域名进行访问，例如 a.domain1.com