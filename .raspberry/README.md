# HappyRow Core - Raspberry Pi Deployment

This directory contains all the necessary configuration files to deploy HappyRow Core on a Raspberry Pi 3 with secure external access via Cloudflare Tunnel.

## 📁 Files Overview

- **`Dockerfile`** - ARM-optimized Docker image for Raspberry Pi
- **`docker-compose.yml`** - Complete stack with backend, PostgreSQL, NGINX, and Cloudflare Tunnel
- **`nginx.conf`** - NGINX reverse proxy configuration with CORS and security headers
- **`.env.example`** - Template for environment variables
- **`deploy.sh`** - Automated deployment script
- **`README.md`** - This file

## 🚀 Quick Start

### Prerequisites
1. Complete the security hardening guide: [docs/RASPBERRY_PI_SECURITY.md](../docs/RASPBERRY_PI_SECURITY.md)
2. Ensure Docker and Docker Compose are installed
3. Have a Cloudflare account ready

### 1. Set Up Environment Variables

```bash
# Copy the example file
cp .env.example .env

# Edit with your values
nano .env
```

Required values:
- `DB_PASSWORD` - Generate with: `openssl rand -base64 32`
- `CLOUDFLARE_TUNNEL_TOKEN` - Get from Cloudflare Zero Trust dashboard
- `ALLOWED_ORIGINS` - Your Vercel frontend URL(s)

### 2. Deploy

```bash
# Make deploy script executable
chmod +x deploy.sh

# Run deployment
./deploy.sh
```

The script will:
- Check system requirements
- Build Docker images (15-30 minutes on RPi 3)
- Start all services
- Wait for health checks
- Display status

### 3. Verify Deployment

```bash
# Check services are running
docker-compose ps

# Test locally
curl http://localhost/health

# Test externally (use your Cloudflare domain)
curl https://api.your-domain.com/health
```

## 🛠️ Common Commands

### Service Management

```bash
# Start services
./deploy.sh start
# or
docker-compose up -d

# Stop services
./deploy.sh stop
# or
docker-compose down

# Restart services
./deploy.sh restart
# or
docker-compose restart

# View logs
./deploy.sh logs
# or
docker-compose logs -f

# Check status
./deploy.sh status
# or
docker-compose ps
```

### Updates

```bash
# Update application
./deploy.sh update
# or manually:
git pull origin main
docker-compose build backend
docker-compose up -d backend
```

### Cleanup

```bash
# Clean up Docker (careful!)
./deploy.sh clean
```

### Troubleshooting

```bash
# View specific service logs
docker-compose logs -f backend
docker-compose logs -f postgres
docker-compose logs -f cloudflared
docker-compose logs -f nginx

# Check container health
docker-compose exec backend wget -qO- http://localhost:8080/health
docker-compose exec postgres pg_isready -U happyrow_user

# Restart specific service
docker-compose restart backend

# Check system resources
htop
docker stats
free -h
df -h
```

## 📊 Architecture

```
Internet
    ↓
Cloudflare Zero Trust (Tunnel)
    ↓
Raspberry Pi
    ↓
cloudflared → NGINX → Backend (Ktor) → PostgreSQL
```

### Container Network

All containers run on a private Docker network:
- **postgres:5432** - Database (internal only)
- **backend:8080** - Ktor application (internal only)
- **nginx:80/443** - Reverse proxy (internal only)
- **cloudflared** - Tunnel to Cloudflare (public access)

## 🔒 Security Features

1. **No port forwarding** - Uses Cloudflare Tunnel instead
2. **Internal database** - PostgreSQL not exposed externally
3. **NGINX rate limiting** - Protects against DDoS
4. **CORS configured** - Only allows specified origins
5. **Security headers** - X-Frame-Options, CSP, etc.
6. **Non-root containers** - All services run as non-root users
7. **Resource limits** - Memory limits prevent resource exhaustion

## 📝 Configuration

### Environment Variables

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `DB_NAME` | No | Database name | `happyrow_db` |
| `DB_USERNAME` | No | Database user | `happyrow_user` |
| `DB_PASSWORD` | **Yes** | Database password | Generate with openssl |
| `ALLOWED_ORIGINS` | **Yes** | Comma-separated list of allowed origins | `https://app.vercel.app` |
| `CLOUDFLARE_TUNNEL_TOKEN` | **Yes** | Cloudflare Tunnel token | Get from dashboard |

### Resource Limits

Optimized for Raspberry Pi 3 (1GB RAM):
- **PostgreSQL**: 256MB max
- **Backend**: 512MB max
- **NGINX**: 64MB max
- **Cloudflared**: 64MB max

Total: ~896MB (leaving ~128MB for system)

## 🔄 Backup & Restore

### Manual Database Backup

```bash
# Backup
docker exec happyrow-postgres pg_dump -U happyrow_user happyrow_db > backup.sql

# Restore
cat backup.sql | docker exec -i happyrow-postgres psql -U happyrow_user -d happyrow_db
```

### Automated Backups

Set up cron job:
```bash
crontab -e
# Add:
0 2 * * * cd ~/happyrow-core/.raspberry && docker exec happyrow-postgres pg_dump -U happyrow_user happyrow_db > ~/backups/db_$(date +\%Y\%m\%d).sql
```

## 📈 Monitoring

### Check Resource Usage

```bash
# CPU and memory
htop

# Docker containers
docker stats

# Disk space
df -h

# Temperature (Raspberry Pi)
vcgencmd measure_temp
```

### Health Endpoints

- `/health` - Service health status
- `/info` - Application information

### Logs Location

```bash
# Docker logs
docker-compose logs -f

# NGINX access logs
docker-compose exec nginx cat /var/log/nginx/access.log

# NGINX error logs
docker-compose exec nginx cat /var/log/nginx/error.log

# System logs
journalctl -u docker
```

## 🐛 Troubleshooting

### Services Not Starting

1. Check logs: `docker-compose logs -f`
2. Check .env file: `cat .env`
3. Check disk space: `df -h`
4. Check memory: `free -h`
5. Restart: `docker-compose restart`

### Cloudflare Tunnel Not Connecting

1. Verify token is correct in .env
2. Check logs: `docker-compose logs cloudflared`
3. Verify tunnel in Cloudflare dashboard
4. Restart: `docker-compose restart cloudflared`

### Backend Not Responding

1. Check logs: `docker-compose logs backend`
2. Check database connection: `docker-compose exec postgres pg_isready`
3. Check memory: `docker stats`
4. Restart: `docker-compose restart backend`

### Database Connection Errors

1. Check PostgreSQL is running: `docker-compose ps postgres`
2. Check credentials in .env match
3. Check logs: `docker-compose logs postgres`
4. Restart: `docker-compose restart postgres`

### High CPU/Memory Usage

1. Check container stats: `docker stats`
2. Check temperature: `vcgencmd measure_temp`
3. Consider reducing memory limits
4. Add cooling if temperature > 75°C

### CORS Errors from Frontend

1. Verify ALLOWED_ORIGINS in .env includes your domain
2. Check NGINX logs: `docker-compose logs nginx`
3. Test CORS manually:
   ```bash
   curl -H "Origin: https://your-app.vercel.app" \
        -H "Access-Control-Request-Method: GET" \
        -X OPTIONS \
        https://api.your-domain.com/health -v
   ```

## 📚 Documentation

- **[Security Guide](../docs/RASPBERRY_PI_SECURITY.md)** - Harden your Raspberry Pi
- **[Deployment Guide](../docs/RASPBERRY_PI_DEPLOYMENT.md)** - Complete deployment instructions
- **[Cloudflare Tunnel Docs](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/)** - Official documentation

## 💡 Tips & Best Practices

1. **Regular updates**: Run `sudo apt update && sudo apt upgrade` weekly
2. **Monitor temperature**: Add cooling if consistently > 70°C
3. **Backup regularly**: Set up automated database backups
4. **Check logs**: Review logs periodically for errors
5. **Resource monitoring**: Use `htop` and `docker stats` to monitor resources
6. **Security scans**: Run security checks monthly
7. **Test backups**: Verify backups work by testing restore
8. **Document changes**: Keep notes of any configuration changes
9. **Use version control**: Commit configuration changes to git
10. **Keep secrets safe**: Never commit .env to git

## 🆘 Support

If you need help:
1. Check the troubleshooting section above
2. Review logs: `docker-compose logs -f`
3. Check documentation in `/docs` folder
4. Verify all prerequisites are met
5. Test internal connectivity before external

## 📊 Performance Benchmarks (Raspberry Pi 3)

- **Build time**: 15-30 minutes
- **Startup time**: 30-60 seconds
- **Memory usage**: ~800MB
- **CPU usage**: 30-50% average
- **Power consumption**: ~3W
- **Cost**: ~$0.30/month (electricity only!)

## 🎉 Success Criteria

Your deployment is successful when:
- ✅ All containers show "Up" status
- ✅ `/health` endpoint returns 200 OK
- ✅ Cloudflare Tunnel shows "Connected" in dashboard
- ✅ External access works: `curl https://api.your-domain.com/health`
- ✅ Frontend can make API calls successfully
- ✅ CORS works correctly
- ✅ Database queries execute without errors

---

**Made with ❤️ for Raspberry Pi deployment**
