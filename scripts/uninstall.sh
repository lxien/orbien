#!/bin/bash

set -e

if [ "$(uname -s)" = "Darwin" ]; then
    ORBIEN_HOME="$HOME/.orbien"
else
    ORBIEN_HOME="/opt/orbien"
fi

docker rm -f orbien-server >/dev/null 2>&1 || true
docker rm -f orbien-mysql >/dev/null 2>&1 || true
docker network rm orbien-net >/dev/null 2>&1 || true
rm -rf "$ORBIEN_HOME"
docker container prune -f

echo "orbien-server 已卸载"
