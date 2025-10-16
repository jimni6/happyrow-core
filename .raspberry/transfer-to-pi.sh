#!/bin/bash

# HappyRow Core - Transfer to Raspberry Pi Script
# This script helps transfer your project files to the Raspberry Pi

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  HappyRow Core - Transfer to Pi${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""

# Check if rsync is available
if ! command -v rsync &> /dev/null; then
    echo -e "${RED}Error: rsync is not installed${NC}"
    echo "Install it with: brew install rsync"
    exit 1
fi

# Get Raspberry Pi details
read -p "Enter Raspberry Pi IP address: " PI_IP
read -p "Enter SSH username (default: happyrow): " PI_USER
PI_USER=${PI_USER:-happyrow}
read -p "Enter SSH port (default: 2222): " SSH_PORT
SSH_PORT=${SSH_PORT:-2222}
read -p "Enter remote directory (default: ~/happyrow-core): " REMOTE_DIR
REMOTE_DIR=${REMOTE_DIR:-~/happyrow-core}

echo ""
echo -e "${YELLOW}Transfer Configuration:${NC}"
echo "  IP:        $PI_IP"
echo "  User:      $PI_USER"
echo "  Port:      $SSH_PORT"
echo "  Directory: $REMOTE_DIR"
echo ""
read -p "Continue? (y/n) " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Transfer cancelled."
    exit 0
fi

# Test SSH connection
echo ""
echo -e "${GREEN}Testing SSH connection...${NC}"
if ! ssh -p $SSH_PORT -o ConnectTimeout=5 ${PI_USER}@${PI_IP} "echo 'Connection successful'" &> /dev/null; then
    echo -e "${RED}Error: Cannot connect to Raspberry Pi${NC}"
    echo "Please check:"
    echo "  1. IP address is correct"
    echo "  2. SSH is running on the Pi"
    echo "  3. Port number is correct"
    echo "  4. Network connection is working"
    exit 1
fi
echo -e "${GREEN}Connection successful!${NC}"
echo ""

# Create remote directory
echo -e "${GREEN}Creating remote directory...${NC}"
ssh -p $SSH_PORT ${PI_USER}@${PI_IP} "mkdir -p $REMOTE_DIR"

# Get project root (parent of .raspberry directory)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo ""
echo -e "${GREEN}Starting file transfer...${NC}"
echo "Source: $PROJECT_ROOT"
echo "Destination: ${PI_USER}@${PI_IP}:${REMOTE_DIR}"
echo ""

# Transfer files with rsync
rsync -avz --progress \
  -e "ssh -p $SSH_PORT" \
  --exclude 'build/' \
  --exclude '.gradle/' \
  --exclude '.kotlin/' \
  --exclude '.idea/' \
  --exclude 'out/' \
  --exclude 'target/' \
  --exclude 'node_modules/' \
  --exclude '.env' \
  --exclude '*.iml' \
  --exclude '*.ipr' \
  --exclude '*.iws' \
  --exclude '.DS_Store' \
  --exclude '*.log' \
  "$PROJECT_ROOT/" \
  "${PI_USER}@${PI_IP}:${REMOTE_DIR}/"

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}======================================${NC}"
    echo -e "${GREEN}  Transfer Complete! ✓${NC}"
    echo -e "${GREEN}======================================${NC}"
    echo ""
    echo -e "${YELLOW}Next steps:${NC}"
    echo "1. SSH into your Raspberry Pi:"
    echo "   ssh -p $SSH_PORT ${PI_USER}@${PI_IP}"
    echo ""
    echo "2. Navigate to the project:"
    echo "   cd $REMOTE_DIR/.raspberry"
    echo ""
    echo "3. Create .env file:"
    echo "   cp .env.example .env"
    echo "   nano .env"
    echo ""
    echo "4. Run deployment:"
    echo "   chmod +x deploy.sh"
    echo "   ./deploy.sh"
    echo ""
else
    echo ""
    echo -e "${RED}Transfer failed!${NC}"
    echo "Please check the error messages above."
    exit 1
fi
