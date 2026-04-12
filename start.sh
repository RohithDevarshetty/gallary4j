#!/usr/bin/env bash
set -e

APP_DIR="$(cd "$(dirname "$0")" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Ports
DOCKER_CHECK_PORT=5432
BACKEND_PORT=8080
ADMIN_PORT=3000
GALLERY_PORT=3001

# PID file
PID_FILE="$APP_DIR/.app.pids"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  PhotoVault Platform - Start Script${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Function to check if port is in use
check_port() {
    local port=$1
    local service=$2
    lsof -ti tcp:$port 2>/dev/null || true
}

# Function to wait for a service to be ready
wait_for_service() {
    local port=$1
    local service=$2
    local max_attempts=60
    local attempt=1

    echo -ne "${YELLOW}Waiting for $service on port $port...${NC}"

    while [ $attempt -le $max_attempts ]; do
        if lsof -ti tcp:$port >/dev/null 2>&1; then
            echo -e " ${GREEN}Ready!${NC}"
            return 0
        fi
        echo -ne "."
        sleep 1
        attempt=$((attempt + 1))
    done

    echo -e " ${RED}Timeout!${NC}"
    return 1
}

# Cleanup function
cleanup() {
    echo -e "\n${YELLOW}Cleaning up...${NC}"
    if [ -f "$PID_FILE" ]; then
        while IFS= read -r pid; do
            if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
                kill "$pid" 2>/dev/null || true
            fi
        done < "$PID_FILE"
        rm -f "$PID_FILE"
    fi
}

trap cleanup EXIT

# Step 1: Start Docker services
echo -e "${BLUE}[1/4] Starting Infrastructure (Docker services)...${NC}"

# Kill any existing containers
existing=$(docker compose -f "$APP_DIR/docker-compose.yml" ps -q 2>/dev/null || true)
if [ -n "$existing" ]; then
    echo -e "${YELLOW}Stopping existing containers...${NC}"
    docker compose -f "$APP_DIR/docker-compose.yml" down 2>/dev/null || true
    sleep 2
fi

# Start containers
cd "$APP_DIR"
docker compose up -d

echo -e "${GREEN}Docker services started${NC}"

# Wait for PostgreSQL to be healthy
wait_for_service $DOCKER_CHECK_PORT "PostgreSQL" || {
    echo -e "${RED}PostgreSQL failed to start${NC}"
    docker compose logs postgres
    exit 1
}

sleep 3  # Give Kafka time to fully initialize

# Step 2: Start Backend
echo -e "\n${BLUE}[2/4] Starting Backend API (Spring Boot)...${NC}"

# Kill any existing backend processes
existing_backend=$(check_port $BACKEND_PORT "Backend")
if [ -n "$existing_backend" ]; then
    echo -e "${YELLOW}Killing existing backend process (PID: $existing_backend)...${NC}"
    kill "$existing_backend" 2>/dev/null || true
    sleep 2
fi

cd "$APP_DIR/photovault-backend"

# Use Maven if mvnw not available
MAVEN_CMD="./mvnw"
if [ ! -f "./mvnw" ]; then
    MAVEN_CMD="mvn"
fi

# Build if needed
if [ ! -d "target" ]; then
    echo -e "${YELLOW}Building backend (first time setup)...${NC}"
    $MAVEN_CMD clean install -DskipTests -q 2>/dev/null || true
fi

# Start in background
$MAVEN_CMD spring-boot:run > "$APP_DIR/.backend.log" 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID >> "$PID_FILE"

wait_for_service $BACKEND_PORT "Backend API" || {
    echo -e "${RED}Backend failed to start${NC}"
    tail -50 "$APP_DIR/.backend.log"
    exit 1
}

# Step 3: Start Admin Dashboard
echo -e "\n${BLUE}[3/4] Starting Admin Dashboard (Next.js)...${NC}"

# Kill any existing admin processes
existing_admin=$(check_port $ADMIN_PORT "Admin")
if [ -n "$existing_admin" ]; then
    echo -e "${YELLOW}Killing existing admin process (PID: $existing_admin)...${NC}"
    kill "$existing_admin" 2>/dev/null || true
    sleep 2
fi

cd "$APP_DIR/photovault-admin"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing admin dependencies...${NC}"
    npm install -q
fi

# Start in background
npm run dev > "$APP_DIR/.admin.log" 2>&1 &
ADMIN_PID=$!
echo $ADMIN_PID >> "$PID_FILE"

wait_for_service $ADMIN_PORT "Admin Dashboard" || {
    echo -e "${RED}Admin Dashboard failed to start${NC}"
    tail -50 "$APP_DIR/.admin.log"
    exit 1
}

# Step 4: Start Client Gallery
echo -e "\n${BLUE}[4/4] Starting Client Gallery (Next.js)...${NC}"

# Kill any existing gallery processes
existing_gallery=$(check_port $GALLERY_PORT "Gallery")
if [ -n "$existing_gallery" ]; then
    echo -e "${YELLOW}Killing existing gallery process (PID: $existing_gallery)...${NC}"
    kill "$existing_gallery" 2>/dev/null || true
    sleep 2
fi

cd "$APP_DIR/photovault-gallery"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing gallery dependencies...${NC}"
    npm install -q
fi

# Start in background
npm run dev > "$APP_DIR/.gallery.log" 2>&1 &
GALLERY_PID=$!
echo $GALLERY_PID >> "$PID_FILE"

wait_for_service $GALLERY_PORT "Client Gallery" || {
    echo -e "${RED}Client Gallery failed to start${NC}"
    tail -50 "$APP_DIR/.gallery.log"
    exit 1
}

# All services started successfully
echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}  PhotoVault Platform Started Successfully!${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""
echo -e "${BLUE}Services running:${NC}"
echo -e "  ${GREEN}✓${NC} Backend API:      ${BLUE}http://localhost:$BACKEND_PORT${NC}"
echo -e "  ${GREEN}✓${NC} Admin Dashboard:  ${BLUE}http://localhost:$ADMIN_PORT${NC}"
echo -e "  ${GREEN}✓${NC} Client Gallery:   ${BLUE}http://localhost:$GALLERY_PORT${NC}"
echo -e "  ${GREEN}✓${NC} PostgreSQL:       ${BLUE}localhost:5432${NC}"
echo -e "  ${GREEN}✓${NC} Kafka:            ${BLUE}localhost:9092${NC}"
echo ""
echo -e "${BLUE}Log files:${NC}"
echo -e "  Backend:  $APP_DIR/.backend.log"
echo -e "  Admin:    $APP_DIR/.admin.log"
echo -e "  Gallery:  $APP_DIR/.gallery.log"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo -e "  1. Open http://localhost:3000 to access the admin dashboard"
echo -e "  2. Run ${YELLOW}bash ./stop.sh${NC} to stop all services"
echo -e "  3. Run ${YELLOW}tail -f .backend.log${NC} to watch backend logs"
echo ""

# Keep the script running
wait
