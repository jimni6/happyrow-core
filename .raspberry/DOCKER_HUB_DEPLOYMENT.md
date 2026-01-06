# Docker Hub Deployment Strategy

Deploy HappyRow Core to Raspberry Pi using pre-built Docker images from Docker Hub.

## 🎯 Why This Approach?

**✅ Advantages:**
- **10x faster**: Build on your powerful Mac instead of Raspberry Pi 3
- **Saves Pi resources**: No CPU-intensive compilation on Pi
- **Production-ready**: Proper CI/CD workflow
- **Multi-platform**: Single image works on Mac, Linux, and Raspberry Pi

**vs Building on Pi:**
- Building on Pi 3: ~15-30 minutes
- Pulling pre-built image: ~2-5 minutes

---

## 📋 Prerequisites

1. **Docker Hub account** (free): https://hub.docker.com/signup
2. **Docker Desktop** installed on Mac
3. **Docker logged in**: `docker login`

---

## 🚀 Deployment Workflow

```
┌─────────────┐         ┌──────────────┐         ┌──────────────┐
│   Mac       │  Build  │ Docker Hub   │  Pull   │ Raspberry Pi │
│             │────────▶│              │────────▶│              │
│ Development │         │ Image Store  │         │ Production   │
└─────────────┘         └──────────────┘         └──────────────┘
```

---

## Step 1: Build and Push from Mac

### 1.1 Login to Docker Hub

```bash
docker login
# Enter your Docker Hub username and password
```

### 1.2 Build Multi-Architecture Image

```bash
cd /Users/j.ni/IdeaProjects/happyrow-core

# Make script executable
chmod +x build-push-docker.sh

# Build and push (will prompt for Docker Hub username)
./build-push-docker.sh
```

**What this does:**
- Builds for AMD64, ARM64, and **ARMv7** (Raspberry Pi 3)
- Tags as `your-username/happyrow-core:latest`
- Pushes to Docker Hub
- Takes ~5-10 minutes on Mac

**Example output:**
```
Building for AMD64, ARM64, ARMv7 (Raspberry Pi 3)...
[+] Building 234.5s (42/42) FINISHED
✓ Done! Image pushed to Docker Hub
View at: https://hub.docker.com/r/your-username/happyrow-core
```

---

## Step 2: Transfer Config Files to Pi

Only transfer configuration files (not source code):

```bash
# From your Mac
cd /Users/j.ni/IdeaProjects/happyrow-core

# Create directory on Pi
ssh j.ni@192.168.1.79 "mkdir -p ~/happyrow-core/.raspberry"

# Transfer docker-compose and configs
scp -r .raspberry/docker-compose.yml \
       .raspberry/.env \
       .raspberry/nginx.conf \
       .raspberry/deploy.sh \
       j.ni@192.168.1.79:~/happyrow-core/.raspberry/

# Transfer database init script
scp init-db.sql j.ni@192.168.1.79:~/happyrow-core/
```

---

## Step 3: Add Docker Hub Username to Pi's .env

SSH to your Pi:

```bash
ssh j.ni@192.168.1.79
```

Edit the `.env` file:

```bash
cd ~/happyrow-core/.raspberry
nano .env
```

**Add this line** (replace with your Docker Hub username):
```bash
DOCKER_USERNAME=your_dockerhub_username
```

**Save**: Ctrl+O, Enter, Ctrl+X

---

## Step 4: Deploy on Raspberry Pi

Still on the Pi:

```bash
cd ~/happyrow-core/.raspberry

# Make deploy script executable
chmod +x deploy.sh

# Pull image and start services
docker-compose pull backend
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f backend
```

---

## 🔄 Update Workflow

When you make code changes:

### On Mac:
```bash
# 1. Make your code changes
# 2. Build and push new image
./build-push-docker.sh

# Or if you want to version tag:
export DOCKER_USERNAME=your-username
docker buildx build \
  --platform linux/amd64,linux/arm64,linux/arm/v7 \
  -t $DOCKER_USERNAME/happyrow-core:v1.0.1 \
  -t $DOCKER_USERNAME/happyrow-core:latest \
  --push .
```

### On Raspberry Pi:
```bash
cd ~/happyrow-core/.raspberry

# Pull latest image
docker-compose pull backend

# Restart with new image
docker-compose up -d backend

# Or use the deploy script
./deploy.sh update
```

---

## 📦 What Gets Transferred

### To Docker Hub (Large):
- ✅ Application code
- ✅ Compiled JAR
- ✅ Java runtime
- ✅ All dependencies
- **~400MB total**

### To Raspberry Pi (Small):
- ✅ docker-compose.yml
- ✅ .env file
- ✅ nginx.conf
- ✅ init-db.sql
- ✅ deploy scripts
- **~50KB total**

---

## 🎨 Full Deployment Commands

### Complete deployment from scratch:

```bash
# ===== ON MAC =====
cd /Users/j.ni/IdeaProjects/happyrow-core

# 1. Build and push
docker login
./build-push-docker.sh

# 2. Transfer configs
ssh j.ni@192.168.1.79 "mkdir -p ~/happyrow-core/.raspberry"
cd .raspberry
scp docker-compose.yml .env nginx.conf deploy.sh j.ni@192.168.1.79:~/happyrow-core/.raspberry/
cd ..
scp init-db.sql j.ni@192.168.1.79:~/happyrow-core/

# ===== ON RASPBERRY PI =====
ssh j.ni@192.168.1.79
cd ~/happyrow-core/.raspberry

# 3. Add Docker Hub username to .env
nano .env
# Add: DOCKER_USERNAME=your-username

# 4. Deploy
chmod +x deploy.sh
docker-compose pull
docker-compose up -d

# 5. Check
docker-compose ps
docker-compose logs -f
```

---

## 🔍 Verify Deployment

### Check containers:
```bash
docker-compose ps
# Should show:
# - happyrow-postgres    (healthy)
# - happyrow-backend     (healthy)
# - happyrow-nginx       (healthy)
# - happyrow-cloudflared (running)
```

### Test endpoints:
```bash
# Local test
curl http://localhost/health

# External test (from your Mac)
curl https://your-cloudflare-url.com/health
```

### Check logs:
```bash
# All services
docker-compose logs -f

# Just backend
docker-compose logs -f backend

# Last 50 lines
docker-compose logs --tail=50
```

---

## 🐛 Troubleshooting

### Image pull fails
```bash
# Check you're logged into Docker Hub on Pi
docker login

# Manually pull image
docker pull your-username/happyrow-core:latest

# Check platform
docker inspect your-username/happyrow-core:latest | grep Architecture
# Should show: "arm" or "arm/v7"
```

### Wrong architecture
```bash
# Check what was built
docker buildx imagetools inspect your-username/happyrow-core:latest

# Should show:
# - linux/amd64
# - linux/arm64
# - linux/arm/v7
```

### Backend won't start
```bash
# Check logs
docker-compose logs backend

# Check if image pulled correctly
docker images | grep happyrow-core

# Try rebuilding on Pi as fallback
docker-compose build backend
docker-compose up -d backend
```

---

## 💰 Docker Hub Limits (Free Tier)

- **Storage**: Unlimited repositories (1 private)
- **Pulls**: 200 pulls per 6 hours (anonymous)
- **Pulls**: 5000 pulls per day (authenticated)
- **Bandwidth**: Unlimited

**For this project:** Well within free tier limits! ✅

---

## 🔒 Security Best Practices

### 1. Use Docker Hub Access Tokens
Instead of your password:

```bash
# Create token at: https://hub.docker.com/settings/security
docker login -u your-username
# Enter token instead of password
```

### 2. Keep Images Updated
```bash
# Scan for vulnerabilities (on Mac)
docker scout quickview your-username/happyrow-core:latest
```

### 3. Use Version Tags
```bash
# Instead of only :latest
docker tag your-username/happyrow-core:latest your-username/happyrow-core:v1.0.0
docker push your-username/happyrow-core:v1.0.0
```

---

## 📊 Build Time Comparison

| Method | Time | Resources |
|--------|------|-----------|
| **Build on Mac** | ~5-8 min | 💪 8-core CPU, 16GB RAM |
| **Push to Hub** | ~2-3 min | ⚡ Fast internet |
| **Pull on Pi** | ~2-5 min | 📥 Download only |
| **Total** | **~10 min** | ✅ Efficient |
|  |  |  |
| **Build on Pi 3** | ~15-30 min | 🐌 Slow 4-core ARM |
| **Total** | **~15-30 min** | ❌ Inefficient |

**Winner:** Docker Hub approach is **2-3x faster!** 🏆

---

## ✅ Deployment Checklist

- [ ] Docker Hub account created
- [ ] Logged into Docker Hub on Mac
- [ ] Built multi-arch image on Mac
- [ ] Pushed image to Docker Hub
- [ ] Transferred config files to Pi
- [ ] Added DOCKER_USERNAME to Pi's .env
- [ ] Pulled image on Pi
- [ ] Started containers with docker-compose
- [ ] Verified all containers healthy
- [ ] Tested local endpoints
- [ ] Tested external Cloudflare URL

---

## 🎯 Next Steps

1. ✅ Build and push image from Mac
2. ✅ Transfer configs to Pi
3. ✅ Deploy on Pi
4. 🔄 Set up CI/CD (optional - GitHub Actions)
5. 📊 Set up monitoring (optional)

---

**Ready to deploy?** Start with Step 1: Build and Push from Mac! 🚀
