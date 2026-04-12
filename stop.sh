#!/usr/bin/env bash

APP_DIR="$(cd "$(dirname "$0")" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Ports
BACKEND_PORT=8080
ADMIN_PORT=3000
GALLERY_PORT=3001
DOCKER_POSTGRES_PORT=5432
DOCKER_KAFKA_PORT=9092
DOCKER_ZOOKEEPER_PORT=2181

PID_FILE="$APP_DIR/.app.pids"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  PhotoVault Platform - Stop Script${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

stopped=0

# Function to kill process on port
kill_port() {
    local port=$1
    local name=$2

    local pid=$(lsof -ti tcp:$port 2>/dev/null || true)
    if [ -n "$pid" ]; then
        echo -e "${YELLOW}Stopping $name (PID: $pid)...${NC}"
        kill "$pid" 2>/dev/null || true
        sleep 1

        # Force kill if still running
        if kill -0 "$pid" 2>/dev/null; then
            kill -9 "$pid" 2>/dev/null || true
        fi
        echo -e "${GREEN}✓${NC} $name stopped"
        stopped=1
        return 0
    fi
    return 1
}

# Step 1: Stop application services
echo -e "${BLUE}[1/2] Stopping Application Services...${NC}"

kill_port $BACKEND_PORT "Backend API" || echo -e "${YELLOW}Backend API not running${NC}"
kill_port $ADMIN_PORT "Admin Dashboard" || echo -e "${YELLOW}Admin Dashboard not running${NC}"
kill_port $GALLERY_PORT "Client Gallery" || echo -e "${YELLOW}Client Gallery not running${NC}"

# Step 2: Stop Docker services
echo -e "\n${BLUE}[2/2] Stopping Infrastructure (Docker services)...${NC}"

cd "$APP_DIR"

# Stop containers gracefully
if docker compose ps 2>/dev/null | grep -q "Up"; then
    echo -e "${YELLOW}Stopping Docker containers...${NC}"
    docker compose down
    echo -e "${GREEN}✓${NC} Docker services stopped"
    stopped=1
else
    echo -e "${YELLOW}No Docker containers running${NC}"
fi

# Cleanup PID file
if [ -f "$PID_FILE" ]; then
    rm -f "$PID_FILE"
fi

# Cleanup log files (optional)
rm -f "$APP_DIR/.backend.log"
rm -f "$APP_DIR/.admin.log"
rm -f "$APP_DIR/.gallery.log"

echo ""
if [ "$stopped" -eq 1 ]; then
    echo -e "${GREEN}================================================${NC}"
    echo -e "${GREEN}  PhotoVault Platform Stopped Successfully${NC}"
    echo -e "${GREEN}================================================${NC}"
else
    echo -e "${YELLOW}PhotoVault Platform was not running${NC}"
fi

echo ""
echo -e "${BLUE}Available commands:${NC}"
echo -e "  Start:    ${YELLOW}bash ./start.sh${NC}"
echo -e "  Stop:     ${YELLOW}bash ./stop.sh${NC}"
echo ""
