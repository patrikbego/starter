# Common GCP Infrastructure Setup Guide

Shared infrastructure setup for `starter-dev` and `starter-prod` environments.

Replace `[PROJECT_ID]` with `starter-dev` or `starter-prod`.

## 1. Enable Required GCP APIs

```bash
gcloud config set project [PROJECT_ID]

gcloud services enable firestore.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable secretmanager.googleapis.com
gcloud services enable iamcredentials.googleapis.com
gcloud services enable cloudbuild.googleapis.com
```

For file upload extension, also enable:

```bash
gcloud services enable storage.googleapis.com
```

## 2. Create Firestore Database

Native mode, Europe:

```bash
gcloud firestore databases create \
  --project=[PROJECT_ID] \
  --location=eur3 \
  --type=firestore-native
```

Add `--delete-protection` for PROD.

Or via Console: https://console.cloud.google.com/datastore/setup?project=[PROJECT_ID]

## 3. Create Service Account

```bash
gcloud iam service-accounts create starter-api \
  --display-name="Starter API Service Account" \
  --project=[PROJECT_ID]

# Firestore
gcloud projects add-iam-policy-binding [PROJECT_ID] \
  --member="serviceAccount:starter-api@[PROJECT_ID].iam.gserviceaccount.com" \
  --role="roles/datastore.user"

# Secret Manager
gcloud projects add-iam-policy-binding [PROJECT_ID] \
  --member="serviceAccount:starter-api@[PROJECT_ID].iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Cloud Run runtime (service account used by Cloud Run service)
gcloud iam service-accounts add-iam-policy-binding starter-api@[PROJECT_ID].iam.gserviceaccount.com \
  --project=[PROJECT_ID] \
  --member="serviceAccount:starter-api@[PROJECT_ID].iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"
```

For GCS extension, add `roles/storage.objectAdmin` and `roles/iam.serviceAccountTokenCreator`.

## 4. Create Artifact Registry

```bash
gcloud artifacts repositories create starter-api \
  --repository-format=docker \
  --location=europe-west2 \
  --project=[PROJECT_ID]

gcloud projects add-iam-policy-binding [PROJECT_ID] \
  --member="serviceAccount:starter-api@[PROJECT_ID].iam.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"
```

## 5. Create Secrets

```bash
echo -n "sk-or-v1-YOUR_OPENROUTER_KEY" | gcloud secrets create openai-api-key --data-file=-
echo -n "your-secure-actuator-password" | gcloud secrets create actuator-password --data-file=-

# Grant Cloud Run SA access
gcloud secrets add-iam-policy-binding openai-api-key \
  --member="serviceAccount:starter-api@[PROJECT_ID].iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor" \
  --project=[PROJECT_ID]

gcloud secrets add-iam-policy-binding actuator-password \
  --member="serviceAccount:starter-api@[PROJECT_ID].iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor" \
  --project=[PROJECT_ID]
```

## 6. Workload Identity Federation (GitHub Actions)

Create a pool and provider for GitHub OIDC so CI can deploy without long-lived keys.

```bash
# Create pool
gcloud iam workload-identity-pools create github-pool \
  --location=global \
  --project=[PROJECT_ID]

# Create provider (replace GITHUB_ORG and GITHUB_REPO)
gcloud iam workload-identity-pools providers create-oidc github-provider \
  --location=global \
  --workload-identity-pool=github-pool \
  --issuer-uri=https://token.actions.githubusercontent.com \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository=='GITHUB_ORG/starter-backend'" \
  --project=[PROJECT_ID]

# Create CI service account
gcloud iam service-accounts create github-actions \
  --display-name="GitHub Actions" \
  --project=[PROJECT_ID]

# Allow GitHub repo to impersonate SA
gcloud iam service-accounts add-iam-policy-binding github-actions@[PROJECT_ID].iam.gserviceaccount.com \
  --project=[PROJECT_ID] \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool/attribute.repository/GITHUB_ORG/starter-backend"

# Grant deploy permissions
gcloud projects add-iam-policy-binding [PROJECT_ID] \
  --member="serviceAccount:github-actions@[PROJECT_ID].iam.gserviceaccount.com" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding [PROJECT_ID] \
  --member="serviceAccount:github-actions@[PROJECT_ID].iam.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"

gcloud projects add-iam-policy-binding [PROJECT_ID] \
  --member="serviceAccount:github-actions@[PROJECT_ID].iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"
```

Record `WIF_PROVIDER` and `WIF_SERVICE_ACCOUNT` for GitHub secrets.

## 7. Link Firebase

1. Firebase Console → Add project → select existing GCP project `[PROJECT_ID]`
2. Enable Authentication → Email/Password
3. Download client config for mobile apps

## 8. Optional: Cloud Storage Bucket

Only if implementing `STORAGE_EXTENSION.md`:

```bash
gsutil mb -p [PROJECT_ID] -c standard -l EU gs://[PROJECT_ID]-documents/
gsutil uniformbucketlevelaccess set on gs://[PROJECT_ID]-documents/
gsutil pap set enforced gs://[PROJECT_ID]-documents/
```

## Related docs

- [DEV_SETUP.md](./DEV_SETUP.md)
- [PROD_SETUP.md](./PROD_SETUP.md)
- [DEV_LOCAL_SETUP.md](./DEV_LOCAL_SETUP.md)
- [../docs/cicd_deployment_plan.md](../docs/cicd_deployment_plan.md)
