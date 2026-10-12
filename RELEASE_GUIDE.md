# GitHub Release Setup & Guide for Gemmy

This guide walks you through setting up your GitHub repository, running GitHub Actions, and releasing APKs to your users.

---

## 1. Pushing the Project to GitHub

If you haven't yet pushed this repository to GitHub:

```bash
# Initialize git if not already initialized
git init

# Add remote repository
git remote add origin https://github.com/<your-username>/<your-repo-name>.git

# Stage all files (Gradle wrapper, workflows, sources)
git add .

# Initial commit
git commit -m "Initial commit with GitHub Actions release workflow"

# Rename branch to main and push
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

*(Note: The workflow file `.github/workflows/release.yml` already includes `permissions: contents: write`, but enabling this in repo settings ensures full permissions are granted).*

---

## 3. Creating a Release

### Option 1: Automatic Release with Git Tag (Standard)

Whenever you are ready to publish a new version:

```bash
# Create a tag with your version number
git tag v1.0.0

# Push the tag to GitHub
git push origin v1.0.0
```

GitHub Actions will automatically:
1. Detect the `v*` tag.
2. Compile and sign `Gemmy-v1.0.0-release.apk` and `Gemmy-v1.0.0-debug.apk`.
3. Create a GitHub Release titled **Gemmy v1.0.0**.
4. Attach both APK files to the release assets.
5. Generate release notes from git commits.

### Option 2: Manual Release via GitHub Actions UI

1. Open the repository on GitHub.
2. Click the **Actions** tab at the top.
3. Select **Build and Release APK** from the left list.
4. Click **Run workflow** (top right):
   - **Publish as a GitHub Release**: checked (`true`).
   - **Custom tag name**: e.g., `v1.0.0`.
   - **Release title**: e.g., `Gemmy v1.0.0 Release`.
5. Click the green **Run workflow** button.

---

## 4. Setting up a Production Signing Key (Optional)

By default, the workflow will generate a signing key during the CI run so the Release APK is always signed and ready to install.

If you have a dedicated release keystore (`.jks` or `.keystore`) and want to use it for official updates:

1. Convert your keystore file to Base64:
   ```bash
   # On macOS / Linux:
   base64 -i my-release-key.jks | tr -d '\n' > keystore_base64.txt
   
   # Or on Linux:
   base64 -w 0 my-release-key.jks > keystore_base64.txt
   ```
2. In your GitHub repository, go to **Settings** > **Secrets and variables** > **Actions**.
3. Click **New repository secret** and add:
   - `RELEASE_KEYSTORE_BASE64`: Paste the content of `keystore_base64.txt`
   - `KEYSTORE_PASSWORD`: Password of your keystore
   - `KEY_PASSWORD`: Password of your key alias
   - `GEMINI_API_KEY`: (Optional) Your Google Gemini API key

---

## 5. Downloading APKs

- **GitHub Releases Page**: Direct public download links for `Gemmy-<version>-release.apk` and `Gemmy-<version>-debug.apk`.
- **Actions Artifacts**: For every commit on `main` or `master`, the APK files are saved as a downloadable zip artifact under the workflow run summary for 30 days.
