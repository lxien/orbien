---
sidebar_position: 1
---

# TLS 加密

orbien 支持 TLS 传输加密，保证代理服务端与客户端之间安全传输。

- **TCP / WebSocket**：JDK 11+ 优先协商 **TLS 1.3**，对端不支持时自动降级 **TLS 1.2**；JDK 8 仅 TLS 1.2。
- **QUIC**：规范要求内嵌 **TLS 1.3**（不可降级)。

:::warning
未配置自定义证书时，系统**默认采用自签名证书**，生产环境请使用正式证书。
:::

## 配置位置

### 单向 TLS（服务端）

**服务端 orbiens.toml：**

```toml
[transport.tls]
enabled = true
cert_file = "cert/transport/server.crt"
key_file = "cert/transport/server.key"
```

客户端可不配置 `[transport.tls]`（单向信任服务端）。

### 双向 TLS / mTLS

**服务端：**

```toml
[transport.tls]
enabled = true
cert_file = "cert/transport/server.crt"
key_file = "cert/transport/server.key"
ca_file = "cert/transport/ca.crt"
```

**客户端：**

```toml
[transport.tls]
enabled = true
cert_file = "cert/transport/client.crt"
key_file = "cert/transport/client.key"
ca_file = "cert/transport/ca.crt"
```

## 参数说明

| 参数名       | 类型      | 默认值  | 描述               | 必填    |
|:----------|:--------|:-----|:-----------------|:------|
| enabled   | Boolean | true | 是否启用 TLS         | 否     |
| cert_file | String  | -    | 证书 PEM 路径        | 启用时必填 |
| key_file  | String  | -    | 私钥 PEM 路径        | 启用时必填 |
| ca_file   | String  | -    | CA 路径；配置后启用 mTLS | 否     |
| key_pass  | String  | -    | 私钥密码             | 否     |
