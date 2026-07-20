# Run — PROD (Cloud)

Release the mobile app to the **App Store** and **Google Play** for real users.

**Who is this for?** Team leads or release managers. Requires Apple Developer and Google Play accounts.

**Critical rule:** Only submit after DEV testing is complete and backend PROD is deployed.

---

## Before you start

### What you will have at the end

The app available on the App Store (iOS) and/or Google Play (Android), connected to the production backend.

### The release flow (read this)

```text
1. DEV mobile build tested     ✓  login, home, chat work on DEV
2. Backend PROD deployed       ✓  see backend run-prod guide
3. Submit same build to stores →  you are here
4. Verify on TestFlight/Play   ✓  final smoke test
```

**Do not create a new untested build for PROD** if the DEV-tested build still matches the current API.

### What "PROD mobile" means

| Setting | Value |
|---------|-------|
| `APP_ENV` | `production` |
| API | PROD Cloud Run URL |
| Firebase | `starter-prod` project |
| Distribution | App Store / Google Play |
| EAS profile | `production` |

---

## Pre-submit checklist

Complete every item:

- [ ] DEV preview build tested and **EAS build ID recorded**
- [ ] Backend PROD deployed and health check passes
- [ ] EAS `production` env vars set (PROD API URL + PROD Firebase keys)
- [ ] API contract unchanged since DEV test (or new DEV build created and re-tested)
- [ ] Apple Developer account active
- [ ] Google Play Console app created
- [ ] `eas.json` updated with your App Store Connect app ID (`ascAppId`)
- [ ] Team notified of upcoming release

---

## Step 1 — Confirm backend PROD is live

```bash
curl https://starter-api-prod-XXXX.europe-west2.run.app/actuator/health
```

**Expected:** `{"status":"UP"}`

If not, deploy backend first: [../starter-backend/run/run-prod.md](../starter-backend/run/run-prod.md)

---

## Step 2 — One-time PROD setup (team lead)

Skip if already configured.

### 2a. Firebase PROD

1. Create/link `starter-prod` in [Firebase Console](https://console.firebase.google.com)
2. Enable Email/Password authentication
3. Register iOS and Android apps
4. Note the web config keys

### 2b. EAS production environment variables

```bash
eas env:create --name API_BASE_URL_PROD \
  --value "https://starter-api-prod-XXXX.europe-west2.run.app" \
  --environment production

eas env:create --name EXPO_PUBLIC_FIREBASE_API_KEY \
  --value "AIza..." \
  --environment production

eas env:create --name EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN \
  --value "starter-prod.firebaseapp.com" \
  --environment production

eas env:create --name EXPO_PUBLIC_FIREBASE_PROJECT_ID \
  --value "starter-prod" \
  --environment production
```

### 2c. App Store and Play Console

- **iOS:** Create app in [App Store Connect](https://appstoreconnect.apple.com), note the App ID
- **Android:** Create app in [Google Play Console](https://play.google.com/console)
- Update `eas.json`:

```json
"submit": {
  "production": {
    "ios": { "ascAppId": "YOUR_APP_STORE_CONNECT_APP_ID" },
    "android": { "track": "internal" }
  }
}
```

Start with `"track": "internal"` for staged rollout, then promote to production in Play Console.

---

## Step 3 — Decide: reuse DEV build or create new one

| Situation | Action |
|-----------|--------|
| DEV build tested, no native changes, API unchanged | **Reuse DEV build ID** — go to Step 4 |
| Native modules changed, Expo SDK upgraded, or API breaking change | **Create new build**, test on DEV first, then submit |

### Create a new production build (only if needed)

```bash
cd starter/starter-mobile
eas build --profile production --platform all
```

Note the new **build ID** from [expo.dev](https://expo.dev) → Builds.

---

## Step 4 — Submit to app stores

You need the **EAS build ID** from the DEV-tested build (looks like `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`).

### Option A: Command line

```bash
cd starter/starter-mobile

# iOS
eas submit --platform ios --id YOUR-BUILD-ID

# Android
eas submit --platform android --id YOUR-BUILD-ID

# Both
eas submit --platform all --id YOUR-BUILD-ID
```

### Option B: GitHub Actions (recommended for teams)

1. Open GitHub → **Actions** → **EAS Submit PROD**
2. Click **Run workflow**
3. Enter the **EAS build ID** from DEV testing
4. Approve the `production` environment gate if prompted
5. Wait for completion

---

## Step 5 — Verify after submission

### iOS (TestFlight)

1. Open [App Store Connect](https://appstoreconnect.apple.com) → TestFlight
2. Wait for build processing (can take 15–30 minutes)
3. Install via TestFlight on your phone
4. Sign in with a **PROD Firebase user** (not DEV)
5. Verify: login → home profile → chat → sign out

### Android (internal track)

1. Open [Google Play Console](https://play.google.com/console) → your app → Testing → Internal testing
2. Install from the opt-in link
3. Same verification as iOS

**You are done when** the app works end-to-end against PROD backend with a PROD user.

---

## Record the release

```text
Date:              2026-07-20
Commit SHA:        abc123def
EAS build ID:      xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
Platforms:         ios, android
Backend image:     abc123def (same commit)
Submitted by:      your-name
DEV tested:        yes — 2026-07-19
Store status:      TestFlight / Play internal
```

---

## Promote to public release

After TestFlight / internal testing passes:

- **iOS:** App Store Connect → submit for App Review
- **Android:** Play Console → promote internal track → production → start rollout

These steps are done in Apple/Google consoles, not via EAS.

---

## Rollback

### Stop a bad release

- **iOS:** App Store Connect → remove version from sale, or submit expedited fix
- **Android:** Play Console → halt rollout

### JavaScript-only rollback (OTA)

If the issue is JS-only and you use EAS Update:

```bash
eas update:rollback
```

Native issues always require a new build and store submission.

---

## Troubleshooting

| Problem | What to do |
|---------|------------|
| Submit fails — missing credentials | Run `eas credentials` or configure in expo.dev |
| App works on DEV but not PROD | Check EAS production env vars point to PROD API and `starter-prod` Firebase |
| 401 for all PROD users | Firebase project mismatch — PROD app must use `starter-prod` |
| Apple review rejection | Check privacy policy, login flow, and crash-free TestFlight build |
| Wrong build submitted | Halt rollout; submit correct build ID |

---

## What comes next?

| Task | Guide |
|------|-------|
| Set up a new product from scratch | [../../docs/NEW_APP_WORKFLOW.md](../../docs/NEW_APP_WORKFLOW.md) |
| Backend PROD deploy | [../starter-backend/run/run-prod.md](../starter-backend/run/run-prod.md) |
| All environment variables | [../../docs/ENVIRONMENT_MATRIX.md](../../docs/ENVIRONMENT_MATRIX.md) |
