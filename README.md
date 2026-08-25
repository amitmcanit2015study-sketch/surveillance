# Gallery

**Developed by:** Amit Bharat  
**Company:** Rooys Soft Tech  
**Email:** rooyssofttech2020@gmail.com  
**Language:** Java  
**Build System:** Gradle (Groovy DSL)  
**Architecture:** MVVM (Model-View-ViewModel)  
**UI Framework:** Material Design 3 (M3)  
**Minimum SDK:** API 24+ (Android 7.0)  
**Target SDK:** API 34 (Android 14)  

---

## 📸 Overview

**Gallery** is a free, ad-free gallery and file manager designed for a fast, simple, and seamless experience. Easily organize, browse, and manage your photos, security footage, and files with a clean interface, smooth performance, and privacy at its core.

Built with hardware-accelerated CameraX video capture, dynamic Material 3 gallery views (Grid & List), rich EXIF/codec metadata inspection, embedded Media3 ExoPlayer engine with Picture-in-Picture (PiP), and full APK & description sharing.

---

## ✨ Features

### 1. 🛡️ User Privacy & Compliance
- **Explicit Recording Only:** Recording initiates only when the user explicitly triggers it via the recording shutter.
- **Visual Recording HUD:** Active recording duration timer, pulsing red dot indicator, and status alerts.
- **Android Privacy Compliant:** Adheres to Android background camera and microphone restrictions.

### 2. 📹 High-Performance CameraX Recording Engine
- **Lock-Screen Compatible Access:** Allows users to immediately capture security footage even when the device is locked (`showWhenLocked`, `turnScreenOn`, and keyguard protection).
- **Stealth / Screen Saver Mode (OLED Black Mode):** Dims preview to pure black with minimal battery consumption for discrete recording (Double-tap to wake).
- **Pinch-to-Zoom & Double-Tap Zoom:** Gesture-based smooth digital zoom up to 5x with live zoom level HUD badge.
- **Tap-to-Focus & Metering:** Interactive focus ring animation with CameraX `FocusMeteringAction`.
- **Hardware Volume Shutter:** Trigger start/stop recording with physical Volume Up / Down buttons.
- **Rule-of-Thirds Grid Overlay:** 3x3 framing grid for precise security positioning.
- **Front/Rear Camera Switching:** Seamless toggle between lenses.
- **Torch / Flash Control:** Hardware flash support in low-light environments.
- **Quality Selectors:** Supports **480p (SD)**, **720p (HD)**, **1080p (Full HD)**, and **4K (UHD)** with automatic fallback.
- **Audio Control:** Optional microphone capture with instant toggle.
- **GPS Location Tagging:** Optional embedding of precise latitude/longitude coordinates into video metadata.
- **Hardware Stabilization:** Video EIS (Electronic Image Stabilization) where supported.
- **Live Battery & Storage Monitoring:** Automatic safety shutdown when storage drops below 100MB to preserve system integrity.

### 3. 🖼️ Video Gallery & Archive
- **Standardized Naming Convention:** `YYYY-MM-DD_HH-MM-SS.mp4` (e.g. `2026-08-23_14-30-45.mp4`).
- **Dynamic Layout Toggle:** Instant switch between **2-Column Grid** and **List View** with persisted preference.
- **Fast Scrolling & Caching:** Powered by Glide thumbnail loading and Room DB metadata indexing.
- **Search & Filters:**
  - Real-time search by filename or date.
  - Quick filter chips: **All**, **Today**, **This Week**, **This Month**, and **Favorites**.
- **Favorite Marking:** Quick star toggle directly from video cards or player.

### 4. 🎬 Built-in Media3 ExoPlayer
- **Seamless Playback:** Play, pause, seekbar scrubbing, and auto-hiding controls.
- **Playback Speed Control:** 0.5x, 1.0x (Normal), 1.25x, 1.5x, 2.0x.
- **Picture-in-Picture (PiP):** Multitask while surveillance footage continues playing in a floating window.
- **EXIF & Metadata Sheet:** Detailed inspection of Resolution, Codec (H.264/AAC), Duration, File Size, Timestamp, GPS Coordinates, and Absolute File Path.
- **File Actions:** Quick Rename, Delete with confirmation, and Share via Android FileProvider.

### 5. 🔐 Security & Access Control
- **Biometric & PIN Lock:** Protect surveillance archives with Fingerprint, Face recognition, or a 4-Digit Security PIN.
- **App Startup Authentication:** Restricts unauthorized access when opening or returning to the app.

### 6. 💾 Storage Management & Maintenance
- **Visual Breakdown:** Internal storage used/free breakdown, external SD card capacity, and total footage footprint.
- **Auto-Cleanup Worker:** Periodic background WorkManager task to automatically purge non-favorite footage older than X days (7, 14, 30, 60 days).
- **Settings Backup & Restore:** Export configuration preferences to clipboard/JSON and restore on any device.

---

## 🏗️ Architecture & Project Structure

The project follows clean **MVVM** architecture in Java:

```
app/src/main/
├── AndroidManifest.xml
├── java/com/securityrecorder/app/
│   ├── SecurityRecorderApp.java          // Application class, channels, theme setup
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.java          // Room DB singleton
│   │   │   ├── VideoRecordDao.java       // Room DAO queries
│   │   │   └── VideoRecordEntity.java    // Video Room Entity
│   │   ├── model/
│   │   │   ├── VideoItem.java            // Presentation model
│   │   │   ├── StorageInfo.java          // Storage capacity model
│   │   │   └── FilterType.java           // Filter categories enum
│   │   ├── preferences/
│   │   │   └── AppPreferences.java       // SharedPreferences & JSON backup/restore
│   │   └── repository/
│   │       └── VideoRepository.java      // Mediates Room DB and Filesystem
│   ├── ui/
│   │   ├── auth/                         // PIN & Biometric lock
│   │   │   ├── AuthActivity.java
│   │   │   └── BiometricHelper.java
│   │   ├── main/                         // Gallery & Archive
│   │   │   ├── MainActivity.java
│   │   │   ├── MainViewModel.java
│   │   │   ├── VideoAdapter.java
│   │   │   └── VideoViewHolder.java
│   │   ├── recorder/                     // CameraX Video Capture
│   │   │   ├── CameraRecorderActivity.java
│   │   │   ├── CameraRecorderViewModel.java
│   │   │   └── CameraHelper.java
│   │   ├── player/                       // Media3 ExoPlayer & Metadata
│   │   │   ├── VideoPlayerActivity.java
│   │   │   └── MetadataBottomSheetDialog.java
│   │   ├── settings/                     // Preferences & Configurations
│   │   │   ├── SettingsActivity.java
│   │   │   └── SettingsViewModel.java
│   │   ├── storage/                      // Storage Breakdown & Cleanup
│   │   │   └── StorageManagerActivity.java
│   │   └── common/                       // Dialogs
│   │       ├── ConfirmationDialog.java
│   │       └── RenameDialog.java
│   ├── utils/
│   │   ├── DateTimeUtils.java
│   │   ├── FileUtils.java
│   │   ├── HapticUtils.java
│   │   ├── LocationHelper.java
│   │   └── NotificationHelper.java
│   └── workers/
│       └── CleanupWorker.java            // WorkManager periodic cleaner
└── res/
    ├── drawable/                         // Adaptive vectors, buttons, HUD pills
    ├── layout/                           // Material 3 XML layouts
    ├── values/                           // Colors, strings, themes, dimens
    ├── values-night/                     // Material 3 Dark theme
    └── xml/                              // FileProvider & backup rules
```

---

## 🛠️ Build & Installation Instructions

### Prerequisites
1. **Android Studio** (Hedgehog 2023.1.1 or newer recommended)
2. **JDK 17** (Configured as Gradle JDK in Android Studio)
3. **Android SDK 34** (Build Tools 34.0.0)

### Steps to Open & Run in Android Studio
1. Open Android Studio.
2. Select **Open** and choose the project directory `d:\_test\_-_survilliance`.
3. Allow Gradle to sync dependencies automatically.
4. Connect a physical Android device (API 24+) or launch an Android Virtual Device (AVD).
5. Click **Run 'app'** (`Shift + F10`) to build and launch **Gallery Security Recorder**.

### Building via Gradle CLI
```bash
# Debug APK build
./gradlew assembleDebug

# Release APK build
./gradlew assembleRelease
```
The generated APK will be available in `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📜 License & Credits
- **Developed by:** Amit Bharat
- **Designed for:** Personal Security & Local Video Surveillance
