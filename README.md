# FS-Monitor

A self-hosted server monitoring application for Linux (built for RHEL9),
packaged as a single executable JAR. FS-Monitor watches directories,
services and log files, monitors system resources and scheduled file
cleanup — all through a clean web UI served directly by the backend.

## Features

- **File integrations** — watch directories for new files (by type),
  with thresholds and schedules, and get alerted when nothing arrives
- **Service monitoring** — ping, port, HTTP(S), SMB and SFTP checks with
  optional credentials and strict `known_hosts` host-key verification
- **Log monitoring** — keyword search across log files with context,
  bounded scans and persistent results
- **Storage monitoring** — system-wide disk usage, largest files and
  per-directory breakdowns
- **Delete services** — scheduled cleanup of files older than a
  configurable age, with recursive deletion, file-type filters and
  protected-path safeguards
- **Email notifications** — SMTP alerts on threshold breaches and matches
- **Security** — JWT authentication, admin/reader roles, login rate
  limiting, encrypted credentials at rest (AES-256-GCM), audit logging

## Tech stack

| Layer    | Technology |
|----------|------------|
| Backend  | Java 17, Spring Boot, Spring Security (JWT), Spring Data JPA |
| Database | Embedded file-based H2 — zero external dependencies |
| Frontend | React, Vite, Tailwind CSS, Headless UI, Recharts |
| Deploy   | systemd on RHEL9 (see `deploy/` and `PRODUCTION.md`) |

## Quick start (development)

```bash
# Terminal 1 — backend API on :8089
cd backend && mvn spring-boot:run

# Terminal 2 — frontend dev server on :8088
cd frontend && npm install && npm run dev
```

## Building a production JAR

```bash
./build.sh
```

This builds the frontend, embeds it in the backend's static resources and
produces `FS-Monitor.jar` in the repository root. Run it with:

```bash
java -jar FS-Monitor.jar --spring.profiles.active=production
```

On first start an `admin` account is created — set
`FS_MONITOR_ADMIN_PASSWORD` beforehand, or log in with `admin`/`password`
and change it immediately. All data (H2 database + generated secrets) is
stored under `FS_MONITOR_DATA_DIR` (default `./data`).

## Releases

GitHub Actions builds the JAR automatically: create a release (or push a
`v*` tag) and the workflow compiles the frontend, packages the backend
with tests and attaches the JAR to the release.

## Production deployment

See [PRODUCTION.md](PRODUCTION.md) for the full RHEL9 guide: systemd
installation (`deploy/install.sh`), configuration, firewall, backup &
restore and upgrades.

## Tests

```bash
cd backend && mvn test
```

Covers scheduling logic, filesystem safety guards, credential encryption,
log searching and the full API authorization matrix (401/403/admin).
