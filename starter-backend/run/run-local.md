# Run — Local (macOS)

Run the backend API on your Mac. **No Google Cloud account required** for the recommended beginner path.

**Time needed:** ~10 minutes first time  
**Difficulty:** Easy (Option A) / Medium (Option B)

---

## Before you start

### What you will have at the end

A REST API running at `http://localhost:8080` that responds to health checks and test requests.

### What you need installed

Open Terminal and check each item:

```bash
# 1. Java 21 or newer (required)
java -version
# Expected: openjdk version "21" or higher

# 2. Git (required — to clone the repo)
git --version
```

If Java is missing, install it:

```bash
brew install openjdk@21
```

Maven is included in the project (`./mvnw`) — you do not need to install it separately.

### Get the code

If you have not cloned the repo yet:

```bash
git clone <your-repo-url> starter
cd starter/starter-backend
```

All commands below assume you are in the `starter-backend/` folder.

---

## Choose your path

| Path | Cloud accounts needed? | Best for |
|------|------------------------|----------|
| **Option A** (recommended first time) | No | Learning the project, offline work |
| **Option B** | Yes (GCP + OpenRouter) | Testing with real Firestore data |

**New to the project? Start with Option A.**

---

## Option A: Fully offline (recommended for beginners)

This mode uses **fake in-memory services**. No Firebase, no Google Cloud, no API keys.

### Step 1 — Start the server

```bash
cd starter-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Wait until you see a line like:

```text
Started StarterApplication in X seconds
```

The server is now running on port **8080**. Leave this terminal open.

### Step 2 — Run the automated smoke test

Open a **second terminal**:

```bash
cd starter-backend
./scripts/test/test-local.sh
```

**You are done when** you see:

```text
Local Profile Tests Complete!
```

If the test fails with "is the server running?", go back to Step 1 and confirm the server started without errors.

### Step 3 — Try requests manually (optional)

```bash
# Health check — no login required
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# User profile — any fake token works in local mode
curl -H "Authorization: Bearer test-token" http://localhost:8080/api/me
# Expected: JSON with id, email, displayName

# AI chat — returns an echo, not real AI
curl -X POST -H "Authorization: Bearer test-token" \
  -H "Content-Type: application/json" \
  -d '{"message":"hello"}' \
  http://localhost:8080/api/chat
# Expected: {"reply":"Echo: hello","sessionId":"..."}
```

### Step 4 — Stop the server

In the first terminal, press `Ctrl+C`.

---

## Option B: Real cloud data from your Mac

Use this when you need **real Firestore** and **real AI** while still running the server locally.

**Prerequisites:** DEV Google Cloud project already set up by your team. See [../scripts/DEV_LOCAL_SETUP.md](../scripts/DEV_LOCAL_SETUP.md) for full one-time setup.

### One-time setup (skip if already done)

1. Install Google Cloud CLI: `brew install google-cloud-sdk`
2. Install Firebase CLI: `brew install firebase-cli`
3. Log in: `gcloud auth login`
4. Download a service account key (ask your team lead if unsure):

```bash
mkdir -p ~/.gcp
gcloud iam service-accounts keys create ~/.gcp/starter-dev-sa.json \
  --iam-account=starter-api@starter-dev.iam.gserviceaccount.com \
  --project=starter-dev
```

**Never commit this key file to git.**

### Daily workflow

**Terminal 1 — Firebase Auth emulator:**

```bash
cd starter-backend
firebase emulators:start --project starter-dev --only auth
```

**Terminal 2 — Backend:**

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$HOME/.gcp/starter-dev-sa.json"
export GCP_PROJECT_ID="starter-dev"
export FIREBASE_AUTH_EMULATOR_HOST="localhost:9099"
export OPENAI_API_KEY="sk-or-v1-YOUR-KEY"   # get from openrouter.ai

cd starter-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev-local
```

**You are done when** `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`.

---

## Connect the mobile app (optional)

After the backend is running locally, you can run the mobile app. See [../../starter-mobile/run/run-local.md](../../starter-mobile/run/run-local.md).

| Your setup | Mobile `.env` value |
|------------|---------------------|
| iOS Simulator on same Mac | `API_BASE_URL_DEV=http://localhost:8080` |
| Physical phone on same Wi‑Fi | `API_BASE_URL_DEV=http://YOUR-MAC-IP:8080` |

Find your Mac's IP: **System Settings → Network → Wi‑Fi → Details → IP Address**

For Option B, mobile Firebase must use project `starter-dev`.

---

## Troubleshooting

| Problem | What to do |
|---------|------------|
| `java: command not found` | Install Java 21: `brew install openjdk@21` |
| `Port 8080 already in use` | Another app is using port 8080. Stop it or change port in `application-local.yml` |
| Smoke test says "server not running" | Wait for "Started StarterApplication" before running tests |
| `dev-local`: Firestore permission denied | Check `GOOGLE_APPLICATION_CREDENTIALS` points to a valid key file |
| `dev-local`: Chat returns 502 | Set a valid `OPENAI_API_KEY` from [openrouter.ai](https://openrouter.ai) |
| Mobile on phone can't reach API | Use your Mac's LAN IP, not `localhost` |

---

## What comes next?

| When you are ready to… | Read |
|------------------------|------|
| Test the team's shared cloud API | [run-dev.md](./run-dev.md) |
| Deploy to production | [run-prod.md](./run-prod.md) |
| Run the mobile app | [../../starter-mobile/run/run-local.md](../../starter-mobile/run/run-local.md) |
| Understand the full system | [../../docs/ARCHITECTURE_OVERVIEW.md](../../docs/ARCHITECTURE_OVERVIEW.md) |
