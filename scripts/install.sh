#!/bin/bash

set -e

ORBIENCS_IMAGE="lxien/orbiens:0.10.0"
ORBIENS_VERSION="0.10.0"

if [ "$(uname -s)" = "Darwin" ]; then
    ORBIENS_HOME="$HOME/.orbiens"
else
    ORBIENS_HOME="/opt/orbiens"
fi

docker network create orbiens-net >/dev/null 2>&1 || true

docker run -d \
  --name orbiens-mysql \
  --network orbiens-net \
  --restart unless-stopped \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=orbiens \
  -v "$ORBIENS_HOME/mysql/data:/var/lib/mysql" \
  mysql:8.4

docker run -d \
  --name orbiens \
  --network orbiens-net \
  --restart unless-stopped \
  -p 8080:8080 \
  -p 8020:8020 \
  -p 9527:9527 \
  -p 9050-9099:9050-9099 \
  -e MYSQL_HOST=orbiens-mysql \
  -e MYSQL_DATABASE=orbiens \
  -e MYSQL_PASSWORD=123456 \
  -e JAVA_OPTS="-Xms256m -Xmx256m -XX:MaxDirectMemorySize=512m -XX:+UseG1GC" \
  -e TZ=Asia/Shanghai \
  -v "$ORBIENS_HOME/logs:/app/logs" \
  "$ORBIENS_IMAGE"

echo "ORBIENS ${ORBIENS_VERSION} 部署完成"