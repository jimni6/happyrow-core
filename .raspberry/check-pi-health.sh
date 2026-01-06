#!/bin/bash

# Raspberry Pi Health Check Script
# Run this ON your Raspberry Pi to check if it's ready for deployment

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  Raspberry Pi Health Check${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Function to print status
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

# System Information
echo -e "${BLUE}=== System Information ===${NC}"
if [ -f /proc/device-tree/model ]; then
    MODEL=$(cat /proc/device-tree/model)
    echo "Model: $MODEL"
fi
echo "Hostname: $(hostname)"
echo "Kernel: $(uname -r)"
echo "Uptime: $(uptime -p)"
echo ""

# CPU Temperature
echo -e "${BLUE}=== Temperature ===${NC}"
if command -v vcgencmd &> /dev/null; then
    TEMP=$(vcgencmd measure_temp | sed 's/temp=//' | sed 's/'"'"'C//')
    echo "CPU Temperature: ${TEMP}°C"
    
    if (( $(echo "$TEMP < 60" | bc -l) )); then
        print_check "ok" "Temperature is good (< 60°C)"
    elif (( $(echo "$TEMP < 75" | bc -l) )); then
        print_check "warn" "Temperature is getting warm (60-75°C)"
    else
        print_check "error" "Temperature is HIGH! (> 75°C) - Add cooling!"
    fi
else
    echo "vcgencmd not available"
fi
echo ""

# Memory Check
echo -e "${BLUE}=== Memory ===${NC}"
free -h
TOTAL_MEM=$(free -m | awk 'NR==2 {print $2}')
USED_MEM=$(free -m | awk 'NR==2 {print $3}')
AVAILABLE_MEM=$(free -m | awk 'NR==2 {print $7}')
MEM_PERCENT=$(echo "scale=1; $USED_MEM * 100 / $TOTAL_MEM" | bc)

echo ""
echo "Memory Usage: ${MEM_PERCENT}% (${USED_MEM}MB / ${TOTAL_MEM}MB)"
echo "Available: ${AVAILABLE_MEM}MB"

if [ "$AVAILABLE_MEM" -gt 300 ]; then
    print_check "ok" "Sufficient memory available"
elif [ "$AVAILABLE_MEM" -gt 200 ]; then
    print_check "warn" "Memory is getting low"
else
    print_check "error" "Low memory! Consider freeing up resources"
fi
echo ""

# Disk Space
echo -e "${BLUE}=== Disk Space ===${NC}"
df -h /
DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
DISK_AVAILABLE=$(df -h / | awk 'NR==2 {print $4}')

echo ""
echo "Disk Usage: ${DISK_USAGE}%"
echo "Available: ${DISK_AVAILABLE}"

if [ "$DISK_USAGE" -lt 70 ]; then
    print_check "ok" "Sufficient disk space"
elif [ "$DISK_USAGE" -lt 85 ]; then
    print_check "warn" "Disk space is getting low"
else
    print_check "error" "Critical: Low disk space! Clean up before deployment"
fi
echo ""

# Swap Space
echo -e "${BLUE}=== Swap ===${NC}"
SWAP_TOTAL=$(free -m | awk 'NR==3 {print $2}')
SWAP_USED=$(free -m | awk 'NR==3 {print $3}')
echo "Swap: ${SWAP_USED}MB / ${SWAP_TOTAL}MB"

if [ "$SWAP_TOTAL" -gt 1000 ]; then
    print_check "ok" "Swap is configured (${SWAP_TOTAL}MB)"
elif [ "$SWAP_TOTAL" -gt 500 ]; then
    print_check "warn" "Swap is small (${SWAP_TOTAL}MB) - consider increasing"
else
    print_check "warn" "No/low swap - may need more for Docker builds"
fi
echo ""

# Docker Check
echo -e "${BLUE}=== Docker ===${NC}"
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    print_check "ok" "Docker installed: $DOCKER_VERSION"
    
    # Check Docker service
    if systemctl is-active --quiet docker; then
        print_check "ok" "Docker service is running"
    else
        print_check "error" "Docker service is NOT running"
        echo "  Start with: sudo systemctl start docker"
    fi
    
    # Check Docker permissions
    if docker ps &> /dev/null; then
        print_check "ok" "User has Docker permissions"
    else
        print_check "warn" "User may need Docker permissions"
        echo "  Add with: sudo usermod -aG docker $USER"
    fi
else
    print_check "error" "Docker is NOT installed"
    echo "  Install: curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh"
fi
echo ""

# Docker Compose Check
echo -e "${BLUE}=== Docker Compose ===${NC}"
if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose --version)
    print_check "ok" "Docker Compose installed: $COMPOSE_VERSION"
elif docker compose version &> /dev/null; then
    COMPOSE_VERSION=$(docker compose version)
    print_check "ok" "Docker Compose (plugin) installed: $COMPOSE_VERSION"
else
    print_check "error" "Docker Compose is NOT installed"
    echo "  Install: sudo apt-get install docker-compose-plugin"
fi
echo ""

# Network Check
echo -e "${BLUE}=== Network ===${NC}"
echo "IP Address(es):"
ip -4 addr show | grep inet | grep -v 127.0.0.1 | awk '{print "  " $2}'

echo ""
if ping -c 1 -W 2 8.8.8.8 &> /dev/null; then
    print_check "ok" "Internet connectivity working"
else
    print_check "error" "No internet connectivity"
fi

if ping -c 1 -W 2 google.com &> /dev/null; then
    print_check "ok" "DNS resolution working"
else
    print_check "error" "DNS resolution failed"
fi
echo ""

# SSH Check
echo -e "${BLUE}=== SSH ===${NC}"
if systemctl is-active --quiet ssh; then
    print_check "ok" "SSH service is running"
    SSH_PORT=$(grep "^Port" /etc/ssh/sshd_config 2>/dev/null | awk '{print $2}')
    if [ -n "$SSH_PORT" ]; then
        echo "  SSH Port: $SSH_PORT"
    else
        echo "  SSH Port: 22 (default)"
    fi
else
    print_check "warn" "SSH service status unknown"
fi
echo ""

# Check for existing Docker containers
echo -e "${BLUE}=== Existing Docker Containers ===${NC}"
if command -v docker &> /dev/null && docker ps &> /dev/null; then
    CONTAINER_COUNT=$(docker ps -a | tail -n +2 | wc -l)
    if [ "$CONTAINER_COUNT" -gt 0 ]; then
        echo "Found $CONTAINER_COUNT container(s):"
        docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        echo ""
        print_check "warn" "Some containers exist - may need cleanup"
    else
        print_check "ok" "No existing containers"
    fi
else
    echo "Cannot check Docker containers"
fi
echo ""

# Check for port conflicts
echo -e "${BLUE}=== Port Check ===${NC}"
PORTS_TO_CHECK=(80 443 8080 5432)
PORT_CONFLICTS=0

for port in "${PORTS_TO_CHECK[@]}"; do
    if ss -tuln | grep -q ":$port "; then
        print_check "warn" "Port $port is in use"
        PORT_CONFLICTS=$((PORT_CONFLICTS + 1))
    else
        print_check "ok" "Port $port is available"
    fi
done

if [ $PORT_CONFLICTS -gt 0 ]; then
    echo ""
    echo -e "${YELLOW}Warning: Some ports are in use. Existing services may conflict.${NC}"
fi
echo ""

# Load Average
echo -e "${BLUE}=== System Load ===${NC}"
LOAD=$(uptime | awk -F'load average:' '{print $2}' | awk '{print $1}' | sed 's/,//')
echo "1-minute load average: $LOAD"

# RPi typically has 4 cores, load > 4 is high
if (( $(echo "$LOAD < 2.0" | bc -l) )); then
    print_check "ok" "System load is normal"
elif (( $(echo "$LOAD < 4.0" | bc -l) )); then
    print_check "warn" "System load is moderate"
else
    print_check "warn" "System load is high - may be busy"
fi
echo ""

# Security checks
echo -e "${BLUE}=== Security ===${NC}"
if [ -f ~/.ssh/authorized_keys ]; then
    print_check "ok" "SSH keys configured"
else
    print_check "warn" "No SSH keys found - using password auth?"
fi

if command -v ufw &> /dev/null; then
    if ufw status | grep -q "Status: active"; then
        print_check "ok" "Firewall (UFW) is active"
    else
        print_check "warn" "Firewall (UFW) is not active"
    fi
else
    print_check "warn" "UFW firewall not installed"
fi

if command -v fail2ban-client &> /dev/null; then
    if systemctl is-active --quiet fail2ban; then
        print_check "ok" "Fail2ban is active"
    else
        print_check "warn" "Fail2ban installed but not active"
    fi
else
    print_check "warn" "Fail2ban not installed"
fi
echo ""

# Summary
echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  Summary${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

READY=true

# Critical checks
if [ "$AVAILABLE_MEM" -lt 200 ]; then
    echo -e "${RED}✗ Insufficient memory${NC}"
    READY=false
fi

if [ "$DISK_USAGE" -gt 85 ]; then
    echo -e "${RED}✗ Insufficient disk space${NC}"
    READY=false
fi

if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker not installed${NC}"
    READY=false
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}✗ Docker Compose not installed${NC}"
    READY=false
fi

if $READY; then
    echo -e "${GREEN}✓ Raspberry Pi is ready for deployment!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Transfer project files from your local machine"
    echo "2. Create .env file with your configuration"
    echo "3. Run ./deploy.sh to start deployment"
else
    echo -e "${RED}✗ Raspberry Pi needs attention before deployment${NC}"
    echo ""
    echo "Please address the issues above before proceeding."
fi

echo ""
