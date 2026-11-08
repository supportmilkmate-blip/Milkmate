# Implementation Plan: Google Play Store Release Preparation for MilkMate Dairy OS

Prepare **MilkMate Dairy OS** for publishing on the Google Play Console by standardizing app identity, ensuring Google Play policy compliance, optimizing the build configuration for Android App Bundle (AAB) release, and providing the exact store listing checklist.

---

## 1. App Identity & Policy Alignment

### A. App Name & Platform Metadata Synchronization
- Update app title across the project from temporary `"Remix MilkMate"` to official **`"MilkMate Dairy OS"`**:
  - `app/src/main/res/values/strings.xml` (`app_name` -> `"MilkMate Dairy OS"`, length: 17 characters, well within Play Store 30-character limit, zero emojis/buzzwords).
  - `metadata.json` (`name` -> `"MilkMate Dairy OS"`).
  - `settings.gradle.kts` (`rootProject.name` -> `"MilkMate Dairy OS"`).

### B. Google Play Developer Program Policy Audit
- **Permissions Audit**: Verify `<uses-permission>` in `AndroidManifest.xml` complies with Google Play policy (least privilege principle):
  - Normal permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `BLUETOOTH`.
  - Scoped Bluetooth: `android.permission.BLUETOOTH_SCAN` correctly uses `neverForLocation` flag.
  - Runtime permissions properly requested in Compose context.
- **Privacy Policy**: Prepare a standard Play Store privacy policy link reference for user accounts, phone authentication, and local/cloud data storage.
- **Data Safety Section Prep**: Document data collected (Phone number, business name, milk collection and delivery records) and usage (app functionality, cloud sync).

---

## 2. Release Build & Android App Bundle (AAB) Generation

### A. Versioning & Build Configuration
- Verify `app/build.gradle.kts`:
  - `compileSdk`: 36
  - `targetSdk`: 36 (compliant with Google Play target API level requirements)
  - `versionCode`: 1
  - `versionName`: "1.0.0"
  - `applicationId`: `com.aistudio.milkmate.ewsofz` (unique)
- Ensure release signing configuration is properly wired to use environment keystore variables when exporting AAB / APK.

---

## 3. Step-by-Step Google Play Console Upload Roadmap

Provide the user with clear instructions for:
1. **How to export the signed AAB bundle from AI Studio**:
   - Using the AI Studio Settings menu -> Export APK / AAB.
2. **Setting up the Google Play Console**:
   - Creating the app under "MilkMate Dairy OS" (Category: Business / Productivity).
   - Setting content rating (Everyone / 3+).
   - Completing the Data Safety form.
   - Uploading the `.aab` file to Closed Testing / Production track.

---

## Verification Plan

### Automated Verification
- Run `compile_applet` to ensure name updates and manifest sync compile cleanly.
