#!/bin/bash

set -e

ORBIEN_SERVER_IMAGE="lxien/orbien-server:0.20.0"
ORBIEN_SERVER_VERSION="0.20.0"

if [ "$(uname -s)" = "Darwin" ]; then
    ORBIEN_HOME="$HOME/.orbien"
else
    ORBIEN_HOME="/opt/orbien"
fi

docker network create orbien-net >/dev/null 2>&1 || true

docker run -d \
  --name orbien-mysql \
  --network orbien-net \
  --restart unless-stopped \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=orbien \
  -v "$ORBIEN_HOME/mysql/data:/var/lib/mysql" \
  mysql:8.4

docker run -d \
  --name orbien-server \
  --network orbien-net \
  --restart unless-stopped \
  -p 8080:8080 \
  -p 8020:8020 \
  -p 9527:9527 \
  -p 9050-9099:9050-9099 \
  -e MYSQL_HOST=orbien-mysql \
  -e MYSQL_DATABASE=orbien \
  -e MYSQL_PASSWORD=123456 \
  -e JAVA_OPTS="-Xms256m -Xmx256m -XX:MaxDirectMemorySize=512m -XX:+UseG1GC --enable-native-access=ALL-UNNAMED" \
  -e TZ=Asia/Shanghai \
  -v "$ORBIEN_HOME/logs:/app/logs" \
  "$ORBIEN_SERVER_IMAGE"

echo "orbien-server ${ORBIEN_SERVER_VERSION} 部署完成"
echo "管理面板: http://服务器IP:8020 (默认账号见配置)"
