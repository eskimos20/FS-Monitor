# FS-Monitor Production Deployment

## Quick Start

### Running the Application

After building with `./build.sh`, you have two options:

**Option 1: Use the production runner script (Recommended)**
```bash
./run-production.sh
```

**Option 2: Run JAR directly with custom port**
```bash
java -jar FS-Monitor.jar --server.port=8085
```

**Option 3: Run JAR with default port (8080)**
```bash
java -jar FS-Monitor.jar
```

## Configuration

The application uses the following ports by default:
- **Backend**: 8085 (configurable)
- **Frontend**: Served as static files from the JAR on the same port

### Custom Configuration

You can override any Spring Boot property at runtime:

```bash
java -jar FS-Monitor.jar \
    --server.port=8085 \
    --spring.datasource.url=jdbc:h2:file:/custom/path/data/fsmonitor \
    --spring.mail.host=smtp.example.com
```

### Environment Variables

You can also use environment variables:

```bash
export SERVER_PORT=8085
export SPRING_DATASOURCE_URL=jdbc:h2:file:/custom/path/data/fsmonitor
java -jar FS-Monitor.jar
```

## Database

The application uses H2 database by default. Data is stored in:
- `./data/fsmonitor.mv.db` (relative to where you run the JAR)

### H2 Console

Access the H2 console at: `http://localhost:8085/h2-console`
- **JDBC URL**: `jdbc:h2:file:./data/fsmonitor`
- **Username**: `sa`
- **Password**: (empty)

## Default Credentials

- **Username**: `admin`
- **Password**: `password`

**⚠️ IMPORTANT**: Change the default password after first login!

## Accessing the Application

Once started, access the application at:
- `http://localhost:8085` (or your configured port)

## Stopping the Application

Press `Ctrl+C` in the terminal where the application is running.

## Running as a Service (systemd)

Create a systemd service file `/etc/systemd/system/fs-monitor.service`:

```ini
[Unit]
Description=FS-Monitor Application
After=network.target

[Service]
Type=simple
User=your-username
WorkingDirectory=/path/to/fs-monitor
ExecStart=/usr/bin/java -jar /path/to/fs-monitor/FS-Monitor.jar --server.port=8085
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start the service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable fs-monitor
sudo systemctl start fs-monitor
sudo systemctl status fs-monitor
```

## Logs

View application logs:
```bash
# If running as systemd service
sudo journalctl -u fs-monitor -f

# If running in terminal, logs are printed to stdout
```

## Troubleshooting

### Port Already in Use
If port 8085 is already in use, either:
1. Stop the service using that port
2. Use a different port: `java -jar FS-Monitor.jar --server.port=9090`

### Database Issues
If you encounter database issues, you can reset the database by:
1. Stop the application
2. Delete `./data/fsmonitor.mv.db`
3. Restart the application (it will create a new database)

### Memory Issues
If the application runs out of memory, increase the heap size:
```bash
java -Xmx1024m -jar FS-Monitor.jar --server.port=8085
```

## Version Information

The application version is displayed in the top-left corner of the web interface.
Current version is defined in `backend/pom.xml`.
