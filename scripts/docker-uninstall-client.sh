#!/usr/bin/env bash
set -euo pipefail

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

docker rm -f orbien >/dev/null 2>&1 || true
rm -f "${ORBIEN_HOME}/orbien.toml"

echo "orbien client uninstalled"
