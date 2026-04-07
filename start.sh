#!/bin/bash

# FS-Monitor Development Setup with Dynamic Ports
# This script reads port configuration from config.json

set -e

echo "🚀 Starting FS-Monitor Development Setup..."
echo "=========================================="

# Function to read port from config.json (fixed parsing)
get_port() {
    local service=$1
    local environment=${NODE_ENV:-development}
    
    if [ -f "config.json" ]; then
        # Use jq for proper JSON parsing if available, otherwise use improved regex
        if command -v jq &> /dev/null; then
            port=$(jq -r ".$environment.$service.port" config.json 2>/dev/null)
            if [ "$port" != "null" ] && [ -n "$port" ]; then
                echo "$port"
                return 0
            fi
        else
            # Improved regex parsing
            port=$(grep -A 20 "\"$environment\"" config.json | \
                   grep -A 10 "\"$service\"" | \
                   grep "\"port\"" | \
                   head -1 | \
                   sed 's/.*"port": *\([0-9]*\).*/\1/')
            if [ -n "$port" ] && [[ "$port" =~ ^[0-9]+$ ]]; then
                echo "$port"
                return 0
            fi
        fi
    fi
    
    # Fallback to defaults
    case $service in
        "frontend") echo "3000" ;;
        "backend") echo "8080" ;;
        *) echo "8080" ;;
    esac
}

# Function to read host from config.json (fixed parsing)
get_host() {
    local service=$1
    local environment=${NODE_ENV:-development}
    
    if [ -f "config.json" ]; then
        if command -v jq &> /dev/null; then
            host=$(jq -r ".$environment.$service.host" config.json 2>/dev/null)
            if [ "$host" != "null" ] && [ -n "$host" ]; then
                echo "$host"
                return 0
            fi
        else
            # Improved regex parsing
            host=$(grep -A 20 "\"$environment\"" config.json | \
                  grep -A 10 "\"$service\"" | \
                  grep "\"host\"" | \
                  head -1 | \
                  sed 's/.*"host": *"\([^"]*\)".*/\1/')
            if [ -n "$host" ]; then
                echo "$host"
                return 0
            fi
        fi
    fi
    
    # Fallback to defaults
    echo "localhost"
}

# Get ports and hosts from config
FRONTEND_PORT=$(get_port "frontend")
BACKEND_PORT=$(get_port "backend")
FRONTEND_HOST=$(get_host "frontend")
BACKEND_HOST=$(get_host "backend")

echo "📋 Configuration from config.json:"
echo "   Frontend: $FRONTEND_HOST:$FRONTEND_PORT"
echo "   Backend:  $BACKEND_HOST:$BACKEND_PORT"
echo ""

# Check dependencies
echo "🔍 Checking dependencies..."

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 21 or higher."
    exit 1
fi

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 18 or higher."
    exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven."
    exit 1
fi

echo "✅ All dependencies are installed!"
echo ""

# Build backend
echo "🔨 Building backend..."
cd backend
mvn clean compile -q
echo "✅ Backend build completed successfully!"
cd ..

# Install frontend dependencies and build
echo "📦 Installing frontend dependencies..."
cd frontend
npm install --silent
echo "✅ Frontend dependencies installed!"

# Build frontend to ensure latest changes are included
echo "🔨 Building frontend..."
npm run build --silent 2>/dev/null || npm run build
echo "✅ Frontend build completed!"
cd ..

# Start services
echo "🚀 Starting services with REAL-TIME BACKEND LOGGING..."
echo ""

# Start frontend in background (logs to file)
# Using 'preview' to serve the built version instead of 'dev' for hot reload
echo "🌐 Starting frontend on port $FRONTEND_PORT..."
cd frontend
FRONTEND_PORT=$FRONTEND_PORT npm run preview > ../frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

# Wait for frontend to start
echo "⏳ Waiting for frontend to start..."
sleep 3

# Check if frontend is running
if ! kill -0 $FRONTEND_PID 2>/dev/null; then
    echo "❌ Frontend failed to start. Check frontend.log for details."
    exit 1
fi

echo ""
echo "🎉 FS-Monitor is now running!"
echo "=========================================="
echo "🌐 Frontend: http://$FRONTEND_HOST:$FRONTEND_PORT"
echo "🔧 Backend:  http://$BACKEND_HOST:$BACKEND_PORT"
echo "📊 H2 Console: http://$BACKEND_HOST:$BACKEND_PORT/h2-console"
echo "👤 Default Login: admin / password"
echo "=========================================="
echo ""
echo "� BACKEND LOGS ARE SHOWN BELOW IN REAL-TIME"
echo "💡 Frontend logs: tail -f frontend.log"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

# Start backend in background but follow its log
echo "🔧 Starting backend on port $BACKEND_PORT..."
echo "=========================================="
echo "📝 BACKEND LOGS (Real-time):"
echo "=========================================="

cd backend
SERVER_PORT=$BACKEND_PORT mvn spring-boot:run > ../backend.log 2>&1 &
BACKEND_PID=$!
cd ..

# Wait a moment for backend to start writing to log
sleep 2

# Follow backend log in real-time
tail -f backend.log &
TAIL_PID=$!

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "🛑 Stopping services..."
    if [ ! -z "$TAIL_PID" ]; then
        kill $TAIL_PID 2>/dev/null || true
    fi
    if [ ! -z "$BACKEND_PID" ]; then
        kill $BACKEND_PID 2>/dev/null || true
    fi
    if [ ! -z "$FRONTEND_PID" ]; then
        kill $FRONTEND_PID 2>/dev/null || true
    fi
    echo "✅ All services stopped."
}

# Set trap to cleanup on exit
trap cleanup EXIT INT TERM

# Wait for backend process (this keeps the script running)
wait $BACKEND_PID