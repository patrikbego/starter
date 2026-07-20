# New App Workflow

Step-by-step guide to fork the starter kit into a new product. Replace placeholders consistently:

| Placeholder | Example | Description |
|-------------|---------|-------------|
| `{app}` | `myvault` | Short slug (lowercase, no spaces) |
| `{App}` | `MyVault` | Display name |
| `com.starter` | `com.myvault` | Java package / Android package |
| `starter-dev` | `myvault-dev` | GCP + Firebase DEV project |
| `starter-prod` | `myvault-prod` | GCP + Firebase PROD project |

## Prerequisites

- [ ] Google Cloud organization or billing account with project creation rights
- [ ] Firebase console access (linked to GCP projects)
- [ ] GitHub account with repo creation rights
- [ ] Expo account with EAS access
- [ ] OpenRouter account (for AI features)
- [ ] Apple Developer + Google Play Console (when ready for store submit)

## Phase 1: Create repository

### 1.1 Fork or copy the monorepo

```bash
# Option A: GitHub fork (recommended)
# Fork patrikbego/starter → {app} (or {app}-monorepo)

# Option B: Local copy
cp -r ~/develop/starter ~/develop/{app}
cd ~/develop/{app}
git remote set-url origin git@github.com:YOU/{app}.git
```

You keep the monorepo structure:

```text
{app}/
├── docs/
├── starter-backend/    # rename package folder if desired: {app}-backend/
└── starter-mobile/     # rename package folder if desired: {app}-mobile/
```

### 1.2 Rename checklist

Apply these search-replace targets across the **entire monorepo** (`docs/`, `starter-backend/`, `starter-mobile/`):

| Find | Replace with |
|------|--------------|
| `starter` | `{app}` |
| `Starter` | `{App}` |
| `com.starter` | `com.{app}` |
| `starter-dev` | `{app}-dev` |
| `starter-prod` | `{app}-prod` |
| `starter-api-dev` | `{app}-api-dev` |
| `starter-api-prod` | `{app}-api-prod` |
| `starter-api@` | `{app}-api@` |
| `STARTER_CORS` | `{APP}_CORS` (e.g. `MYVAULT_CORS`) |

Files to update (after code exists):

- Backend (`starter-backend/`): `pom.xml`, `application*.yml`, `Dockerfile`, Java package directories
- Mobile (`starter-mobile/`): `app.json`, `app.config.ts`, `eas.json`, bundle identifier, Android package name
- Root: `.github/workflows/*` (path-filtered per package)

## Phase 2: GCP bootstrap

Repeat for **DEV** (`{app}-dev`) and **PROD** (`{app}-prod`).

### 2.1 Create GCP project

```bash
gcloud projects create {app}-dev --name="{App} DEV"
gcloud billing projects link {app}-dev --billing-account=YOUR_BILLING_ACCOUNT
```

### 2.2 Enable APIs and create resources

Follow [starter-backend/scripts/COMMON_GCP_SETUP.md](../starter-backend/scripts/COMMON_GCP_SETUP.md) for each project:

- Enable APIs (Firestore, Cloud Run, Artifact Registry, Secret Manager, IAM Credentials)
- Create Firestore database (native mode, `eur3`)
- Create service account `{app}-api`
- Create Artifact Registry repository
- Configure Workload Identity Federation for GitHub Actions

**Note:** Storage bucket creation is optional for the starter MVP. Add when you implement file upload — see `docs/STORAGE_EXTENSION.md`.

### 2.3 Create secrets

```bash
gcloud config set project {app}-dev

echo -n "sk-or-v1-YOUR_KEY" | gcloud secrets create openai-api-key --data-file=-
echo -n "your-actuator-password" | gcloud secrets create actuator-password --data-file=-
```

Repeat for `{app}-prod` with production values.

## Phase 3: Firebase setup

For each GCP project (`{app}-dev`, `{app}-prod`):

1. Open [Firebase Console](https://console.firebase.google.com/) → Add project → select existing GCP project
2. Enable **Authentication** → Email/Password (add social providers as needed)
3. Register apps:
   - **iOS:** download `GoogleService-Info.plist`
   - **Android:** download `google-services.json`
4. Note the web API key and auth domain for mobile `app.config.ts`

Firebase project ID should match GCP project ID (`{app}-dev` / `{app}-prod`).

## Phase 4: Configure CI/CD

All workflows live at the **monorepo root** in `.github/workflows/` (when implemented). Use path filters so backend and mobile deploy independently:

```yaml
# Example: backend deploy only when starter-backend/ changes
on:
  push:
    paths:
      - 'starter-backend/**'
```

### 4.1 Backend GitHub Actions

Set **repository** secrets (single monorepo on GitHub):

| Secret | Value |
|--------|-------|
| `WIF_PROVIDER` | From WIF setup in COMMON_GCP_SETUP |
| `WIF_SERVICE_ACCOUNT` | `github-actions@{app}-dev.iam.gserviceaccount.com` |

Update workflow files (when implemented) with:

- `GCP_PROJECT_ID`: `{app}-dev` / `{app}-prod`
- Cloud Run service name: `{app}-api-dev` / `{app}-api-prod`
- Region: `europe-west2`

### 4.2 Mobile GitHub Actions

Set repository secret on the same monorepo:

| Secret | Value |
|--------|-------|
| `EXPO_TOKEN` | From [expo.dev/settings/access-tokens](https://expo.dev/settings/access-tokens) |

Create a new EAS project for `{app}-mobile` (or update `extra.eas.projectId` in app config).

Set EAS environment variables per profile:

| Variable | DEV | PROD |
|----------|-----|------|
| `API_BASE_URL_DEV` | DEV Cloud Run URL | — |
| `API_BASE_URL_PROD` | — | PROD Cloud Run URL |
| Firebase public keys | DEV values | PROD values |

## Phase 5: First deploy

### 5.1 Backend DEV

```bash
# After code + workflows exist:
git push origin main
# GitHub Actions deploys to Cloud Run DEV
```

Verify:

```bash
curl https://{app}-api-dev-XXXX.europe-west2.run.app/actuator/health
# Expect: {"status":"UP"}
```

### 5.2 Mobile DEV

```bash
# Trigger EAS build (after eas.json exists)
eas build --profile preview --platform all
```

Install on device, sign in, confirm:

- Home shows user from `GET /api/me`
- Chat sends message and receives AI reply from `POST /api/chat`

### 5.3 PROD promote

Only after DEV validation:

1. **Backend:** Run manual `deploy-prod` workflow (same Docker image tag as DEV)
2. **Mobile:** Run manual `eas-submit-prod` workflow with the **same EAS build ID** tested in DEV

Record for each release: commit SHA, image tag / build ID, submitter, timestamp.

## Phase 6: Customize domain logic

Keep infra stable; add product features in these locations:

### Backend

| Layer | Add here |
|-------|----------|
| Domain models | `domain/` — new entities |
| Ports | `ports/` — new interfaces |
| Adapters | `adapters/` — Firestore, external APIs |
| Business logic | `application/` — new services |
| REST API | `api/` — new controllers |

Do **not** change: `SecurityConfig` auth flow, profile structure, CI/CD workflows (unless adding new secrets).

### Mobile

| Layer | Add here |
|-------|----------|
| Screens | `app/` — new routes |
| Feature logic | `src/features/{feature}/` |
| API calls | `src/adapters/` or feature hooks |
| Ports | `src/ports/` — new interfaces |

Do **not** change: auth gate pattern, env config structure, EAS profile names.

## Phase 7: Optional extensions

| Need | Action |
|------|--------|
| File uploads | Implement `STORAGE_EXTENSION.md`, add GCS bucket in GCP setup |
| Subscriptions | Add RevenueCat (see docsera reference), webhook endpoint |
| Background jobs | Add Cloud Tasks + worker endpoints |
| Search / RAG | Add embedding port, vector search, chat context |

## Troubleshooting

| Symptom | Check |
|---------|-------|
| 401 on `/api/me` | Firebase project mismatch between mobile and backend |
| CORS errors | `STARTER_CORS_ALLOWED_ORIGINS` / `{APP}_CORS_ALLOWED_ORIGINS` on backend |
| AI chat fails | `openai-api-key` secret in Secret Manager; OpenRouter credits |
| Mobile can't reach API | `API_BASE_URL_*` in EAS env; Cloud Run URL correct |
| Firestore permission denied | Service account has `roles/datastore.user` |

## Related docs

- [ENVIRONMENT_MATRIX.md](./ENVIRONMENT_MATRIX.md) — full variable reference
- [ARCHITECTURE_OVERVIEW.md](./ARCHITECTURE_OVERVIEW.md) — system design
- [starter-backend/docs/cicd_deployment_plan.md](../starter-backend/docs/cicd_deployment_plan.md) — backend deploy detail
- [starter-mobile/docs/mobile_cicd_deployment_plan.md](../starter-mobile/docs/mobile_cicd_deployment_plan.md) — mobile deploy detail
