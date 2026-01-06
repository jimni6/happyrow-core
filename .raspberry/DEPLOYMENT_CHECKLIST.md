# Raspberry Pi Deployment Checklist

## Prerequisites ✓
- [x] Raspberry Pi 3 secured and accessible via SSH
- [x] Docker and Docker Compose installed on Pi
- [x] Project files ready for transfer
- [ ] Cloudflare account created

---

## Step 1: Set Up Cloudflare Tunnel

### 1.1 Create Cloudflare Account
- Go to: https://dash.cloudflare.com/
- Sign up for free account (if you don't have one)

### 1.2 Create Tunnel
1. Navigate to **Zero Trust** dashboard: https://one.dash.cloudflare.com/
2. Go to **Access** → **Tunnels**
3. Click **Create a tunnel**
4. Choose **Cloudflared** as connector
5. Name it: `happyrow-rpi`
6. Click **Save tunnel**

### 1.3 Get Tunnel Token
- Copy the full token (starts with `eyJ...`)
- **SAVE THIS TOKEN** - you'll need it for `.env` file

### 1.4 Configure Public Hostname
In tunnel configuration:
- **Subdomain**: `api` (or your choice)
- **Domain**: Your domain or use free Cloudflare subdomain
- **Service Type**: `HTTP`
- **Service URL**: `nginx:80`
- Click **Save**

---

## Step 2: Configure Environment Variables

### On your local machine:
```bash
cd /Users/j.ni/IdeaProjects/happyrow-core/.raspberry
cp .env.example .env
nano .env
```

### Fill in these values:
```bash
# Generate strong password first:
openssl rand -base64 32

# Then edit .env with:
DB_NAME=happyrow_db
DB_USERNAME=happyrow_user
DB_PASSWORD=<PASTE_GENERATED_PASSWORD>
ALLOWED_ORIGINS=https://your-vercel-app.vercel.app
CLOUDFLARE_TUNNEL_TOKEN=<PASTE_YOUR_CLOUDFLARE_TOKEN>
```

### Secure the file:
```bash
chmod 600 .env
```

---

## Step 3: Transfer Files to Raspberry Pi

### Option A: Use transfer script (Recommended)
```bash
cd /Users/j.ni/IdeaProjects/happyrow-core/.raspberry
chmod +x transfer-to-pi.sh
./transfer-to-pi.sh
```

Follow the prompts:
- **IP address**: Your Raspberry Pi's IP
- **Username**: `happyrow` (or your Pi username)
- **SSH port**: `2222` (or your custom port)
- **Directory**: `~/happyrow-core`

### Option B: Manual rsync
```bash
rsync -avz -e "ssh -p 2222" \
  --exclude 'build' \
  --exclude '.gradle' \
  --exclude '.kotlin' \
  /Users/j.ni/IdeaProjects/happyrow-core/ \
  happyrow@<PI_IP>:~/happyrow-core/
```

---

## Step 4: Deploy on Raspberry Pi

### SSH into your Pi:
```bash
ssh -p 2222 happyrow@<PI_IP>
```

### Navigate and configure:
```bash
cd ~/happyrow-core/.raspberry

# Copy your .env file (if not already there)
cp .env.example .env
nano .env  # Fill in your values

# Make deploy script executable
chmod +x deploy.sh
```

### Run deployment:
```bash
./deploy.sh
```

**Note**: Building will take 15-30 minutes on RPi 3. Be patient! ☕

---

## Step 5: Verify Deployment

### Check services are running:
```bash
docker-compose ps
```

Should show 4 containers:
- ✓ happyrow-postgres
- ✓ happyrow-backend
- ✓ happyrow-nginx
- ✓ happyrow-cloudflared

### Test local endpoints:
```bash
# Health check
curl http://localhost/health

# Info endpoint
curl http://localhost/info

# View logs
docker-compose logs -f
```

### Test external access:
```bash
# From your local machine (not Pi):
curl https://api.your-domain.com/health
```

### Check in browser:
- https://api.your-domain.com/health
- https://api.your-domain.com/info

---

## Step 6: Update Vercel Frontend

### In Vercel project settings:
```
NEXT_PUBLIC_API_URL=https://api.your-domain.com
```

### Update CORS in Pi's .env:
```bash
ALLOWED_ORIGINS=https://your-app.vercel.app,https://your-app-git-main.vercel.app
```

### Restart backend:
```bash
cd ~/happyrow-core/.raspberry
docker-compose restart backend
```

---

## Common Commands

```bash
# View logs
docker-compose logs -f
docker-compose logs -f backend

# Check status
docker-compose ps
docker stats

# Restart services
docker-compose restart backend
docker-compose restart

# Stop all
docker-compose down

# Start all
docker-compose up -d

# Update application
./deploy.sh update

# Check Pi temperature
vcgencmd measure_temp

# Check resources
htop
free -h
df -h
```

---

## Troubleshooting

### Backend won't start
```bash
docker-compose logs backend
docker-compose restart backend
```

### Cloudflare Tunnel issues
```bash
docker-compose logs cloudflared
docker-compose restart cloudflared
```

### Database issues
```bash
docker-compose logs postgres
docker-compose exec postgres pg_isready -U happyrow_user
```

### Out of memory
```bash
free -h
docker stats
# Consider increasing swap or reducing memory limits
```

---

## Quick Recovery

### Restart everything:
```bash
cd ~/happyrow-core/.raspberry
docker-compose down
docker-compose up -d
```

### Clean start:
```bash
docker-compose down -v
docker system prune -af
./deploy.sh
```

---

## Success Checklist

- [ ] All 4 containers running (`docker-compose ps`)
- [ ] Health endpoint responds locally
- [ ] Health endpoint responds via Cloudflare URL
- [ ] Frontend can connect to backend
- [ ] No errors in logs
- [ ] Pi temperature under 75°C
- [ ] Memory usage reasonable

---

## Next Steps After Deployment

1. **Set up database backups** (see main guide)
2. **Monitor resource usage** regularly
3. **Test all API endpoints** from frontend
4. **Document your specific domain/URLs**
5. **Set up alerts** (optional)

---

## Need Help?

Refer to full guide: `docs/RASPBERRY_PI_DEPLOYMENT.md`

Check troubleshooting section for specific issues.
