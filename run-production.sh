#!/bin/bash

# FS-Monitor Production Runner
# This script runs the JAR file with production settings

echo "🚀 Starting FS-Monitor in production mode..."
echo "=========================================="

# Check if JAR file exists
if [ ! -f "FS-Monitor.jar" ]; then
    echo "❌ Error: FS-Monitor.jar not found!"
    echo "Please run build.sh first to create the JAR file."
    exit 1
fi

# Set production port (from config.json)
BACKEND_PORT=8085

echo "📋 Configuration:"
echo "   Backend Port: $BACKEND_PORT"
echo ""

# Run the application
java -jar FS-Monitor.jar \
    --server.port=$BACKEND_PORT \
    --server.address=0.0.0.0 \
    --spring.profiles.active=production

echo ""
echo "✅ Application stopped"
