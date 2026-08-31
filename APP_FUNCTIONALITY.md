# 🛡️ Surveillance — App Functionality & User Guide

**Version:** 1.0.1 (Production Build)  
**Developer:** Amit Bharat  
**Company:** Rooys Soft Tech  
**Contact:** rooyssofttech2020@gmail.com  
**Privacy:** 100% Offline • No Ads • Zero Tracking  

---

## 📌 Overview
**Surveillance** is an offline security and recording manager engineered for reliability, stealth operations, and automated evidence management. Powered by modern Android CameraX and Media3 architectures, it enables background video recording, low-power audio capture, GPS coordinate watermarking, biometric encryption, and automated storage lifecycle maintenance.

---

## 📱 Core Features & User Guide

### 1. 🗂️ Navigation & Screen Layout (4 Tabs)
- **Home Tab:** Active recording operations center with one-touch triggers for video, audio, stealth photo capture, camera flipping, and GPS location stamping.
- **Files Tab:** Chronological recording archives with filter chips (Today, This Week, This Month, Favorites) and search.
- **Vault Tab:** High-security biometric and PIN-locked safe for sensitive footage.
- **Settings Tab:** Complete control over video resolution, frame rates, auto-cleanup policies, and security locks.

---

### 2. 📹 Recording Capabilities
- **Background Video Recording:**
  - Foreground service keeps recording continuously even when switching apps or navigating elsewhere.
  - Supports 480p, 720p, 1080p Full HD, and 4K UHD resolutions.
  - Target Frame Rate options: 30 FPS (battery efficient) or 60 FPS (high fluid motion).
  - Software & hardware video stabilization toggle.
- **Stealth Audio Recording:**
  - Records crystal-clear M4A audio in the background with minimal battery and CPU usage.
- **Stealth Photo Capture:**
  - One-tap instantaneous snapshot with zero shutter delays.
- **Power Button Auto-Save:**
  - Automatically finalizes and safely closes open video/audio files when the physical device screen turns off or locks, preventing file corruption.

---

### 3. 📍 GPS Geo-Tagging & Watermarking
- **Sensor Stamping:** Automatically embeds live GPS coordinates (Latitude/Longitude), timestamp, and device model directly into video/image EXIF metadata and on-screen watermarks.
- **Location Filter & Inspector:** View recording coordinates directly in the built-in media player info dialog.

---

### 4. 🔒 Biometric Security & App Lock
- **Biometric & PIN Lock:** Protect the entire application with fingerprint, facial recognition, or a custom 4-digit security PIN.
- **Encrypted Private Vault:** Move sensitive clips into a hidden directory (`.vault`) isolated from system galleries and external media scanners.
- **Instant Vault Lock:** One-tap button to immediately lock vault archives upon exiting.

---

### 5. 🎬 Built-in Player & Media Manager
- **Playback Controls:** Play, pause, scrub, and change playback speed (0.5x, 1.0x, 1.25x, 1.5x, 2.0x).
- **Picture-in-Picture (PiP):** Keep monitoring recordings in a floating window while using other apps.
- **EXIF & Metadata Dialog:** Inspect resolution, video codec, audio sample rate, file size, recording duration, exact timestamp, and GPS coordinates.
- **Filter Chips:** Instant chronological filtering (*Today*, *This Week*, *This Month*, *Favorites*).
- **Grid / List Toggle:** Switch between 2-column visual grid and detailed metadata list view.

---

### 6. ⚡ Multi-Selection Action Bar
- **Batch Selection Mode:** Long-press any file to activate the bottom action bar:
  - ℹ️ **Metadata Inspector:** View comprehensive technical specs.
  - 📤 **Batch Share:** Share multiple videos or audio recordings at once.
  - ❤️ **Favorite:** Star important evidence for quick access.
  - 🔒 **Move to Vault:** Instantly transfer selected clips into private storage.
  - 🗑️ **Delete:** Batch remove files with confirmation.

---

### 7. 🧹 Automated Storage Maintenance & Auto-Cleanup
- **Storage Metrics:** Live tracking of internal storage, SD card capacity, and app footage size.
- **Automatic Cleanup Worker:** Background worker automatically removes recordings older than 7, 14, 30, or 60 days to prevent storage exhaustion.
- **Low Storage Safeguard:** Automatically halts active recording if free device storage drops below 100MB to preserve device stability.
- **Settings Backup & Restore:** Export and import full app configurations using secure clipboard JSON tokens.

---

### 8. ℹ️ About & Distribution Tools
- **3-Dot Overflow Menu:** Tapping the 3 dots in the top header displays the **About** menu item.
- **Direct APK Downloader:** Saves `surveillance-amit-bharat.apk` into your device's public `Downloads` directory with an *Open Downloads* notification action.
- **APK Sharing:** Packages the branded APK and shares it directly via standard Android share targets.
- **Developer Support:** Directly compose feedback and bug reports to developer support (`rooyssofttech2020@gmail.com`).
