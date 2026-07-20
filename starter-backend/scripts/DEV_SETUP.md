# DEV Environment Setup

One-time setup for the `starter-dev` GCP project and Cloud Run DEV deployment.

## Prerequisites

- GCP billing enabled
- `gcloud` CLI authenticated
- GitHub monorepo (`starter` — contains `starter-backend/` and `starter-mobile/`)

## Steps

### 1. Create GCP project

```bash
gcloud projects create starter-dev --name="Starter DEV"
gcloud billing projects link starter-dev --billing-account=YOUR_BILLING_ACCOUNT
```

### 2. Run common GCP setup

Follow [COMMON_GCP_SETUP.md](./COMMON_GCP_SETUP.md) with `[PROJECT_ID]=starter-dev`.

### 3. Configure GitHub Actions secrets

In the **starter monorepo** GitHub repository (root `.github/workflows/`):

| Secret | Value |
|--------|-------|
| `WIF_PROVIDER` | From WIF setup |
| `WIF_SERVICE_ACCOUNT` | `github-actions@starter-dev.iam.gserviceaccount.com` |
| `GCP_PROJECT_ID` | `starter-dev` |

### 4. First deployment

After application code and `deploy-dev.yml` exist:

```bash
git push origin main
```

Verify:

```bash
curl https://starter-api-dev-XXXX.europe-west2.run.app/actuator/health
```

### 5. Note Cloud Run URL

Record the DEV API URL for mobile `API_BASE_URL_DEV` in EAS environment variables.

## DEV characteristics

- Auto-deploy on merge to `main`
- CORS may allow `*`
- Test/synthetic data acceptable
- OpenRouter uses same key as local (monitor usage)

## Related docs

- [PROD_SETUP.md](./PROD_SETUP.md)
- [DEV_LOCAL_SETUP.md](./DEV_LOCAL_SETUP.md)
- [../docs/cicd_deployment_plan.md](../docs/cicd_deployment_plan.md)
