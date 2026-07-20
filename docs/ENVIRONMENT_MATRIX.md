# Environment Matrix

DEV/PROD variable mapping across backend, mobile, GCP, Firebase, and CI. Replace `starter` with your app slug (`{app}`) when forking.

## GCP projects

| | DEV | PROD |
|---|-----|------|
| GCP project ID | `starter-dev` | `starter-prod` |
| Firebase project | Same as GCP project | Same as GCP project |
| Cloud Run service | `starter-api-dev` | `starter-api-prod` |
| Artifact Registry repo | `europe-west2-docker.pkg.dev/starter-dev/starter-api` | `europe-west2-docker.pkg.dev/starter-prod/starter-api` |
| Firestore database | Native mode, `eur3` | Native mode, `eur3` |
| Service account | `starter-api@starter-dev.iam.gserviceaccount.com` | `starter-api@starter-prod.iam.gserviceaccount.com` |
| Region | `europe-west2` | `europe-west2` |

## Backend environment variables

### Cloud Run (dev / prod profiles)

| Variable | DEV example | PROD example | Source |
|----------|-------------|--------------|--------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` | Cloud Run env |
| `GCP_PROJECT_ID` | `starter-dev` | `starter-prod` | Cloud Run env |
| `STARTER_CORS_ALLOWED_ORIGINS` | `*` | `https://yourdomain.com` | Cloud Run env |
| `OPENAI_API_KEY` | — | — | Secret Manager → `openai-api-key` |
| `ACTUATOR_PASSWORD` | — | — | Secret Manager → `actuator-password` |

### Local development (dev-local profile)

| Variable | Required | Example |
|----------|----------|---------|
| `GOOGLE_APPLICATION_CREDENTIALS` | Yes | `~/.gcp/starter-dev-sa.json` |
| `GCP_PROJECT_ID` | No (defaults to `starter-dev`) | `starter-dev` |
| `FIREBASE_AUTH_EMULATOR_HOST` | No | `localhost:9099` |
| `OPENAI_API_KEY` | Yes | OpenRouter key (`sk-or-v1-...`) |

### Local development (local profile)

| Variable | Required | Default |
|----------|----------|---------|
| `FIRESTORE_EMULATOR_HOST` | No | `localhost:8080` |
| `FIREBASE_AUTH_EMULATOR_HOST` | No | `localhost:9099` |
| `LOCAL_AI_URL` | No | `http://localhost:8081/v1` |

## Mobile environment variables

Set via EAS build profile `env` block or `app.config.ts` at build time.

| Variable | DEV (`APP_ENV=development`) | PROD (`APP_ENV=production`) |
|----------|----------------------------|----------------------------|
| `APP_ENV` | `development` | `production` |
| `API_BASE_URL_DEV` | `https://starter-api-dev-XXXX.europe-west2.run.app` | — |
| `API_BASE_URL_PROD` | — | `https://starter-api-prod-XXXX.europe-west2.run.app` |
| `EXPO_PUBLIC_FIREBASE_API_KEY` | DEV Firebase web API key | PROD Firebase web API key |
| `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN` | `starter-dev.firebaseapp.com` | `starter-prod.firebaseapp.com` |
| `EXPO_PUBLIC_FIREBASE_PROJECT_ID` | `starter-dev` | `starter-prod` |

Runtime access: `Constants.expoConfig?.extra` (after `app.config.ts` is implemented).

## CI/CD secrets

### GitHub Actions — backend repo

| Secret | Used by | Description |
|--------|---------|-------------|
| `WIF_PROVIDER` | deploy workflows | Workload Identity Federation provider |
| `WIF_SERVICE_ACCOUNT` | deploy workflows | GCP SA for CI deploy |
| `GCP_PROJECT_ID` | deploy-dev | `starter-dev` or `starter-prod` |

### GitHub Actions — mobile repo

| Secret | Used by | Description |
|--------|---------|-------------|
| `EXPO_TOKEN` | EAS build/submit | Expo account token |

### GCP Secret Manager (per project)

| Secret name | Used by | Description |
|-------------|---------|-------------|
| `openai-api-key` | Cloud Run | OpenRouter API key |
| `actuator-password` | Cloud Run | Admin actuator HTTP Basic password |

## EAS build profiles (planned)

| Profile | `APP_ENV` | API URL var | Distribution |
|---------|-----------|-------------|--------------|
| `development` | `development` | `API_BASE_URL_DEV` | Internal dev client |
| `preview` | `development` | `API_BASE_URL_DEV` | Internal QA |
| `production` | `production` | `API_BASE_URL_PROD` | Store submit |

## Pairing checklist

Before testing end-to-end, confirm:

- [ ] Mobile `APP_ENV=development` points to DEV Cloud Run URL
- [ ] Mobile Firebase config matches `starter-dev` Firebase project
- [ ] Backend `dev` profile uses same Firebase project as mobile DEV build
- [ ] PROD mobile build uses PROD API URL and PROD Firebase — never cross-wired

## Related docs

- [NEW_APP_WORKFLOW.md](./NEW_APP_WORKFLOW.md) — setup steps
- [starter-backend/scripts/INTEGRATION_ENV_CONFIG.md](../starter-backend/scripts/INTEGRATION_ENV_CONFIG.md) — full backend env reference
- [starter-mobile/docs/BACKEND_INTEGRATION.md](../starter-mobile/docs/BACKEND_INTEGRATION.md) — mobile API client config
