#!/bin/bash

# Remote Raspberry Pi Health Check
# Run this FROM your local machine to check your Pi remotely

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  Remote Raspberry Pi Check${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Get Pi connection details
read -p "Enter Raspberry Pi IP address: " PI_IP
read -p "Enter SSH username (default: happyrow): " PI_USER
PI_USER=${PI_USER:-happyrow}
read -p "Enter SSH port (default: 2222): " SSH_PORT
SSH_PORT=${SSH_PORT:-2222}

echo ""
echo -e "${BLUE}Testing connection to: ${PI_USER}@${PI_IP}:${SSH_PORT}${NC}"
echo ""

# Test SSH connection
echo -e "${YELLOW}Testing SSH connectivity...${NC}"
if ssh -p $SSH_PORT -o ConnectTimeout=5 -o BatchMode=yes ${PI_USER}@${PI_IP} "echo 'OK'" &> /dev/null; then
    echo -e "${GREEN}✓ SSH connection successful${NC}"
elif ssh -p $SSH_PORT -o ConnectTimeout=5 ${PI_USER}@${PI_IP} "echo 'OK'" 2>&1 | grep -q "Permission denied"; then
    echo -e "${GREEN}✓ SSH accessible (password required)${NC}"
else
    echo -e "${RED}✗ Cannot connect to Raspberry Pi${NC}"
    echo ""
    echo "Troubleshooting:"
    echo "  1. Check IP address is correct"
    echo "  2. Check SSH port number"
    echo "  3. Ensure Pi is powered on and connected to network"
    echo "  4. Try: ssh -p $SSH_PORT ${PI_USER}@${PI_IP}"
    exit 1
fi

echo ""
echo -e "${BLUE}Running health checks on Raspberry Pi...${NC}"
echo ""

# Run comprehensive checks
ssh -p $SSH_PORT ${PI_USER}@${PI_IP} 'bash -s' << 'ENDSSH'

# Colors for remote output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

print_check() {
    local status=$1
    local message=$2
    if [ "$status" = "ok" ]; then
        echo -e "${GREEN}✓${NC} $message"
    elif [ "$status" = "warn" ]; then
        echo -e "${YELLOW}⚠${NC} $message"
    else
        echo -e "${RED}✗${NC} $message"
    fi
}

# System Info
echo -e "${BLUE}=== System Information ===${NC}"
if [ -f /proc/device-tree/model ]; then
    MODEL=$(cat /proc/device-tree/model | tr -d '\0')
    echo "Model: $MODEL"
fi
echo "Hostname: $(hostname)"
echo "Uptime: $(uptime -p)"
echo ""

# Temperature
echo -e "${BLUE}=== Temperature ===${NC}"
if command -v vcgencmd &> /dev/null; then
    TEMP=$(vcgencmd measure_temp | sed 's/temp=//' | sed "s/'C//")
    echo "CPU Temperature: ${TEMP}°C"
    TEMP_NUM=$(echo $TEMP | sed 's/°C//')
    if (( $(echo "$TEMP_NUM < 60" | bc -l) )); then
        print_check "ok" "Temperature is good"
    elif (( $(echo "$TEMP_NUM < 75" | bc -l) )); then
        print_check "warn" "Temperature is warm"
    else
        print_check "error" "Temperature is HIGH!"
    fi
fi
echo ""

# Memory
echo -e "${BLUE}=== Memory ===${NC}"
TOTAL_MEM=$(free -m | awk 'NR==2 {print $2}')
USED_MEM=$(free -m | awk 'NR==2 {print $3}')
AVAILABLE_MEM=$(free -m | awk 'NR==2 {print $7}')
echo "Memory: ${USED_MEM}MB / ${TOTAL_MEM}MB used"
echo "Available: ${AVAILABLE_MEM}MB"

if [ "$AVAILABLE_MEM" -gt 300 ]; then
    print_check "ok" "Sufficient memory available"
elif [ "$AVAILABLE_MEM" -gt 200 ]; then
    print_check "warn" "Memory is getting low (${AVAILABLE_MEM}MB available)"
else
    print_check "error" "Low memory! (${AVAILABLE_MEM}MB available)"
fi
echo ""

# Disk Space
echo -e "${BLUE}=== Disk Space ===${NC}"
DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
DISK_AVAILABLE=$(df -h / | awk 'NR==2 {print $4}')
echo "Disk: ${DISK_USAGE}% used"
echo "Available: ${DISK_AVAILABLE}"

if [ "$DISK_USAGE" -lt 70 ]; then
    print_check "ok" "Sufficient disk space"
elif [ "$DISK_USAGE" -lt 85 ]; then
    print_check "warn" "Disk space is getting low (${DISK_USAGE}% used)"
else
    print_check "error" "Critical: Low disk space! (${DISK_USAGE}% used)"
fi
echo ""

# Docker
echo -e "${BLUE}=== Docker ===${NC}"
if command -v docker &> /dev/null; then
    print_check "ok" "Docker installed: $(docker --version | cut -d' ' -f3 | tr -d ',')"
    
    if systemctl is-active --quiet docker 2>/dev/null; then
        print_check "ok" "Docker service is running"
    else
        print_check "error" "Docker service is NOT running"
    fi
    
    if docker ps &> /dev/null; then
        print_check "ok" "User has Docker permissions"
        
        # Check running containers
        RUNNING=$(docker ps --format '{{.Names}}' | wc -l)
        if [ "$RUNNING" -gt 0 ]; then
            echo "  Running containers: $RUNNING"
            docker ps --format "  - {{.Names}} ({{.Status}})"
        fi
    else
        print_check "warn" "User may need Docker permissions"
    fi
else
    print_check "error" "Docker is NOT installed"
fi
echo ""

# Docker Compose
echo -e "${BLUE}=== Docker Compose ===${NC}"
if command -v docker-compose &> /dev/null; then
    print_check "ok" "Docker Compose installed"
elif docker compose version &> /dev/null 2>&1; then
    print_check "ok" "Docker Compose (plugin) installed"
else
    print_check "error" "Docker Compose is NOT installed"
fi
echo ""

# Network
echo -e "${BLUE}=== Network ===${NC}"
IP_ADDR=$(ip -4 addr show | grep inet | grep -v 127.0.0.1 | head -1 | awk '{print $2}')
echo "IP Address: $IP_ADDR"

if ping -c 1 -W 2 8.8.8.8 &> /dev/null; then
    print_check "ok" "Internet connectivity working"
else
    print_check "error" "No internet connectivity"
fi
echo ""

# Port availability
echo -e "${BLUE}=== Required Ports ===${NC}"
for port in 80 443 8080 5432; do
    if ss -tuln 2>/dev/null | grep -q ":$port " || netstat -tuln 2>/dev/null | grep -q ":$port "; then
        print_check "warn" "Port $port is in use"
    else
        print_check "ok" "Port $port is available"
    fi
done
echo ""

# Load
echo -e "${BLUE}=== System Load ===${NC}"
LOAD=$(uptime | awk -F'load average:' '{print $2}' | awk '{print $1}' | sed 's/,//')
echo "Load: $LOAD"
if (( $(echo "$LOAD < 2.0" | bc -l) )); then
    print_check "ok" "System load is normal"
else
    print_check "warn" "System load is elevated"
fi
echo ""

# Summary
echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  Status Summary${NC}"
echo -e "${BLUE}======================================${NC}"

READY=true
ISSUES=""

if [ "$AVAILABLE_MEM" -lt 200 ]; then
    READY=false
    ISSUES="$ISSUES\n- Insufficient memory"
fi

if [ "$DISK_USAGE" -gt 85 ]; then
    READY=false
    ISSUES="$ISSUES\n- Insufficient disk space"
fi

if ! command -v docker &> /dev/null; then
    READY=false
    ISSUES="$ISSUES\n- Docker not installed"
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null 2>&1; then
    READY=false
    ISSUES="$ISSUES\n- Docker Compose not installed"
fi

if $READY; then
    echo -e "${GREEN}✓ Raspberry Pi is ready for deployment!${NC}"
else
    echo -e "${RED}✗ Issues found:${NC}"
    echo -e "$ISSUES"
fi

ENDSSH

echo ""
echo -e "${BLUE}======================================${NC}"
echo -e "${GREEN}Check complete!${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Offer to transfer health check script
read -p "Transfer full health check script to Pi? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Transferring check-pi-health.sh..."
    SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
    scp -P $SSH_PORT "$SCRIPT_DIR/check-pi-health.sh" ${PI_USER}@${PI_IP}:~/
    ssh -p $SSH_PORT ${PI_USER}@${PI_IP} "chmod +x ~/check-pi-health.sh"
    echo -e "${GREEN}✓ Script transferred!${NC}"
    echo ""
    echo "Run on Pi with: ./check-pi-health.sh"
fi

echo ""
echo "Next steps:"
echo "  1. If ready, transfer project files: ./transfer-to-pi.sh"
echo "  2. SSH to Pi: ssh -p $SSH_PORT ${PI_USER}@${PI_IP}"
echo "  3. Deploy: cd ~/happyrow-core/.raspberry && ./deploy.sh"
echo ""
