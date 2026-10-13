# GitHub Release Setup & Automated Remote Updation Guide for Gemmy

This guide walks you through setting up your GitHub repository, running GitHub Actions to automatically build APKs, and remotely delivering new updates to your users' mobile phones.

---

## 1. Pushing the Project to GitHub

If you haven't yet pushed this repository to GitHub:

```bash
# Initialize git if not already initialized
git init

# Add remote repository
git remote add origin https://github.com/<your-username>/<your-repo-name>.git

# Stage all files (Gradle wrapper, workflows, sources, keystore)
git add .

# Commit
git commit -m "Gemmy release with automated in-app updating and modern alarms"

# Push to main branch
git branch -M main
git push -u origin main
```

---

## 2. GitHub Workflow Permissions Check

To allow GitHub Actions to create GitHub Releases and upload APK assets:

1. Open your repository on GitHub.
2. Go to **Settings** > **Actions** > **General**.
3. Scroll down to **Workflow permissions**.
4. Select **Read and write permissions**.
5. Click **Save**.

---

## 3. Creating a Release & Triggering Automated Updation

### Option 1: Automatic Release with Git Tag (Recommended)

Whenever you add new features or bug fixes and want to push an update to your users:

```bash
# Bump version or tag with version number (e.g. v1.1.0)
git tag v1.1.0

# Push tag to GitHub
git push origin v1.1.0
```

GitHub Actions will automatically:
1. Detect the `v*` tag.
2. Compile and sign `Gemmy-v1.1.0-release.apk` and `Gemmy-release.apk`.
3. Create a GitHub Release titled **Gemmy v1.1.0**.
4. Attach the APK files to the release.
5. Generate release notes from git commits.

### Option 2: Manual Release via GitHub Actions UI

1. Open the repository on GitHub.
2. Click the **Actions** tab at the top.
3. Select **Build & Release Android APK** from the left list.
4. Click **Run workflow** (top right):
   - **Publish as a GitHub Release**: checked (`true`).
   - **Custom tag name**: e.g., `v1.1.0`.
   - **Release title**: e.g., `Gemmy v1.1.0 Release`.
5. Click **Run workflow**.

---

## 4. How the Remote Auto-Updation System Works

Gemmy includes a built-in **GitHub Releases In-App Auto-Updater**:

1. **GitHub Releases Monitoring**:
   - The app monitors `https://api.github.com/repos/<owner>/<repo>/releases/latest`.
   - You can configure your repository name (e.g. `your-username/your-repo`) in **Settings -> App Updates & Remote Updation**.
2. **Automatic Detection**:
   - On app launch (or by tapping "Check Updates" in Settings), Gemmy checks if a newer version is published on GitHub.
3. **One-Tap User Experience**:
   - When a new APK is detected, Gemmy presents a friendly update prompt:
     *"New Version Available: v1.1.0! [Download & Install] [Later]"*
   - When the user taps **Download & Install**, Gemmy downloads the APK with a live progress bar and automatically triggers the Android Package Installer.
   - The user taps **Update**, and the new version is installed instantly on their phone without needing Google Play Store!

---

## 5. Setting up a Production Signing Key (Optional)

A release upload key (`my-upload-key.jks`) is already included in the root directory and configured in GitHub Actions with password `android`.

If you prefer to use your own custom keystore secret:

1. Convert your keystore file to Base64:
   ```bash
   base64 -w 0 my-release-key.jks > keystore_base64.txt
   ```
2. In your GitHub repository, go to **Settings** > **Secrets and variables** > **Actions**.
3. Click **New repository secret** and add:
   - `RELEASE_KEYSTORE_BASE64`: Content of `keystore_base64.txt`
   - `KEYSTORE_PASSWORD`: Password of your keystore
   - `KEY_PASSWORD`: Password of your key alias
   - `GEMINI_API_KEY`: (Optional) Your Google Gemini API key

---

## 6. Downloading APKs Directly

- **GitHub Releases Page**: Direct public download links for `Gemmy-release.apk` and `Gemmy-<version>-release.apk`.
- **Actions Artifacts**: For every commit on `main` or `master`, the APK files are saved as downloadable artifacts under the workflow run summary for 30 days.
