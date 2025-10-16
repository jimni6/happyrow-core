# 🚀 Raspberry Pi Quick Start Guide

**Fast-track deployment of HappyRow Core on Raspberry Pi with Cloudflare Tunnel**

## ⚡ Prerequisites Checklist

- [ ] Raspberry Pi 3 with Raspberry Pi OS (64-bit)
- [ ] SSH access configured
- [ ] Cloudflare account (free)
- [ ] Domain name or use Cloudflare's free subdomain

---

## 📋 Step-by-Step Deployment

### 1️⃣ Secure Your Raspberry Pi (Essential!)

**On your Raspberry Pi:**

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Change default password
passwd

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt install docker-compose -y

# Setup firewall
sudo apt install ufw -y
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 2222/tcp comment 'SSH'
sudo ufw enable

# Install Fail2Ban
sudo apt install fail2ban -y
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# Reboot to apply changes
sudo reboot
```

**📖 For complete security hardening, see:** [RASPBERRY_PI_SECURITY.md](./RASPBERRY_PI_SECURITY.md)

---

### 2️⃣ Set Up Cloudflare Tunnel

1. **Go to Cloudflare Zero Trust:**
   - Visit: https://one.dash.cloudflare.com/
   - Navigate to **Access** → **Tunnels**

2. **Create a Tunnel:**
   - Click **Create a tunnel**
   - Name it: `happyrow-rpi`
   - Click **Save tunnel**

3. **Copy the Token:**
   - You'll see a token starting with `eyJ...`
   - **Copy and save this token**

4. **Configure Public Hostname:**
   - **Subdomain**: `api`
   - **Domain**: Your domain (or free subdomain)
   - **Service**: Type = `HTTP`, URL = `nginx:80`
   - Click **Save**

---

### 3️⃣ Deploy HappyRow Core

**On your Raspberry Pi:**

```bash
# Create project directory
mkdir -p ~/happyrow-core
cd ~/happyrow-core

# Clone or transfer project files
# Option A: From GitHub
git clone https://github.com/your-username/happyrow-core.git .

# Option B: From local machine (run on your Mac)
rsync -avz -e "ssh -p 2222" \
  --exclude 'build' --exclude '.gradle' \
  /Users/j.ni/IdeaProjects/happyrow-core/ \
  happyrow@<raspberry-pi-ip>:~/happyrow-core/

# Navigate to deployment directory
cd ~/happyrow-core/.raspberry

# Create environment file
cp .env.example .env
nano .env
```

**Fill in `.env` with your values:**

```bash
# Generate secure password
DB_PASSWORD=$(openssl rand -base64 32)

# Your Cloudflare token from step 2
CLOUDFLARE_TUNNEL_TOKEN=eyJhxxxxxxx...

# Your Vercel frontend URL
ALLOWED_ORIGINS=https://your-app.vercel.app
```

**Make deploy script executable and run:**

```bash
chmod +x deploy.sh
./deploy.sh
```

**This will take 15-30 minutes. Go grab a coffee! ☕**

---

### 4️⃣ Verify Deployment

**Test locally on Raspberry Pi:**

```bash
curl http://localhost/health
# Expected: {"status":"UP","timestamp":"..."}
```

**Test externally from your Mac:**

```bash
curl https://api.your-domain.com/health
# Expected: {"status":"UP","timestamp":"..."}
```

**Check all services are running:**

```bash
docker-compose ps
# All should show "Up" status
```

---

### 5️⃣ Configure Vercel Frontend

**In your Vercel project settings:**

1. Go to **Settings** → **Environment Variables**
2. Add:
   ```
   NEXT_PUBLIC_API_URL=https://api.your-domain.com
   ```
3. Redeploy your frontend

**Update CORS if needed:**

If your Vercel has multiple URLs (preview, production), update `.raspberry/.env`:

```bash
ALLOWED_ORIGINS=https://your-app.vercel.app,https://your-app-git-main-yourname.vercel.app
```

Then restart:

```bash
docker-compose restart backend
```

---

## 🎯 Success Checklist

- [ ] Raspberry Pi is secured (firewall, fail2ban)
- [ ] Cloudflare Tunnel is connected
- [ ] All containers are running (`docker-compose ps`)
- [ ] Health endpoint works locally: `curl http://localhost/health`
- [ ] Health endpoint works externally: `curl https://api.your-domain.com/health`
- [ ] Vercel frontend can call API successfully
- [ ] CORS is working (no errors in browser console)

---

## 📊 Quick Commands Reference

```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend
docker-compose logs -f cloudflared

# Restart services
docker-compose restart

# Stop all services
docker-compose down

# Start all services
docker-compose up -d

# Check status
docker-compose ps

# Check system resources
htop
docker stats

# Check temperature (RPi)
vcgencmd measure_temp

# Update application
cd ~/happyrow-core
git pull origin main
cd .raspberry
docker-compose build backend
docker-compose up -d backend
```

---

## 🐛 Common Issues & Fixes

### ❌ Build Failed

```bash
# Check disk space
df -h
# If low, clean up:
docker system prune -af

# Retry build
docker-compose build --no-cache
```

### ❌ Cloudflare Tunnel Not Connected

```bash
# Check token in .env
cat .env | grep CLOUDFLARE_TUNNEL_TOKEN

# Check logs
docker-compose logs cloudflared

# Restart tunnel
docker-compose restart cloudflared
```

### ❌ Backend Not Responding

```bash
# Check logs for errors
docker-compose logs backend

# Check database is ready
docker-compose exec postgres pg_isready

# Restart backend
docker-compose restart backend
```

### ❌ CORS Errors from Frontend

```bash
# Verify ALLOWED_ORIGINS
cat .env | grep ALLOWED_ORIGINS

# Should include your Vercel domain
# Update if needed, then:
docker-compose restart backend
```

### ❌ Out of Memory

```bash
# Check memory
free -h

# Check container usage
docker stats

# If needed, reduce memory limits in docker-compose.yml
# Restart services
docker-compose restart
```

---

## 🔒 Security Tips

1. **Never expose PostgreSQL port** - Keep it internal
2. **Use Cloudflare Tunnel only** - No port forwarding
3. **Change SSH port** - Use non-standard port
4. **Enable Fail2Ban** - Protect against brute force
5. **Regular updates**:
   ```bash
   sudo apt update && sudo apt upgrade -y
   docker-compose pull
   docker-compose up -d
   ```
6. **Monitor logs**:
   ```bash
   sudo fail2ban-client status sshd
   docker-compose logs --tail=100
   ```

---

## 💾 Backup Your Database

**Manual backup:**

```bash
docker exec happyrow-postgres pg_dump -U happyrow_user happyrow_db > backup.sql
```

**Automated daily backup (cron):**

```bash
crontab -e
# Add:
0 2 * * * docker exec happyrow-postgres pg_dump -U happyrow_user happyrow_db > ~/backups/db_$(date +\%Y\%m\%d).sql
```

---

## 📈 Monitor Your System

**Create a monitoring script:**

```bash
nano ~/check-system.sh
```

```bash
#!/bin/bash
echo "=== System Status ==="
echo "Temperature: $(vcgencmd measure_temp)"
echo "Memory: $(free -h | awk 'NR==2 {print $3 "/" $2}')"
echo "Disk: $(df -h / | awk 'NR==2 {print $3 "/" $2 " (" $5 ")"}')"
echo ""
echo "=== Docker Containers ==="
docker-compose -f ~/happyrow-core/.raspberry/docker-compose.yml ps
echo ""
echo "=== Health Check ==="
curl -s http://localhost/health | jq .
```

```bash
chmod +x ~/check-system.sh
./check-system.sh
```

---

## 📚 Full Documentation

- **[Complete Deployment Guide](./RASPBERRY_PI_DEPLOYMENT.md)** - Detailed instructions
- **[Security Hardening](./RASPBERRY_PI_SECURITY.md)** - Comprehensive security setup
- **[.raspberry/README.md](../.raspberry/README.md)** - Technical reference

---

## 🎉 You're Done!

Your HappyRow Core backend is now:
- ✅ Running on Raspberry Pi
- ✅ Accessible globally via Cloudflare Tunnel
- ✅ Secured with no port forwarding
- ✅ Connected to your Vercel frontend
- ✅ Costing only ~$0.30/month in electricity!

**Need help?** Check the troubleshooting sections in the full docs!

---

## 🚀 Next Steps

1. Test all your API endpoints
2. Set up automated backups
3. Configure monitoring alerts
4. Add your custom business logic
5. Scale as needed!

**Happy coding! 🎯**
