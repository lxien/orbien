#!/usr/bin/env bash
set -euo pipefail

ORBIEN_VERSION="latest"
ORBIEN_IMAGE="lxien/orbien-server:${ORBIEN_VERSION}"

if ! command -v docker >/dev/null 2>&1; then
  echo "error: docker is required" >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "error: cannot connect to docker daemon" >&2
  exit 1
fi

if [ -z "${ORBIEN_HOME:-}" ]; then
  if [ "$(uname -s)" = "Darwin" ]; then
    if [ -n "${SUDO_USER:-}" ]; then
      ORBIEN_HOME="$(eval echo "~${SUDO_USER}")/.orbien"
    else
      ORBIEN_HOME="${HOME}/.orbien"
    fi
  else
    ORBIEN_HOME="/opt/orbien"
  fi
fi

mkdir -p "${ORBIEN_HOME}/data" "${ORBIEN_HOME}/logs"

if [ ! -f "${ORBIEN_HOME}/orbien-server.toml" ]; then
  cat > "${ORBIEN_HOME}/orbien-server.toml" <<'EOF'
server_addr = "0.0.0.0"
server_port = 9527
http_proxy_port = 8080
https_proxy_port = 8443

[dashboard]
enabled = true
addr = "0.0.0.0"
port = 8020
username = "admin"
password = "123456"

[[port_pool.tcp]]
start = 9050
end = 9099

[[port_pool.udp]]
start = 9050
end = 9099
EOF
fi

docker pull "${ORBIEN_IMAGE}"

if ! docker inspect orbien-server >/dev/null 2>&1; then
  docker run -d \
    --name orbien-server \
    --restart unless-stopped \
    -p 8080:8080 \
    -p 8443:8443 \
    -p 8020:8020 \
    -p 9527:9527 \
    -p 9050-9099:9050-9099 \
    -p 9050-9099:9050-9099/udp \
    -e SPRING_PROFILES_ACTIVE=h2 \
    -e H2_DATA_DIR=/app/data/orbien-server \
    -e JAVA_OPTS="-Xms512m -Xmx512m -XX:MaxDirectMemorySize=512m -XX:+UseG1GC --enable-native-access=ALL-UNNAMED" \
    -e TZ=Asia/Shanghai \
    -v "${ORBIEN_HOME}/orbien-server.toml:/app/orbien-server.toml:ro" \
    -v "${ORBIEN_HOME}/data:/app/data" \
    -v "${ORBIEN_HOME}/logs:/app/logs" \
    "${ORBIEN_IMAGE}" >/dev/null
fi

cat <<EOF
orbien-server ${ORBIEN_VERSION} started

  home:      ${ORBIEN_HOME}
  dashboard: http://<host>:8020  (admin / 123456)
  tunnel:    9527
  http:      8080
  https:     8443
  tcp pool:  9050-9099
  udp pool:  9050-9099
EOF
