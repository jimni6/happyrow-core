#!/bin/bash

# HappyRow Core - Raspberry Pi Deployment Script
# This script automates the deployment process

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_requirements() {
    log_info "Checking requirements..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    # Check if .env exists
    if [ ! -f .env ]; then
        log_error ".env file not found. Please create it from .env.example"
        exit 1
    fi
    
    log_info "All requirements met ✓"
}

check_system_resources() {
    log_info "Checking system resources..."
    
    # Check available memory
    AVAILABLE_MEM=$(free -m | awk 'NR==2 {print $7}')
    if [ "$AVAILABLE_MEM" -lt 200 ]; then
        log_warn "Low available memory: ${AVAILABLE_MEM}MB. Consider freeing up memory."
    fi
    
    # Check disk space
    DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
    if [ "$DISK_USAGE" -gt 85 ]; then
        log_warn "Disk usage is high: ${DISK_USAGE}%. Consider cleaning up."
    fi
    
    # Check temperature (RPi specific)
    if command -v vcgencmd &> /dev/null; then
        TEMP=$(vcgencmd measure_temp | sed 's/temp=//' | sed 's/'"'"'C//')
        log_info "CPU Temperature: ${TEMP}°C"
        if (( $(echo "$TEMP > 75" | bc -l) )); then
            log_warn "High CPU temperature! Consider adding cooling."
        fi
    fi
    
    log_info "System resources check complete ✓"
}

build_images() {
    log_info "Building Docker images..."
    log_warn "This may take 15-30 minutes on Raspberry Pi 3. Please be patient..."
    
    docker-compose build --no-cache
    
    log_info "Docker images built successfully ✓"
}

start_services() {
    log_info "Starting services..."
    
    docker-compose up -d
    
    log_info "Services started ✓"
}

wait_for_health() {
    log_info "Waiting for services to be healthy..."
    
    # Wait for PostgreSQL
    log_info "Waiting for PostgreSQL..."
    for i in {1..30}; do
        if docker-compose exec -T postgres pg_isready -U happyrow_user &> /dev/null; then
            log_info "PostgreSQL is ready ✓"
            break
        fi
        if [ $i -eq 30 ]; then
            log_error "PostgreSQL failed to start"
            exit 1
        fi
        sleep 2
    done
    
    # Wait for backend
    log_info "Waiting for backend..."
    for i in {1..60}; do
        if curl -sf http://localhost:8080/health &> /dev/null; then
            log_info "Backend is ready ✓"
            break
        fi
        if [ $i -eq 60 ]; then
            log_error "Backend failed to start"
            exit 1
        fi
        sleep 2
    done
    
    # Wait for NGINX
    log_info "Waiting for NGINX..."
    for i in {1..30}; do
        if curl -sf http://localhost/health &> /dev/null; then
            log_info "NGINX is ready ✓"
            break
        fi
        if [ $i -eq 30 ]; then
            log_error "NGINX failed to start"
            exit 1
        fi
        sleep 2
    done
    
    log_info "All services are healthy ✓"
}

show_status() {
    log_info "Deployment Status:"
    echo ""
    docker-compose ps
    echo ""
    
    log_info "Testing endpoints..."
    echo ""
    
    # Test health endpoint
    HEALTH_RESPONSE=$(curl -s http://localhost/health)
    echo "Health: $HEALTH_RESPONSE"
    
    # Test info endpoint
    INFO_RESPONSE=$(curl -s http://localhost/info)
    echo "Info: $INFO_RESPONSE"
    
    echo ""
    log_info "View logs with: docker-compose logs -f"
    log_info "Stop services with: docker-compose down"
}

# Main deployment flow
main() {
    echo ""
    log_info "=========================================="
    log_info "  HappyRow Core - Raspberry Pi Deploy"
    log_info "=========================================="
    echo ""
    
    # Check if we're in the right directory
    if [ ! -f "docker-compose.yml" ]; then
        log_error "docker-compose.yml not found. Are you in the .raspberry directory?"
        exit 1
    fi
    
    check_requirements
    check_system_resources
    
    # Ask for confirmation
    read -p "Continue with deployment? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Deployment cancelled."
        exit 0
    fi
    
    # Stop existing services
    if docker-compose ps | grep -q "Up"; then
        log_info "Stopping existing services..."
        docker-compose down
    fi
    
    build_images
    start_services
    wait_for_health
    show_status
    
    echo ""
    log_info "=========================================="
    log_info "  Deployment Complete! 🎉"
    log_info "=========================================="
    echo ""
}

# Handle script arguments
case "${1:-}" in
    "start")
        log_info "Starting services..."
        docker-compose up -d
        ;;
    "stop")
        log_info "Stopping services..."
        docker-compose down
        ;;
    "restart")
        log_info "Restarting services..."
        docker-compose restart
        ;;
    "logs")
        docker-compose logs -f
        ;;
    "status")
        docker-compose ps
        ;;
    "clean")
        log_warn "This will remove all containers, volumes, and images!"
        read -p "Are you sure? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker-compose down -v
            docker system prune -af
            log_info "Cleanup complete"
        fi
        ;;
    "update")
        log_info "Updating application..."
        git pull origin main
        docker-compose build backend
        docker-compose up -d backend
        log_info "Update complete"
        ;;
    *)
        main
        ;;
esac
