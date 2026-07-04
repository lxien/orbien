---
sidebar_position: 8
---

# IP访问控制

orbien 提供基于 **IP CIDR** 的访问控制功能，支持白名单和黑名单两种模式，可精确控制代理服务的访问权限。该功能适用于所有代理协议（HTTP
和 TCP）。

## 访问控制模式

### 1. 白名单模式 (allow)

只允许 `allow` 列表中的 IP 或 IP 段访问代理服务，拒绝所有其他来源的请求。**仅使用 `allow` 列表**。

### 2. 黑名单模式 (deny)

拒绝 `deny` 列表中的 IP 或 IP 段访问代理服务，允许所有其他来源的请求。**仅使用 `deny` 列表**。

## 配置示例

### 示例 1：白名单模式 - 只允许特定 IP 段访问

```toml
[[proxies]]
name = "web"
protocol = "http"
custom_domains = ["a.domain.com"]
targets = [
    { host = "127.0.0.1", port = 8001 }
]

[proxies.access_control]
enabled = true
mode = "allow"
allow = ["192.168.1.0/24", "10.0.0.0/8"]
```

### 示例 2：白名单模式 - 只允许单个 IP 访问

```toml
[[proxies]]
name = "database"
protocol = "tcp"
remote_port = 3307
targets = [
    { host = "127.0.0.1", port = 3306 }
]

[proxies.access_control]
enabled = true
mode = "allow"
allow = ["192.168.1.100", "192.168.1.101"]
```

### 示例 3：黑名单模式 - 拒绝特定 IP 访问

```toml
[[proxies]]
name = "api"
protocol = "http"
custom_domains = ["api.domain.com"]
targets = [
    { host = "127.0.0.1", port = 8080 }
]

[proxies.access_control]
enabled = true
mode = "deny"
deny = ["10.0.0.5", "172.16.0.0/16"]
```

## 参数说明

| 参数名     | 类型                  | 默认值   | 描述                                | 必填    |
|:--------|:--------------------|:------|:----------------------------------|:------|
| enabled | Boolean             | false | 是否启用访问控制                          | 是     |
| mode    | String              | -     | 控制模式，值为 "allow"（白名单）或 "deny"（黑名单） | 启用时必填 |
| allow   | Array&lt;String&gt; | []    | 允许访问的 IP/CIDR 列表，白名单模式下使用         | 否     |
| deny    | Array&lt;String&gt; | []    | 拒绝访问的 IP/CIDR 列表，黑名单模式下使用         | 否     |

## IP 格式支持

### 单个 IP 地址

```toml
allow = ["192.168.1.100", "10.0.0.5"]
```

### CIDR 格式

```toml
allow = ["192.168.1.0/24", "10.0.0.0/8", "172.16.0.0/12"]
```

### 单个 IP 的 CIDR 表示法

单个 IP 也可以使用 `/32` 后缀表示：

```toml
allow = ["192.168.1.100/32", "10.0.0.5/32"]
```

### 常见 CIDR 范围

| CIDR             | 说明                          | 数量        |
|:-----------------|:----------------------------|:-----------|
| 192.168.1.0/24   | 192.168.1.1 ~ 192.168.1.254 | 254 个     |
| 10.0.0.0/8       | 10.0.0.1 ~ 10.255.255.254   | ~1677万    |
| 172.16.0.0/12    | 172.16.0.1 ~ 172.31.255.254 | ~104万     |
| 0.0.0.0/0        | 所有 IP 地址                    | ~42.9亿    |
| 192.168.1.100/32 | 仅单个 IP 192.168.1.100        | 1 个       |

## 常见用法

### 场景 1：限制内网访问

```toml
[proxies.access_control]
enabled = true
mode = "allow"
allow = ["192.168.0.0/16", "10.0.0.0/8"]
```

### 场景 2：阻止恶意 IP

```toml
[proxies.access_control]
enabled = true
mode = "deny"
deny = ["118.190.245.213", "47.91.176.194"]
```

### 场景 3：仅允许特定 IP 段访问

```toml
[proxies.access_control]
enabled = true
mode = "allow"
allow = ["203.0.113.0/24", "198.51.100.0/24"]
```