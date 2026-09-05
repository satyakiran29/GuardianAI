<div align="center">
  # 🛡️ GuardianAI
  ### *Autonomous Women Safety, Telemetric Emergency Response & Cloud Command Platform*

  [![Android](https://img.shields.io/badge/Platform-Android%20%7C%20Java%20%26%20XML-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
  [![Django](https://img.shields.io/badge/Backend-Django%206%20%2B%20REST%20Framework-092E20?style=for-the-badge&logo=django&logoColor=white)](https://www.djangoproject.com/)
  [![Supabase](https://img.shields.io/badge/Cloud-Supabase%20Realtime-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)](https://supabase.com/)
  [![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
</div>

---

## 📌 Table of Contents
1. [Overview & Mission](#-overview--mission)
2. [End-to-End System Architecture](#-end-to-end-system-architecture)
3. [Multi-Role Access Control Model](#-multi-role-access-control-model)
4. [Application Lifecycle & Onboarding Flowchart](#-application-lifecycle--onboarding-flowchart)
5. [Emergency SOS Telemetry & Dispatch Flowchart](#-emergency-sos-telemetry--dispatch-flowchart)
6. [Key Features Breakdown](#-key-features-breakdown)
7. [Django & Supabase Cloud Command Dashboard](#-django--supabase-cloud-command-dashboard)
8. [Admin User Data Management System](#-admin-user-data-management-system)
9. [REST API Reference](#-rest-api-reference)
10. [Installation & Setup Guide](#-installation--setup-guide)
11. [Secure Cloud Hosting Guide (HOST.md)](HOST.md)
12. [Engineering Team](#-engineering-team)

---

## 🌟 Overview & Mission

**GuardianAI** is a mission-critical personal safety and rapid emergency response ecosystem designed specifically for women and vulnerable citizens. The platform connects a native **Android Client (Java + XML UI)** with an intelligent **Django 6 + Supabase Cloud Command Dashboard**, offering autonomous panic detection, real-time GPS telemetry, instant police/guardian dispatching, and dynamic 6-digit OTP security.

---

## 🏗️ End-to-End System Architecture

The GuardianAI ecosystem operates across four interconnected tiers: **Client Tier**, **Application & REST Tier**, **Cloud Database Tier**, and **Multi-Channel Dispatch Channels**.

```mermaid
graph TB
    subgraph ClientTier ["📱 Native Android Client Tier (Java & XML UI)"]
        A1["Intro / Onboarding Carousel<br/>(3-Tier Safety Slides)"] --> A2["Authentication Engine<br/>(Email / Password / Phone OTP)"]
        A2 --> A3["6-Digit OTP Verification Dialog<br/>(Auto-Focus & Countdown Timer)"]
        A3 --> A4["GuardianAI Safety Hub<br/>(Home Dashboard)"]
        
        subgraph SafetyEngines ["Safety & Telemetry Engines"]
            S1["1-Tap Big SOS Button"]
            S2["Shake Gesture Detector<br/>(Accelerometer Sensor)"]
            S3["Voice Keyword Trigger<br/>('Help' / 'Guardian SOS')"]
            S4["Safety Timer & Check-In"]
            S5["Fake Call Simulator"]
            S6["AI Crisis Advisor"]
            S7["Safe Mode & App Stopper"]
            S8["15% Battery Auto-Alert"]
            S9["3 Home Screen Widgets"]
        end
        A4 --> SafetyEngines
    end

    subgraph BackendTier ["🖥️ Backend & Command Dashboard Tier (Django 6)"]
        B1["Django REST Framework API<br/>(/api/auth/, /api/sos/, /api/users/)"]
        B2["Role-Based Access Controller<br/>(SuperAdmin, Guardian, User)"]
        B3["OTP Generator & Inspector<br/>(10-Min Lifecycle & Passcode Vault)"]
        B4["Live Command Center Web UI<br/>(Leaflet.js Interactive Dark Radar)"]
        B5["Emergency Incident Dispatcher<br/>(Unit Assignment & Resolution)"]
    end

    subgraph CloudTier ["☁️ Cloud & Database Tier (Supabase)"]
        C1[("Supabase Real-Time Cloud DB<br/>• guardian_users (Roles & GPS Telemetry)<br/>• emergency_alerts (Live Distress Feed)<br/>• otp_records (Passcodes & Lifecycle)<br/>• emergency_contacts (Alert Directory)")]
    end

    subgraph DispatchChannels ["🚨 Multi-Channel Telemetry Fan-Out"]
        D1["SMS Dispatcher<br/>(SmsManager with Coordinates & Maps Link)"]
        D2["WhatsApp Live Location Broadcaster<br/>(Automated Message + Coordinates)"]
        D3["Police Siren Audio Generator<br/>(100% Volume Alarm)"]
        D4["112 Emergency Auto-Dialer"]
        D5["Live Web Dashboard Alert Ping<br/>(Pulsing Victim Marker on Radar)"]
    end

    %% Connections
    SafetyEngines -->|REST Telemetry Beacon| B1
    A2 -->|OTP Request & Verify| B1
    B1 --> C1
    B1 --> B4
    SafetyEngines --> DispatchChannels
    B5 -->|Dispatch Nearest Unit| D5
```

---

## 👥 Multi-Role Access Control Model

GuardianAI structures users into three distinct security tiers:

| Role | Badge | Permissions & Capabilities | Target Audience |
| :--- | :---: | :--- | :--- |
| **SuperAdmin** | `👑 SUPERADMIN` | System-wide command over all users, live incident dispatches, audit logs, OTP transaction inspection, and system metrics. | System Administrators, Law Enforcement Leads |
| **Guardian** | `🛡️ GUARDIAN` | Receives real-time proximity distress alerts, assigned to victims, views live GPS telemetry, and confirms safety resolution. | Verified Patrol Units, SHE Teams, Campus Security, Parents |
| **Protected User** | `🌸 USER` | Access to 1-Tap SOS, Shake & Voice Detection, AI Safety Assistant, Safe Route, Fake Call, Safety Timer, and Emergency Contacts. | Women, Students, Solo Commuters, Citizens |

---

## 🔄 Application Lifecycle & Onboarding Flowchart

```mermaid
flowchart TD
    Start(["Launch GuardianAI App"]) --> CheckAuth{"Is Session Active?<br/>(Supabase Session / OTP / Demo)"}
    
    %% First Page Flow
    CheckAuth -- No --> P1["Page 1: Onboarding Carousel<br/>(Slide 1: AI Shield | Slide 2: SOS Radar | Slide 3: Secure OTP)"]
    P1 --> ActionChoice{"User Action"}
    ActionChoice -- "Get Started / Skip" --> P2["Page 2: Login / Register Screen"]
    
    %% Second Page Flow
    P2 --> AuthMethod{"Choose Sign-In Option"}
    AuthMethod -- "🌟 One-Click Demo Mode" --> DemoMode["Load Demo Profile & Mock Contacts"]
    DemoMode --> P3["Page 3: Guardian Home Dashboard"]
    
    AuthMethod -- "🔑 Sign In with Phone OTP" --> PromptPhone["Enter Phone Number"]
    PromptPhone --> DispatchOtp["Backend generates 6-Digit OTP<br/>(Synced with Supabase)"]
    DispatchOtp --> OtpDialog["Show OtpVerificationDialog<br/>(Auto-Focus PIN Boxes & 60s Timer)"]
    
    AuthMethod -- "Register Account" --> FillReg["Fill Name, Phone, Email & Role<br/>(Protected User vs Guardian)"]
    FillReg --> DispatchOtp
    
    OtpDialog --> VerifyCode{"Enter Code or<br/>Use Demo 123456"}
    VerifyCode -- Valid --> SaveSession["Save User Session in Prefs<br/>(Store Phone, Name, Role)"]
    VerifyCode -- Invalid --> RetryOtp["Show Error Toast / Resend Option"]
    RetryOtp --> OtpDialog
    
    SaveSession --> P3
    CheckAuth -- Yes --> P3
    
    %% Third Page Flow
    subgraph HomeOperations ["Page 3: GuardianAI Safety Operations"]
        P3 --> SOS_Ops["1-Tap SOS Panic Button"]
        P3 --> Safe_Ops["Activate Safe Mode & Kill Background Apps"]
        P3 --> Fake_Ops["Simulate Urgent Fake Call"]
        P3 --> AI_Ops["Consult 24/7 AI Safety Assistant"]
        P3 --> Route_Ops["Safe Route & Safe Arrival Shield"]
        P3 --> Timer_Ops["Safety Countdown Check-In Timer"]
        P3 --> Voice_Ops["Voice SOS Keyword Listening"]
        P3 --> Settings_Ops["Settings & Display Customization"]
        Settings_Ops --> LogoutAction["🚪 Log Out -> Clear Session -> Onboarding"]
    end
```

---

## 🚨 Emergency SOS Telemetry & Dispatch Flowchart

When an emergency occurs, GuardianAI dispatches telemetry simultaneously across offline and online channels in **under 2 seconds**:

```mermaid
sequenceDiagram
    autonumber
    actor Victim as 🌸 Protected Citizen
    participant App as 📱 GuardianAI App
    participant Sensors as 🧭 GPS & Sensors
    participant Backend as 🖥️ Django Backend
    participant Supabase as ☁️ Supabase Cloud
    participant Contacts as 👥 Emergency Contacts
    actor Guardian as 🛡️ Guardian / Police Patrol

    Victim->>App: Triggers SOS (Button / Shake / Voice / Timer / Battery 15%)
    App->>Sensors: Fast-path GPS Location Request (Lat/Lng Fix)
    Sensors-->>App: Fresh Coordinates (e.g. 17.4250 N, 78.4520 E)
    
    par Multi-Channel Emergency Dispatch
        App->>Contacts: 📩 Send Emergency SMS with Live Google Maps Link
        App->>Contacts: 💬 Send WhatsApp Alert with Location Pin
        App->>App: 🔊 Activate Loud Police Operation Siren (100% Vol)
        App->>App: 📳 Vibrate Device Haptic Pattern
    and Cloud Command Synchronization
        App->>Backend: POST /api/sos/trigger/ (Phone, GPS, Battery, Siren Status)
        Backend->>Supabase: Insert into emergency_alerts & guardian_users
        Backend-->>App: Alert Broadcasted (#SOS ID)
    end

    Backend->>Guardian: 🚨 Real-time Radar Beeps on Command Dashboard (Pulsing Red Marker)
    Guardian->>Backend: Dispatches Nearest Patrol Unit
    Backend->>Supabase: Update status -> 'dispatched'
    Guardian->>Victim: Rescues Victim & Confirms Safe Resolution
    Guardian->>Backend: Mark SOS as Resolved
    Backend->>Supabase: Update status -> 'resolved'
```

---

## ⚡ Key Features Breakdown

### 📱 Android Application (Java & XML UI)
- **1-Tap Panic SOS Button**: Big pulsating emergency trigger with countdown cancellation dialog.
- **Shake Detection**: Background accelerometer listener that sends SOS upon vigorous device shake.
- **Voice SOS Keyword Detection**: Hands-free trigger using predefined safety phrases (*"Help"*, *"Guardian SOS"*, *"Save Me"*).
- **Fake Call Simulator**: Realistic incoming call simulator with customizable caller names (*Mom 💖*, *Boss*) and audio playback to gracefully escape uncomfortable situations.
- **24/7 AI Safety Assistant**: Crisis advisor offering real-time advice on stalking, public transit safety, self-defense moves, and legal rights.
- **Safe Mode Engine**: Stops battery-draining background apps to maximize tracking battery life and broadcasts live location via WhatsApp and SMS.
- **Safety Timer & Check-In**: Dead-man's switch countdown that automatically alerts guardians if a check-in is missed.
- **Taxi & Trip Monitoring**: Logs cab numbers and driver details with abnormal route diversion alerts.
- **15% Battery Guardian Alert**: Automatic emergency GPS broadcast to parents/guardians before device power depletion.
- **3 Home Screen Widgets**: 1-Tap SOS (2x2), Guardian Safety Hub (4x2), and Quick Safety Bar (4x1).
- **Theme & Multilingual Engine**: Light mode, Dark mode, Pure AMOLED Black mode 🖤, localized in **English 🇬🇧**, **Telugu 🇮🇳 (తెలుగు)**, and **Hindi 🇮🇳 (हिन्दी)**.
- **Settings & Safe Session Logout**: Comprehensive preferences suite with session clearance and account sign-out.

---

## 🖥️ Django & Supabase Cloud Command Dashboard

The GuardianAI Web Dashboard provides a **cyber-glassmorphism dark UI** for command center operators:

- **Interactive OpenFreeMap Telemetric Radar**: Displays live victim SOS beacons with expanding red radar waves and green shield pins for Guardian responders using **OpenFreeMap** (100% free, zero rate-limits, no API keys required, with Dark Radar, Liberty Street, and Bright layer switchers).
- **Live Emergency Feed**: Instant action triggers for *"Dispatch Nearest Guardian"* and *"Confirm Safe Resolution"*.
- **Role Switcher Tabs**: Filter live command desk by SuperAdmin, Guardian, or User view.
- **OTP Security Inspector & Simulator**: Live transaction ledger with an interactive testing tool to dispatch and verify passcodes.

---

## 👑 Admin User Data Management System

The Admin Dashboard provides full CRUD data controls at **[`/users/`](http://127.0.0.1:8000/users/)**:

- **✏️ Comprehensive User Data Editor (Modal)**:
  - Edit personal details: Full Name, Phone Number, Email Address.
  - Modify security roles (`👑 SuperAdmin`, `🛡️ Guardian`, `🌸 Protected User`).
  - Update live telemetry: Street Address, GPS Latitude, GPS Longitude, and Device Battery (%).
  - Toggle verification status (`is_verified`) and account status (`is_active`).
  - **Instant Supabase Cloud Sync**: All changes automatically push to Supabase `guardian_users`.
- **➕ Create User Records**: Quick account creator with pre-verification options.
- **🔄 1-Click Role Switcher**: Change permissions directly from the table dropdown.
- **📥 CSV Data Export**: Download full system user records (`guardian_ai_users.csv`) for security audits.
- **🗑️ Safe Deletion**: Permanently remove accounts with confirmation safety dialogs.

---

## 🔌 REST API Reference

| Endpoint | Method | Description | Sample Payload |
| :--- | :---: | :--- | :--- |
| `/api/auth/send-otp/` | `POST` | Generate & dispatch 6-digit OTP | `{"target": "+919876543210", "purpose": "login"}` |
| `/api/auth/verify-otp/` | `POST` | Validate 6-digit passcode | `{"target": "+919876543210", "otp_code": "123456"}` |
| `/api/auth/register/` | `POST` | Create account with role & cloud sync | `{"name": "Sneha", "phone": "+919876543210", "role": "user"}` |
| `/api/auth/login/` | `POST` | Sign in via password or phone OTP | `{"identifier": "+919876543210", "otp_code": "123456"}` |
| `/api/sos/trigger/` | `POST` | Broadcast emergency distress beacon | `{"phone": "+919876543210", "latitude": 17.425, "longitude": 78.452}` |
| `/api/sos/resolve/` | `POST` | Mark alert resolved | `{"alert_id": 1, "notes": "Safely resolved"}` |
| `/api/location/ping/` | `POST` | Update victim live GPS telemetry | `{"phone": "+919876543210", "latitude": 17.425, "longitude": 78.452}` |
| `/api/dashboard/stats/` | `GET` | Fetch real-time metrics & recent alerts | *None* |

---

## 🚀 Installation & Setup Guide

### 1. Backend & Dashboard Setup (Django + Supabase)
```bash
# Navigate to backend directory
cd backend

# Configure environment secrets
cp .env.example .env

# Run database migrations
python manage.py makemigrations guardian_api
python manage.py migrate

# Seed multi-role demo data
python manage.py seed_demo_data

# Start local server on port 8000
python manage.py runserver 127.0.0.1:8000
```
Visit **[http://127.0.0.1:8000/](http://127.0.0.1:8000/)** in your browser to access the live command center.

### 2. Android Mobile App Setup
- Open the root project in **Android Studio** (Giraffe or above).
- Build the debug APK via Gradle:
```powershell
.\gradlew.bat assembleDebug
```
- The compiled APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 👥 Engineering Team

- 👤 **[Pampana Satya Kiran](http://psatyakiran.in/)** — *Lead Developer & System Architect*
- 👤 **Amarthaluri Harshavardhan** — *Core Android & Security Engineer*
- 👤 **Madeli Narasimha** — *Backend & Cloud Integration*
- 👤 **[Mammula Sneha](https://www.linkedin.com/in/sneha-mammula-b0651832a/)** — *UI/UX & Safety Systems*
- 👤 **Kadagala Meghana** — *QA & Location Telemetry*

---

## 🤝 Credits & Inquiries
- Website: [psatyakiran.in](http://psatyakiran.in/)
- In-App Icons: [icons8.com](https://icons8.com) & Material Symbols
