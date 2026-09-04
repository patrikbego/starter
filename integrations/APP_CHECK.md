# APP_CHECK.md — Firebase App Check per Product App

**Status: implemented (web); native wired in code, activation still per-app.** App Check proves a
request comes from a *genuine build of your app*, not curl/a scraper. It is NOT authentication —
identity stays the Firebase ID token. The backend *requires* a valid App Check token on the public
AI endpoint (`/api/v1/ai/chat`) when enabled, and *verifies when present* on other protected
routes. Web uses the Firebase JS SDK (reCAPTCHA Enterprise); native uses
`@react-native-firebase/app-check` (Play Integrity / App Attest), pulled in only when the opt-in
toggle is on. Console registration + an EAS-build live smoke are the remaining activation steps.

Implementation lives in `starter-backend` (JWT/JWKS verification) and `starter-mobile` (web
provider + header). See "[Pending / to-do](#pending--to-do)" below for what remains.

## The key implementation fact

**The Java Admin SDK has no `AppCheck.verifyToken` API** (verified: `firebase-admin:9.10.0` ships
no `appcheck` package). The "firebase-admin v12+" upgrade path documented earlier was a
Node.js-SDK version number and does not apply. So the backend verifies App Check tokens itself —
the same way Firebase's
[custom-backend guidance](https://firebase.google.com/docs/app-check/custom-resource-backend) shows
for SDK-less languages: App Check tokens are RS256 JWTs; fetch the project's public keys and check
signature, `aud`, `iss`, `exp`.

## Pending / to-do

Not fully rolled out — the template ships the mechanism, but activation needs user + follow-up work:

- **Native mobile is wired in code** (`@react-native-firebase/app` + `@react-native-firebase/app-check`
  v23.8.8 + their Expo config plugins, included **only when** `EXPO_PUBLIC_APP_CHECK_ENABLED=true`,
  so default builds stay JS-SDK-only). Activation still needs, per app + env: console app
  registrations (Android: Play Integrity + signing-cert SHA-256; iOS: App Attest + DeviceCheck),
  the two console files dropped at the mobile repo root (`google-services.json`,
  `GoogleService-Info.plist` - gitignored, fail-closed at prebuild), optionally
  `EXPO_PUBLIC_APP_CHECK_DEBUG_TOKEN` for the debug provider in non-production, and an EAS build +
  live smoke before any enforcement flip. **This gates Phase A of the sign-up abuse gate**
  ([SIGNUP_ABUSE_GATE.md](./SIGNUP_ABUSE_GATE.md)).
- **Console provisioning is manual** (see below) — Get started + app registration in both DEV and
  PROD Firebase projects; no API creates it.
- **Enforcement is off by default.** Flip the **repo variables** on the backend repository
  (`APP_CHECK_ENABLED_DEV/PROD` + `APP_CHECK_PROJECT_NUMBER_DEV/PROD`) — both deploy workflows now
  carry an opt-in "Configure App Check extension" step that mirrors billing/email. Then run
  `npm run test:e2e:app-check` (browser E2E with real tokens) as the live smoke before it bites in
  PROD. DEV stays off until that smoke exists.
- **reCAPTCHA Enterprise site key** must be provisioned in Google Cloud and linked, then set as
  `EXPO_PUBLIC_RECAPTCHA_ENTERPRISE_SITE_KEY` (REQUIRED for DEV E2E and PROD when the `EXPO_PUBLIC_APP_CHECK_ENABLED=true` toggle is on; see Mobile wiring)。 For DEV, a reCAPTCHA
  Enterprise **test key** can be used — deterministic tokens that still verify against the real
  JWKS, which is what makes the browser E2E runnable headless.

## Backend wiring (starter-backend)

| Piece | Path |
|---|---|
| Config (`starter.app-check.*`) | [`../starter-backend/src/main/java/com/starter/config/AppCheckConfig.java`](../starter-backend/src/main/java/com/starter/config/AppCheckConfig.java) |
| Port | [`../starter-backend/src/main/java/com/starter/security/AppCheckVerifier.java`](../starter-backend/src/main/java/com/starter/security/AppCheckVerifier.java) |
| Real verifier (JWT/JWKS, `!local`) | [`../starter-backend/src/main/java/com/starter/adapters/appcheck/JwksAppCheckVerifier.java`](../starter-backend/src/main/java/com/starter/adapters/appcheck/JwksAppCheckVerifier.java) |
| Local mock | [`../starter-backend/src/main/java/com/starter/adapters/appcheck/MockAppCheckVerifier.java`](../starter-backend/src/main/java/com/starter/adapters/appcheck/MockAppCheckVerifier.java) |
| Filter (require on ai/chat, verify-if-present else) | [`../starter-backend/src/main/java/com/starter/security/AppCheckFilter.java`](../starter-backend/src/main/java/com/starter/security/AppCheckFilter.java) |
| Backend doc (config, workflows, verification) | [`../starter-backend/docs/APP_CHECK_EXTENSION.md`](../starter-backend/docs/APP_CHECK_EXTENSION.md) |

Config keys (all `starter.app-check.*`, env-tunable):

```yaml
starter:
  app-check:
    enabled: ${APP_CHECK_ENABLED:false}
    project-number: ${APP_CHECK_PROJECT_NUMBER:}
    jwks-uri: ${APP_CHECK_JWKS_URI:https://firebaseappcheck.googleapis.com/v1/jwks}
```

- **Enabled requires `project-number`** (the token `aud`); startup fails otherwise (fail closed).
- **Default off.** Enable per environment: DEV stays off until a live smoke passes; PROD is the
  v1 target.
- Verification policy: `POST /api/v1/ai/chat` **requires** a valid token (`401 APP_CHECK_REQUIRED` /
  `APP_CHECK_INVALID`); other routes **verify when present**, never as an auth replacement.

## Mobile wiring (starter-mobile) — web provider

| Piece | Path |
|---|---|
| Port | [`src/ports/AppCheckPort.ts`](../starter-mobile/src/ports/AppCheckPort.ts) |
| Web adapter (reCAPTCHA Enterprise via JS SDK) | [`src/adapters/FirebaseAppCheckAdapter.ts`](../starter-mobile/src/adapters/FirebaseAppCheckAdapter.ts) |
| Header injection | [`src/adapters/HttpApiClient.ts`](../starter-mobile/src/adapters/HttpApiClient.ts) |

- Web uses `@firebase/app-check` with a **reCAPTCHA Enterprise** provider; site key comes from
  `EXPO_PUBLIC_RECAPTCHA_ENTERPRISE_SITE_KEY` (verbatim in `app.config.ts` → `extra.recaptchaEnterpriseSiteKey`).
- The header `X-Firebase-AppCheck` is attached next to `Authorization` when a token is available;
  a `null` token simply omits the header.
- **App Check is opt-in per product**: a `EXPO_PUBLIC_APP_CHECK_ENABLED=true` toggle turns
  it on (absent/false = off, no token sent anywhere). PROD builds must set the site key
  **only when the toggle is on** (`envValidation.ts` + `app.config.ts` fail otherwise). The site key
  comes from `EXPO_PUBLIC_RECAPTCHA_ENTERPRISE_SITE_KEY` (verbatim in `app.config.ts` → `extra`).

## Native mobile (iOS/Android) — wired in code, activation per app

| Piece | Path |
|---|---|
| Native adapter (RNFB tokens + JS bridge) | [`src/adapters/FirebaseAppCheckNativeAdapter.ts`](../starter-mobile/src/adapters/FirebaseAppCheckNativeAdapter.ts) |
| Factory (web/native/dispatch) | [`src/adapters/FirebaseAppCheckAdapter.ts`](../starter-mobile/src/adapters/FirebaseAppCheckAdapter.ts) |
| Debug token config | `EXPO_PUBLIC_APP_CHECK_DEBUG_TOKEN` (`.env.example`, `app.config.ts` -> `extra`) |

The native adapter is built on `@react-native-firebase/app` + `@react-native-firebase/app-check`
(pinned 23.8.8, the line matching Expo SDK 54 / RN 0.81). Both packages and their Expo config
plugins enter the build **only when `EXPO_PUBLIC_APP_CHECK_ENABLED=true`**; the core plugin then
requires the console files at the mobile repo root (`google-services.json`,
`GoogleService-Info.plist`, both gitignored, fail-closed at prebuild) and wires
`FirebaseApp.configure()`.

Two App Check layers exist on native, and both are needed:

1. **RNFB (native)** mints tokens via Play Integrity (Android) / App Attest with DeviceCheck
   fallback (iOS). Non-production builds use the `debug` provider (optionally with
   `EXPO_PUBLIC_APP_CHECK_DEBUG_TOKEN`, registered in the Firebase console for
   CI/simulator use). These tokens feed the `X-Firebase-AppCheck` header to the backend.
2. **JS bridge**: the app's auth runs on the Firebase JS SDK, which attaches App Check tokens to
   Identity Toolkit calls (sign-up/sign-in/reset) only from a JS-layer instance. The adapter
   initializes `firebase/app-check` with a `CustomProvider` delegating to RNFB. Without this
   bridge, Phase A enforcement ([SIGNUP_ABUSE_GATE.md](./SIGNUP_ABUSE_GATE.md)) would reject every
   auth call on native even though native mints valid tokens.

**Enable on PROD only after an EAS native build + live smoke** verifies the Play Integrity /
App Attest round-trips and the console provider registrations below.

## Console provisioning (per environment project) — user action

Same console-only bootstrap nature as Auth (no API creates it):

1. **Build → App Check → Get started** once, in both `*-dev` and `*-prod` Firebase projects.
2. Register apps per project: Android (Play Integrity, needs signing-cert SHA-256), iOS (App
   Attest + DeviceCheck), Web (reCAPTCHA Enterprise site key, created in Google Cloud console and
   linked). Native App Check additionally needs the per-project console files dropped at the
   mobile repo root (`google-services.json`, `GoogleService-Info.plist`; gitignored) and, for
   non-production, a debug token registered under App Check > Apps > Manage debug tokens.
3. **Keep enforcement OFF** in the console — this backend is the enforcement point (console
   enforcement only governs Firebase services, not this Spring API). **One exception:** App Check
   enforcement on **Identity Platform** (Auth sign-up/sign-in/reset) is Phase A of the sign-up
   abuse gate — flip it only after native App Check is wired and a live smoke passed, per
   [SIGNUP_ABUSE_GATE.md](./SIGNUP_ABUSE_GATE.md).
4. Distribute values: site key → `EXPO_PUBLIC_RECAPTCHA_ENTERPRISE_SITE_KEY` (EAS env + GitHub
   repo vars, dual-location rule); project number → backend **repo variables**
   `APP_CHECK_PROJECT_NUMBER_DEV/PROD` (not a secret — public in Firebase app config). Both deploy
   workflows read them behind `APP_CHECK_ENABLED_DEV/PROD=true` toggles (see Backend wiring).

## Verify (per environment)

1. **In-process E2E** (runs in `./mvnw verify`, no deployment): `AppCheckApiIntegrationTest`
   drives the real filter chain end-to-end — ai/chat without a token → `401 APP_CHECK_REQUIRED`,
   invalid → `401 APP_CHECK_INVALID`, valid → `200`; other routes verify-if-present; health
   untouched. Cryptography is unit-covered by `JwksAppCheckVerifierTest`; fail-closed config by
   `AppCheckConfigTest`.
2. Mobile `npm run validate:contract` (re-pinned digest), `npm run lint`, `npx tsc --noEmit`, `npm test`.
   Native adapter is unit-covered by `FirebaseAppCheckNativeAdapter.test.ts` (provider selection,
   JS bridge delegation, failure-returns-null; RNFB modules mocked).
3. **Browser E2E** (`npm run test:e2e:app-check`, dispatch-only — excluded from the PR gate,
   self-skips unless `E2E_APP_CHECK=1` + verified creds): exported web bundle mints a real
   reCAPTCHA Enterprise token → asserts the `X-Firebase-AppCheck` header on the outgoing chat
   request → the backend (with App Check ON) verifies and replies. This is the **live smoke** the
   deployment tracks deferred: run it against a deployed DEV/PROD backend before enabling
   enforcement there. Prereqs: console provisioning + site key +
   `APP_CHECK_ENABLED_DEV/PROD=true` repo variables + bundle exported with the site key (see
   Pending / to-do).
4. **Native smoke** (gates the sign-up gate's Phase A flip): EAS preview build with
   `EXPO_PUBLIC_APP_CHECK_ENABLED=true` + console files + registered debug token → sign-in on a
   real device → the request carries a valid `X-Firebase-AppCheck` (backend accepted a
   required-token route). This proves the RNFB + JS-bridge stack end to end; unit tests alone
   cannot (verified against SDK contracts, not a live attestation).
   The flag is **pre-baked `true` in `eas.json` `preview` and `production`** (the `development`
   profile stays off) — pass nothing on the command line; just don't strip it from the profiles.

## Cost

App Check itself is free; it is *not* a billing line. The only coupling is web reCAPTCHA
Enterprise assessments (~10k/mo free, snapshot 2026 — re-verify). **The 10k pool is org-wide**
— sign-up assessments (sign-up gate Phase C) share it; Essentials hard-stops with `429` beyond
the limit ([SIGNUP_ABUSE_GATE.md](./SIGNUP_ABUSE_GATE.md)). Native providers (Play
Integrity / App Attest) are free.

## Rules that must not be broken

- App Check is an **auxiliary signal, never an authentication replacement** — it proves *what the
  client is*, not *who the user is*.
- Enabled requires `project-number`; startup fails otherwise (fail closed).
- DEV stays off until a live smoke passes (the browser E2E with a reCAPTCHA Enterprise **test**
  key is that smoke — it mints real, verifiable tokens headless).
- Never log the App Check token or JWT internals.
- PROD binary requires the site key **only when `EXPO_PUBLIC_APP_CHECK_ENABLED=true`**; native
  providers require console registration.
- Enable via backend repo variables (`APP_CHECK_ENABLED_DEV/PROD` + `APP_CHECK_PROJECT_NUMBER_*`);
  never hand-edit a Cloud Run service's env for this.