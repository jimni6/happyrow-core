# SSH Keys Explained - Simple Guide

## 🔐 How SSH Keys Work (Simple Analogy)

Think of it like a lock and key system:

```
┌─────────────────────┐          ┌──────────────────────┐
│   Your Mac          │          │   Raspberry Pi       │
│                     │          │                      │
│  🔑 Private Key     │   SSH    │   🔓 Public Key      │
│  (Secret - Keep!)   │─────────▶│   (Shared - Safe)    │
│                     │          │                      │
│  ~/.ssh/id_ed25519  │          │  ~/.ssh/authorized   │
│        _pi          │          │        _keys         │
└─────────────────────┘          └──────────────────────┘
```

**Private Key** = Your secret key (like your house key)  
**Public Key** = Lock on the door (anyone can see it, but only your key opens it)

---

## 📖 Step-by-Step Visual Guide

### Current Situation (Password Auth)

```
You ──[username/password]──▶ Raspberry Pi
     ❌ Password can be:
        - Guessed
        - Stolen
        - Brute-forced
```

### After SSH Keys Setup

```
You ──[Private Key proves identity]──▶ Raspberry Pi checks Public Key
     ✅ Much more secure:
        - No password to steal
        - Cryptographically secure
        - Can't be brute-forced
```

---

## 🛠️ Setup Process (5 Minutes)

### Step 1: Generate Keys on Your Mac

```bash
ssh-keygen -t ed25519 -C "happyrow-pi" -f ~/.ssh/id_ed25519_pi
```

**What this does:**
```
┌─────────────────────────────────┐
│ Creates two files:              │
│                                 │
│ 1. id_ed25519_pi      (Private) │
│    └─▶ Keep secret! 🔒          │
│                                 │
│ 2. id_ed25519_pi.pub  (Public)  │
│    └─▶ Safe to share 📢         │
└─────────────────────────────────┘
```

**Press Enter** when asked for passphrase (or set one for extra security)

---

### Step 2: Copy Public Key to Pi

```bash
ssh-copy-id -i ~/.ssh/id_ed25519_pi.pub j.ni@192.168.1.79
```

**What this does:**
```
Your Mac                    Raspberry Pi
────────                    ────────────

Public Key ─────────────▶  ~/.ssh/authorized_keys
(copied)                    (saved here)

                           Now Pi knows to trust
                           your private key!
```

You'll need to enter your password **one last time**.

---

### Step 3: Test Connection

```bash
ssh -i ~/.ssh/id_ed25519_pi j.ni@192.168.1.79
```

**What happens:**
```
1. Your Mac: "Here's my private key signature"
2. Pi checks: "Does it match the public key I have?"
3. Match! ✅ "Welcome! No password needed."
```

---

## 🎨 Visual Connection Flow

### Before (Password)
```
┌─────────┐                           ┌──────────┐
│  You    │ "Password: hunter2"       │   Pi     │
│         │──────────────────────────▶│          │
│  Mac    │                           │          │
│         │◀──────────────────────────│  Checks  │
│         │  "OK, you can log in"     │          │
└─────────┘                           └──────────┘
```

### After (SSH Keys)
```
┌─────────┐                           ┌──────────┐
│  You    │ "Here's my signature"     │   Pi     │
│         │  [Signed with private key]│          │
│  Mac    │──────────────────────────▶│          │
│         │                           │  Verifies│
│  🔑     │◀──────────────────────────│  with    │
│         │  "Signature valid! ✅"    │  🔓      │
└─────────┘                           └──────────┘
```

---

## 💡 Making It Easier with SSH Config

Create `~/.ssh/config` on your Mac:

```bash
nano ~/.ssh/config
```

Add:
```
Host raspberrypi
    HostName 192.168.1.79
    User j.ni
    IdentityFile ~/.ssh/id_ed25519_pi
    Port 22
```

Now you can connect with just:
```bash
ssh raspberrypi
```

**Much simpler!** 🎉

---

## 🔒 Security Benefits

| Password Auth | SSH Keys |
|--------------|----------|
| ❌ Can be guessed | ✅ Cryptographically secure |
| ❌ Weak passwords common | ✅ 256-bit encryption |
| ❌ Vulnerable to keyloggers | ✅ Key never transmitted |
| ❌ Same password everywhere? | ✅ Unique per device |
| ❌ Brute-force attacks work | ✅ Brute-force impossible |

---

## 📁 File Locations Reference

### On Your Mac
```
~/.ssh/
├── id_ed25519_pi           ← Private key (KEEP SECRET!)
├── id_ed25519_pi.pub       ← Public key (can share)
└── config                  ← Optional: SSH shortcuts
```

### On Your Raspberry Pi
```
~/.ssh/
└── authorized_keys         ← Contains your public key
```

---

## 🚀 Quick Start Commands

```bash
# 1. Generate key on Mac
ssh-keygen -t ed25519 -C "happyrow-pi" -f ~/.ssh/id_ed25519_pi

# 2. Copy to Pi (enter password one last time)
ssh-copy-id -i ~/.ssh/id_ed25519_pi.pub j.ni@192.168.1.79

# 3. Test connection (no password needed!)
ssh -i ~/.ssh/id_ed25519_pi j.ni@192.168.1.79

# 4. Set up shortcut (optional)
echo "Host raspberrypi
    HostName 192.168.1.79
    User j.ni
    IdentityFile ~/.ssh/id_ed25519_pi" >> ~/.ssh/config

# 5. Connect easily
ssh raspberrypi
```

---

## ✅ Verification Checklist

After setup, verify:

- [ ] Can connect without password: `ssh -i ~/.ssh/id_ed25519_pi j.ni@192.168.1.79`
- [ ] Private key has correct permissions: `ls -l ~/.ssh/id_ed25519_pi` (should show `-rw-------`)
- [ ] Public key is on Pi: `ssh j.ni@192.168.1.79 "cat ~/.ssh/authorized_keys"`
- [ ] SSH config shortcut works: `ssh raspberrypi`

---

## 🎯 Next Steps

1. ✅ Generate SSH keys
2. ✅ Copy public key to Pi
3. ✅ Test passwordless login
4. 🔄 Disable password authentication (for maximum security)
5. 🚀 Proceed with deployment!

---

## 💬 Common Questions

### Q: What if I lose my private key?
**A:** You'll need physical access to the Pi to add a new key or re-enable password auth.

### Q: Can I use the same key on multiple computers?
**A:** Yes! Copy your private key (securely) or generate unique keys per device (more secure).

### Q: Should I use a passphrase for my key?
**A:** Optional but recommended for extra security. You'll need to enter it when using the key.

### Q: Is it safe to share my public key?
**A:** Yes! That's the point. It's like a lock - anyone can see it, but only your private key opens it.

### Q: What if I want to connect from my phone?
**A:** Use SSH client apps like Termius (iOS/Android) and import your private key.

---

**Ready to set up SSH keys?** Follow the Quick Start Commands above! 🚀
