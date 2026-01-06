# Raspberry Pi Health & Security Checks

Complete documentation of all checks performed and how to set up passwordless SSH access.

---

## 📊 Health Checks Performed

### System Information
```bash
# Get all IP addresses
hostname -I

# Check Pi model
cat /proc/device-tree/model

# Get hostname
hostname
```

**Your Results:**
- **IPs**: 192.168.1.79, 192.168.1.14, 172.17.0.1, 2a01:e0a:528:6ab0:6a79:b0f:8dce:7347, 2a01:e0a:528:6ab0:4934:db41:d1fa:6049
- **Primary IP**: 192.168.1.79
- **Model**: Raspberry Pi 3 Model B Rev 1.2
- **Hostname**: raspberrypi

### Temperature Check
```bash
vcgencmd measure_temp
```
**Result**: 49.9°C ✅ (Excellent - below 60°C)

### Memory Check
```bash
free -h
```
**Results**:
- Total: 906MB
- Used: 337MB
- Available: 569MB ✅ (Sufficient for deployment)
- Swap: 905MB

### Disk Space Check
```bash
df -h /
```
**Results**:
- Total: 14GB
- Used: 5.9GB (46%)
- Available: 7.2GB ✅ (Sufficient for Docker & app)

### Docker Verification
```bash
# Check Docker version
docker --version

# Check Docker Compose version
docker-compose --version

# Check Docker service status
sudo systemctl status docker --no-pager
```
**Results**:
- Docker: v26.1.5 ✅
- Docker Compose: v2.24.0 ✅
- Service: Active & Running ✅

---

## 🔒 Security Checks Performed

### 1. Firewall Status (UFW)
```bash
sudo ufw status
```
**Result**: ✅ Active
```
To                         Action      From
--                         ------      ----
22/tcp                     ALLOW       Anywhere                   # SSH
22/tcp (v6)                ALLOW       Anywhere (v6)              # SSH
```

### 2. Fail2ban Status
```bash
sudo systemctl status fail2ban --no-pager
```
**Result**: ✅ Active (running since Wed 2025-10-15 21:06:42 CEST)

### 3. SSH Configuration
```bash
# Check if SSH is running
sudo systemctl status ssh --no-pager

# Check SSH port
sudo grep "^Port" /etc/ssh/sshd_config || echo "Using default port 22"
```
**Results**:
- SSH Service: ✅ Active & Running
- SSH Port: 22 (default)
- Current Auth: Password-based

### 4. SSH Keys Check
```bash
ls -la ~/.ssh/authorized_keys 2>/dev/null && echo "SSH keys configured" || echo "No SSH keys found"
```
**Result**: ❌ No SSH keys found (needs setup)

### 5. System Updates
```bash
sudo apt update
apt list --upgradable
```
**Result**: ✅ All packages up to date

### 6. Network Connectivity
```bash
# Test internet connectivity
ping -c 2 8.8.8.8

# Test DNS
ping -c 2 google.com
```

### 7. Port Availability Check
```bash
# Check if required ports are available
for port in 80 443 8080 5432; do
    if ss -tuln | grep -q ":$port "; then
        echo "Port $port: IN USE"
    else
        echo "Port $port: AVAILABLE"
    fi
done
```

---

## 🔑 Setting Up Passwordless SSH Access

### What is SSH Key Authentication?

SSH keys provide a more secure way to log into your Pi without typing a password. It uses:
- **Private key**: Stays on your Mac (like a secret password)
- **Public key**: Goes on your Pi (like a lock)

Only your private key can unlock the public key, making it much more secure than passwords.

---

## 📝 Step-by-Step Setup Guide

### Step 1: Generate SSH Key on Your Mac

Open Terminal on your **Mac** and run:

```bash
# Generate a new SSH key pair
ssh-keygen -t ed25519 -C "happyrow-pi" -f ~/.ssh/id_ed25519_pi
```

**What happens:**
- Prompts for passphrase (optional - press Enter to skip for no passphrase)
- Creates two files:
  - `~/.ssh/id_ed25519_pi` (private key - NEVER share this!)
  - `~/.ssh/id_ed25519_pi.pub` (public key - safe to share)

**Example output:**
```
Generating public/private ed25519 key pair.
Enter passphrase (empty for no passphrase): [Press Enter]
Enter same passphrase again: [Press Enter]
Your identification has been saved in ~/.ssh/id_ed25519_pi
Your public key has been saved in ~/.ssh/id_ed25519_pi.pub
```

---

### Step 2: Copy Public Key to Raspberry Pi

```bash
# Copy your public key to the Pi
ssh-copy-id -i ~/.ssh/id_ed25519_pi.pub j.ni@192.168.1.79
```

**What happens:**
- Asks for your Pi password (one last time!)
- Copies your public key to `~/.ssh/authorized_keys` on the Pi
- Sets correct permissions automatically

**Example output:**
```
/usr/bin/ssh-copy-id: INFO: attempting to log in with the new key(s)
j.ni@192.168.1.79's password: [Enter your password]
Number of key(s) added: 1

Now try logging into the machine with:
   "ssh -i ~/.ssh/id_ed25519_pi j.ni@192.168.1.79"
```

---

### Step 3: Test Passwordless Login

Open a **NEW terminal window** (keep current SSH session open as backup!) and test:

```bash
# Test connection with your new key
ssh -i ~/.ssh/id_ed25519_pi j.ni@192.168.1.79
```

**Success:** You should log in WITHOUT being asked for a password! 🎉

---

### Step 4: Make it Easier (Optional)

Add this to your Mac's `~/.ssh/config` file to simplify the command:

```bash
# Edit SSH config
nano ~/.ssh/config
```

**Add these lines:**
```
Host raspberrypi
    HostName 192.168.1.79
    User j.ni
    IdentityFile ~/.ssh/id_ed25519_pi
    Port 22
```

**Save** (Ctrl+O, Enter, Ctrl+X)

Now you can connect with just:
```bash
ssh raspberrypi
```

Much easier! 🚀

---

### Step 5: Disable Password Authentication (Recommended)

Once you've confirmed SSH keys work, disable password login for better security.

**On your Raspberry Pi:**

```bash
# Backup SSH config first
sudo cp /etc/ssh/sshd_config /etc/ssh/sshd_config.backup

# Edit SSH configuration
sudo nano /etc/ssh/sshd_config
```

**Find and change these lines:**
```
# Change from:
#PasswordAuthentication yes

# To:
PasswordAuthentication no
PubkeyAuthentication yes
```

**Or use sed to do it automatically:**
```bash
sudo sed -i 's/#PasswordAuthentication yes/PasswordAuthentication no/' /etc/ssh/sshd_config
sudo sed -i 's/PasswordAuthentication yes/PasswordAuthentication no/' /etc/ssh/sshd_config
```

**Restart SSH service:**
```bash
sudo systemctl restart ssh
```

**Test in a new terminal** to make sure key-based login still works before closing your current session!

---

## 🔄 How to Connect from Different Machines

### From Your Mac (Primary)
```bash
# Simple (if you set up ~/.ssh/config)
ssh raspberrypi

# Or with full details
ssh -i ~/.ssh/id_ed25519_pi j.ni@192.168.1.79
```

### From Another Computer

You need to either:
1. Copy your **private key** (`~/.ssh/id_ed25519_pi`) to the new machine (secure USB)
2. Or generate a new key pair and add it to the Pi's `authorized_keys`

**To add another key:**
```bash
# On new computer: generate key
ssh-keygen -t ed25519 -C "another-machine"

# Copy public key to Pi
ssh-copy-id -i ~/.ssh/id_ed25519.pub j.ni@192.168.1.79
```

---

## 🚨 Troubleshooting

### "Permission denied (publickey)"

**Problem**: Your key isn't being recognized

**Solutions**:
```bash
# 1. Check key permissions on your Mac
chmod 600 ~/.ssh/id_ed25519_pi
chmod 644 ~/.ssh/id_ed25519_pi.pub

# 2. Verify key is on the Pi
ssh j.ni@192.168.1.79 "cat ~/.ssh/authorized_keys"

# 3. Check Pi permissions
ssh j.ni@192.168.1.79 "chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys"

# 4. Use verbose mode to debug
ssh -v -i ~/.ssh/id_ed25519_pi j.ni@192.168.1.79
```

### "Connection refused"

**Problem**: SSH service isn't running or firewall blocking

**Solutions**:
```bash
# On Pi: Check SSH service
sudo systemctl status ssh

# Start SSH if stopped
sudo systemctl start ssh

# Check firewall
sudo ufw status
sudo ufw allow 22/tcp
```

### Locked Out Completely

**Recovery**:
1. Connect keyboard and monitor directly to Pi
2. Log in locally
3. Check `/var/log/auth.log` for issues
4. Restore backup config: `sudo cp /etc/ssh/sshd_config.backup /etc/ssh/sshd_config`
5. Restart SSH: `sudo systemctl restart ssh`

---

## 📋 Security Checklist Summary

| Security Item | Status | Notes |
|--------------|--------|-------|
| System updates | ✅ | All packages up to date |
| UFW firewall | ✅ | Active, port 22 allowed |
| Fail2ban | ✅ | Running, monitoring SSH |
| SSH service | ✅ | Active on port 22 |
| SSH keys | ⚠️ | Need to set up (see guide above) |
| Password auth | ⚠️ | Should disable after keys work |
| Docker | ✅ | v26.1.5 installed & running |
| Temperature | ✅ | 49.9°C (healthy) |
| Memory | ✅ | 569MB available |
| Disk space | ✅ | 7.2GB available |

---

## ✅ Pre-Deployment Checklist

Before deploying HappyRow Core:

- [x] System updated
- [x] Firewall enabled (UFW)
- [x] Fail2ban running
- [x] Docker installed
- [ ] **SSH keys configured** ← Do this next!
- [ ] Password authentication disabled (after keys work)
- [ ] Test passwordless SSH from Mac
- [ ] Backup SSH configuration

---

## 🎯 Next Steps

1. **Set up SSH keys** (follow guide above)
2. **Test passwordless login**
3. **Disable password authentication**
4. **Set up Cloudflare Tunnel**
5. **Configure environment variables**
6. **Transfer project files**
7. **Deploy HappyRow Core**

---

## 📚 Quick Reference Commands

### Connect to Pi
```bash
ssh raspberrypi                                    # If ~/.ssh/config set up
ssh -i ~/.ssh/id_ed25519_pi j.ni@192.168.1.79     # Full command
```

### Check Pi Status
```bash
# Temperature
vcgencmd measure_temp

# Memory
free -h

# Disk
df -h

# Docker containers
docker ps

# System load
uptime
```

### Security Commands
```bash
# Check firewall
sudo ufw status

# Check fail2ban
sudo fail2ban-client status sshd

# Check failed logins
sudo grep "Failed password" /var/log/auth.log | tail -20

# Check SSH config
sudo cat /etc/ssh/sshd_config | grep -E "^(Port|PasswordAuthentication|PubkeyAuthentication)"
```

---

**Document created**: 2025-10-17  
**Last updated**: 2025-10-17  
**Raspberry Pi IP**: 192.168.1.79  
**User**: j.ni
