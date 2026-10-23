# CineFlow Tube — Android Build, Release & Delivery Protocol

This rule governs the automated build, release, and packaging workflow for CineFlow Tube.
The agent must adhere to this protocol whenever changes are ready for compilation, testing, or delivery.

---

## 1. Automated Build Pipeline (GitHub Actions CI)
- Do not attempt to compile locally if Android SDK / JDK are absent.
- Always push commits to the remote repository on GitHub (`origin main`):
  `https://github.com/omkaarpatil787-dev/CineFlow-Tube.git`
- Automated CI pipeline `.github/workflows/build.yml` runs on `ubuntu-latest` with Temurin JDK 17, Android 36 platform, and build-tools 35.0.0.
- The pipeline executes unit tests (`./gradlew test`), performs R8/ProGuard optimizations, assembles both Release (`assembleRelease`) and Debug (`assembleDebug`) APKs, and publishes a GitHub Release tagged `v1.0.0`.

---

## 2. APK Download & Desktop Delivery
Every time a build is triggered:
1. Query and monitor the GitHub Actions run status using GitHub REST API until `conclusion: success`.
2. Fetch the latest release asset `CineFlow-Tube-v1.0.0.apk` from GitHub Releases.
3. Save the installable APK directly to the user's Desktop:
   `C:\Users\onkar\OneDrive\Desktop\CineFlow-Tube-v1.0.0.apk`
4. Also keep a copy in the repository under:
   `C:\Users\onkar\OneDrive\Documents\GitHub\CineFlow-Tube\release_output\CineFlow-Tube-v1.0.0.apk`

---

## 3. APK Extraction (Mandatory Step)
Every time an APK is delivered, immediately extract its full contents into:
`C:\Users\onkar\OneDrive\Desktop\CineFlow-Tube-v1.0.0-extracted`
- Ensure safe extraction handling (e.g. case-collision handling on Windows NTFS for `res/` entries).
- Verify the presence of `AndroidManifest.xml`, `classes.dex`, `resources.arsc`, `res/`, `lib/`, and `assets/`.

---

## 4. Clean Source Archive
- Maintain a clean snapshot archive exported via `git archive` to:
  `C:\Users\onkar\OneDrive\Desktop\CineFlow_Tube_Source.zip`
