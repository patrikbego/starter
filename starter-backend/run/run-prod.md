# Run — PROD (Cloud)

Deploy the backend API to **production** — the live environment used by real users.

**Who is this for?** Team leads or release managers. New developers should read this to understand the process but usually do not perform PROD deploys.

**Critical rule:** Only deploy to PROD after DEV testing is fully complete (backend + mobile).

---

## Before you start

### What "PROD cloud" means

| Setting | Value |
|---------|-------|
| Google Cloud project | `starter-prod` |
| Service name | `starter-api-prod` |
| Region | `europe-west2` |
| URL looks like | `https://starter-api-prod-XXXX.europe-west2.run.app` |
| Data | Real production Firestore and Firebase |
| Deploy trigger | **Manual only** — never automatic |

### The release flow (read this)

```text
1. Code merged to main        →  auto-deploys to DEV
2. Team tests on DEV          →  mobile + API verified
3. Manual PROD deploy         →  you are here
4. Submit mobile to stores    →  see mobile run-prod guide
```

**Never skip step 2.**

---

## Pre-deploy checklist

Complete every item before deploying:

- [ ] DEV health check passes: `curl .../actuator/health` → `{"status":"UP"}`
- [ ] DEV mobile app tested: login, profile, chat all work
- [ ] You know the **commit SHA** being deployed (check GitHub Actions DEV deploy)
- [ ] PROD secrets exist and are **different** from DEV (`openai-api-key`, `actuator-password`)
- [ ] CORS origins updated for your production domain (not `*`)
- [ ] Firestore delete protection enabled on `starter-prod`
- [ ] Mobile EAS `production` profile has PROD API URL and PROD Firebase keys
- [ ] Team is aware a production deploy is happening

---

## Step 1 — Deploy to PROD via GitHub Actions

This is the recommended and only normal way to deploy.

1. Open the GitHub repo in your browser
2. Go to **Actions** tab
3. In the left sidebar, click **Deploy Backend to PROD**
4. Click **Run workflow** (dropdown on the right)
5. In the `confirm_deploy` field, type exactly: **`deploy`**
6. Click the green **Run workflow** button
7. Wait for the workflow to finish (green checkmark)

**If you type anything other than `deploy`, the workflow will not deploy.** This is intentional — it prevents accidental production releases.

### Required GitHub secrets (team lead sets these once)

| Secret | Purpose |
|--------|---------|
| `GCP_WORKLOAD_IDENTITY_PROVIDER_PROD` | GCP auth for CI |
| `GCP_SERVICE_ACCOUNT_PROD` | Deploy service account |

---

## Step 2 — Verify PROD is healthy

```bash
# Get the URL (or ask team lead)
gcloud run services describe starter-api-prod \
  --region europe-west2 \
  --project starter-prod \
  --format 'value(status.url)'

# Health check — no login required
curl https://starter-api-prod-XXXX.europe-west2.run.app/actuator/health
```

**Expected:** `{"status":"UP"}`

---

## Step 3 — Test with a PROD Firebase user

Use a **production** Firebase account — never a DEV test user.

```bash
TOKEN="prod-firebase-id-token"

curl -H "Authorization: Bearer $TOKEN" \
  https://starter-api-prod-XXXX.europe-west2.run.app/api/me
```

**You are done when** health passes and `/api/me` returns a valid user.

---

## Step 4 — Release the mobile app

Backend PROD alone is not enough — users need the mobile app too.

Follow [../../starter-mobile/run/run-prod.md](../../starter-mobile/run/run-prod.md) to submit the **same EAS build ID** that was tested in DEV (if the API contract did not change).

---

## Record the release

Always log every production deploy:

```text
Date:           2026-07-20
Commit SHA:     abc123def
Docker image:   europe-west2-docker.pkg.dev/starter-prod/starter/starter-api:abc123def
Deployed by:    your-name
DEV tested:     yes — build ID / date
Notes:          initial PROD release
```

---

## Rollback (if something goes wrong)

Cloud Run keeps previous versions. To roll back:

```bash
# 1. List recent revisions
gcloud run revisions list \
  --service starter-api-prod \
  --region europe-west2 \
  --project starter-prod

# 2. Route all traffic to a previous revision
gcloud run services update-traffic starter-api-prod \
  --to-revisions REVISION_NAME=100 \
  --region europe-west2 \
  --project starter-prod
```

Replace `REVISION_NAME` with the revision from step 1 (the one before the broken deploy).

Also roll back the mobile app if needed — see [../../starter-mobile/run/run-prod.md](../../starter-mobile/run/run-prod.md).

---

## One-time PROD infrastructure setup

**Skip if PROD already exists.** For team leads setting up a new project:

1. Create GCP project `starter-prod` — [../scripts/PROD_SETUP.md](../scripts/PROD_SETUP.md)
2. Run common GCP setup — [../scripts/COMMON_GCP_SETUP.md](../scripts/COMMON_GCP_SETUP.md)
3. Enable Firestore delete protection
4. Create PROD secrets (different values from DEV!)
5. Set up Firebase `starter-prod` with auth
6. Configure GitHub PROD secrets
7. Perform first manual deploy using Step 1 above

---

## Troubleshooting

| Problem | What to do |
|---------|------------|
| Workflow did not deploy | Confirm you typed `deploy` exactly in `confirm_deploy` |
| Health check fails after deploy | Check GitHub Actions logs; verify secrets in Secret Manager |
| 401 for all users | Firebase project mismatch — PROD mobile must use `starter-prod` |
| Chat 502 in PROD | Check `openai-api-key` secret in `starter-prod` Secret Manager |
| Accidental deploy | Roll back immediately using steps above |

---

## What comes next?

| Task | Guide |
|------|-------|
| Submit mobile to app stores | [../../starter-mobile/run/run-prod.md](../../starter-mobile/run/run-prod.md) |
| Set up a new product from scratch | [../../docs/NEW_APP_WORKFLOW.md](../../docs/NEW_APP_WORKFLOW.md) |
| All environment variables | [../../docs/ENVIRONMENT_MATRIX.md](../../docs/ENVIRONMENT_MATRIX.md) |
