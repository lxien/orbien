---
sidebar_position: 1
title: CLI 命令行
description: 用 orbien 登录、暴露 HTTP / TCP / UDP
---

# CLI 命令行

:::info Agent 模式
命令行用于快速将服务暴露到公网，提供了比较简洁的操作，采用的是**SESSION**类型客户端，意味着一旦客户端停止，服务端就会清理所有相关数据，
不会将数据持久化，如果需要持久化配置信息或使用更多高级特性以及长期运行，建议采用**配置文件**的方式启动。
:::

## 安装

从 [Releases](https://github.com/lxien/orbien/tags) 下载对应平台的 `orbien`

```js
chmod +x orbien
```

## 命令一览

```js
./orbien -h
```

```js
Usage: orbien [-hV] [-c=<configFile>] [COMMAND]
Orbien 隧道客户端
  -c=<configFile>    配置文件路径
  -h, --help         Show this help message and exit.
  -V, --version      Print version information and exit.
Commands:
  login   保存服务端凭据
  logout  清除本地凭据
  run     根据配置文件启动客户端
  http    启动 HTTP 代理
  tcp     启动 TCP 代理
  udp     启动 UDP 代理
```

查看某个命令具体用法：

```js
./orbien <command> -h
```

## 使用案例

```js
orbien login --server <server-addr>:9527 --token YOUR_TOKEN
```

(1) 将web应用暴露到公网

```js
./orbien http 8080 # 自动生成子域名

./orbien http 8080 --domain web # 自定义子域名前缀 web.domain.com
```

(2) 将MySQL暴露到公网

```js
./orbien tcp 3306
```

