# Run — DEV (Cloud)

Use the backend API running in the **shared DEV environment** on Google Cloud. This is what the team tests against daily before releasing to production.

**Who is this for?** Developers verifying a deploy, QA testers, or anyone running the mobile app against the real cloud backend.

**Prerequisites:** DEV infrastructure must already be set up (usually by a team lead). See [One-time infrastructure setup](#one-time-infrastructure-setup) below.

---

## Before you start

### What "DEV cloud" means

The API runs on **Google Cloud Run** — a managed server in the cloud — not on your Mac.

| Setting | Value |
|---------|-------|
| Google Cloud project | `starter-dev` |
| Service name | `starter-api-dev` |
| Region | `europe-west2` |
| URL looks like | `https://starter-api-dev-XXXX.europe-west2.run.app` |
| Data | Real Firestore, real Firebase Auth, real AI |
| Purpose | Team testing — safe to break things |

### What you need

- The DEV API URL (ask your team lead, or find it below)
- For authenticated tests: a Firebase user in the `starter-dev` project
- For deploying: access to the GitHub repo and GCP configured (team lead)

---

## Step 1 — Get the DEV API URL

Ask your team lead for the URL, or find it yourself if you have `gcloud` access:

```bash
gcloud run services describe starter-api-dev \
  --region europe-west2 \
  --project starter-dev \
  --format 'value(status.url)'
```

Example output:

```text
https://starter-api-dev-abc123.europe-west2.run.app
```

Save this URL — the mobile app needs it as `API_BASE_URL_DEV`.

---

## Step 2 — Verify the API is healthy

No login required for this check:

```bash
curl https://starter-api-dev-XXXX.europe-west2.run.app/actuator/health
```

**Expected response:**

```json
{"status":"UP"}
```

**If this fails:**
- The service may not be deployed yet — see [How to deploy to DEV](#how-to-deploy-to-dev)
- Check you have the correct URL
- Ask your team lead if the DEV environment exists

---

## Step 3 — Test login and API (optional)

Authenticated endpoints need a **Firebase ID token** from the `starter-dev` project.

The easiest way to test is through the **mobile app** — see [../../starter-mobile/run/run-dev.md](../../starter-mobile/run/run-dev.md).

For manual API testing with curl, you need a token from a logged-in Firebase user. Ask your team lead for a test account, or create one in [Firebase Console](https://console.firebase.google.com) → `starter-dev` → Authentication → Add user.

```bash
TOKEN="paste-firebase-id-token-here"

# Get your profile
curl -H "Authorization: Bearer $TOKEN" \
  https://starter-api-dev-XXXX.europe-west2.run.app/api/me

# Send a chat message
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"hello"}' \
  https://starter-api-dev-XXXX.europe-west2.run.app/api/chat
```

**You are done when** `/api/me` returns your user JSON and `/api/chat` returns an AI reply.

---

## How to deploy to DEV

### Automatic deploy (normal workflow)

Every time code is merged to the `main` branch and it changes files in `starter-backend/`, GitHub Actions automatically:

1. Builds the Java project
2. Creates a Docker image
3. Pushes it to Google Artifact Registry
4. Deploys to Cloud Run DEV

**You do not need to do anything** — just merge your PR to `main`.

To watch a deploy:

1. Open the GitHub repo in your browser
2. Go to **Actions** tab
3. Click **Deploy Backend to DEV**
4. Wait for green checkmark

### Manual re-deploy (without code changes)

Useful after rotating secrets or if you need a fresh deploy:

1. Open GitHub → **Actions** → **Deploy Backend to DEV**
2. Click **Run workflow** (dropdown on the right)
3. Click the green **Run workflow** button
4. Wait for completion, then repeat Step 2 above to verify health

### Manual deploy from your Mac (advanced)

Only use this if CI is broken or you are setting up infrastructure for the first time. Prefer GitHub Actions for normal work.

```bash
cd starter-backend

# Build the project
./mvnw clean package -DskipTests -B

# Log in to Google Cloud
gcloud auth login
gcloud config set project starter-dev
gcloud auth configure-docker europe-west2-docker.pkg.dev

# Build and push Docker image
IMAGE="europe-west2-docker.pkg.dev/starter-dev/starter/starter-api:manual-$(date +%Y%m%d)"
docker build -t "$IMAGE" .
docker push "$IMAGE"

# Deploy to Cloud Run
gcloud run deploy starter-api-dev \
  --image "$IMAGE" \
  --region europe-west2 \
  --platform managed \
  --allow-unauthenticated \
  --service-account starter-api@starter-dev.iam.gserviceaccount.com \
  --set-env-vars "SPRING_PROFILES_ACTIVE=dev,GCP_PROJECT_ID=starter-dev,STARTER_CORS_ALLOWED_ORIGINS=*" \
  --set-secrets "OPENAI_API_KEY=openai-api-key:latest,ACTUATOR_PASSWORD=actuator-password:latest"
```

---

## One-time infrastructure setup

**Skip this section** if DEV is already working. This is for team leads setting up a new project.

1. Create GCP project `starter-dev` — [../scripts/DEV_SETUP.md](../scripts/DEV_SETUP.md)
2. Run common GCP setup — [../scripts/COMMON_GCP_SETUP.md](../scripts/COMMON_GCP_SETUP.md)
3. Create secrets in Secret Manager:
   - `openai-api-key` — your OpenRouter API key
   - `actuator-password` — admin password for monitoring endpoints
4. Configure GitHub secrets in the monorepo:

| GitHub secret | What it is |
|---------------|------------|
| `GCP_WORKLOAD_IDENTITY_PROVIDER_DEV` | From GCP Workload Identity setup |
| `GCP_SERVICE_ACCOUNT_DEV` | `github-actions@starter-dev.iam.gserviceaccount.com` |

5. Merge to `main` to trigger first deploy
6. Share the Cloud Run URL with the team

---

## DEV testing checklist

Complete this before moving to PROD:

- [ ] Health endpoint returns `{"status":"UP"}`
- [ ] Can sign in via mobile app with a DEV Firebase user
- [ ] Home screen shows user profile
- [ ] Chat returns a real AI reply
- [ ] No DEV credentials mixed with PROD

---

## Connect the mobile app

Mobile DEV builds must point to this Cloud Run URL:

```bash
API_BASE_URL_DEV=https://starter-api-dev-XXXX.europe-west2.run.app
EXPO_PUBLIC_FIREBASE_PROJECT_ID=starter-dev
```

Full mobile guide: [../../starter-mobile/run/run-dev.md](../../starter-mobile/run/run-dev.md)

---

## Troubleshooting

| Problem | What to do |
|---------|------------|
| Health check fails | Check GitHub Actions deploy succeeded; ask team lead |
| 401 on `/api/me` | Firebase project mismatch — mobile must use `starter-dev` |
| Chat returns 502 | OpenRouter key missing or invalid in Secret Manager |
| Deploy workflow fails | Check GitHub secrets and GCP permissions with team lead |
| CORS errors from mobile | Backend `STARTER_CORS_ALLOWED_ORIGINS` should be `*` in DEV |

---

## What comes next?

| When you are ready to… | Read |
|------------------------|------|
| Release to production | [run-prod.md](./run-prod.md) |
| Build mobile test app | [../../starter-mobile/run/run-dev.md](../../starter-mobile/run/run-dev.md) |
| Run backend on your Mac instead | [run-local.md](./run-local.md) |
