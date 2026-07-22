#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT_DIR}"
mvn clean deploy -pl core,common,client -am

cd "${ROOT_DIR}/orbien-spring-boot-starter"
mvn clean deploy
