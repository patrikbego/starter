# Run — DEV (Cloud)

Install a **test version** of the mobile app on your phone that connects to the team's shared DEV backend in the cloud.

**Who is this for?** QA testers, developers verifying a release, or anyone who needs a real installable app (not Expo Go).

**Prerequisites:** DEV backend must be deployed and healthy. See [../starter-backend/run/run-dev.md](../starter-backend/run/run-dev.md).

---

## Before you start

### What you will have at the end

An installable app on your iPhone or Android phone that talks to the real DEV API in Google Cloud.

### What "DEV mobile build" means

| Setting | Value |
|---------|-------|
| Build type | EAS internal distribution (not App Store) |
| API | DEV Cloud Run URL |
| Firebase | `starter-dev` project |
| EAS profile | `preview` |
| Who can install | Team members with the install link |

### What you need

| Requirement | Who provides it |
|-------------|-----------------|
| DEV API URL | Team lead |
| Firebase `starter-dev` test account | Team lead or create in Firebase Console |
| Expo account | You — sign up at [expo.dev](https://expo.dev) |
| EAS CLI | You — install below |

---

## Step 1 — Confirm the DEV backend is healthy

Before building the app, verify the API works:

```bash
curl https://starter-api-dev-XXXX.europe-west2.run.app/actuator/health
```

**Expected:** `{"status":"UP"}`

If this fails, stop here and follow [../starter-backend/run/run-dev.md](../starter-backend/run/run-dev.md).

---

## Step 2 — One-time setup (team lead or first developer)

Skip this if EAS is already configured for the project.

### 2a. Install EAS CLI and log in

```bash
npm install -g eas-cli
eas login
```

### 2b. Link the project to Expo

```bash
cd starter/starter-mobile
eas init
```

Follow the prompts. This registers the app on [expo.dev](https://expo.dev).

### 2c. Set EAS environment variables

Replace `XXXX` with your real DEV Cloud Run URL. Get Firebase keys from Firebase Console → `starter-dev` → Project settings.

```bash
eas env:create --name API_BASE_URL_DEV \
  --value "https://starter-api-dev-XXXX.europe-west2.run.app" \
  --environment preview

eas env:create --name EXPO_PUBLIC_FIREBASE_API_KEY \
  --value "AIza..." \
  --environment preview

eas env:create --name EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN \
  --value "starter-dev.firebaseapp.com" \
  --environment preview

eas env:create --name EXPO_PUBLIC_FIREBASE_PROJECT_ID \
  --value "starter-dev" \
  --environment preview
```

Verify:

```bash
eas env:list
```

### 2d. GitHub CI (team lead)

Add `EXPO_TOKEN` to GitHub repo secrets so merges to `main` auto-build. Get token from [expo.dev/settings/access-tokens](https://expo.dev/settings/access-tokens).

---

## Step 3 — Build the DEV app

### Option A: Wait for CI (easiest for testers)

When a developer merges to `main`, GitHub Actions automatically builds iOS and Android preview apps.

1. Open GitHub → **Actions** → **EAS Build DEV**
2. Wait for green checkmark
3. Open [expo.dev](https://expo.dev) → your project → **Builds**
4. Copy the install link for your platform

### Option B: Build manually (developers)

```bash
cd starter/starter-mobile

# iOS only
eas build --profile preview --platform ios

# Android only
eas build --profile preview --platform android

# Both platforms
eas build --profile preview --platform all
```

Build takes 10–20 minutes. When done, EAS shows an **install link** or QR code.

---

## Step 4 — Install on your device

### iOS

1. Open the install link from EAS on your iPhone
2. Follow prompts to install (may need to trust the developer in Settings → General → VPN & Device Management)
3. For TestFlight builds: install via TestFlight app

### Android

1. Open the install link on your Android phone
2. Allow installation from unknown sources if prompted
3. Install the APK

---

## Step 5 — Test the app

**You are done when** you complete this checklist:

- [ ] App installs and opens without crashing
- [ ] Login screen appears
- [ ] Sign in with a `starter-dev` Firebase user works
- [ ] **Home** tab shows your email and name
- [ ] Health badge is **green** ("Connected")
- [ ] **Chat** tab: send "hello" → receive AI reply
- [ ] Sign out returns to login screen

### Record your test (important for PROD release)

```text
Date:        2026-07-20
Commit SHA:  abc123
EAS build ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
Platform:    ios / android
Tester:      your-name
Result:      pass / fail
Notes:       ...
```

Save the **EAS build ID** — you need it for production submission.

---

## OTA updates (JavaScript-only fixes)

If a developer fixes a bug that only changes JavaScript (no new native packages):

```bash
eas update --branch preview --message "fix: description"
```

Users get the update without reinstalling.

**You need a new native build when:**
- Firebase SDK version changes
- New native modules are added
- Expo SDK is upgraded

---

## Troubleshooting

| Problem | What to do |
|---------|------------|
| Build fails immediately | Run `eas env:list` — all 4 variables must be set |
| 401 on every screen after login | Firebase project mismatch — must be `starter-dev` |
| Health badge red | Wrong `API_BASE_URL_DEV` or backend is down |
| Can't install on iOS | Trust developer certificate in Settings |
| CI build fails | Check `EXPO_TOKEN` in GitHub secrets |
| Chat returns error | Backend OpenRouter key issue — see backend run-dev guide |

---

## What comes next?

| When you are ready to… | Read |
|------------------------|------|
| Release to App Store / Play Store | [run-prod.md](./run-prod.md) |
| Develop on your Mac instead | [run-local.md](./run-local.md) |
| Deploy backend changes | [../starter-backend/run/run-dev.md](../starter-backend/run/run-dev.md) |
