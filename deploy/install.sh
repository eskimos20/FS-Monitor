#!/bin/bash
#
# FS-Monitor installer for RHEL9 (and other systemd-based distros)
# Usage: sudo ./install.sh [/path/to/fs-monitor.jar]
#
set -euo pipefail

APP_NAME=fs-monitor
APP_USER=fsmonitor
APP_GROUP=fsmonitor
INSTALL_DIR=/opt/fs-monitor
DATA_DIR=/var/lib/fs-monitor
CONFIG_DIR=/etc/fs-monitor

JAR_SOURCE="${1:-./FS-Monitor.jar}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ $EUID -ne 0 ]]; then
    echo "Error: this installer must be run as root (sudo)." >&2
    exit 1
fi

if [[ ! -f "$JAR_SOURCE" ]]; then
    echo "Error: JAR file not found: $JAR_SOURCE" >&2
    echo "Run build.sh first, or pass the jar path: sudo ./install.sh /path/to/fs-monitor.jar" >&2
    exit 1
fi

if ! command -v java >/dev/null 2>&1; then
    echo "Error: Java is not installed. On RHEL9: dnf install java-17-openjdk-headless" >&2
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [[ "${JAVA_VERSION:-0}" -lt 17 ]]; then
    echo "Error: Java 17 or newer is required (found ${JAVA_VERSION})." >&2
    exit 1
fi

echo "Installing FS-Monitor..."

# 1. Service user (system account, no login)
if ! id "$APP_USER" >/dev/null 2>&1; then
    useradd --system --no-create-home --shell /sbin/nologin "$APP_USER"
    echo "  Created user: $APP_USER"
fi

# 2. Directories
mkdir -p "$INSTALL_DIR" "$DATA_DIR" "$CONFIG_DIR"
chown "$APP_USER:$APP_GROUP" "$DATA_DIR"
chmod 750 "$DATA_DIR"

# 3. Application JAR
install -m 644 -o root -g root "$JAR_SOURCE" "$INSTALL_DIR/fs-monitor.jar"
echo "  Installed $INSTALL_DIR/fs-monitor.jar"

# 4. Environment file (only on first install - never overwrite existing config)
if [[ ! -f "$CONFIG_DIR/fs-monitor.env" ]]; then
    install -m 640 -o root -g "$APP_GROUP" "$SCRIPT_DIR/fs-monitor.env.example" "$CONFIG_DIR/fs-monitor.env"
    echo "  Installed $CONFIG_DIR/fs-monitor.env - EDIT THIS FILE before starting"
else
    echo "  Keeping existing $CONFIG_DIR/fs-monitor.env"
fi

# 5. systemd unit
install -m 644 -o root -g root "$SCRIPT_DIR/fs-monitor.service" "/etc/systemd/system/$APP_NAME.service"
systemctl daemon-reload
systemctl enable "$APP_NAME.service"
echo "  Installed and enabled $APP_NAME.service"

cat <<'EOF'

Done. Next steps:

  1. Edit /etc/fs-monitor/fs-monitor.env
     (set FS_MONITOR_ADMIN_PASSWORD and CORS_ALLOWED_ORIGINS)
  2. Start:    systemctl start fs-monitor
  3. Verify:   systemctl status fs-monitor
               curl http://localhost:8085/api/version
               curl http://localhost:8085/actuator/health
  4. Logs:     journalctl -u fs-monitor -f

Firewall (firewalld) - only if accessed from other hosts:
  firewall-cmd --permanent --add-port=8085/tcp && firewall-cmd --reload

Backup: the H2 database and generated secrets live in /var/lib/fs-monitor -
stop the service and copy that directory (or use it in your backup routine).
EOF
