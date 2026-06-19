#!/bin/bash

set -e

ETPS_IMAGE="xiaoniucode/etps:0.10.0"
ETPS_VERSION="0.10.0"

if [ "$(uname -s)" = "Darwin" ]; then
    ETPS_HOME="$HOME/.etps"
else
    ETPS_HOME="/opt/etps"
fi

docker network create etps-net >/dev/null 2>&1 || true

docker run -d \
  --name etps-mysql \
  --network etps-net \
  --restart unless-stopped \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=etps \
  -v "$ETPS_HOME/mysql/data:/var/lib/mysql" \
  mysql:8.4

docker run -d \
  --name etps \
  --network etps-net \
  --restart unless-stopped \
  -p 8080:8080 \
  -p 8020:8020 \
  -p 9527:9527 \
  -p 9050-9099:9050-9099 \
  -e MYSQL_HOST=etps-mysql \
  -e MYSQL_DATABASE=etps \
  -e MYSQL_PASSWORD=123456 \
  -e JAVA_OPTS="-Xms256m -Xmx256m -XX:MaxDirectMemorySize=512m -XX:+UseG1GC" \
  -e TZ=Asia/Shanghai \
  -v "$ETPS_HOME/logs:/app/logs" \
  "$ETPS_IMAGE"

echo "ETPS ${ETPS_VERSION} 部署完成"