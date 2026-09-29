# FS-Monitor — Production Guide (RHEL9)

FS-Monitor is a Spring Boot + React application packaged as a single
executable JAR. The backend serves the frontend statically — no separate
web server is required.

## Requirements

- RHEL9 (or another systemd-based distro)
- Java 17+: `sudo dnf install java-17-openjdk-headless`
- A built `FS-Monitor.jar` (see `build.sh`)

## Installation (systemd)

```bash
# Build on your build machine first
./build.sh            # produces FS-Monitor.jar in the repository root

# Install on the server
sudo deploy/install.sh /path/to/FS-Monitor.jar
```

The installer creates:

| Path | Contents |
|---|---|
| `/opt/fs-monitor/fs-monitor.jar` | The application |
| `/etc/fs-monitor/fs-monitor.env` | Configuration (edit before first start) |
| `/var/lib/fs-monitor/` | H2 database + generated secrets |
| `/etc/systemd/system/fs-monitor.service` | Hardened systemd unit |

## Configuration

All settings live in `/etc/fs-monitor/fs-monitor.env`:

```ini
FS_MONITOR_ADMIN_PASSWORD=<choose a strong password>   # first admin password
FS_MONITOR_DATA_DIR=/var/lib/fs-monitor
SERVER_PORT=8085
SERVER_ADDRESS=0.0.0.0
CORS_ALLOWED_ORIGINS=http://your-server:8085
JAVA_OPTS=-Xms256m -Xmx512m
```

- `JWT_SECRET` and `FS_MONITOR_ENCRYPTION_KEY` may be left empty — they are
  generated automatically and stored with 0600 permissions in the data
  directory.
- `FSMONITOR_SFTP_KNOWNHOSTS` points to a `known_hosts` file; when present,
  SFTP checks run with strict host key verification.
- `FS_MONITOR_LOGIN_MAX_ATTEMPTS` / `FS_MONITOR_LOGIN_LOCKOUT_MINUTES`
  control login throttling (defaults: 5 failures → 15-minute lockout).
- `FS_MONITOR_TRUST_FORWARDED_HEADERS=true` should only be set when the app
  sits behind a reverse proxy that sanitizes `X-Forwarded-For`.
- The H2 console is **disabled** in the production profile.

## Operations

```bash
systemctl start fs-monitor
systemctl status fs-monitor
journalctl -u fs-monitor -f          # follow logs
systemctl restart fs-monitor
```

Health check: `curl http://localhost:8085/actuator/health`

### First login

Log in with `admin` + the password from `FS_MONITOR_ADMIN_PASSWORD`
(or `admin`/`password` if the variable was left empty — change it
immediately under Settings → Change password).

### Firewall

Only if the UI should be reachable from other hosts:

```bash
firewall-cmd --permanent --add-port=8085/tcp
firewall-cmd --reload
```

Recommendation: place the application behind a reverse proxy
(nginx/Apache) if it is exposed beyond the internal network.

## Backup & restore

All data lives in `FS_MONITOR_DATA_DIR` (default `/var/lib/fs-monitor`):

- `fsmonitor.mv.db` — the H2 database (configurations, users, statuses)
- `jwt.secret`, `encryption.key` — generated secrets

### Safe backup (recommended routine)

**Never** copy `fsmonitor.mv.db` while the service is running — a file copy
of a live database can be inconsistent. Stop the service first:

```bash
systemctl stop fs-monitor
install -d -m 750 -o fs-monitor -g fs-monitor /var/backups/fs-monitor
sudo -u fs-monitor tar czf \
  /var/backups/fs-monitor/fs-monitor-$(date +%F-%H%M).tar.gz \
  -C /var/lib fs-monitor
systemctl start fs-monitor
```

Schedule it as a cron job or systemd timer for daily runs, e.g.:

```bash
# /etc/cron.d/fs-monitor-backup
15 3 * * * root systemctl stop fs-monitor && \
  sudo -u fs-monitor tar czf /var/backups/fs-monitor/fs-monitor-$(date +\%F).tar.gz -C /var/lib fs-monitor; \
  systemctl start fs-monitor
```

Keep e.g. 14 days of copies locally and sync them to another host
(`rsync`, `scp`) — the database is usually only a few MB.

### Restore

```bash
systemctl stop fs-monitor
rm -rf /var/lib/fs-monitor
tar xzf /var/backups/fs-monitor/fs-monitor-YYYY-MM-DD.tar.gz -C /var/lib
chown -R fs-monitor:fs-monitor /var/lib/fs-monitor
restorecon -Rv /var/lib/fs-monitor   # if SELinux is enforcing
systemctl start fs-monitor
```

**Important:** always restore `encryption.key` together with the database —
without the key, stored service/mail credentials cannot be decrypted. If
`jwt.secret` is missing, a new one is generated and all existing sessions
are invalidated.

## Upgrading

```bash
systemctl stop fs-monitor
sudo install -m 644 /path/to/new.jar /opt/fs-monitor/fs-monitor.jar
systemctl start fs-monitor
```

## Without systemd

`run-production.sh` runs the JAR directly with the production profile:

```bash
SERVER_PORT=8085 FS_MONITOR_DATA_DIR=/var/lib/fs-monitor ./run-production.sh
```

## Security notes

- The API requires JWT authentication; user registration and the file
  browser are admin-only endpoints.
- Service/mail credentials are stored AES-256-GCM-encrypted in the
  database and are never exposed in API responses.
- Login attempts are rate-limited; changing a password invalidates all
  previously issued tokens.
- HTTP is used internally — front with a reverse proxy and TLS for
  external access.
