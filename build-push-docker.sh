#!/bin/bash

# Build and push multi-architecture Docker image to Docker Hub
set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}Building HappyRow Core for Docker Hub${NC}"

# Get Docker Hub username
if [ -z "$DOCKER_USERNAME" ]; then
    read -p "Enter your Docker Hub username: " DOCKER_USERNAME
fi

IMAGE_NAME="$DOCKER_USERNAME/happyrow-core"

echo -e "${YELLOW}Image: $IMAGE_NAME${NC}"
echo ""

# Create buildx builder
docker buildx create --name multiarch --use 2>/dev/null || docker buildx use multiarch
docker buildx inspect --bootstrap

# Build and push for multiple architectures (including Raspberry Pi)
echo -e "${YELLOW}Building for AMD64, ARM64 (Raspberry Pi)...${NC}"
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t $IMAGE_NAME:latest \
  --push \
  .

echo ""
echo -e "${GREEN}✓ Done! Image pushed to Docker Hub${NC}"
echo -e "${YELLOW}View at: https://hub.docker.com/r/$DOCKER_USERNAME/happyrow-core${NC}"