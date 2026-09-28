# FS-Monitor — Produktionsguide (RHEL9)

FS-Monitor är en Spring Boot + React-applikation som paketeras till en enda
körbar JAR. Backend serverar frontendstatiken internt — ingen separat
webbserver krävs.

## Krav

- RHEL9 (eller annan systemd-dist)
- Java 17+: `sudo dnf install java-17-openjdk-headless`
- En byggd `FS-Monitor.jar` (se `build.sh`)

## Installation (systemd)

```bash
# Bygg först på din byggmaskin
./build.sh            # producerar FS-Monitor.jar i repo-roten

# Installera på servern
sudo deploy/install.sh /path/to/FS-Monitor.jar
```

Installationsprogrammet skapar:

| Sökväg | Innehåll |
|---|---|
| `/opt/fs-monitor/fs-monitor.jar` | Applikationen |
| `/etc/fs-monitor/fs-monitor.env` | Konfiguration (redigera innan start) |
| `/var/lib/fs-monitor/` | H2-databas + genererade hemligheter |
| `/etc/systemd/system/fs-monitor.service` | Hårdnad systemd-enhet |

## Konfiguration

Alla inställningar görs i `/etc/fs-monitor/fs-monitor.env`:

```ini
FS_MONITOR_ADMIN_PASSWORD=<välj starkt lösenord>   # första admin-lösenordet
FS_MONITOR_DATA_DIR=/var/lib/fs-monitor
SERVER_PORT=8085
SERVER_ADDRESS=0.0.0.0
CORS_ALLOWED_ORIGINS=http://servern:8085
JAVA_OPTS=-Xms256m -Xmx512m
```

- `JWT_SECRET` och `FS_MONITOR_ENCRYPTION_KEY` kan lämnas tomma — de
  genereras automatiskt och lagras med 0600-rättigheter i datakatalogen.
- `FSMONITOR_SFTP_KNOWNHOSTS` pekar på en `known_hosts`-fil; när den finns
  körs SFTP-kontroller med strikt host key-verifiering.
- H2-console är **avstängd** i produktionsprofilen.

## Drift

```bash
systemctl start fs-monitor
systemctl status fs-monitor
journalctl -u fs-monitor -f          # följ loggar
systemctl restart fs-monitor
```

Hälsokontroll: `curl http://localhost:8085/actuator/health`

### Första inloggningen

Logga in med `admin` + lösenordet i `FS_MONITOR_ADMIN_PASSWORD`
(eller `admin`/`password` om fältet lämnats tomt — byt omedelbart under
Inställningar → Byt lösenord).

### Brandvägg

Endast om UI:t ska nås från andra värdar:

```bash
firewall-cmd --permanent --add-port=8085/tcp
firewall-cmd --reload
```

Rekommendation: sätt applikationen bakom en reverse proxy (nginx/Apache)
om den ska exponeras utanför det interna nätet.

## Backup & återställning

All data finns i `FS_MONITOR_DATA_DIR` (standard `/var/lib/fs-monitor`):

- `fsmonitor.mv.db` — H2-databasen (konfigurationer, användare, statusar)
- `jwt.secret`, `encryption.key` — genererade hemligheter

### Säker backup (rekommenderad rutin)

Kopiera **aldrig** `fsmonitor.mv.db` medan tjänsten körs — en filkopia av en
live-databas kan vara inkonsekvent. Stoppa tjänsten först:

```bash
systemctl stop fs-monitor
install -d -m 750 -o fs-monitor -g fs-monitor /var/backups/fs-monitor
sudo -u fs-monitor tar czf \
  /var/backups/fs-monitor/fs-monitor-$(date +%F-%H%M).tar.gz \
  -C /var/lib fs-monitor
systemctl start fs-monitor
```

Lägg in som ett cron-jobb eller systemd-timer för daglig körning, t.ex.:

```bash
# /etc/cron.d/fs-monitor-backup
15 3 * * * root systemctl stop fs-monitor && \
  sudo -u fs-monitor tar czf /var/backups/fs-monitor/fs-monitor-$(date +\%F).tar.gz -C /var/lib fs-monitor; \
  systemctl start fs-monitor
```

Behåll t.ex. 14 dygns kopior lokalt och synka dem till en annan host
(`rsync`, `scp`) — databasen är ofta bara några MB.

### Återställning

```bash
systemctl stop fs-monitor
rm -rf /var/lib/fs-monitor
tar xzf /var/backups/fs-monitor/fs-monitor-YYYY-MM-DD.tar.gz -C /var/lib
chown -R fs-monitor:fs-monitor /var/lib/fs-monitor
restorecon -Rv /var/lib/fs-monitor   # om SELinux enforcing
systemctl start fs-monitor
```

**Viktigt:** återställ alltid `encryption.key` tillsammans med databasen —
utan nyckeln kan lagrade tjänste-/mail-credentials inte dekrypteras. Saknas
`jwt.secret` genereras en ny och alla befintliga sessioner ogiltigförklaras.

## Uppgradering

```bash
systemctl stop fs-monitor
sudo install -m 644 /path/to/new.jar /opt/fs-monitor/fs-monitor.jar
systemctl start fs-monitor
```

## Utan systemd

`run-production.sh` kör JAR-filen direkt med produktionsprofilen:

```bash
SERVER_PORT=8085 FS_MONITOR_DATA_DIR=/var/lib/fs-monitor ./run-production.sh
```

## Säkerhetsanteckningar

- API:t kräver JWT-autentisering; registrering och filläsaren är
  admin-endpoints.
- Tjänste-/mail-credentials lagras AES-256-GCM-krypterade i databasen och
  exponeras aldrig i API-svar.
- HTTP används internt — använd reverse proxy med TLS för extern åtkomst.
