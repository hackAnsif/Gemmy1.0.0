# GitHub Release Setup Guide for Gemmy

This guide explains how to build and publish signed release and debug APKs for **Gemmy – Smart Reminder** using GitHub Actions.

---

## 1. Pushing the Project to GitHub

```bash
git add .
git commit -m "Update Gemmy – Smart Reminder"
git push -u origin main
```

---

## 2. GitHub Workflow Permissions Check

To allow GitHub Actions to create GitHub Releases and attach APK assets:

1. Open your repository on GitHub.
2. Go to **Settings** > **Actions** > **General**.
3. Scroll down to **Workflow permissions**.
4. Select **Read and write permissions**.
5. Click **Save**.

---

## 3. Creating a Release

### Option 1: Automatic Release with Git Tag (Recommended)

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will automatically:
1. Run `:app:testDebugUnitTest`.
2. Build and sign `Gemmy-v1.0.0-release.apk` and `Gemmy-v1.0.0-debug.apk`.
3. Create a GitHub Release titled **Gemmy v1.0.0** with the APKs attached.

### Option 2: Manual Release via GitHub Actions UI

1. Open the repository on GitHub.
2. Click the **Actions** tab.
3. Select **Build and Release APK**.
4. Click **Run workflow**:
   - **Publish as a GitHub Release**: checked (`true`).
   - **Custom tag name**: e.g. `v1.0.0`.
   - **Release title**: e.g. `Gemmy v1.0.0`.
5. Click **Run workflow**.

---

## 4. Custom Production Signing Key (Optional)

If you do not provide custom signing secrets, the workflow automatically generates a keystore during CI using default credentials. To sign releases with your own keystore:

1. Convert your `.jks` keystore to Base64:
   ```bash
   base64 -w 0 my-release-key.jks > keystore_base64.txt
   ```
2. In your GitHub repository, go to **Settings** > **Secrets and variables** > **Actions** and add:
   - `RELEASE_KEYSTORE_BASE64`: Base64 string of your `.jks` file
   - `KEYSTORE_PASSWORD`: Keystore password
   - `KEY_PASSWORD`: Password for key alias `upload`
