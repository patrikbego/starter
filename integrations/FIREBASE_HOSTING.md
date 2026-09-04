# Firebase Hosting — Permanent Web URL (DEV)

**Status: ✅ implemented 2026-09-04.** The Expo web export (`dist/`) deploys to Firebase
Hosting on every `main` push — **gated on the browser e2e suite passing** (fail closed; the
site only goes live when e2e is green against the exact exported bundle). This is the answer to
"where's the frontend link?":
[`https://starter-demo-dev.web.app`](https://starter-demo-dev.web.app).

## Try it — live DEV app (links + credentials)

| What | Link |
|---|---|
| Web app (DEV) | https://starter-demo-dev.web.app (also https://starter-demo-dev.firebaseapp.com) |
| Backend API (DEV) | https://starter-api-dev-906316354955.europe-west2.run.app |
| Liveness probe | https://starter-api-dev-906316354955.europe-west2.run.app/health/live |
| Cloud Run console | https://console.cloud.google.com/run?project=starter-demo-dev |
| Firebase console | https://console.firebase.google.com/project/starter-demo-dev/overview |

**Demo login (DEV test user):**

```text
email:    demo@starter-demo-dev.com
password: StarterDemo!2026
```

- This is a throwaway DEV account with `emailVerified=true` (so the AI chat round-trip works —
  the backend gates `POST /api/v1/ai/chat` on the verified claim). Created 2026-09-04 via
  Firebase Auth + Admin SDK.
- It is a **test credential, not a secret** (DEV project only; no same-password reuse anywhere
  in PROD). Recreate it any time with the steps in [FIREBASE_AUTH.md](./FIREBASE_AUTH.md) +
  an Admin-SDK `updateUser(emailVerified: true)` call, or just register a fresh user on the
  site (sign-up is self-service in DEV).
- The API root returns `401 {"code":"UNAUTHORIZED",…}` for unauthenticated requests — that is
  expected, not a bug. Sign in on the web app first.

## What this adds vs the e2e flow

| | Browser e2e (`web-e2e.yml`) | Hosted web (`deploy-web.yml`) |
|---|---|---|
| Frontend URL | `http://localhost:8081` (CI-local, ephemeral) | `https://starter-demo-dev.web.app` (permanent) |
| Who boots it | Playwright `webServer` (export → serve) | Firebase Hosting |
| Gate | PR gate (self-registering auth flow) | **e2e gate in-workflow — deploy only on pass** |
| Backend | live DEV Cloud Run (same for both) | live DEV Cloud Run (same for both) |
| Purpose | PR gate proving real UI × real stack | human-facing link, quick demos, web smoke |

Both bake the same DEV env at export time (`APP_ENV=development`, `API_BASE_URL_DEV`,
`EXPO_PUBLIC_FIREBASE_*` from repo variables).

## Files

| Path | Role |
|---|---|
| `starter-mobile/firebase.json` | Hosting config: site `starter-demo-dev`, public `dist`, SPA rewrite `**` → `/index.html` |
| `starter-mobile/.firebaserc` | Default project `starter-demo-dev` |
| `starter-mobile/.github/workflows/deploy-web.yml` | Export → `firebase deploy --only hosting` on `main` push |
| `starter-backend/.github/workflows/deploy-dev.yml:181` | `STARTER_CORS_ALLOWED_ORIGINS` now includes `https://starter-demo-dev.web.app` + `.firebaseapp.com` |

## Identity & permissions

- Hosting site: default site `starter-demo-dev` of GCP project `starter-demo-dev` (created by
  Firebase when the project was provisioned; no separate site creation needed).
- Deploy identity: dedicated SA `starter-mobile-hosting@starter-demo-dev.iam.gserviceaccount.com`.
  - Roles: `roles/firebasehosting.admin` + `roles/firebase.viewer` (scope: project).
  - Key JSON stored ONLY as the GitHub Actions secret `FIREBASE_DEPLOY_SA_KEY` on the mobile repo.
  - **Deliberately NOT** the backend's WIF service accounts: the repos stay decoupled.
- Auth in CI: `google-github-actions/auth` with `credentials_json` → `setup-gcloud` →
  `FIREBASE_TOKEN=${{ steps.auth.outputs.access_token }}` for `firebase-tools`.

## CORS (load-bearing)

The hosted origin must be on the backend's allow-list, or browser calls to `/api/v1/*` fail
preflight (`Access-Control-Allow-Origin` missing). Both `https://starter-demo-dev.web.app` and
`https://starter-demo-dev.firebaseapp.com` are in `STARTER_CORS_ALLOWED_ORIGINS` in
`deploy-dev.yml`. **Deploy the backend before/with the web when changing this list** — Cloud Run
reads it at deploy time.

## Local deploy (optional)

```bash
cd starter-mobile
npm run deploy:web   # = export:web + firebase deploy --only hosting, requires local auth
```

CI does not need the local auth; it uses the SA key. Local devs sign in via `firebase login`.

## Costs

Firebase Hosting is free-tier friendly at this scale (10 GB storage, 360 MB/day transfer on the
Spark plan; see [Firebase pricing](https://firebase.google.com/pricing)). The web export is an
uncompressed directory of a small Expo app — well within that. Professional tier only if traffic
grows beyond the free limits.

## Rollback / re-run

Every push to `main` re-deploys. To re-publish an older state you must check out that commit
(hosting deploys the *current* `dist/` export from the checked-out source; hosting keeps prior
releases but the CLI only pushes active content). For a quick manual redeploy of the same code,
run `npm run deploy:web` locally.

## Not included (intentionally)

- No custom domain, no `firebase.json` `headers`/`redirects` beyond the SPA rewrite.
- No PROD hosting site yet (`starter-demo-prod` untouched) — add one when the prod pipeline ships.
- No CDN cache tuning; defaults are fine for a dev prototype.

## Incident log

- **2026-09-04 — web sign-ups returned `502 ACCOUNT_PROVIDER_ERROR`** (fresh register in the
  browser e2e and on the hosted site both failed; the deploy gate went red, correctly). Root
  cause: the backend runtime SA `starter-api@…` lacked `roles/firebaseauth.admin`, so the
  Admin-SDK `createUser` behind `POST /api/v1/auth/sign-up` failed. Fixed by granting the role;
  codified in `starter-backend/infra/main.tf` and recorded in
  [SIGNUP_ABUSE_GATE.md](./SIGNUP_ABUSE_GATE.md). Re-ran the full e2e suite (8 pass) and
  verified a fresh live-site sign-up before reopening the gate.