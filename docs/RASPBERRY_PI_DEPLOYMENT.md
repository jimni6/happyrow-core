# Raspberry Pi Deployment Guide

Complete guide to deploy HappyRow Core backend on Raspberry Pi 3 with secure external access via Cloudflare Tunnel.

## Architecture Overview

```
┌─────────────────┐
│  Vercel Website │ (Frontend)
└────────┬────────┘
         │ HTTPS
         ▼
┌─────────────────────────────────────┐
│     Cloudflare Zero Trust           │
│    (Cloudflare Tunnel)              │
└────────┬────────────────────────────┘
         │ Encrypted Tunnel
         ▼
┌─────────────────────────────────────┐
│      Raspberry Pi 3                 │
│  ┌──────────────────────────────┐   │
│  │  Docker Compose Stack        │   │
│  │  ┌────────────────────────┐  │   │
│  │  │  Cloudflared Container │  │   │
│  │  └───────────┬────────────┘  │   │
│  │              │               │   │
│  │  ┌───────────▼────────────┐  │   │
│  │  │  NGINX (Reverse Proxy) │  │   │
│  │  └───────────┬────────────┘  │   │
│  │              │               │   │
│  │  ┌───────────▼────────────┐  │   │
│  │  │  Ktor Backend          │  │   │
│  │  │  (HappyRow Core)       │  │   │
│  │  └───────────┬────────────┘  │   │
│  │              │               │   │
│  │  ┌───────────▼────────────┐  │   │
│  │  │  PostgreSQL Database   │  │   │
│  │  └────────────────────────┘  │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

## Prerequisites

### 1. Secured Raspberry Pi
✅ Complete the security hardening guide first: [RASPBERRY_PI_SECURITY.md](./RASPBERRY_PI_SECURITY.md)

### 2. Required Software
- Docker and Docker Compose installed on Raspberry Pi
- SSH access to Raspberry Pi
- Cloudflare account (free tier works)

### 3. Domain Requirements
- A domain name (can use Cloudflare's free subdomain)
- Domain configured in Cloudflare (if using custom domain)

---

## Step 1: Prepare Your Raspberry Pi

### 1.1 Connect to Your Raspberry Pi

```bash
# SSH into your Pi (use your custom port if changed)
ssh -p 2222 happyrow@<raspberry-pi-ip>
```

### 1.2 Create Project Directory

```bash
# Create directory for the application
mkdir -p ~/happyrow-core
cd ~/happyrow-core
```

### 1.3 Clone or Transfer Project Files

**Option A: Clone from GitHub**
```bash
git clone https://github.com/your-username/happyrow-core.git .
```

**Option B: Transfer files from local machine**
```bash
# From your local machine
rsync -avz -e "ssh -p 2222" \
  --exclude 'build' \
  --exclude '.gradle' \
  --exclude 'node_modules' \
  /Users/j.ni/IdeaProjects/happyrow-core/ \
  happyrow@<raspberry-pi-ip>:~/happyrow-core/
```

---

## Step 2: Set Up Cloudflare Tunnel

### 2.1 Create Cloudflare Account
1. Go to [Cloudflare](https://dash.cloudflare.com/)
2. Sign up for a free account if you don't have one

### 2.2 Create a Tunnel

1. Navigate to **Zero Trust** dashboard: https://one.dash.cloudflare.com/
2. Go to **Access** → **Tunnels**
3. Click **Create a tunnel**
4. Choose **Cloudflared** as the connector
5. Give it a name: `happyrow-rpi`
6. Click **Save tunnel**

### 2.3 Get Tunnel Token

1. After creating the tunnel, you'll see a token
2. Copy the full token (it starts with `eyJ...`)
3. **Save this token securely** - you'll need it for the `.env` file

### 2.4 Configure Public Hostname

1. In the tunnel configuration, add a **Public hostname**:
   - **Subdomain**: `api` (or your choice)
   - **Domain**: Choose your domain (or use free Cloudflare subdomain: `<your-name>.trycloudflare.com`)
   - **Service**: 
     - Type: `HTTP`
     - URL: `nginx:80` (this is the Docker container name)
2. Click **Save**

### 2.5 Additional Tunnel Settings (Recommended)

In the tunnel settings, configure:
- **TLS verification**: Enabled
- **HTTP/2**: Enabled
- **WebSockets**: Enabled (if needed)

---

## Step 3: Configure Environment Variables

### 3.1 Create Environment File

```bash
cd ~/happyrow-core/.raspberry
cp .env.example .env
nano .env
```

### 3.2 Fill in Your Values

```bash
# Database Configuration
DB_NAME=happyrow_db
DB_USERNAME=happyrow_user
DB_PASSWORD=<GENERATE_STRONG_PASSWORD>  # Use: openssl rand -base64 32

# CORS Configuration
ALLOWED_ORIGINS=https://your-vercel-app.vercel.app,https://api.your-domain.com

# Cloudflare Tunnel Token
CLOUDFLARE_TUNNEL_TOKEN=<YOUR_CLOUDFLARE_TUNNEL_TOKEN>
```

**Generate secure password:**
```bash
openssl rand -base64 32
```

### 3.3 Secure the Environment File

```bash
chmod 600 .env
```

---

## Step 4: Build and Deploy

### 4.1 Navigate to Raspberry Pi Directory

```bash
cd ~/happyrow-core/.raspberry
```

### 4.2 Build the Application

```bash
# Build Docker images (this takes 15-30 minutes on RPi 3)
docker-compose build --no-cache
```

**Note:** The build process is CPU-intensive on Raspberry Pi 3. Be patient!

### 4.3 Start All Services

```bash
# Start all containers
docker-compose up -d

# Follow logs
docker-compose logs -f
```

### 4.4 Verify Services Are Running

```bash
# Check container status
docker-compose ps

# Should show all 4 containers running:
# - happyrow-postgres
# - happyrow-backend
# - happyrow-nginx
# - happyrow-cloudflared
```

### 4.5 Check Health

```bash
# Check backend health
curl http://localhost/health

# Expected response:
# {"status":"UP","timestamp":"..."}

# Check container logs
docker-compose logs backend
docker-compose logs cloudflared
```

---

## Step 5: Test External Access

### 5.1 Test Cloudflare Tunnel

```bash
# From your local machine (not the Pi)
curl https://api.your-domain.com/health

# Should return:
# {"status":"UP","timestamp":"..."}
```

### 5.2 Test from Browser

Open your browser and visit:
- https://api.your-domain.com/health
- https://api.your-domain.com/info

---

## Step 6: Configure Vercel Frontend

### 6.1 Update Vercel Environment Variables

In your Vercel project settings, add:

```
NEXT_PUBLIC_API_URL=https://api.your-domain.com
```

### 6.2 Update CORS Settings

Ensure your `.raspberry/.env` file includes your Vercel domain:

```bash
ALLOWED_ORIGINS=https://your-app.vercel.app,https://your-app-git-main.vercel.app
```

### 6.3 Restart Backend with New CORS Settings

```bash
cd ~/happyrow-core/.raspberry
docker-compose restart backend
```

---

## Step 7: Monitoring and Maintenance

### 7.1 View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f postgres
docker-compose logs -f cloudflared
```

### 7.2 Check Resource Usage

```bash
# Overall system
htop

# Docker containers
docker stats

# Disk space
df -h
```

### 7.3 Database Backup

```bash
# Create backup script
nano ~/backup-db.sh
```

**Add this content:**
```bash
#!/bin/bash
BACKUP_DIR=~/backups/db
mkdir -p $BACKUP_DIR
DATE=$(date +%Y%m%d_%H%M%S)

docker exec happyrow-postgres pg_dump -U happyrow_user happyrow_db > \
  $BACKUP_DIR/happyrow_backup_$DATE.sql

# Keep only last 7 days
find $BACKUP_DIR -name "happyrow_backup_*.sql" -mtime +7 -delete

echo "Backup completed: $BACKUP_DIR/happyrow_backup_$DATE.sql"
```

**Make executable and add to crontab:**
```bash
chmod +x ~/backup-db.sh

# Add to crontab (daily at 2 AM)
crontab -e
# Add line:
0 2 * * * /home/happyrow/backup-db.sh >> /home/happyrow/backup-db.log 2>&1
```

### 7.4 Update Application

```bash
cd ~/happyrow-core

# Pull latest changes
git pull origin main

# Rebuild and restart
cd .raspberry
docker-compose build backend
docker-compose up -d backend
```

---

## Step 8: Performance Optimization for Raspberry Pi 3

### 8.1 PostgreSQL Tuning

Edit database settings for RPi:
```bash
nano ~/happyrow-core/.raspberry/postgres.conf
```

Add:
```
# Raspberry Pi 3 optimizations
shared_buffers = 64MB
effective_cache_size = 256MB
maintenance_work_mem = 32MB
checkpoint_completion_target = 0.9
wal_buffers = 4MB
default_statistics_target = 50
random_page_cost = 1.1
effective_io_concurrency = 200
work_mem = 2MB
min_wal_size = 512MB
max_wal_size = 1GB
```

Update docker-compose.yml to use this config:
```yaml
postgres:
  volumes:
    - ./postgres.conf:/etc/postgresql/postgresql.conf:ro
  command: postgres -c config_file=/etc/postgresql/postgresql.conf
```

### 8.2 Enable Swap (if needed)

```bash
# Check current swap
free -h

# If swap is low, increase it
sudo dphys-swapfile swapoff
sudo nano /etc/dphys-swapfile
# Change: CONF_SWAPSIZE=2048
sudo dphys-swapfile setup
sudo dphys-swapfile swapon
```

### 8.3 Monitor Temperature

```bash
# Check temperature
vcgencmd measure_temp

# Add cooling if temperature exceeds 70°C regularly
```

---

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker-compose logs <service-name>

# Check container status
docker-compose ps

# Restart specific service
docker-compose restart <service-name>

# Full restart
docker-compose down
docker-compose up -d
```

### Cloudflare Tunnel Not Connecting

```bash
# Check cloudflared logs
docker-compose logs cloudflared

# Verify token is correct
docker-compose exec cloudflared cloudflared tunnel info

# Restart tunnel
docker-compose restart cloudflared
```

### Database Connection Issues

```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Check if database is ready
docker-compose exec postgres pg_isready -U happyrow_user

# Connect to database manually
docker-compose exec postgres psql -U happyrow_user -d happyrow_db
```

### Backend Not Responding

```bash
# Check backend logs
docker-compose logs backend

# Check backend health
curl http://localhost:8080/health

# Check NGINX logs
docker-compose logs nginx

# Restart backend
docker-compose restart backend
```

### Out of Memory Issues

```bash
# Check memory usage
free -h
docker stats

# Reduce Docker memory limits in docker-compose.yml
# Restart with lower limits
docker-compose down
docker-compose up -d
```

### CORS Issues from Vercel

```bash
# Verify ALLOWED_ORIGINS in .env
cat .env | grep ALLOWED_ORIGINS

# Check NGINX CORS headers
docker-compose logs nginx | grep CORS

# Test CORS manually
curl -H "Origin: https://your-vercel-app.vercel.app" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS \
     https://api.your-domain.com/health -v
```

---

## Security Best Practices

1. **Never expose PostgreSQL port** - Keep it internal to Docker network
2. **Use Cloudflare Tunnel only** - No port forwarding on your router
3. **Regular updates**:
   ```bash
   sudo apt update && sudo apt upgrade -y
   docker-compose pull
   docker-compose up -d
   ```
4. **Monitor logs regularly**:
   ```bash
   sudo journalctl -u docker
   docker-compose logs --tail=100
   ```
5. **Rotate secrets periodically** - Change DB password, regenerate tunnel token
6. **Enable Cloudflare security features**:
   - WAF (Web Application Firewall)
   - Rate limiting
   - DDoS protection

---

## Cost Analysis

### Raspberry Pi 3 Resource Usage
- **CPU**: ~30-50% average (spikes to 100% during requests)
- **RAM**: ~800MB used (out of 1GB)
- **Storage**: ~5GB for Docker images and data
- **Power**: ~3W average = ~$0.30/month (at $0.12/kWh)

### Cloudflare Costs
- **Zero Trust Tunnel**: FREE (up to 50 users)
- **Cloudflare DNS**: FREE
- **Bandwidth**: FREE (unlimited)

### Total Monthly Cost
- **~$0.30** (just electricity for Raspberry Pi!)

---

## Backup and Disaster Recovery

### Full System Backup

```bash
# Backup script
nano ~/full-backup.sh
```

```bash
#!/bin/bash
BACKUP_DIR=~/backups/full
mkdir -p $BACKUP_DIR
DATE=$(date +%Y%m%d_%H%M%S)

echo "Starting full backup..."

# Database
docker exec happyrow-postgres pg_dump -U happyrow_user happyrow_db > \
  $BACKUP_DIR/db_$DATE.sql

# Docker volumes
docker run --rm -v happyrow_postgres_data:/data -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/postgres_volume_$DATE.tar.gz /data

# Application config
tar czf $BACKUP_DIR/config_$DATE.tar.gz ~/.raspberry/.env ~/happyrow-core/.raspberry/

echo "Backup completed: $BACKUP_DIR"
```

### Restore from Backup

```bash
# Stop services
cd ~/happyrow-core/.raspberry
docker-compose down

# Restore database
cat ~/backups/full/db_YYYYMMDD_HHMMSS.sql | \
  docker exec -i happyrow-postgres psql -U happyrow_user -d happyrow_db

# Restore config
tar xzf ~/backups/full/config_YYYYMMDD_HHMMSS.tar.gz -C ~

# Start services
docker-compose up -d
```

---

## Next Steps

1. ✅ **Test your application** thoroughly from the Vercel frontend
2. ✅ **Set up monitoring** (optional): Prometheus + Grafana
3. ✅ **Configure alerts** for system health
4. ✅ **Document your specific API endpoints**
5. ✅ **Set up automated backups** (cron job)

---

## Useful Commands Reference

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Restart service
docker-compose restart <service>

# View logs
docker-compose logs -f <service>

# Check status
docker-compose ps

# Update and restart
git pull && docker-compose up -d --build

# Clean up
docker system prune -af

# Database access
docker-compose exec postgres psql -U happyrow_user -d happyrow_db

# Backend shell
docker-compose exec backend sh

# Check Raspberry Pi temperature
vcgencmd measure_temp

# Check resource usage
htop
docker stats
```

---

## Additional Resources

- [Cloudflare Tunnel Documentation](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [NGINX Reverse Proxy Guide](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)
- [Raspberry Pi Documentation](https://www.raspberrypi.org/documentation/)

---

## Support

If you encounter issues:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review logs: `docker-compose logs -f`
3. Check Raspberry Pi resources: `htop`, `free -h`, `df -h`
4. Verify Cloudflare Tunnel status in Zero Trust dashboard
5. Test internal connectivity before external access

---

**Congratulations!** 🎉 Your HappyRow Core backend is now running securely on your Raspberry Pi with global access via Cloudflare Tunnel!
