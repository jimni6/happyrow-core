#!/bin/bash

# Pull and run HappyRow Core from Docker Hub
set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}HappyRow Core - Pull and Run from Docker Hub${NC}"
echo ""

# Get Docker Hub username
if [ -z "$DOCKER_USERNAME" ]; then
    read -p "Enter your Docker Hub username: " DOCKER_USERNAME
fi

IMAGE_NAME="$DOCKER_USERNAME/happyrow-core:latest"

echo -e "${YELLOW}Pulling image: $IMAGE_NAME${NC}"
docker pull $IMAGE_NAME

echo ""
echo -e "${YELLOW}Checking for .env file...${NC}"

if [ ! -f .env ]; then
    echo -e "${RED}Warning: .env file not found!${NC}"
    echo -e "${YELLOW}Please create a .env file with your database configuration.${NC}"
    echo -e "You can copy .env.example: ${GREEN}cp .env.example .env${NC}"
    echo ""
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
    ENV_FILE_ARG=""
else
    echo -e "${GREEN}✓ Found .env file${NC}"
    ENV_FILE_ARG="--env-file .env"
fi

echo ""
echo -e "${YELLOW}Stopping existing container if any...${NC}"
docker stop happyrow-app 2>/dev/null || true
docker rm happyrow-app 2>/dev/null || true

echo ""
echo -e "${YELLOW}Starting container...${NC}"
docker run -d \
  --name happyrow-app \
  -p 8080:8080 \
  $ENV_FILE_ARG \
  --restart unless-stopped \
  $IMAGE_NAME

echo ""
echo -e "${GREEN}✓ Container started successfully!${NC}"
echo ""
echo -e "Container name: ${YELLOW}happyrow-app${NC}"
echo -e "Application URL: ${YELLOW}http://localhost:8080${NC}"
echo ""
echo -e "Useful commands:"
echo -e "  View logs:    ${GREEN}docker logs -f happyrow-app${NC}"
echo -e "  Stop:         ${GREEN}docker stop happyrow-app${NC}"
echo -e "  Start:        ${GREEN}docker start happyrow-app${NC}"
echo -e "  Remove:       ${GREEN}docker rm -f happyrow-app${NC}"
echo -e "  Shell access: ${GREEN}docker exec -it happyrow-app /bin/sh${NC}"
echo ""

# Show first few log lines
echo -e "${YELLOW}Initial logs:${NC}"
sleep 2
docker logs happyrow-app --tail 20
