# Gemmy 📱✨

**Gemmy** is an AI-powered smart memory and reminder assistant built for Android with Jetpack Compose, Room database, Material Design 3, and Google Gemini integration.

---

## 🚀 Features

- 🎙️ **Voice & Audio Transcription**: Capture notes, thoughts, and reminders on the fly with speech-to-text.
- 🧠 **Smart Learning Cards & Spaced Repetition**: Flashcards and scheduled reviews to retain knowledge.
- ⏰ **Reliable Alarms & Scheduled Notifications**: High-priority exact alarms and reboot-resilient reminders.
- 📊 **Review Analytics & History**: Track learning progress and review logs.
- 🧩 **Home Screen Widget**: Quick glance at pending reminders and daily items.
- 🎨 **Modern Material 3 UI**: Dynamic color support, smooth animations, and edge-to-edge design.

---

## 📦 Building Locally

### Prerequisites
- JDK 21 (Temurin or OpenJDK recommended)
- Android SDK (API 36)

### Build Commands

```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease

# Run Unit & Robolectric Tests
./gradlew testDebugUnitTest
```

The compiled APKs will be located in:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

---

## 🤖 GitHub Actions CI/CD Workflows

This repository is pre-configured with two automated GitHub Actions workflows:

### 1. **Build and Release APK (`.github/workflows/release.yml`)**
- **Triggers**:
  - Pushing any version tag (e.g., `git tag v1.0.0 && git push origin v1.0.0`)
  - Manual trigger via the GitHub Actions **Run workflow** button (`workflow_dispatch`)
  - Push to `main` / `master` (builds APKs and saves them as download artifacts)
- **What it does**:
  1. Sets up JDK 21 and Gradle 9.3.1.
  2. Injects `.env` configuration (supports `GEMINI_API_KEY` secret).
  3. Configures release signing (automatically generates a signing key or uses your custom keystore).
  4. Runs all unit tests (`testDebugUnitTest`).
  5. Compiles both **Release APK** and **Debug APK**.
  6. Uploads the APKs as workflow artifacts (available for download directly in the Actions tab).
  7. On version tags or when requested, **automatically creates a GitHub Release** and attaches both APKs (`Gemmy-v1.0.0-release.apk` and `Gemmy-v1.0.0-debug.apk`) with auto-generated release notes!

### 2. **Pull Request CI (`.github/workflows/ci.yml`)**
- Validates every pull request by executing tests and building the debug APK to ensure no broken code enters the main branch.

---

## 🏷️ How to Create a Release on GitHub

### Method A: Using Git Tags (Recommended)

1. Ensure your latest changes are pushed to your main branch:
   ```bash
   git push origin main
   ```
2. Create and push a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
3. GitHub Actions will automatically start the build, run tests, create the **v1.0.0** Release on GitHub, and attach the release and debug APKs!

### Method B: Via GitHub Web Interface (Manual)

1. Go to your GitHub repository in your browser.
2. Click on the **Actions** tab.
3. In the left sidebar, click **Build and Release APK**.
4. Click **Run workflow**:
   - Enter your desired tag name (e.g. `v1.0.0`).
   - Enter an optional release title (e.g. `Gemmy v1.0.0 Initial Release`).
   - Click the green **Run workflow** button.
5. Once completed, your release with the attached APKs will be live under **Releases**!

---

## 🔐 GitHub Repository Secrets (Optional)

You can customize your build by adding Secrets in GitHub:
Navigate to **Settings** > **Secrets and variables** > **Actions** > **New repository secret**:

| Secret Name | Description | Default / Fallback |
|-------------|-------------|---------------------|
| `GEMINI_API_KEY` | Your Gemini API Key for AI features | Uses `.env.example` placeholder |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded production `.jks` keystore | Auto-generates a CI signing key |
| `KEYSTORE_PASSWORD` | Password for the release keystore | `android` (when auto-generated) |
| `KEY_PASSWORD` | Key password for the keystore alias | `android` (when auto-generated) |

> 💡 **Tip**: Even without configuring any secrets, the workflow works out-of-the-box! A valid release key is automatically generated during CI so your release APK is always properly signed.

---

## 📄 License
This project is open-source.
