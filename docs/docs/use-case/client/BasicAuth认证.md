---
sidebar_position: 4
---

# BasicAuth 认证（HTTP）

BasicAuth 认证是一种简单的 HTTP 身份验证方式，通过在 HTTP 请求头中携带用户名和密码进行身份验证。orbien 支持为每个 HTTP
代理配置独立的 BasicAuth 认证，仅对 HTTP 协议代理生效。

## 配置示例

```toml
[[proxies]]
name = "web"
protocol = "http"
custom_domains = ["a.domain.com", "b.domain.com"]
targets = [
    { host = "127.0.0.1", port = 8001, weight = 1, name = "web-instance-1" }
]

[proxies.basic_auth]
enabled = true
users = [
    { user = "user1", pass = "123456" },
    { user = "user2", pass = "123456" }
]
```

## 参数说明

### basic_auth 配置

| 参数名     | 类型                  | 默认值   | 描述                | 必填    |
|:--------|:--------------------|:------|:------------------|:------|
| enabled | Boolean             | false | 是否启用 BasicAuth 认证 | 是     |
| users   | Array&lt;Object&gt; | -     | 用户列表，支持多用户        | 启用时必填 |

### users 子参数说明

| 参数名  | 类型     | 默认值 | 描述           | 必填 |
|:-----|:-------|:----|:-------------|:---|
| user | String | -   | 用户名          | 是  |
| pass | String | -   | 用户密码，密码会加密存储 | 是  |

## 使用说明


启用 BasicAuth 后，首次访问代理服务时，浏览器会弹出一个对话框，需要输入用户名和密码。
