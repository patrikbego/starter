# PROD Environment Setup

One-time setup for the `starter-prod` GCP project and Cloud Run PROD deployment.

## Prerequisites

- DEV environment validated end-to-end (API + mobile)
- GCP billing enabled
- `gcloud` CLI authenticated

## Steps

### 1. Create GCP project

```bash
gcloud projects create starter-prod --name="Starter PROD"
gcloud billing projects link starter-prod --billing-account=YOUR_BILLING_ACCOUNT
```

### 2. Run common GCP setup

Follow [COMMON_GCP_SETUP.md](./COMMON_GCP_SETUP.md) with `[PROJECT_ID]=starter-prod`.

**PROD-specific:**

- Enable Firestore delete protection
- Use strong `actuator-password`
- Restrict CORS origins in Cloud Run env

### 3. Configure GitHub Actions

Add PROD secrets or use GitHub `environment: production`:

| Secret | Value |
|--------|-------|
| `WIF_PROVIDER` | PROD WIF provider (separate pool recommended) |
| `WIF_SERVICE_ACCOUNT` | `github-actions@starter-prod.iam.gserviceaccount.com` |
| `GCP_PROJECT_ID` | `starter-prod` |

### 4. Firebase PROD

1. Link `starter-prod` in Firebase Console
2. Enable Authentication (same providers as DEV)
3. Register production iOS/Android apps
4. Update mobile EAS `production` profile with PROD Firebase keys

### 5. First PROD deployment

**Only after DEV validation.**

Run manual `deploy-prod.yml` workflow with confirmation input.

Verify:

```bash
curl https://starter-api-prod-XXXX.europe-west2.run.app/actuator/health
```

### 6. Mobile PROD submit

Use the **same EAS build ID** tested against DEV API, submitted via manual `eas-submit-prod` workflow.

## PROD checklist

- [ ] Separate Firebase project from DEV
- [ ] CORS restricted to production origins
- [ ] Secrets in Secret Manager (not env vars in YAML)
- [ ] Cloud Run max instances appropriate for expected load
- [ ] Monitoring alerts configured (optional MVP)
- [ ] Rollback procedure documented and tested

## Related docs

- [DEV_SETUP.md](./DEV_SETUP.md)
- [../docs/cicd_deployment_plan.md](../docs/cicd_deployment_plan.md)
- [../../docs/NEW_APP_WORKFLOW.md](../../docs/NEW_APP_WORKFLOW.md)
