# FIREBASE_AUTH.md — Firebase Authentication per Product App

**Status: implemented in both templates.** Firebase is the identity provider: the mobile app
signs users in with the Firebase Web SDK; the backend verifies each token with the Admin SDK.
This doc covers provisioning Firebase for a *new product app* and the traps that have bitten.
Implementation details live in [starter-backend/docs/AUTHENTICATION.md](../starter-backend/docs/AUTHENTICATION.md).

## Where the implementation lives

| Piece | Path |
|---|---|
| Admin SDK init + project binding | [`starter-backend/src/main/java/com/starter/config/FirebaseConfig.java`](../starter-backend/src/main/java/com/starter/config/FirebaseConfig.java) |
| Token verification (Admin SDK) | [`starter-backend/src/main/java/com/starter/security/FirebaseAuthServiceImpl.java`](../starter-backend/src/main/java/com/starter/security/FirebaseAuthServiceImpl.java) |
| Bearer filter (protected routes) | [`starter-backend/src/main/java/com/starter/security/FirebaseAuthenticationFilter.java`](../starter-backend/src/main/java/com/starter/security/FirebaseAuthenticationFilter.java) |
| Local mock (no network) | [`starter-backend/src/main/java/com/starter/adapters/firebase/MockFirebaseAuthService.java`](../starter-backend/src/main/java/com/starter/adapters/firebase/MockFirebaseAuthService.java) |
| Mobile sign-in adapter | [`starter-mobile/src/adapters/FirebaseAuthAdapter.ts`](../starter-mobile/src/adapters/FirebaseAuthAdapter.ts) |
| Mobile env guards | [`starter-mobile/src/config/envValidation.ts`](../starter-mobile/src/config/envValidation.ts) |
| Auth email flows (verification, reset) | [starter-backend/docs/AUTHENTICATION.md](../starter-backend/docs/AUTHENTICATION.md) |

## The model

- **One Firebase project per environment, added to the same GCP project the backend deploys
  to** (`myapp-dev` Firebase ⊂ `myapp-dev` GCP project, `myapp-prod` likewise). Do NOT create a
  separate `*-local-*` Firebase project for DEV.
- Why: the mobile token's `aud` must equal the project the backend's Admin SDK verifies
  against. One project per environment keeps them aligned by construction.
- ⚠️ Incident 2026-08-23: mobile DEV pointed at a standalone `starter-local-b9525` Firebase
  project while the backend lived in `starter-demo-dev` → every `/api/v1/me` returned 401
  (audience mismatch) even though sign-in itself worked. Caught only by the browser-E2E P0 spike.

## Console-only bootstrap

A fresh project's Auth config does not exist until someone opens **Authentication → Get
started** once in the console — **no API creates it**. Do this before any deploy smoke.

## Per-app provisioning checklist

For each of `{app}-dev` and `{app}-prod`:

1. Link Firebase to the corresponding GCP project (console → Add project → import).
2. Open **Authentication → Get started** once (console-only bootstrap, see above).
3. Enable only the required sign-in providers (templates use **Email/Password**).
4. Register the **Web app** (hosting off) — click-path below.
5. Create test users for the authenticated smoke (see below).
6. Put the public client config (`EXPO_PUBLIC_FIREBASE_*` trio) in the matching EAS
   environment, and never let a prod binary point at a dev project.

### Register a Firebase web app (click-path)

Same flow in DEV and PROD projects, with strictly separated values:

1. [console.firebase.google.com](https://console.firebase.google.com) → open the project →
   ⚙️ **Project settings → General → Your apps**.
2. No Web app yet? Click the Web `</>` icon, register one (hosting off).
3. Copy `firebaseConfig.apiKey` (long `AIzaSy…` string), `authDomain`, `projectId`.
4. Enable Email/Password: **Build → Authentication → Sign-in method → Enable**.
5. Keep PROD values out of the DEV secret set; the mobile PROD build gets its own bucket.

### Test users for the authenticated smoke

The backend smoke signs in via the Identity Toolkit REST API
(`accounts:signInWithPassword?key=<WEB_API_KEY>`), so it needs a **real Firebase user**, not a
service account. All three values are GitHub repo secrets on the *backend* repo
(*Settings → Secrets and variables → Actions → New repository secret*):

1. `FIREBASE_WEB_API_KEY` — the DEV project's web-app `apiKey` from above.
2. `FIREBASE_TEST_USER_EMAIL` / `FIREBASE_TEST_USER_PASSWORD` — **Authentication → Users →
   Add user**, e.g. `smoke-test@myapp-dev.com`. DEV-project-only; exists purely so CI can
   prove an authorized `/api/v1/me` returns 200.

## Mobile wiring rules

- ⚠️ **Name projects to respect the env guards** (`starter-mobile/src/config/envValidation.ts`):
  a PROD build fails if `EXPO_PUBLIC_FIREBASE_PROJECT_ID` matches `-dev([.-]|$)`; DEV/preview
  builds fail on `-prod` ids. Keep dev/prod Firebase ids unambiguous (`myapp-dev`, `myapp-prod`).
- ⚠️ **Dual-location rule**: the Firebase trio must exist BOTH as GitHub repo variables
  (runner env) AND in the EAS `preview`/`production` environments (cloud build env). Repo
  variables reach only the runner; EAS env only the worker. Same for `API_BASE_URL_*`.
- When changing the Firebase trio, update BOTH places — the mismatch class is exactly what the
  2026-08-23 incident was.

## Auth emails vs product email

Firebase Auth sends the auth emails (verification, password reset). Product email the backend
originates (welcome, notifications, receipts) is the separate Resend integration —
[RESEND.md](./RESEND.md), designed but not yet implemented.

## Verify (per environment)

1. **Backend smoke** — deploy the backend; the workflow's authenticated smoke proves the chain
   automatically when the three `FIREBASE_*` secrets are set (sign-in via Identity Toolkit →
   `GET /api/v1/me` → 200). Manually:

```bash
curl -s -o /dev/null -w '%{http_code}\n' https://<app>-api-<env>.run.app/api/v1/me
```

   `401` unauthenticated (fail closed), `200` with a Firebase Bearer token from that
   environment's project.
2. **Mobile smoke** — install the preview build on a device: `login → me → sign-out` works
   against the DEV backend (STEP6 §7).
3. Failure signatures:
   - `CONFIGURATION_NOT_FOUND` on token verify → the project's Auth config was never created —
     open **Authentication → Get started** once in the console.
   - Sign-in works but every authed endpoint 401s → **audience mismatch**: the mobile app and
     the backend Admin SDK are pointed at different Firebase projects (the 2026-08-23 incident).

## Rules that must not be broken

- **One Firebase project per environment**, added to the same GCP project the backend deploys
  to. Never a separate `*-local-*` project for DEV; never share a project between products.
- **Never point a prod binary at a dev project** — the `envValidation.ts` guards fail the build
  on `-dev`/`-prod` id collisions; keep project ids unambiguous.
- **The Web API key is public-by-design** (same key class the app ships). The Firebase Admin
  SDK private key is NOT — Secret Manager only, never in the repo, EAS config, or logs.
- **The `EXPO_PUBLIC_FIREBASE_*` trio lives in two places** (GitHub repo variables AND EAS
  environment); change both together or you recreate the audience-mismatch class.
- **Auth config bootstrap is console-only** — no API creates it; do it before any deploy smoke.

## What is shared vs per-app

Account/membership: shared. Projects, web apps, providers config, test users, web API keys:
per app, per environment — see `guides/STEP6_NEW_APP_FROM_STARTER.md` §2.
