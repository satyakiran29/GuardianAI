# 🚀 Secure Hosting & Deployment Guide (Zero API Key Leakage)

This guide provides an end-to-end walkthrough on how to securely host and deploy the **GuardianAI Django Backend & Supabase Cloud Command Dashboard** to production without ever exposing, hardcoding, or leaking your secret API keys, Supabase credentials, Groq AI tokens, or Django secret keys to GitHub or public repositories.

---

## 📌 Table of Contents
1. [Core Security Architecture (Zero Secret Exposure)](#-1-core-security-architecture-zero-secret-exposure)
2. [Step-by-Step Cloud Deployment Options](#-2-step-by-step-cloud-deployment-options)
   - [Option A: Render.com (Recommended Free/Low Cost)](#option-a-hosting-on-rendercom-recommended)
   - [Option B: Railway.app (1-Click Auto Deployment)](#option-b-hosting-on-railwayapp)
   - [Option C: Fly.io / Koyeb](#option-c-hosting-on-flyio-or-koyeb)
   - [Option D: Standalone Linux VPS (AWS EC2 / DigitalOcean / Linode)](#option-d-hosting-on-standalone-linux-vps-aws-ec2--digitalocean--ubuntu)
3. [Securing AI & Map Services](#-3-securing-ai--map-services)
   - [OpenFreeMap (Zero Keys Required)](#openfreemap-radar-zero-api-key-exposure)
   - [Groq AI Secret Management](#groq-ai-crisis-advisor-key-protection)
4. [Supabase Cloud Row Level Security (RLS) Configuration](#-4-supabase-cloud-row-level-security-rls-configuration)
5. [Connecting the Android Mobile App to Hosted Backend](#-5-connecting-the-android-mobile-app-to-hosted-backend)
6. [Automated Leak Prevention & Git Pre-Commit Guard](#-6-automated-leak-prevention--git-pre-commit-guard)
7. [Emergency Secret Revocation & Key Rotation](#-7-emergency-secret-revocation--key-rotation)

---

## 🔒 1. Core Security Architecture (Zero Secret Exposure)

### The Golden Rule
**Never commit `.env`, passwords, or private API tokens to Git.**

The GuardianAI codebase is built using a **two-tier configuration pattern**:
- **In Local Development**: The backend loads from [backend/.env](file:///h:/Github/GuardianAI/App/GuardianAi/backend/.env) (which is permanently excluded in [.gitignore](file:///h:/Github/GuardianAI/App/GuardianAi/.gitignore)).
- **In Cloud Production**: The backend reads directly from the cloud platform's encrypted **Environment Variables Vault** (Render, Railway, Fly.io, or VPS system environment).

```
┌────────────────────────────────────────────────────────┐
│               LOCAL DEVELOPMENT MACHINE                │
│  backend/.env  ──(Loaded by python-dotenv)──> Django   │
│       │                                                │
│       └──(Excluded by .gitignore)──> ❌ NEVER PUSHED   │
└────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────┐
│               CLOUD PRODUCTION SERVER                  │
│  Platform Vault (Render / Railway / AWS System Env)   │
│       │                                                │
│       └──(Injected securely at runtime)───> Django     │
└────────────────────────────────────────────────────────┘
```

---

## ☁️ 2. Step-by-Step Cloud Deployment Options

### Option A: Hosting on Render.com (Recommended)

Render offers free web service hosting with automatic HTTPS SSL, continuous deployment from GitHub, and an encrypted secrets manager.

1. **Verify `.gitignore`** contains:
   ```gitignore
   .env
   backend/.env
   *.sqlite3
   backend/db.sqlite3
   ```
2. **Push your repository to GitHub**:
   ```bash
   git add .
   git commit -m "Deploy GuardianAI"
   git push origin main
   ```
3. Go to [Render Dashboard](https://dashboard.render.com/) and click **New +** ➡️ **Web Service**.
4. Connect your GitHub repository.
5. Configure the service settings:
   - **Name**: `guardianai-backend`
   - **Region**: Singapore / Frankfurt / Oregon (closest to your users)
   - **Branch**: `main`
   - **Root Directory**: `backend`
   - **Runtime**: `Python 3`
   - **Build Command**:
     ```bash
     pip install -r requirements.txt && python manage.py migrate && python manage.py seed_demo_data
     ```
   - **Start Command**:
     ```bash
     gunicorn guardian_backend.wsgi:application --bind 0.0.0.0:$PORT
     ```
6. **Inject Encrypted Environment Variables**:
   Click **Advanced** ➡️ **Add Environment Variable** and enter the following keys and values:

   | Key | Value | Purpose |
   | :--- | :--- | :--- |
   | `DJANGO_SECRET_KEY` | *(Generate a 50+ char random string)* | Cryptographic security salt |
   | `DEBUG` | `False` | Disables debug tracebacks in production |
   | `ALLOWED_HOSTS` | `*` or `guardianai-backend.onrender.com` | Host header security whitelist |
   | `SUPABASE_URL` | `https://jwntzspmzapxablkmqhp.supabase.co` | Supabase Cloud Database URL |
   | `SUPABASE_KEY` | `sb_publishable_jB5ChDHJa-XPwBPyoHMLNQ_1kZb3AMv` | Supabase Cloud Access Key |
   | `GROQ_API_KEY` | `your_groq_api_token_here` | 24/7 AI Safety Assistant LLM Key |

7. Click **Create Web Service**. Render will securely build and deploy your service with free SSL (e.g. `https://guardianai-backend.onrender.com`).

---

### Option B: Hosting on Railway.app

Railway provides automatic GitHub webhooks with instant deployment and zero-exposure variable management.

1. Log in to [Railway.app](https://railway.app/) and click **New Project** ➡️ **Deploy from GitHub Repo**.
2. Select your `GuardianAI` repository.
3. In **Settings**:
   - Set **Root Directory** to `/backend`.
4. In the **Variables** tab, click **Raw Editor** and paste your production variables:
   ```env
   DJANGO_SECRET_KEY=django-insecure-prod-key-xyz9876543210!@#$%^
   DEBUG=False
   ALLOWED_HOSTS=*
   SUPABASE_URL=https://jwntzspmzapxablkmqhp.supabase.co
   SUPABASE_KEY=sb_publishable_jB5ChDHJa-XPwBPyoHMLNQ_1kZb3AMv
   GROQ_API_KEY=your_groq_api_key_here
   PORT=8000
   ```
5. Set **Start Command**:
   ```bash
   python manage.py migrate && python manage.py seed_demo_data && gunicorn guardian_backend.wsgi:application --bind 0.0.0.0:$PORT
   ```
6. Click **Deploy**. Railway will generate your secure domain with automated SSL.

---

### Option C: Hosting on Fly.io or Koyeb

For lightweight containerized deployment:

1. Install Fly CLI:
   ```bash
   flyctl auth login
   ```
2. Initialize app in `backend/`:
   ```bash
   flyctl launch
   ```
3. Set secrets via CLI without committing any files:
   ```bash
   flyctl secrets set DJANGO_SECRET_KEY="production_secret_key_here" \
                      SUPABASE_URL="https://jwntzspmzapxablkmqhp.supabase.co" \
                      SUPABASE_KEY="sb_publishable_jB5ChDHJa-XPwBPyoHMLNQ_1kZb3AMv" \
                      DEBUG="False"
   ```
4. Deploy:
   ```bash
   flyctl deploy
   ```

---

### Option D: Hosting on Standalone Linux VPS (AWS EC2 / DigitalOcean / Ubuntu)

On a Linux VPS, your credentials live in a protected `.env` file created **directly on the server via SSH**, with permissions locked down to `chmod 600`.

1. **SSH into your server**:
   ```bash
   ssh ubuntu@your_server_ip
   ```
2. **Clone your repository**:
   ```bash
   git clone https://github.com/your-username/GuardianAI.git
   cd GuardianAI/backend
   ```
3. **Create the production `.env` file on the remote server**:
   ```bash
   nano .env
   ```
   Paste your variables:
   ```env
   DJANGO_SECRET_KEY=generate_a_strong_50_char_secret_key
   DEBUG=False
   ALLOWED_HOSTS=yourdomain.com,your_server_ip
   SUPABASE_URL=https://jwntzspmzapxablkmqhp.supabase.co
   SUPABASE_KEY=sb_publishable_jB5ChDHJa-XPwBPyoHMLNQ_1kZb3AMv
   GROQ_API_KEY=your_groq_api_key_here
   ```
   *Save and exit (`Ctrl+O` ➡️ `Enter` ➡️ `Ctrl+X`).*
4. **Lock file permissions to root/app user only**:
   ```bash
   chmod 600 .env
   ```
5. **Set up Python virtual environment & Gunicorn**:
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   python manage.py migrate
   python manage.py seed_demo_data
   ```
6. **Set up Nginx Reverse Proxy with Let's Encrypt SSL**:
   ```bash
   sudo apt install nginx certbot python3-certbot-nginx -y
   sudo certbot --nginx -d yourdomain.com
   ```

---

## 🗺️ 3. Securing AI & Map Services

### OpenFreeMap Radar (Zero API Key Exposure)
The GuardianAI telemetric radar is powered by **OpenFreeMap** (`https://openfreemap.org/`).
- **100% Free and Open-Source**
- **Zero API Keys required**
- **No rate limits or credit card required**
- It is impossible to leak map keys because no key exists!

### Groq AI Crisis Advisor Key Protection
To prevent your Groq LLM API Key from being decompiled out of the Android APK:
- Keep the Groq key stored on the Django backend in `GROQ_API_KEY`.
- Have the Android app communicate with the AI advisor via the backend endpoint `/api/ai/chat/`, keeping your private Groq credentials completely hidden on the server.

---

## 🛡️ 4. Supabase Cloud Row Level Security (RLS) Configuration

To ensure your Supabase database remains protected even if client-side endpoints are inspected:

1. Open your [Supabase Project Dashboard](https://supabase.com/dashboard/project/jwntzspmzapxablkmqhp/editor).
2. Go to **Authentication** ➡️ **Policies** (or **Table Editor** ➡️ Select Table ➡️ Click **Enable RLS**).
3. Enable RLS on:
   - `guardian_users`
   - `emergency_alerts`
   - `otp_records`
   - `contacts`
4. Add policies:
   - **Insert Policy**: Allow `service_role` and authenticated API backend requests to insert and update records.
   - **Select Policy**: Allow users to read only their own contact records and distress feeds.

---

## 📱 5. Connecting the Android Mobile App to Hosted Backend

Once your backend is live (e.g. `https://guardianai-backend.onrender.com`), connect the Android application:

In [ApiClient.java](file:///h:/Github/GuardianAI/App/GuardianAi/app/src/main/java/com/android/sheguard/api/ApiClient.java):
```java
// Change local emulator URL to your production cloud endpoint:
private static final String BASE_URL = "https://guardianai-backend.onrender.com/api/";
```

Or configure it dynamically in `app/build.gradle`:
```groovy
android {
    defaultConfig {
        buildConfigField "String", "API_BASE_URL", "\"https://guardianai-backend.onrender.com/api/\""
    }
}
```

---

## 🚨 6. Automated Leak Prevention & Git Pre-Commit Guard

### Step 1: Pre-Push Verification Commands
Run these commands before any commit or push:
```bash
# 1. Verify that .env is NOT tracked or staged
git status

# 2. Check all staged code for accidental secrets
git diff --staged
```

### Step 2: Install Automatic Git Pre-Commit Hook
You can install an automatic secret guard on your local machine that blocks any commit containing `.env` or API secrets.

Create a file at `.git/hooks/pre-commit`:
```bash
#!/bin/sh
# GuardianAI Secret Leak Guard Hook

if git diff --cached --name-only | grep -E "(\.env|\.sqlite3|id_rsa)"; then
    echo "❌ ERROR: Attempted to commit a sensitive file (.env or database)!"
    echo "Aborting commit. Remove the file using: git reset HEAD <file>"
    exit 1
fi

if git diff --cached | grep -iE "(sb_publishable_|DJANGO_SECRET_KEY|GROQ_API_KEY)" | grep -v "\.env\.example"; then
    echo "⚠️ WARNING: Potential hardcoded secret detected in staged changes!"
    echo "Please check your staged files before committing."
fi

exit 0
```
Make it executable:
```bash
chmod +x .git/hooks/pre-commit
```

---

## 🔄 7. Emergency Secret Revocation & Key Rotation

If you ever accidentally push a secret to a public Git repository:

1. **Immediately purge the file from Git history**:
   ```bash
   git rm --cached backend/.env
   git commit -m "chore: remove untracked secrets"
   git push origin main
   ```
2. **Rotate Supabase Keys**:
   - Go to [Supabase API Settings](https://supabase.com/dashboard/project/jwntzspmzapxablkmqhp/settings/api).
   - Click **Generate New API Key / JWT Secret**.
3. **Rotate Django Secret Key**:
   - Change `DJANGO_SECRET_KEY` in your cloud platform dashboard (Render / Railway / VPS).
   - Restart the server process. No database data will be lost.
