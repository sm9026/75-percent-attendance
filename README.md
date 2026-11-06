# 75% (Attendance Radar) — Native Android Application

**75%** is an offline-first, strictly local native Android application for tracking subject-wise period attendance across semesters, calculating exact safe-absence buffers and recovery requirements, simulating hypothetical What-If scenarios, inspecting calendar and trend history, and exporting/restoring user-controlled backups.

---

## 1. Requirements & Toolchain

- **JDK:** Java 17 (required by Android Gradle Plugin `8.5.2`).
- **Android SDK:**
  - `minSdk = 29` (Android 10)
  - `compileSdk = 34` / `targetSdk = 34` (Android 14)
  - Android SDK Build-Tools `34.0.0` and Android SDK Platform `34` installed via Android Studio SDK Manager.
- **Gradle:** Uses the included Gradle Wrapper (`8.7`). You do **not** need a globally installed Gradle distribution.

---

## 2. Windows PowerShell Build, Test & Install Instructions

Open PowerShell in the extracted `AttendanceRadar` directory and run:

```powershell
# 1. Build the debug APK
.\gradlew.bat :app:assembleDebug

# 2. Run local JVM unit tests (Domain calculator, aggregation, backup validation, CSV escaping)
.\gradlew.bat :app:testDebugUnitTest

# 3. Compile Android instrumentation tests (Room & Compose UI smoke tests)
.\gradlew.bat :app:assembleDebugAndroidTest

# 4. List connected Android devices / emulators (using ANDROID_HOME)
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices

# (If your environment uses ANDROID_SDK_ROOT instead of ANDROID_HOME:)
& "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe" devices

# 5. Install the debug APK on a connected Android device
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Expected debug APK output path after `:app:assembleDebug`:
```text
app\build\outputs\apk\debug\app-debug.apk
```

---

## 3. Architecture Overview

- **Presentation Layer (`feature/*`, `core/design`, `core/navigation`):**
  - Built 100% with **Jetpack Compose** and **Material 3**.
  - ViewModels expose immutable `StateFlow<UiState>` consumed via `collectAsStateWithLifecycle()`.
  - Five top-level destinations (`Home`, `Subjects`, `Calendar`, `Insights`, `Settings`) plus nested destinations for Onboarding, Add/Edit Subject, Subject Detail, Mark Attendance, Calculator & What-If, Semester Management, and Backup/Restore/CSV Export.
- **Domain Layer (`domain/model`, `domain/calculator`, `domain/repository`, `domain/usecase`):**
  - Pure Kotlin `AttendanceCalculator` with zero dependencies on Room, Compose, or Android Context.
  - Uses integer period counts, integer basis points (`1..10000`, where `7500 = 75.00%`), and `BigInteger` / checked 64-bit `Long` arithmetic (`Math.addExact`) so large histories never overflow.
- **Data Layer (`data/local`, `data/repository`, `data/preferences`, `data/backup`):**
  - **Room (SQLite)** is the single source of truth for `semesters`, `subjects`, and `attendance_records`.
  - **DataStore Preferences** stores UI-only preferences (`themeMode`, `displayPrecision`, `weekStartsOn`, `defaultNewSemesterTargetBp`, `onboardingCompleted`).
  - **Storage Access Framework (`ActivityResultContracts.CreateDocument` / `OpenDocument`)** powers user-controlled JSON backup, validated atomic restore, and RFC-4180 CSV export.

---

## 4. Local-Only Privacy & Backup Exclusions

- **No Network Access:** `AndroidManifest.xml` does **not** declare `android.permission.INTERNET` or any location, camera, microphone, contacts, Bluetooth, or broad storage permissions.
- **No Telemetry or Third-Party Cloud SDKs:** Zero analytics, crash-reporting, advertising, Firebase, or HTTP client dependencies.
- **Automatic Cloud / Device-Transfer Backup Disabled:**
  - `android:allowBackup="false"` is set on `<application>`.
  - `res/xml/backup_rules.xml` (`android:fullBackupContent`) and `res/xml/data_extraction_rules.xml` (`android:dataExtractionRules` for Android 12+) explicitly exclude `database`, `sharedpref`, `file`, and `root` domains from automatic cloud backup and device-to-device transfer.
  - *Platform note:* On rooted devices or via `adb backup` on debuggable builds, OS-level debugging tools may still inspect app-private storage; for standard non-rooted consumer devices, data leaves the app only when the user explicitly exports a JSON backup or CSV file through the system document picker.
