#!/bin/bash
#
# FS-Monitor production runner (without systemd)
# For a permanent installation use deploy/install.sh instead.
#
set -euo pipefail

JAR_FILE="${1:-FS-Monitor.jar}"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: $JAR_FILE not found. Run build.sh first."
    exit 1
fi

# Defaults (all overridable via environment)
export SERVER_PORT="${SERVER_PORT:-8085}"
export SERVER_ADDRESS="${SERVER_ADDRESS:-0.0.0.0}"
export FS_MONITOR_DATA_DIR="${FS_MONITOR_DATA_DIR:-./data}"
export JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"

echo "Starting FS-Monitor (production profile)"
echo "  Port:     $SERVER_PORT"
echo "  Address:  $SERVER_ADDRESS"
echo "  Data dir: $FS_MONITOR_DATA_DIR"

# shellcheck disable=SC2086
exec java $JAVA_OPTS -jar "$JAR_FILE" --spring.profiles.active=production
