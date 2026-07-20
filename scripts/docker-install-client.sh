#!/usr/bin/env bash
set -euo pipefail

ORBIEN_VERSION="latest"
ORBIEN_IMAGE="lxien/orbien:${ORBIEN_VERSION}"

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

mkdir -p "${ORBIEN_HOME}/logs"

if [ ! -f "${ORBIEN_HOME}/orbien.toml" ]; then
  cat > "${ORBIEN_HOME}/orbien.toml" <<'EOF'
server_addr = "127.0.0.1"
server_port = 9527

[auth]
token = "token"

#[[proxies]]
#name = "MySQL"
#protocol = "tcp"
#local_ip="127.0.0.1"
#local_port = 3306
EOF
fi

docker pull "${ORBIEN_IMAGE}"

if ! docker inspect orbien >/dev/null 2>&1; then
  docker run -d \
    --name orbien \
    --restart unless-stopped \
    --network host \
    -e TZ=Asia/Shanghai \
    -v "${ORBIEN_HOME}/orbien.toml:/app/orbien.toml:ro" \
    -v "${ORBIEN_HOME}/logs:/app/logs" \
    "${ORBIEN_IMAGE}" >/dev/null
fi

cat <<EOF
orbien ${ORBIEN_VERSION} installed

  home:   ${ORBIEN_HOME}
  config: ${ORBIEN_HOME}/orbien.toml
  logs:   ${ORBIEN_HOME}/logs
EOF
