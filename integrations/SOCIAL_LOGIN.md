# Social Login (Google / Apple) — Runbook

**Status: ✅ implemented — web scope (2026-09-05).** Code: Google + Apple web popup sign-in
implemented (see [Code checklist — web-first](#code-checklist--web-first-scope-google--apple-on-web-only)).
Console: providers + Apple credential are wired for the template's own DEV project, but every
product app redoes the console setup itself (see §2 — that is the per-app work, not a shared
resource).
Backend needed **zero changes** (see [The model](#the-model)). Native Google/Apple is the
follow-up (see [native checklist](#code-checklist--native-later-scope-a-full)) — remember
**Sign in with Apple is mandatory on iOS once Google ships on native** (Guideline 4.8).

## The model — why the backend is untouched

| Claim | Evidence |
|---|---|
| Backend auto-provisions any authenticated user from the ID token | [`MeController.java:27`](../starter-backend/src/main/java/com/starter/api/MeController.java) → `getOrCreateUser(uid, email, name)` ([`UserService.java:20`](../starter-backend/src/main/java/com/starter/application/UserService.java)) |
| Token shape is provider-agnostic | The Firebase Admin SDK verifies the same RS256 ID token for email/password, Google, and Apple |
| `email_verified` arrives `true` for social | Google/Apple verify the email at OAuth time → the AI-chat gate (`403 EMAIL_NOT_VERIFIED`) passes at first sign-in — **no verification email step** |
| The reCAPTCHA sign-up gate is bypassed by design | `POST /api/v1/auth/sign-up` is email/password-mediated; Google/Apple accounts are gated by the provider itself instead ([SIGNUP_ABUSE_GATE.md](./SIGNUP_ABUSE_GATE.md)) |

So the work is: Firebase console providers + Apple Developer credential + OAuth client IDs +
mobile code to call `signInWithCredential` with the provider's account. All of the mobile
machinery already exists (`getFirebaseAuthAdapter`, `onAuthStateChanged`, token plumbing in
[`HttpApiClient.ts`](../starter-mobile/src/adapters/HttpApiClient.ts)).

## The Apple mandate (read first)

App Store [Guideline 4.8](https://developer.apple.com/app-store/review/guidelines/#sign-in-with-apple):
if your iOS app offers **any** third-party login option, **Sign in with Apple is mandatory**.
Consequences for scope:

- **Google-web-only is fine today** (the hosted site is web, not an iOS app).
- The moment Google lands on a **native iOS** build, Apple must be in the same release.
- Recommended scope A = Google (web + native) + Apple (web + native iOS) in one go.

## Console checklist (per environment: DEV `starter-demo-dev`, PROD later)

> Same steps in each Firebase project, strictly separated values. The DEV project currently
> has only Email/Password enabled (per [FIREBASE_AUTH.md](./FIREBASE_AUTH.md) step 3).

### 1. Google provider — Firebase console

1. [console.firebase.google.com](https://console.firebase.google.com) → project `starter-demo-dev`
   → **Build → Authentication → Sign-in method → Google → Enable** → pick the support email.
2. Copy the **Web SDK configuration** values for the runbook (they enter the repo later):
   - **Web client ID** (`…apps.googleusercontent.com`) → used by the web popup flow.
   - **Android client ID** + **iOS client ID** (created in step 3/4 below) → used by native.
3. **Android SHA-1 fingerprints** (required for native Google):
   - Firebase console → ⚙️ **Project settings → Your apps → Android** → add the app
     (`com.starter.mobile.dev` for DEV) with the **debug + release SHA-1**.
   - Get them: `cd starter-mobile/android && ./gradlew signingReport` (debug) and from the
     release keystore (`keytool -list -v -keystore <release.keystore>`).
4. **iOS bundle id** (`com.starter.mobile.dev`) → **Project settings → Your apps → iOS** add
   if missing (needed for the iOS OAuth client; no `.plist` was added — the JS SDK
   credential flow doesn't need `googleServicesFile`).

### 2. Apple provider — Apple Developer + Firebase console

> The banner *"Apple sign-in requires additional configuration steps. Follow the steps for
> your platform."* means exactly this section is unfinished: Apple is toggled ON in Firebase,
> but the four Apple Developer values (Team ID, Services ID, Key ID, Private Key) are not
> entered yet. Until they are, the provider cannot mint accounts.

Prerequisites from [developer.apple.com](https://developer.apple.com/account/):
[Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources/identifiers):

1. **App ID** with `Sign in with Apple` capability, matching the bundle id (`com.starter.mobile.dev`).
   If the App ID already exists, edit it (→ **Capabilities** → check **Sign in with Apple**), no delete needed.
2. **Services ID** (identifier e.g. `com.starter.mobile.dev.signin`) — **the web-facing
   client**. Web only needs this and the Key; the native App ID is not used by the web flow.
3. **Key** → **Keys → Create** → enable *Sign in with Apple* → download the `.p8` (shown
   once) → note the **Key ID**; your **Team ID** is top-right of your membership page.

> **⚠️ Order matters — enable the capability on steps 1 & 2 BEFORE step 3.**
> The Key creation form can only associate identifiers that already have *Sign in with Apple*
> switched on. If you create the key first you get the dead checkbox + *"There are no
> identifiers available that can be associated with the key"* ([Apple forums
> thread](https://developer.apple.com/forums/thread/827208)). Doing steps 1–2 first makes the
> checkbox selectable, and you can then attach both the App ID and the Services ID to the key.

**⚠️ The step people miss for web — configure the Services ID itself:**

Open the created Services ID → **Sign in with Apple → Configure** and set both fields
([Firebase reference](https://firebase.google.com/docs/auth/web/apple)):

| Services ID field | Value (DEV) |
|---|---|
| Domains and subdomains | `starter-demo-dev.firebaseapp.com` — your **authDomain host** (where the OAuth redirect lands). Apple rejects the redirect if its domain isn't listed here |
| Return URLs | `https://starter-demo-dev.firebaseapp.com/__/auth/handler` — Firebase's OAuth handler under your authDomain |

- The authDomain is the `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN` value of the DEV web app (default
  `starter-demo-dev.firebaseapp.com`; PROD gets its own). The hosted site's `*.web.app`
  domain is the SPA origin and is **not** part of the Apple redirect.
- `__/auth/handler` is a Firebase-owned route on the authDomain — you don't build it.
- Typical failure if skipped: Apple shows *"Invalid web redirect URL"* in the popup
  ([Apple forums](https://developer.apple.com/forums/thread/757884)).

Then in the Firebase console **Authentication → Sign-in method → Apple → Enable** and **enter
all four fields** (Save stays greyed until each is valid):

| Firebase field | Value from |
|---|---|
| Apple Team ID | Apple Developer account top-right |
| Services ID | the Services ID above. The console labels it *"not required for Apple"* — that's for native-only apps; **for web it IS required**, so fill it |
| Key ID | the created Key's ID |
| Private Key | the downloaded `.p8` **entire contents** — paste verbatim including the `-----BEGIN PRIVATE KEY-----` / `-----END PRIVATE KEY-----` lines |

Save stays greyed until Team ID + Key ID + Private Key are all valid; the
*"additional configuration steps"* banner clears the moment the provider saves.

### 3. Authorized domains (web only)

- **Authentication → Settings → Authorized domains**. `*.web.app` / `*.firebaseapp.com` are
  already authorized (the hosted site is `starter-demo-dev.web.app`).
- If a **PROD hosting custom domain** is added later, register it here or the redirect flow fails.

### 4. Where these values land (after you have them)

| Value | Goes into (mobile repo) |
|---|---|
| Google Web client ID | hard-coded in [`FirebaseAuthAdapter.ts`](../starter-mobile/src/adapters/FirebaseAuthAdapter.ts) web branch (public by design — client identifier, not a secret) |
| Google Android/iOS client IDs | same adapter, native branch (`expo-auth-session` `clientId` per platform) |
| Apple Team/Key/Services ID | Firebase console only (step 2) — client Apple flow needs **nothing** from them |

No new `EXPO_PUBLIC_*` env vars are strictly required (these are public client IDs); if you
prefer env-driven, add them to [`src/config/env.ts`](../starter-mobile/src/config/env.ts) +
[`envValidation.ts`](../starter-mobile/src/config/envValidation.ts) + `app.config.ts` `extra`
following the existing PostHog pattern.

## Code checklist — web-first scope (Google + Apple on web only)

**✅ Implemented 2026-09-05 — no new npm packages needed.** Both popups go through the
already-installed Firebase JS SDK: `signInWithPopup` + `GoogleAuthProvider` /
`OAuthProvider('apple.com')`. Firebase handles the Apple nonce internally on web (a
client-supplied nonce is only required on native). `expo-auth-session` /
`expo-apple-authentication` / `expo-crypto` stay out until native.

| # | File | Change (web-only) — shipped |
|---|---|---|
| 1 | [`src/ports/AuthPort.ts`](../starter-mobile/src/ports/AuthPort.ts) | Added `signInWithGoogle(): Promise<void>` + `signInWithApple(): Promise<void>` |
| 2 | [`src/adapters/FirebaseAuthAdapter.ts`](../starter-mobile/src/adapters/FirebaseAuthAdapter.ts) | Web branch: `signInWithPopup(auth, new GoogleAuthProvider())` / `signInWithPopup(auth, new OAuthProvider('apple.com'))`; **native throws "web-only at v1"** (the popup APIs are web-only in the JS SDK) |
| 3 | [`src/features/auth/AuthProvider.tsx`](../starter-mobile/src/features/auth/AuthProvider.tsx) | Both methods exposed on the context value via `useCallback` |
| 4 | [`app/(auth)/login.tsx`](<../starter-mobile/app/(auth)/login.tsx>) | "Continue with Google" / "Continue with Apple" buttons below a divider — **rendered only when `Platform.OS === 'web'`**; shared `socialSubmitting` state + `getAuthErrorMessage` |
| 5 | [`src/features/auth/authErrors.ts`](../starter-mobile/src/features/auth/authErrors.ts) | Added popup codes: `popup-blocked`, `popup-closed-by-user`, `account-exists-with-different-credential`, `cancelled-popup-request`, `timeout` |

Verified: `npx tsc --noEmit` clean (my files), `npm run lint` 0 errors, full jest suite
**34 suites / 201 tests pass**, `expo export --platform web` builds the bundle including
`/login`.

## Code checklist — native later (scope A full)

When native Google/Apple land (mandatory pair once Google ships on iOS, Guideline 4.8):

| # | File | Change (native) |
|---|---|---|
| 1 | [`package.json`](../starter-mobile/package.json) | `npx expo install expo-auth-session expo-apple-authentication expo-crypto` (SDK-54-pinned; `expo-web-browser` already present) |
| 2 | [`src/adapters/FirebaseAuthAdapter.ts`](../starter-mobile/src/adapters/FirebaseAuthAdapter.ts) | **Native Google**: `expo-auth-session` → `GoogleAuthProvider.credential(idToken)` → `signInWithCredential` (needs the Google Android/iOS client IDs from §4 above); **Native Apple (iOS)**: `expo-apple-authentication` `signInAsync` → `OAuthProvider('apple.com').credential({ idToken, rawNonce })` — the **nonce is mandatory** (Firebase rejects `apple.com` without it; generate with `expo-crypto` `getRandomBytes` → base64url) |
| 3 | [`app.config.ts`](../starter-mobile/app.config.ts) | Adds the config plugin / entitlements for `expo-apple-authentication`; variant `scheme` (lines 116–135) already gives `expo-auth-session` its redirect URI (`startermobile-dev://`); add Android `intentFilters`/iOS `associatedDomains` only if the native Google flow demands it |

Native flow reference (use the versioned docs for the pinned SDK 54): Expo
[Google authentication](https://docs.expo.dev/versions/latest/sdk/auth-session/) and
[Sign in with Apple](https://docs.expo.dev/versions/latest/sdk/apple-authentication/).

## CSP impact (web hosted site)

The site's CSP lives in [`firebase.json`](../starter-mobile/firebase.json) (added 2026-09-05,
see [FIREBASE_HOSTING.md](./FIREBASE_HOSTING.md) § Security headers). Expect to extend it:

- `connect-src` likely needs `https://accounts.google.com` and `https://appleid.apple.com`
  (sign-in token exchange). `securetoken.googleapis.com` is already present.
- **Popup flows are separate windows — NOT governed by `frame-src`**, so `frame-src 'none'`
  can stay. If you switch Google web to `signInWithRedirect`, both are fine over plain HTTPS.
- **Verify empirically after enabling**: deploy, then watch the browser console on the live
  site; add only the origins the console reports. `upgrade-insecure-requests` stays.

## Verification / smoke

Status: code-level verified (typecheck, lint, 201 unit tests, web export) — **the live-site
browser smoke is yours** (needs your Google/Apple accounts + the deploy):

1. **Deploy** — push `starter-mobile` to `main` (web deploy workflow runs; e2e gate passes on
   the email/password suite — social popups are excluded from CI, see below).
2. **Live smoke** — open `https://starter-demo-dev.web.app/login`:
   - **CSP watch**: open DevTools console **before** clicking; clicks below may report
     blocked requests — extend `firebase.json` `connect-src` with exactly what the console
     shows (candidates: `https://accounts.google.com`, `https://appleid.apple.com`), redeploy,
     re-test. `frame-src 'none'` should hold (popups are separate windows).
   - Click **Continue with Google** → complete the consent → you land signed-in.
   - Sign out → click **Continue with Apple** → same.
   - Sign out → email/password sign-in still works (regression).
   - Profile screen shows the right user; `/api/v1/me` returns the social account's uid/email.
3. **Backend**: no deploy needed — the existing DEV backend smoke
   ([FIREBASE_AUTH.md](./FIREBASE_AUTH.md) § Verify) already proves the token path.
4. **Native**: not yet — EAS preview build on a **physical device** when the native checklist
   (§ native later) ships; Apple needs a real device (simulator returns not-supported).
5. **CI/e2e**: Google/Apple popups are **not drivable headless** (both providers block
   automation) — the Playwright email/password suite stays the PR gate
   ([BROWSER_E2E.md](./BROWSER_E2E.md)); social gets manual + device smoke (same posture as
   native App Check).

## Caveats

- **CSP may need extending on first live sign-in**: the hosted site's CSP
  (`starter-mobile/firebase.json`, see [FIREBASE_HOSTING.md](./FIREBASE_HOSTING.md) § Security
  headers) gates `connect-src`. If the browser console reports blocked requests during the
  Google/Apple popup, add exactly those origins (candidates `https://accounts.google.com`,
  `https://appleid.apple.com`) to `connect-src`, redeploy, retest. `frame-src 'none'` should
  hold — popups are separate windows.
- **Not covered by CI/e2e**: Google/Apple popups cannot be driven headless (both providers
  block automation). The Playwright suite stays email/password; social login gets manual live
  smoke + physical-device smoke ([BROWSER_E2E.md](./BROWSER_E2E.md), same posture as native
  App Check).
- **Apple is mandatory on iOS the moment Google ships native** (App Store Guideline 4.8).
  Web-only Google is fine today; native Google + native Apple must land in the same iOS
  release (see the native checklist above).
- **Privacy-relay emails**: Apple users may present `…@privaterelay.appleid.com`. Treat it as
  verified (Firebase does) — don't build features that assume a public email address.
- **Account linking is out of scope at v1**: a person with email/password AND Google becomes
  **two Firebase accounts** unless `linkWithCredential(...)` is added. Product decision, not
  template default.

## Traps / decisions

- **`auth/operation-not-allowed`** after console work → the provider isn't enabled in *that*
  project (DEV vs PROD mismatch).
- **Popups blocked** → the browser blocked the popup; `signInWithRedirect` is the fallback.
- **Apple `1000` / "not supported"** on iOS → entitlement missing (config plugin) or not a
  physical device.
- **Privacy-relay emails**: Apple users may present `…@privaterelay.appleid.com`; treat it as
  verified (Firebase does) — don't build email-based features that assume a public address.
- **Account linking is out of scope at v1** (decision): the same person with email/password
  AND Google becomes **two Firebase accounts** unless you add
  `user.linkWithCredential(...)` — a product decision, not template default. Document it, don't
  build it.
- **Per-environment separation**: enable DEV providers in the DEV project and PROD providers
  in PROD; the `envValidation.ts` guards keep binaries from crossing projects
  ([FIREBASE_AUTH.md](./FIREBASE_AUTH.md) — dual-location rule still applies to the Firebase trio).
- **Abuse**: social sign-in sidesteps the reCAPTCHA sign-up gate; the provider gating is the
  control here. If scripted-account risk matters, App Check enforcement is the follow-on
  ([APP_CHECK.md](./APP_CHECK.md)).

## Status tracking (done 2026-09-05)

- [`docs/integrations-plan.md`](../docs/integrations-plan.md) — Social login row: 🟡 runbook ready → ✅ web implemented
- [`integrations/README.md`](./README.md) — index table: runbook row added (❌ → ✅ web scope)
- [`docs/SECURITY_ARCHITECTURE_PLAN.md`](../docs/SECURITY_ARCHITECTURE_PLAN.md) — §6 sign-up abuse note stays; revisit when App Check/blocking functions land