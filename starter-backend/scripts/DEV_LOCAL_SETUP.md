# DEV Local Profile Setup Guide

Setup for the `dev-local` profile: real DEV Firestore and OpenRouter from your IDE, with optional Firebase Auth emulator.

## What dev-local uses

| Service | Implementation |
|---------|----------------|
| Firestore | Real (`starter-dev`) |
| Firebase Auth | Emulator (`localhost:9099`) or real |
| AI / OpenRouter | Real API |
| Cloud Storage | Not used in starter MVP |

---

## One-time setup

### 1. Complete DEV GCP setup

Follow [COMMON_GCP_SETUP.md](./COMMON_GCP_SETUP.md) for `starter-dev`.

### 2. Download service account key

```bash
mkdir -p ~/.gcp
gcloud iam service-accounts keys create ~/.gcp/starter-dev-sa.json \
  --iam-account=starter-api@starter-dev.iam.gserviceaccount.com \
  --project=starter-dev
```

**Security:** Never commit this file. Add to `.gitignore`.

---

## Daily workflow

### Step 1: Start Firebase Auth emulator (recommended)

```bash
firebase emulators:start --project starter-dev --only auth
```

### Step 2: Set environment variables

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$HOME/.gcp/starter-dev-sa.json"
export GCP_PROJECT_ID="starter-dev"
export FIREBASE_AUTH_EMULATOR_HOST="localhost:9099"
export OPENAI_API_KEY="sk-or-v1-..."
```

### Step 3: Run application

**IntelliJ:**

- VM options: `-Dspring.profiles.active=dev-local`
- Environment variables: exports above

**Terminal:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev-local
```

### Step 4: Test endpoints

```bash
# Use token from Firebase emulator or test script
curl -H "Authorization: Bearer test-token" http://localhost:8080/api/me
curl -X POST -H "Authorization: Bearer test-token" \
  -H "Content-Type: application/json" \
  -d '{"message":"hello"}' \
  http://localhost:8080/api/chat
```

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Firestore permission denied | Check SA has `roles/datastore.user` |
| Firebase auth fails | Confirm emulator running or use real Firebase |
| OpenRouter 401 | Verify `OPENAI_API_KEY` |

## Related docs

- [INTEGRATION_ENV_CONFIG.md](./INTEGRATION_ENV_CONFIG.md)
- [DEV_SETUP.md](./DEV_SETUP.md)
