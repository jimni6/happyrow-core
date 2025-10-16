# 🥧 Raspberry Pi Deployment - Complete Setup Resume

**Date:** 2025-10-15  
**Project:** HappyRow Core Backend on Raspberry Pi 3  
**Status:** Ready for deployment (waiting for proper 5V 3A power supply)

---

## 📊 Current System Status

| Component | Status | Notes |
|-----------|--------|-------|
| **Raspberry Pi** | ✅ Fresh install | Raspberry Pi OS (64-bit) |
| **SD Card** | ✅ Fresh flash | 16GB, 7.2GB free |
| **System Updated** | ✅ Complete | All packages up-to-date |
| **Docker** | ✅ Installed | v26.1.5 |
| **Docker Compose** | ✅ Installed | v2.24.0 |
| **Security** | ✅ Configured | UFW + Fail2Ban |
| **WiFi** | ✅ Disabled | Power savings |
| **Bluetooth** | ✅ Disabled | Power savings |
| **Project Files** | ❌ Not transferred yet | Next step |
| **Power Supply** | ⚠️ **INSUFFICIENT** | Need 5V 3A! |

**⚠️ CRITICAL:** `throttled=0x50005` - Active under-voltage and CPU throttling

---

## 🔌 IMPORTANT: Power Supply Issue

**Current Status:** Active throttling detected (`0x50005`)

**What this means:**
- Your current power supply is insufficient
- CPU is being throttled RIGHT NOW
- Risk of crashes, corruption, and failed deployment

**Required:**
- **5V 3A power supply** for Raspberry Pi 3
- Official Raspberry Pi PSU or equivalent quality
- ~10-15€, 1-day delivery

**Where to buy (France):**
- Amazon.fr: "Raspberry Pi 3 alimentation officielle"
- Kubii.fr (official reseller)
- LDLC / Materiel.net
- Local Fnac/Darty

**DO NOT proceed with deployment until you have proper power!**

---

## 🖥️ Network Configuration

- **IP Address:** 192.168.1.79
- **Hostname:** raspberrypi.local
- **Username:** j.ni
- **Connection:** Ethernet (eth0)
- **SSH Port:** 22

---

## ✅ Completed Setup Steps

### 1. Fresh SD Card Install
```bash
# On Mac - Used Raspberry Pi Imager
# OS: Raspberry Pi OS (64-bit)
# Configured: SSH, username, password, locale
```

### 2. First Boot & SSH Connection
```bash
# Find Raspberry Pi
arp -a | grep -i "b8:27:eb\|dc:a6:32\|e4:5f:01"
# Found: 192.168.1.79

# Remove old SSH keys
ssh-keygen -R raspberrypi.local
ssh-keygen -R 192.168.1.79

# Connect
ssh j.ni@192.168.1.79
```

### 3. Initial Health Check
```bash
# Check power/throttling
sudo vcgencmd get_throttled
# Result: throttled=0x50000 (historical only at first)

# Check temperature
sudo vcgencmd measure_temp
# Result: 47.2'C (good)

# Check disk space
df -h
# Result: 7.5G available (good)
```

### 4. System Update
```bash
# Update package lists
sudo apt update

# Upgrade all packages
sudo apt full-upgrade -y

# Fix broken package (shared-mime-info)
sudo dpkg --configure -a

# Clean up
sudo apt autoremove -y
sudo apt clean

# Reboot
sudo reboot
```

### 5. Locale Fix
```bash
# Fix locale warnings
export LC_ALL=C
echo "export LC_ALL=C" >> ~/.bashrc
```

### 6. Power Optimization
```bash
# Disable WiFi (using Ethernet)
sudo rfkill block wifi

# Verify WiFi is off
ip link show wlan0 | head -1
# Result: state DOWN (good)

# Disable Bluetooth
sudo systemctl disable bluetooth
sudo systemctl stop bluetooth
```

### 7. Security Configuration
```bash
# Install firewall
sudo apt install -y ufw

# Configure UFW
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp comment 'SSH'
sudo ufw --force enable

# Verify firewall
sudo ufw status
# Result: Active, SSH port 22 allowed

# Install Fail2Ban
sudo apt install -y fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# Verify Fail2Ban
sudo systemctl is-active fail2ban
# Result: active
```

### 8. Docker Installation
```bash
# Clean any previous Docker attempts
sudo systemctl stop docker 2>/dev/null
sudo dpkg --remove --force-remove-reinstreq docker-buildx docker-compose docker-buildx-plugin docker-compose-plugin docker.io docker-ce docker-ce-cli containerd.io containerd 2>/dev/null
sudo apt purge -y docker* containerd* 2>/dev/null
sudo dpkg --configure -a
sudo apt --fix-broken install -y
sudo apt clean && sudo apt update

# Install Docker.io
sudo apt install -y docker.io

# Start and enable Docker
sudo systemctl start docker
sudo systemctl enable docker

# Add user to docker group
sudo usermod -aG docker $USER

# Install docker-compose binary
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify installation
sudo docker --version
# Result: Docker version 26.1.5+dfsg1, build a72d7cd

docker-compose --version
# Result: Docker Compose version v2.24.0

# Log out and back in
exit
ssh j.ni@192.168.1.79

# Test Docker without sudo
docker ps
# Result: Works! (no containers running)
```

### 9. Final Health Check
```bash
echo "=== System Ready for Deployment ===" && \
docker --version && \
docker-compose --version && \
docker ps && \
echo "" && \
sudo vcgencmd get_throttled && \
sudo vcgencmd measure_temp && \
df -h /

# Results:
# - Docker: v26.1.5
# - Docker Compose: v2.24.0
# - throttled: 0x50005 (ACTIVE THROTTLING - PROBLEM!)
# - temp: 46.7'C (good)
# - disk: 7.2G free (good)
```

---

## ⚠️ Issue Identified: Power Supply

**Final status check revealed:**
- `throttled=0x50005` - **Active under-voltage and CPU throttling**
- This will cause deployment failures and potential data corruption
- **MUST get proper 5V 3A power supply before continuing**

---

## 🚀 Next Steps (After Getting Proper PSU)

### Step 1: Power On with New PSU
```bash
# Connect new 5V 3A power supply
# Boot Raspberry Pi
# Wait 2-3 minutes

# SSH in
ssh j.ni@192.168.1.79

# CRITICAL: Verify throttling is gone
sudo vcgencmd get_throttled
# MUST show: throttled=0x0 (or 0x0 without any bits set)
# If still throttled, PSU is still insufficient!
```

### Step 2: Transfer Project Files

**On your Mac:**
```bash
cd /Users/j.ni/IdeaProjects/happyrow-core/.raspberry

# Make transfer script executable
chmod +x transfer-to-pi.sh

# Run transfer
./transfer-to-pi.sh

# When prompted:
# IP: 192.168.1.79
# User: j.ni
# Port: 22
# Directory: ~/happyrow-core
```

### Step 3: Configure Environment

**On Raspberry Pi:**
```bash
cd ~/happyrow-core/.raspberry

# Create environment file
cp .env.example .env

# Generate secure database password
openssl rand -base64 32
# Save this output!

# Edit environment file
nano .env
```

**Fill in .env:**
```bash
# Database
DB_NAME=happyrow_db
DB_USERNAME=happyrow_user
DB_PASSWORD=<paste_generated_password_here>

# CORS for Vercel frontend
ALLOWED_ORIGINS=https://happyrow-front.vercel.app,https://happyrow-front-jimni6s-projects.vercel.app,https://happyrow-front-git-main-jimni6s-projects.vercel.app

# Cloudflare Tunnel Token (add after creating tunnel)
CLOUDFLARE_TUNNEL_TOKEN=<will_add_next>
```

**Save:** Ctrl+O, Enter, Ctrl+X

### Step 4: Setup Cloudflare Tunnel

**In browser:**
1. Go to: https://one.dash.cloudflare.com/
2. Login to Cloudflare account
3. Navigate: **Access** → **Tunnels**
4. Click: **Create a tunnel**
5. Select: **Cloudflared**
6. Name: `happyrow-rpi`
7. Click: **Save tunnel**
8. **COPY THE TOKEN** (starts with `eyJ...`)
9. Configure **Public Hostname**:
   - **Subdomain:** `api` (or your choice)
   - **Domain:** Select your domain
   - **Service:**
     - Type: `HTTP`
     - URL: `nginx:80`
10. Click: **Save hostname**
11. Note your API URL: `https://api.your-domain.com`

**Add token to .env:**
```bash
# On Raspberry Pi
nano ~/happyrow-core/.raspberry/.env

# Add the token:
CLOUDFLARE_TUNNEL_TOKEN=eyJ... (your actual token)

# Save: Ctrl+O, Enter, Ctrl+X
```

### Step 5: Deploy with Monitoring

**Terminal 1 - Monitoring (keep open):**
```bash
# Watch system health continuously
watch -n 2 'date; echo ""; sudo vcgencmd get_throttled; sudo vcgencmd measure_temp; echo ""; docker ps --format "table {{.Names}}\t{{.Status}}"'
```

**Terminal 2 - Deployment:**
```bash
cd ~/happyrow-core/.raspberry

# Make deploy script executable
chmod +x deploy.sh

# Final pre-deployment check
echo "=== Pre-Deployment Check ===" && \
sudo vcgencmd get_throttled && \
sudo vcgencmd measure_temp && \
df -h / && \
cat .env | grep -v "PASSWORD\|TOKEN"

# If throttled=0x0 and everything looks good:
./deploy.sh

# This will take 15-30 minutes on Raspberry Pi 3
# Watch Terminal 1 for any throttling or temperature issues
```

**🛑 STOP deployment immediately if:**
- `throttled` shows any active bits (0x00005, 0x50005, etc.)
- Temperature exceeds 80°C
- See I/O errors or bus errors
- System becomes unresponsive

**If problems occur:**
```bash
# Stop deployment
Ctrl+C

# Stop containers
cd ~/happyrow-core/.raspberry
docker-compose down

# Check logs
docker-compose logs

# Shutdown if serious issues
sudo shutdown -h now
```

### Step 6: Verify Deployment

**Once deployment completes:**
```bash
# Check all containers are running
docker-compose ps
# Should show 4 containers:
# - happyrow-postgres (Up)
# - happyrow-backend (Up)
# - happyrow-nginx (Up)
# - happyrow-cloudflared (Up)

# Test health locally
curl http://localhost/health
# Expected: {"status":"UP","timestamp":"..."}

# Check logs for errors
docker-compose logs backend | tail -20
docker-compose logs cloudflared | tail -20

# Test externally (from your Mac)
curl https://api.your-domain.com/health
# Expected: {"status":"UP","timestamp":"..."}
```

### Step 7: Update Vercel Frontend

**In Vercel Dashboard:**
1. Go to your project settings
2. **Environment Variables** → Add:
   ```
   NEXT_PUBLIC_API_URL=https://api.your-domain.com
   ```
3. Redeploy frontend

**Test in browser:**
1. Open your Vercel app
2. Open DevTools (F12) → Console
3. Check for CORS errors (should be none)
4. Test API calls

---

## 🔍 Troubleshooting Commands

### Check System Health
```bash
# All-in-one health check
echo "=== System Health ===" && \
sudo vcgencmd get_throttled && \
sudo vcgencmd measure_temp && \
sudo vcgencmd measure_volts && \
free -h && \
df -h /
```

### Check Docker Status
```bash
# Container status
docker-compose ps

# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend
docker-compose logs -f postgres
docker-compose logs -f cloudflared
docker-compose logs -f nginx

# Check resource usage
docker stats
```

### Restart Services
```bash
cd ~/happyrow-core/.raspberry

# Restart specific service
docker-compose restart backend

# Restart all services
docker-compose restart

# Stop all
docker-compose down

# Start all
docker-compose up -d
```

### Check Network Connectivity
```bash
# Check if backend is responding
curl http://localhost:8080/health

# Check if NGINX is proxying
curl http://localhost/health

# Check external access
curl https://api.your-domain.com/health

# Check Cloudflare tunnel status
docker-compose logs cloudflared | grep -i "connected\|error"
```

### Database Access
```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U happyrow_user -d happyrow_db

# Check tables
\dt

# Exit
\q

# Database backup
docker exec happyrow-postgres pg_dump -U happyrow_user happyrow_db > backup_$(date +%Y%m%d).sql
```

---

## 📊 Expected Resource Usage

### Normal Operation (Raspberry Pi 3)
- **CPU:** 30-50% average
- **RAM:** ~800MB total
  - PostgreSQL: ~200MB
  - Backend (Ktor): ~450MB
  - NGINX: ~20MB
  - Cloudflared: ~50MB
- **Disk:** ~5GB for Docker images + database
- **Temperature:** 50-65°C (with cooling)
- **Power:** ~2-2.5A peak (need 3A PSU!)

---

## 🚨 Known Issues & Solutions

### Issue 1: Active Throttling (0x50005)
**Symptom:** `throttled=0x50005` or similar  
**Cause:** Insufficient power supply  
**Solution:** Get proper 5V 3A power supply

### Issue 2: Build Fails During Deployment
**Symptom:** Gradle build crashes or times out  
**Cause:** Throttling or out of memory  
**Solution:** 
- Ensure `throttled=0x0`
- Add swap if needed: `sudo dphys-swapfile swapoff && sudo sed -i 's/CONF_SWAPSIZE=.*/CONF_SWAPSIZE=2048/' /etc/dphys-swapfile && sudo dphys-swapfile setup && sudo dphys-swapfile swapon`

### Issue 3: Cloudflare Tunnel Not Connecting
**Symptom:** Can't access API externally  
**Cause:** Wrong token or tunnel configuration  
**Solution:**
- Verify token in .env
- Check logs: `docker-compose logs cloudflared`
- Verify public hostname in Cloudflare dashboard points to `nginx:80`

### Issue 4: CORS Errors from Frontend
**Symptom:** Browser console shows CORS errors  
**Cause:** Frontend domain not in ALLOWED_ORIGINS  
**Solution:**
- Add all Vercel domains to .env ALLOWED_ORIGINS
- Restart backend: `docker-compose restart backend`

### Issue 5: Database Connection Errors
**Symptom:** Backend can't connect to PostgreSQL  
**Cause:** Database not ready or wrong credentials  
**Solution:**
- Check postgres is running: `docker-compose ps postgres`
- Check credentials match in .env
- Check logs: `docker-compose logs postgres`

---

## 📁 Important Files

### On Raspberry Pi
- **Environment:** `~/happyrow-core/.raspberry/.env` (contains secrets!)
- **Docker Compose:** `~/happyrow-core/.raspberry/docker-compose.yml`
- **NGINX Config:** `~/happyrow-core/.raspberry/nginx.conf`
- **Deploy Script:** `~/happyrow-core/.raspberry/deploy.sh`

### On Your Mac
- **Project Root:** `/Users/j.ni/IdeaProjects/happyrow-core/`
- **Transfer Script:** `/Users/j.ni/IdeaProjects/happyrow-core/.raspberry/transfer-to-pi.sh`
- **Documentation:** `/Users/j.ni/IdeaProjects/happyrow-core/docs/`

---

## 🔐 Security Checklist

- [x] UFW firewall enabled
- [x] Fail2Ban installed and running
- [x] SSH only (port 22)
- [x] WiFi disabled
- [x] Bluetooth disabled
- [x] No port forwarding needed (Cloudflare Tunnel)
- [x] Database not exposed externally
- [x] Environment variables for secrets
- [ ] Change SSH port to non-standard (optional, future)
- [ ] SSH key authentication (optional, future)

---

## 📞 Quick Reference

### SSH Connection
```bash
ssh j.ni@192.168.1.79
# or
ssh j.ni@raspberrypi.local
```

### Project Directory
```bash
cd ~/happyrow-core/.raspberry
```

### View Logs
```bash
docker-compose logs -f
```

### Restart Backend
```bash
docker-compose restart backend
```

### Stop Everything
```bash
docker-compose down
```

### Safe Shutdown
```bash
sudo shutdown -h now
```

---

## 🎯 Success Criteria

Deployment is successful when:
- [ ] `throttled=0x0` (no power issues)
- [ ] All 4 containers show "Up" status
- [ ] `curl http://localhost/health` returns 200 OK
- [ ] `curl https://api.your-domain.com/health` returns 200 OK
- [ ] Cloudflare dashboard shows tunnel "Healthy"
- [ ] Vercel frontend can make API calls
- [ ] No CORS errors in browser console
- [ ] Database queries work
- [ ] System stable for 24+ hours

---

## 💰 Cost Summary

| Item | Cost | Status |
|------|------|--------|
| Raspberry Pi 3 | Already owned | ✅ |
| SD Card (16GB+) | Already owned | ✅ |
| **5V 3A Power Supply** | **~10-15€** | ⚠️ **NEEDED** |
| Cloudflare Tunnel | FREE | ✅ |
| Vercel Hosting | FREE | ✅ |
| Electricity (~3W) | ~€0.30/month | ✅ |
| **Total Monthly Cost** | **~€0.30** | 🎉 |

---

## 📚 Documentation Files Created

1. **RASPBERRY_PI_QUICK_START.md** - Fast-track deployment guide
2. **RASPBERRY_PI_SECURITY.md** - Complete security hardening
3. **RASPBERRY_PI_DEPLOYMENT.md** - Detailed deployment instructions
4. **RASPBERRY_PI_RECOVERY.md** - Emergency recovery procedures
5. **RASPBERRY_PI_SETUP_RESUME.md** - This file (complete resume)

All files are in: `/Users/j.ni/IdeaProjects/happyrow-core/docs/`

---

## ⏭️ Next Session Checklist

When you resume:

1. **✅ Get 5V 3A power supply first!**
2. Connect new PSU and boot Pi
3. SSH in: `ssh j.ni@192.168.1.79`
4. **Verify throttling is gone:** `sudo vcgencmd get_throttled` → MUST be `0x0`
5. Transfer files: `./transfer-to-pi.sh`
6. Configure .env with secrets
7. Create Cloudflare Tunnel and get token
8. Deploy with monitoring: `./deploy.sh`
9. Verify all services running
10. Test from Vercel frontend

---

## 🎉 What We Accomplished Today

- ✅ Fresh Raspberry Pi OS installation
- ✅ Complete system update
- ✅ Security configuration (firewall + fail2ban)
- ✅ Power optimization (WiFi/Bluetooth off)
- ✅ Docker and Docker Compose installed
- ✅ All deployment files ready (.raspberry directory)
- ✅ Complete documentation created
- ✅ Identified power supply issue before data loss

**Great progress! Just need proper PSU to complete deployment.** 🚀

---

**Created:** 2025-10-15  
**Author:** Cascade AI Assistant  
**Project:** HappyRow Core - Raspberry Pi Deployment
