# Environment Matrix

Replace `{app}` with a stable lowercase slug. DEV and PROD are isolated projects and Firebase tenants; never mix their tokens, URLs, data, or credentials.

## Environment intent

| Environment | Backend execution | Mobile execution | Data | Purpose |
|---|---|---|---|---|
| `local` | Explicit local profile with mocks/emulators | Expo local development | Disposable | Fast offline feedback |
| `dev-local` | Local JVM with real DEV adapters | Expo local or preview app | DEV | Debug real integrations |
| `dev` | Cloud Run in `{app}-dev` | EAS preview/internal build | DEV | Shared integration and QA |
| `prod` | Cloud Run in `{app}-prod` | Store-signed build | PROD | Real users |

The backend has no implicit local fallback. The mobile production build has no fallback API URL. Missing required configuration stops startup/build.

## Cloud resources

| Resource | DEV | PROD |
|---|---|---|
| GCP project | `{app}-dev` | `{app}-prod` |
| Firebase project | `{app}-dev` | `{app}-prod` |
| Cloud Run service | `{app}-api-dev` | `{app}-api-prod` |
| Firestore | Native mode, selected region | Native mode, same data-residency policy |
| Runtime service account | `{app}-api@{app}-dev.iam.gserviceaccount.com` | `{app}-api@{app}-prod.iam.gserviceaccount.com` |
| Secrets | DEV values only | PROD values only |

For strict “build once, promote” semantics, store images in a shared Artifact Registry project/repository or grant the PROD deployer and Cloud Run service agent read access to the repository that contains the tested digest. Deploy by `image@sha256:...`, never a mutable tag.

## Backend variables

### Required in `dev` and `prod`

| Variable | Example | Source |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` or `prod` | Cloud Run environment |
| `GCP_PROJECT_ID` | `{app}-dev` | Cloud Run environment |
| `STARTER_CORS_ALLOWED_ORIGINS` | `https://admin.example.com,https://app.example.com` | Cloud Run environment — comma-separated allow-list |
| `OPENAI_API_KEY` | secret value | Secret Manager |
| `AI_CHAT_MODEL` | `deepseek/deepseek-v4-flash-0731` | Environment — provider model identifier (default in `application.yml`) |
| `AI_ENABLED` | `true` | Cloud Run environment — fail-closed kill-switch for the AI money path |
| `ACTUATOR_PASSWORD` | secret value | Secret Manager, only if Basic-auth actuator is retained |
| `AI_REQUEST_TIMEOUT` | `30s` | Environment — provider call bound |
| `AI_MAX_INPUT_CHARS` | `4000` | Environment — input cap before provider call |
| `AI_MAX_REQUESTS_PER_USER` | `120` | Environment — per-user quota window limit |
| `AI_RATE_LIMIT_WINDOW` | `1h` | Environment — quota window (`Duration` syntax) |
| `PORT` | `8080` | Cloud Run env only (web-server port; Spring binds `server.port`) |

AI guardrails fail startup when missing in `dev`/`prod` (same fail-closed rule as
the other required variables). Quota exhaustion returns `429` with a
`Retry-After` header. The per-instance nature of the default quota store and
other limitations are tracked in `./REVIEW_FINDINGS.md` (caveat C1).

#### Recommended operational variables

| Variable | Purpose |
|---|---|
| `AI_BASE_URL` | Provider base URL (default `https://openrouter.ai/api/v1` in `application.yml`) |
| `AI_TEMPERATURE` | Provider sampling temperature (default `0.7` in `application.yml`) |

### Extension variables (opt-in, off by default)

Every extension defaults to disabled; enabling without its required values fails startup
(fail-closed). Names/defaults are defined once in
`starter-backend/src/main/resources/application.yml`. **This table is the single source for
extension env var names — runbooks and extension design docs link here; they do not restate it.**

#### Billing (Stripe) — `integrations/STRIPE.md`

| Variable | Default | Notes |
|---|---|---|
| `BILLING_ENABLED` | `false` | Kill-switch; routes answer `503 BILLING_DISABLED` when off |
| `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` / `STRIPE_PRICE_ID` | — | Secret Manager |
| `BILLING_SUCCESS_URL` / `BILLING_CANCEL_URL` / `BILLING_PORTAL_RETURN_URL` | `http://localhost:8081/billing/...` | Hosted Checkout/portal return URLs |

#### Email (Resend) — `integrations/RESEND.md`

| Variable | Default | Notes |
|---|---|---|
| `EMAIL_ENABLED` | `false` | Kill-switch (no API routes) |
| `RESEND_API_KEY` | — | Secret Manager |
| `EMAIL_FROM` | — | Verified sender |
| `RESEND_BASE_URL` | `https://api.resend.com` | |

#### Push — `integrations/PUSH.md`

| Variable | Default | Notes |
|---|---|---|
| `PUSH_ENABLED` | `false` | Routes answer `503 PUSH_DISABLED` when off |
| `EXPO_ACCESS_TOKEN` | — | Optional (higher rate limits) |
| `EXPO_PUSH_BASE_URL` | `https://exp.host` | |

#### App Check — `integrations/APP_CHECK.md`

| Variable | Source | Notes |
|---|---|---|
| `APP_CHECK_ENABLED_DEV` / `APP_CHECK_ENABLED_PROD` | GitHub repo variables | Set `true` to turn enforcement on for that environment (workflows bind runtime `APP_CHECK_ENABLED`) |
| `APP_CHECK_PROJECT_NUMBER_DEV` / `APP_CHECK_PROJECT_NUMBER_PROD` | GitHub repo variables | The Firebase project number (token `aud`); not a secret (workflows bind runtime `APP_CHECK_PROJECT_NUMBER`) |
| `APP_CHECK_JWKS_URI` | Environment | Optional override; defaults to the Firebase App Check JWKS |

#### Server-mediated sign-up (reCAPTCHA Enterprise) — `integrations/SIGNUP_ABUSE_GATE.md`

| Variable | Default | Notes |
|---|---|---|
| `RECAPTCHA_ENABLED` | `false` | Kill-switch; off by default |
| `RECAPTCHA_PROJECT_ID` / `RECAPTCHA_SITE_KEY` / `RECAPTCHA_API_KEY` | — | `API_KEY` = Secret Manager |
| `RECAPTCHA_EXPECTED_ACTION` / `RECAPTCHA_MIN_SCORE` | `sign-up` / `0.5` | |
| `RECAPTCHA_BASE_URL` | `https://recaptchaenterprise.googleapis.com` | |

#### Sentry — `integrations/SENTRY.md`

| Variable | Default | Notes |
|---|---|---|
| `SENTRY_ENABLED` | `false` | Kill-switch; requires `SENTRY_DSN` when enabled |
| `SENTRY_DSN` | — | Secret Manager |

#### PostHog (analytics + feature flags) — `integrations/POSTHOG.md`

| Variable | Default | Notes |
|---|---|---|
| `FLAGS_ENABLED` | `false` | Backend flag evaluation (kill-switch) |
| `POSTHOG_API_KEY` | — | Project **secret** key, `feature_flag:read` — Secret Manager |
| `POSTHOG_PROJECT_API_KEY` | — | Public `phc_...` |
| `POSTHOG_HOST` | `https://eu.i.posthog.com` | |
| `FLAGS_POLL_INTERVAL` / `FLAGS_TIMEOUT` | `30s` / `3s` | |

Mobile: `EXPO_PUBLIC_POSTHOG_ENABLED`, `EXPO_PUBLIC_POSTHOG_API_KEY`, `EXPO_PUBLIC_POSTHOG_HOST`.

#### Background jobs — `integrations/BACKGROUND_JOBS.md`

| Variable | Default | Notes |
|---|---|---|
| `JOBS_ENABLED` | `false` | No scheduler when off |
| `JOBS_DEMO_INTERVAL` | `PT1M` | `local` demo cadence |

#### Media upload — `integrations/MEDIA_UPLOAD.md`

| Variable | Default | Notes |
|---|---|---|
| `MEDIA_ENABLED` | `true` | On by default (AI posture); `false` gates every route behind `503 MEDIA_DISABLED`. Cloud deploys without `MEDIA_STORAGE_BUCKET` fail fast in the deploy script |
| `MEDIA_STORAGE_BUCKET` | `gs://{project-id}-media` (Terraform) | **Required when enabled**; local profile uses an in-memory mock bucket |
| `MEDIA_UPLOAD_MODE` | `proxy` | `proxy` (implemented) \| `signed` (config-validated only) |
| `MEDIA_MAX_FILE_SIZE` | `5MB` | `413 MEDIA_TOO_LARGE` |
| `MEDIA_VARIANTS_ENABLED` / `MEDIA_VARIANT_FORMAT` | `true` / `webp` | Re-encoding on upload |
| `MEDIA_DOWNLOAD_URL_TTL` | `15m` | Signed-URL lifetime |
| `MEDIA_MAX_UPLOADS_PER_USER` / `MEDIA_RATE_LIMIT_WINDOW` | `120` / `1h` | Per-user upload quota |
| `MAX_FILE_SIZE` / `MAX_REQUEST_SIZE` | `5MB` / `6MB` | Servlet multipart caps — `413 MEDIA_TOO_LARGE` regardless of `MEDIA_*` values |
| `MEDIA_ANALYSIS_ENABLED` | `false` | Opt-in vision AI; bytes leave the server only when enabled + key set |
| `MEDIA_ANALYSIS_MODEL` / `BASE_URL` / `TIMEOUT` / `MAX_ATTEMPTS` / `STUCK_AFTER` / `POLL_INTERVAL` | `qwen/qwen3.7-flash`, `https://openrouter.ai/api/v1`, `30s`, `4`, `5m`, `PT30S` | Vision analysis reuses `OPENROUTER_API_KEY` (Secret Manager `openrouter-api-key`) |

#### Search (Typesense — **planned, not implemented**) — `integrations/SEARCH.md`

| Variable | Default | Notes |
|---|---|---|
| `SEARCH_ENABLED` / `SEARCH_PROVIDER` / `SEARCH_HOST` / `SEARCH_API_KEY` / `SEARCH_COLLECTION` / `SEARCH_TIMEOUT` | off; `typesense`; `documents`; `3s` | Pattern doc only — no code ships until a product needs it |

### `dev-local`

| Variable | Required | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE=dev-local` | Yes | Must be explicit |
| `GOOGLE_APPLICATION_CREDENTIALS` | Usually | Local ADC file outside the repo, or `gcloud auth application-default login` |
| `GCP_PROJECT_ID={app}-dev` | Yes | Guard against accidental PROD access |
| `OPENAI_API_KEY` | Yes for real AI | Use DEV/provider-limited key |

### `local`

| Variable | Required | Default/notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE=local` | Yes | Explicit opt-in to mocks |
| `FIRESTORE_EMULATOR_HOST` | No | Only when using the emulator instead of the in-memory port |
| `FIREBASE_AUTH_EMULATOR_HOST` | No | Only when testing real emulator-issued tokens |

## Mobile build variables

Use EAS environments so the same public variable names resolve to environment-specific values. Firebase web configuration is public app configuration, not a server secret, but it must still match the intended Firebase project.

| Variable | DEV preview | PROD store build |
|---|---|---|
| `APP_ENV` | `development` | `production` |
| `API_BASE_URL_DEV` / `API_BASE_URL_PROD` | DEV Cloud Run URL | PROD API URL (not `EXPO_PUBLIC_`-prefixed; consumed in `app.config.ts`) |
| `EXPO_PUBLIC_FIREBASE_API_KEY` | DEV Firebase value | PROD Firebase value |
| `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN` | DEV domain | PROD domain |
| `EXPO_PUBLIC_FIREBASE_PROJECT_ID` | `{app}-dev` | `{app}-prod` |
| `EXPO_PUBLIC_APP_CHECK_ENABLED` | `false` in `.env`/local; **baked `true` in `eas.json` `preview`** (smoke profile) | **Baked `true` in `eas.json` `production`** — do not strip; App Check enforcement on Identity Platform would otherwise break native auth at the flip |
| `EXPO_PUBLIC_RECAPTCHA_ENTERPRISE_SITE_KEY` | DEV site key (needed only when the toggle is on; test key OK for the DEV E2E smoke) | PROD site key — **required only when the toggle is on** (`envValidation` fails otherwise) |
| `EAS_PROJECT_ID` | Product EAS project | Same product EAS project |

Target behavior:

- Local development may default to `http://localhost:8080` only when `APP_ENV=development` and a deliberate local flag is set.
- Preview builds must require a DEV HTTPS API URL.
- Production builds must require a PROD HTTPS URL and reject `localhost`, DEV project IDs, or empty values.
- API URL and Firebase project pairing is validated in `app.config.ts` before a build starts.

## CI/CD identities and secrets

### Backend repository

| Name | Scope | Purpose |
|---|---|---|
| `GCP_WORKLOAD_IDENTITY_PROVIDER_DEV` | DEV environment/repository | Short-lived GitHub OIDC authentication |
| `GCP_SERVICE_ACCOUNT_DEV` | DEV environment/repository | Build/deploy DEV |
| `GCP_WORKLOAD_IDENTITY_PROVIDER_PROD` | Protected PROD environment | Short-lived production authentication |
| `GCP_SERVICE_ACCOUNT_PROD` | Protected PROD environment | Deploy approved digest only |

Prefer GitHub environment variables for project IDs, region, and service names; they are configuration, not secrets. Restrict OIDC trust to the exact repository and protected branch/environment.

### Mobile repository

| Name | Scope | Purpose |
|---|---|---|
| `EXPO_TOKEN` | Repository or environment secret | EAS CLI authentication |
| Store credentials | EAS-managed / protected environment | App Store Connect and Play submission |

## Pairing guardrails

- [ ] DEV mobile project ID equals DEV backend Firebase project ID
- [ ] PROD mobile project ID equals PROD backend Firebase project ID
- [ ] PROD builds contain no DEV host/project identifiers
- [ ] Backend service accounts cannot read the other environment's Firestore or secrets
- [ ] Production workflow receives an immutable backend digest, not source to rebuild
- [ ] Store release-candidate build uses production configuration and is tested through store-managed testing
