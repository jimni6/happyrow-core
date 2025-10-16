# 🔧 Raspberry Pi Recovery & Troubleshooting

## 🚨 Emergency Recovery - I/O Errors & Bus Errors

If you're experiencing I/O errors and bus errors, follow this guide to recover your Raspberry Pi.

---

## 🔍 Diagnose the Problem

### 1. Check Immediate System Status

If you can still access the terminal:

```bash
# Check for SD card errors
sudo dmesg | grep -i "mmc\|sd\|error" | tail -20

# Check system logs
sudo journalctl -p 3 -xb | tail -50

# Check for undervoltage (power issues)
vcgencmd get_throttled
# Result: 0x0 = OK
#         0x50000 = Undervoltage occurred
#         0x50005 = Undervoltage + throttling

# Check temperature
vcgencmd measure_temp
# Should be < 70°C
```

### 2. Common Issues & Causes

| Issue | Symptom | Cause | Solution |
|-------|---------|-------|----------|
| **I/O Error** | Can't read/write files | SD card failure | Replace SD card |
| **Bus Error** | Memory access error | Hardware/Memory issue | Check power, RAM, cooling |
| **Undervoltage** | Random crashes, throttling | Weak power supply | Use 5V 3A adapter |
| **Overheating** | Slow performance, crashes | No cooling | Add heatsink/fan |

---

## 🛠️ Recovery Steps

### Step 1: Safe Shutdown/Reboot

```bash
# Try graceful reboot first
sudo sync
sudo reboot

# If system is frozen, power cycle:
# 1. Unplug power
# 2. Wait 30 seconds
# 3. Plug back in
```

### Step 2: After Reboot - Check Filesystem

```bash
# Boot in recovery mode if needed
# At boot, hold SHIFT to enter recovery

# Check and repair filesystem
sudo fsck -f /dev/mmcblk0p2

# Check SD card health
sudo badblocks -v /dev/mmcblk0

# Check disk usage
df -h

# Check for read-only filesystem
touch /tmp/test.txt
# If error "Read-only file system", remount:
sudo mount -o remount,rw /
```

### Step 3: Hardware Checks

#### Check Power Supply

**Raspberry Pi 3 Requirements:**
- Minimum: 5V 2.5A (12.5W)
- Recommended: 5V 3A (15W) for Docker workloads

**Test voltage:**
```bash
vcgencmd measure_volts
# Should be close to 5V
# If < 4.8V, power supply is insufficient
```

#### Check Temperature

```bash
vcgencmd measure_temp
# Ideal: 40-60°C
# Warning: 70-80°C
# Critical: > 80°C (will throttle)
```

**If overheating:**
- Add heatsinks to CPU
- Add cooling fan
- Improve airflow
- Reduce ambient temperature

#### Check Memory

```bash
# Check available memory
free -h

# Check for memory errors in logs
sudo dmesg | grep -i "memory\|oom"
```

---

## 🔄 Full Recovery Process

### Option 1: Repair Current Installation

**If SD card is still functional:**

```bash
# 1. Boot into Raspberry Pi
# 2. Update and fix packages
sudo apt update
sudo apt --fix-broken install
sudo apt upgrade -y

# 3. Check and repair filesystem
sudo fsck -f /dev/mmcblk0p2

# 4. Remove unnecessary files
sudo apt autoremove -y
sudo apt clean

# 5. Check disk space
df -h
# Ensure at least 2GB free

# 6. Reboot
sudo reboot
```

### Option 2: Fresh Installation (Recommended)

**Required items:**
- New/reliable SD card (16GB minimum, 32GB recommended)
- SD card reader
- Computer (Mac in your case)
- Raspberry Pi Imager

#### 2.1 Prepare New SD Card

**On your Mac:**

1. **Download Raspberry Pi Imager:**
   - Visit: https://www.raspberrypi.com/software/
   - Install Raspberry Pi Imager

2. **Flash SD Card:**
   ```
   Open Raspberry Pi Imager
   ↓
   Choose OS: Raspberry Pi OS (64-bit) - Recommended
   ↓
   Choose Storage: Your SD card
   ↓
   Click Settings (⚙️ icon):
     ✓ Enable SSH
     ✓ Set username: happyrow
     ✓ Set password: [your strong password]
     ✓ Configure WiFi (if needed)
     ✓ Set locale settings
   ↓
   Click WRITE
   ```

3. **Wait for completion** (~5-10 minutes)

#### 2.2 Initial Boot

1. Insert SD card into Raspberry Pi
2. Connect power (use good 5V 3A power supply!)
3. Wait 2-3 minutes for first boot
4. Find IP address (check your router or use: `arp -a`)

#### 2.3 First Connection

```bash
# SSH into Pi
ssh happyrow@<raspberry-pi-ip>

# Update system
sudo apt update
sudo apt upgrade -y

# Reboot
sudo reboot
```

#### 2.4 Apply Security Hardening

Follow the full guide: [`RASPBERRY_PI_SECURITY.md`](./RASPBERRY_PI_SECURITY.md)

**Quick version:**

```bash
# Install essentials
sudo apt install -y ufw fail2ban docker.io docker-compose

# Configure firewall
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw enable

# Add user to docker group
sudo usermod -aG docker $USER

# Setup Fail2Ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# Reboot
sudo reboot
```

---

## 🛡️ Prevent Future Issues

### 1. Use Quality SD Card

**Recommended brands:**
- SanDisk Extreme/Ultra
- Samsung EVO Plus
- Kingston Canvas
- Transcend

**Minimum specs:**
- Class 10 or UHS-1
- A1 or A2 rated
- 32GB capacity

### 2. Use Adequate Power Supply

**Official Raspberry Pi Power Supply:**
- 5.1V 3.0A USB-C (RPi 4)
- 5.1V 2.5A Micro-USB (RPi 3)

**Check your current supply:**
```bash
vcgencmd get_throttled
# Should always be: throttled=0x0
```

### 3. Add Cooling

**Recommended:**
- Aluminum heatsink kit (~$5)
- Small 5V cooling fan (~$5)
- Official Raspberry Pi case with fan

**Monitor temperature:**
```bash
watch -n 1 vcgencmd measure_temp
# Keep under 70°C
```

### 4. Enable Log2RAM (Reduce SD Writes)

```bash
# Install log2ram to reduce SD card wear
echo "deb http://packages.azlux.fr/debian/ buster main" | sudo tee /etc/apt/sources.list.d/azlux.list
wget -qO - https://azlux.fr/repo.gpg.key | sudo apt-key add -
sudo apt update
sudo apt install log2ram -y
sudo reboot
```

### 5. Regular Maintenance

**Create maintenance script:**

```bash
nano ~/maintenance.sh
```

```bash
#!/bin/bash
echo "=== Raspberry Pi Maintenance ==="
date

echo "Temperature: $(vcgencmd measure_temp)"
echo "Throttled: $(vcgencmd get_throttled)"
echo "Voltage: $(vcgencmd measure_volts)"

echo -e "\n=== Disk Space ==="
df -h / | grep -v Filesystem

echo -e "\n=== Memory ==="
free -h

echo -e "\n=== Update System ==="
sudo apt update
sudo apt upgrade -y
sudo apt autoremove -y
sudo apt clean

echo -e "\n=== Docker Cleanup ==="
docker system prune -af

echo -e "\n=== Check for Errors ==="
sudo dmesg | grep -i "error" | tail -5

echo -e "\n=== Maintenance Complete ==="
```

```bash
chmod +x ~/maintenance.sh

# Run weekly via cron
crontab -e
# Add:
0 2 * * 0 /home/happyrow/maintenance.sh >> /home/happyrow/maintenance.log 2>&1
```

---

## 📊 Health Monitoring

### Create Health Check Script

```bash
nano ~/health-check.sh
```

```bash
#!/bin/bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m'

echo "=== Raspberry Pi Health Check ==="
date
echo ""

# Temperature
TEMP=$(vcgencmd measure_temp | sed 's/temp=//' | sed "s/'C//")
echo -n "Temperature: $TEMP°C - "
if (( $(echo "$TEMP < 70" | bc -l) )); then
    echo -e "${GREEN}OK${NC}"
elif (( $(echo "$TEMP < 80" | bc -l) )); then
    echo -e "${YELLOW}WARNING${NC}"
else
    echo -e "${RED}CRITICAL${NC}"
fi

# Throttling
THROTTLED=$(vcgencmd get_throttled)
echo -n "Throttled: $THROTTLED - "
if [ "$THROTTLED" = "throttled=0x0" ]; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}ISSUE DETECTED${NC}"
fi

# Disk Space
DISK_USAGE=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
echo -n "Disk Usage: ${DISK_USAGE}% - "
if [ "$DISK_USAGE" -lt 80 ]; then
    echo -e "${GREEN}OK${NC}"
elif [ "$DISK_USAGE" -lt 90 ]; then
    echo -e "${YELLOW}WARNING${NC}"
else
    echo -e "${RED}CRITICAL${NC}"
fi

# Memory
MEM_USAGE=$(free | awk 'NR==2 {printf "%.0f", $3/$2 * 100}')
echo -n "Memory Usage: ${MEM_USAGE}% - "
if [ "$MEM_USAGE" -lt 80 ]; then
    echo -e "${GREEN}OK${NC}"
elif [ "$MEM_USAGE" -lt 90 ]; then
    echo -e "${YELLOW}WARNING${NC}"
else
    echo -e "${RED}CRITICAL${NC}"
fi

# SD Card Errors
SD_ERRORS=$(sudo dmesg | grep -i "mmc.*error" | wc -l)
echo -n "SD Card Errors: $SD_ERRORS - "
if [ "$SD_ERRORS" -eq 0 ]; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}ERRORS DETECTED${NC}"
fi

echo ""
echo "=== Docker Status ==="
docker ps 2>/dev/null || echo "Docker not running or not installed"

echo ""
echo "=== Recent Errors ==="
sudo dmesg | grep -i "error" | tail -3
```

```bash
chmod +x ~/health-check.sh
./health-check.sh
```

---

## 🆘 Emergency Contacts & Resources

### SD Card Failed Completely

**Symptoms:**
- Cannot boot at all
- No SSH access
- Continuous red/green LED blinking

**Solution:**
1. Get new SD card
2. Follow "Fresh Installation" above
3. If you had backups, restore from backup

### Backup Your SD Card (Prevention)

**On your Mac, with SD card inserted:**

```bash
# Find SD card device
diskutil list
# Look for your SD card (e.g., /dev/disk2)

# Backup SD card to image file
sudo dd if=/dev/disk2 of=~/rpi-backup.img bs=4m

# Compress backup (takes time but saves space)
gzip ~/rpi-backup.img

# To restore later:
# gunzip ~/rpi-backup.img.gz
# sudo dd if=~/rpi-backup.img of=/dev/disk2 bs=4m
```

**Schedule regular backups!**

---

## 📞 Still Having Issues?

### Debug Checklist

- [ ] SD card is high-quality and not counterfeit
- [ ] Power supply is official or rated 5V 3A
- [ ] Power cable is not damaged
- [ ] Raspberry Pi has proper ventilation/cooling
- [ ] Ambient temperature is reasonable (< 30°C)
- [ ] All connections are secure
- [ ] No physical damage to Pi or components
- [ ] Tried different SD card
- [ ] Tried different power supply
- [ ] Checked for loose connections

### System Information to Collect

```bash
# Gather system info for troubleshooting
echo "=== System Info ===" > ~/system-info.txt
cat /proc/cpuinfo >> ~/system-info.txt
cat /proc/meminfo >> ~/system-info.txt
vcgencmd get_throttled >> ~/system-info.txt
vcgencmd measure_temp >> ~/system-info.txt
dmesg | grep -i "error" >> ~/system-info.txt
df -h >> ~/system-info.txt
free -h >> ~/system-info.txt

cat ~/system-info.txt
```

---

## 🎯 Recovery Success Checklist

Your Raspberry Pi is recovered when:

- [ ] No I/O or bus errors in logs
- [ ] `vcgencmd get_throttled` returns `0x0`
- [ ] Temperature stays under 70°C
- [ ] Can read/write files without errors
- [ ] System is stable for 24+ hours
- [ ] Docker can start containers
- [ ] Network connectivity is stable
- [ ] No SD card errors in dmesg

---

**Once recovered, proceed with deployment: [`RASPBERRY_PI_QUICK_START.md`](./RASPBERRY_PI_QUICK_START.md)**
