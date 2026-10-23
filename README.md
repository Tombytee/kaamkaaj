# KaamKaaj — Your Personal Management Vault

**Your documents. Your deadlines. Your everyday life, organized.**

KaamKaaj is an offline-first Android application designed to help you organize important documents, track bills and payments, manage reminders, keep warranty records, and store everyday information in one convenient place.

With a clean interface, local data storage, useful notifications, and built-in document scanning, KaamKaaj helps simplify everyday personal management.

## ✨ Features

### 📄 Document Vault
- Store and organize important document records.
- Track document expiry dates.
- Set reminders for upcoming expirations.
- Manage document attachments.

### 💳 Bills & Payment Tracking
- Keep track of bills, payments, and due dates.
- Organize recurring payments.
- Attach relevant files to payment records.
- Stay informed about upcoming payment deadlines.

### 🔔 Smart Reminders
- Create reminders for important tasks and events.
- Configure recurring reminders.
- Snooze reminders when needed.
- Receive notifications for scheduled tasks.

### 🛡️ Warranty Management
- Keep product warranty information organized.
- Track warranty expiry dates.
- Maintain important purchase and warranty records.

### 🔎 Unified Search
- Search across supported records from one place.
- Find documents, payments, warranties, and reminders more easily.

### 📷 Built-in Document Scanner
- Scan documents using your device camera.
- Detect document edges and correct perspective.
- Create clearer digital copies of physical documents.

### 🔐 Privacy & App Lock
- Designed for local, personal record management.
- Supports app locking using available device authentication methods.
- No account registration is required for the core experience.

### 💾 Backup & Restore
- Export supported records into a JSON backup.
- Restore records from a compatible backup.
- Keep a backup of important information for recovery.

**Note:** Backup compatibility and attachment restoration may depend on the backup contents. Exported record metadata does not necessarily include the original image or PDF files.

### 🎨 Clean User Interface
- Modern Android interface built with Jetpack Compose and Material 3.
- Light and dark theme support.
- Simple navigation for everyday use.

### 🔄 In-App Updates
- Check for available app updates.
- Download releases using the configured update source.
- Install updates through the Android installation flow.

This repository hosts the update manifest and APK releases used by the app's in-app update mechanism.

## 📱 App Information

| Detail | Information |
|---|---|
| App Name | KaamKaaj |
| Package ID | `com.aistudio.kaamkaaj.kzrq` |
| Current Reported Version | 1.2.4 |
| Reported Version Code | 7 |
| Minimum Android Version | Android 7.0 (API 24) |
| Platform | Android |
| Technology | Kotlin, Jetpack Compose, Material 3 |
| Local Database | Room |

*Version information should be updated whenever a new release is published.*

## 🔒 Data & Privacy

KaamKaaj is designed around local storage for its core personal-management features. Internet access is used for checking and downloading updates through the configured update source.

- No account is required for the core app experience.
- Personal records are intended to be stored locally on the device.
- Users should create backups of important information.
- Protect your device and backup files to help keep personal information secure.

Please review the app's actual permissions and privacy practices before relying on it for sensitive information.

## 📦 Releases & Updates

This repository provides the release files used by KaamKaaj's in-app update system.

Depending on the release configuration, it may contain:

- `update.json` — update information and release metadata.
- `app-release.apk` — Android application package.

The app uses its configured update manifest to check for newer releases.

## 🚀 Getting Started

1. Download the APK from the available release files.
2. Open the APK on your Android device.
3. If prompted, allow installation from the source you used to download the APK.
4. Complete the installation and open KaamKaaj.

Only install APK files from sources you trust.

## 🎯 Our Goal

KaamKaaj aims to make everyday personal organization simpler by bringing documents, bills, payments, warranties, and reminders together in one convenient Android application.

**One app. Less confusion. Better organization.**

---

*KaamKaaj — Keep your everyday life organized.*
