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
| `{APP}_CORS_ALLOWED_ORIGINS` | `https://admin.example.com` | Cloud Run environment |
| `OPENAI_API_KEY` | secret value | Secret Manager |
| `AI_MODEL` | provider model identifier | Environment/managed config |
| `ACTUATOR_PASSWORD` | secret value | Secret Manager, only if Basic-auth actuator is retained |
| `AI_REQUEST_TIMEOUT` | `30s` | Environment — provider call bound |
| `AI_MAX_INPUT_CHARS` | `4000` | Environment — input cap before provider call |
| `AI_MAX_REQUESTS_PER_USER` | `120` | Environment — per-user quota window limit |
| `AI_RATE_LIMIT_WINDOW` | `1h` | Environment — quota window (`Duration` syntax) |

AI guardrails fail startup when missing in `dev`/`prod` (same fail-closed rule as
the other required variables). Quota exhaustion returns `429` with a
`Retry-After` header. The per-instance nature of the default quota store and
other limitations are tracked in `./REVIEW_FINDINGS.md` (caveat C1).

Recommended operational variables:

| Variable | Purpose |
|---|---|
| `AI_RATE_LIMIT_WINDOW` | Length of the per-user quota window (now required in cloud profiles) |
| `LOG_FORMAT` | Human local logs versus structured cloud logs |

Optional App Check extension (off by default; see `integrations/APP_CHECK.md`):

| Variable | Source | Notes |
|---|---|---|
| `APP_CHECK_ENABLED_DEV` / `APP_CHECK_ENABLED_PROD` | GitHub repo variables | Set `true` to turn enforcement on for that environment |
| `APP_CHECK_PROJECT_NUMBER_DEV` / `APP_CHECK_PROJECT_NUMBER_PROD` | GitHub repo variables | The Firebase project number (token `aud`); not a secret |
| `APP_CHECK_JWKS_URI` | Environment | Optional override; defaults to the Firebase App Check JWKS |

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
| `EXPO_PUBLIC_API_BASE_URL` | DEV Cloud Run URL | PROD API URL |
| `EXPO_PUBLIC_FIREBASE_API_KEY` | DEV Firebase value | PROD Firebase value |
| `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN` | DEV domain | PROD domain |
| `EXPO_PUBLIC_FIREBASE_PROJECT_ID` | `{app}-dev` | `{app}-prod` |
| `EXPO_PUBLIC_APP_CHECK_ENABLED` | `false` (default) | `false` — set `true` only when the product enables App Check |
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
