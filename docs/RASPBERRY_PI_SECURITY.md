# Raspberry Pi Security Hardening Guide

This guide will help you secure your Raspberry Pi 3 before deploying the HappyRow Core application.

## Prerequisites
- Raspberry Pi 3 with Raspberry Pi OS (64-bit recommended)
- SSH access to your Pi
- Internet connection

---

## Step 1: Initial System Update

```bash
# Update package lists and upgrade all packages
sudo apt update
sudo apt upgrade -y
sudo apt dist-upgrade -y

# Reboot to apply kernel updates
sudo reboot
```

---

## Step 2: Change Default Password

```bash
# Change password for pi user (or your user)
passwd

# Use a strong password with:
# - At least 16 characters
# - Mix of uppercase, lowercase, numbers, and symbols
```

---

## Step 3: Create a New Admin User (Recommended)

```bash
# Create new user
sudo adduser happyrow

# Add to sudo group
sudo usermod -aG sudo happyrow

# Add to docker group (for later)
sudo usermod -aG docker happyrow

# Test the new user
su - happyrow
sudo ls -la /root

# If successful, exit back to original user
exit
```

---

## Step 4: Configure SSH Security

### 4.1 Set Up SSH Key Authentication

**On your local machine:**
```bash
# Generate SSH key pair (if you don't have one)
ssh-keygen -t ed25519 -C "happyrow-rpi"

# Copy public key to Raspberry Pi
ssh-copy-id happyrow@<raspberry-pi-ip>
```

### 4.2 Harden SSH Configuration

```bash
# Backup original SSH config
sudo cp /etc/ssh/sshd_config /etc/ssh/sshd_config.backup

# Edit SSH configuration
sudo nano /etc/ssh/sshd_config
```

**Update these settings:**
```
# Disable root login
PermitRootLogin no

# Disable password authentication (use SSH keys only)
PasswordAuthentication no
PubkeyAuthentication yes
ChallengeResponseAuthentication no

# Change default SSH port (optional but recommended)
Port 2222

# Limit login attempts
MaxAuthTries 3
MaxStartups 3:50:10

# Only allow specific users
AllowUsers happyrow

# Disable X11 forwarding if not needed
X11Forwarding no

# Use strong ciphers
Ciphers chacha20-poly1305@openssh.com,aes256-gcm@openssh.com,aes128-gcm@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr
```

**Restart SSH service:**
```bash
sudo systemctl restart ssh

# Test new SSH connection in a NEW terminal before closing current one!
ssh -p 2222 happyrow@<raspberry-pi-ip>
```

---

## Step 5: Configure Firewall (UFW)

```bash
# Install UFW if not present
sudo apt install ufw -y

# Default policies
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow SSH (use your custom port if changed)
sudo ufw allow 2222/tcp comment 'SSH'

# Allow HTTP/HTTPS (for Cloudflare Tunnel - optional)
# sudo ufw allow 80/tcp comment 'HTTP'
# sudo ufw allow 443/tcp comment 'HTTPS'

# Enable UFW
sudo ufw enable

# Check status
sudo ufw status verbose
```

---

## Step 6: Install and Configure Fail2Ban

```bash
# Install Fail2Ban
sudo apt install fail2ban -y

# Create local configuration
sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local

# Edit configuration
sudo nano /etc/fail2ban/jail.local
```

**Update these settings:**
```ini
[DEFAULT]
bantime = 1h
findtime = 10m
maxretry = 3
destemail = your-email@example.com
sendername = Fail2Ban-RPi
action = %(action_mwl)s

[sshd]
enabled = true
port = 2222
logpath = /var/log/auth.log
maxretry = 3
```

**Start Fail2Ban:**
```bash
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# Check status
sudo fail2ban-client status
sudo fail2ban-client status sshd
```

---

## Step 7: Disable Unnecessary Services

```bash
# Check running services
systemctl list-unit-files --type=service --state=enabled

# Disable Bluetooth if not needed
sudo systemctl disable bluetooth
sudo systemctl stop bluetooth

# Disable WiFi if using Ethernet
sudo systemctl disable wpa_supplicant

# Disable Avahi (if not using local network discovery)
sudo systemctl disable avahi-daemon
```

---

## Step 8: Install Docker & Docker Compose

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user to docker group
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt install docker-compose -y

# Verify installation
docker --version
docker-compose --version

# Log out and back in for group changes to take effect
exit
```

---

## Step 9: Configure Automatic Security Updates

```bash
# Install unattended-upgrades
sudo apt install unattended-upgrades apt-listchanges -y

# Configure automatic updates
sudo dpkg-reconfigure -plow unattended-upgrades

# Edit configuration
sudo nano /etc/apt/apt.conf.d/50unattended-upgrades
```

**Ensure these are uncommented:**
```
Unattended-Upgrade::Automatic-Reboot "false";
Unattended-Upgrade::Automatic-Reboot-Time "03:00";
Unattended-Upgrade::Remove-Unused-Kernel-Packages "true";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
```

---

## Step 10: Set Up Logging and Monitoring

### 10.1 Configure rsyslog

```bash
# Ensure rsyslog is running
sudo systemctl enable rsyslog
sudo systemctl start rsyslog
```

### 10.2 Install logwatch (optional)

```bash
# Install logwatch for daily log summaries
sudo apt install logwatch -y

# Test report
sudo logwatch --detail High --mailto your-email@example.com --service all --range today
```

---

## Step 11: Harden System Settings

```bash
# Disable IPv6 if not needed
sudo nano /etc/sysctl.conf
```

**Add these lines:**
```
# IP Forwarding
net.ipv4.ip_forward = 0

# Disable IPv6
net.ipv6.conf.all.disable_ipv6 = 1
net.ipv6.conf.default.disable_ipv6 = 1

# Protection against SYN flood attacks
net.ipv4.tcp_syncookies = 1
net.ipv4.tcp_max_syn_backlog = 2048
net.ipv4.tcp_synack_retries = 2

# Ignore ICMP ping requests
net.ipv4.icmp_echo_ignore_all = 1

# Ignore broadcast pings
net.ipv4.icmp_echo_ignore_broadcasts = 1

# Disable source packet routing
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.default.accept_source_route = 0

# Enable reverse path filtering
net.ipv4.conf.all.rp_filter = 1
net.ipv4.conf.default.rp_filter = 1

# Log suspicious packets
net.ipv4.conf.all.log_martians = 1

# Ignore ICMP redirects
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv4.conf.all.secure_redirects = 0

# Disable send redirects
net.ipv4.conf.all.send_redirects = 0
```

**Apply changes:**
```bash
sudo sysctl -p
```

---

## Step 12: Install Additional Security Tools

```bash
# Install rkhunter (rootkit detector)
sudo apt install rkhunter -y
sudo rkhunter --update
sudo rkhunter --propupd
sudo rkhunter --check --sk

# Install ClamAV (antivirus)
sudo apt install clamav clamav-daemon -y
sudo systemctl stop clamav-freshclam
sudo freshclam
sudo systemctl start clamav-freshclam

# Scan system (this takes time)
# sudo clamscan -r /home --bell -i
```

---

## Step 13: Physical Security Considerations

1. **Place Raspberry Pi in secure location** - Limited physical access
2. **Disable USB boot** - Prevents booting from external drives
3. **Enable boot password** (optional):
   ```bash
   sudo nano /boot/config.txt
   # Add: boot_delay=5
   ```

---

## Step 14: Regular Maintenance Tasks

Create a weekly maintenance checklist:

```bash
# Weekly security check script
nano ~/security-check.sh
```

**Add this content:**
```bash
#!/bin/bash
echo "=== System Update ==="
sudo apt update && sudo apt upgrade -y

echo "=== Docker Cleanup ==="
docker system prune -af --volumes

echo "=== Check Failed Login Attempts ==="
sudo grep "Failed password" /var/log/auth.log | tail -20

echo "=== UFW Status ==="
sudo ufw status

echo "=== Fail2Ban Status ==="
sudo fail2ban-client status sshd

echo "=== Disk Space ==="
df -h

echo "=== Memory Usage ==="
free -h

echo "=== Running Containers ==="
docker ps

echo "=== Security scan complete ==="
```

**Make it executable:**
```bash
chmod +x ~/security-check.sh
```

---

## Step 15: Backup Configuration

```bash
# Create backup directory
mkdir -p ~/backups

# Backup important configs
sudo cp /etc/ssh/sshd_config ~/backups/
sudo cp /etc/fail2ban/jail.local ~/backups/
sudo ufw status numbered > ~/backups/ufw-rules.txt
```

---

## Security Checklist Summary

- [x] System updated
- [x] Default password changed
- [x] New admin user created
- [x] SSH key authentication enabled
- [x] Password authentication disabled
- [x] SSH port changed (optional)
- [x] UFW firewall configured and enabled
- [x] Fail2Ban installed and configured
- [x] Unnecessary services disabled
- [x] Docker and Docker Compose installed
- [x] Automatic security updates enabled
- [x] System hardening applied
- [x] Security tools installed
- [x] Backup created

---

## Next Steps

Once your Raspberry Pi is secured:
1. Proceed to Docker Compose setup (see `RASPBERRY_PI_DEPLOYMENT.md`)
2. Configure Cloudflare Tunnel
3. Deploy HappyRow Core application
4. Test connectivity from Vercel frontend

---

## Emergency Access

If you lock yourself out:
1. Connect keyboard and monitor directly to Pi
2. Log in with physical access
3. Review `/var/log/auth.log` for issues
4. Reset SSH configuration if needed
5. Always keep a backup of working SSH config!

---

## Additional Resources

- [Raspberry Pi Official Security Documentation](https://www.raspberrypi.org/documentation/configuration/security.md)
- [UFW Documentation](https://help.ubuntu.com/community/UFW)
- [Fail2Ban Wiki](https://www.fail2ban.org/)
- [Docker Security Best Practices](https://docs.docker.com/engine/security/)
