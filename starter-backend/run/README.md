# How to Run the Backend

This folder explains how to run the **starter backend** — a Spring Boot API that provides authentication, user profiles, and AI chat.

If you have never worked on this project before, read this page first, then open the guide for the environment you need.

---

## What is the backend?

The backend is a REST API that runs as a Java application. It:

- Verifies user logins via **Firebase Authentication**
- Stores user profiles in **Firestore** (Google Cloud database)
- Sends chat messages to **OpenRouter** (AI provider)
- Exposes three main endpoints for the mobile app:
  - `GET /actuator/health` — is the server alive?
  - `GET /api/me` — who is the logged-in user?
  - `POST /api/chat` — send a message to AI

The code lives in `starter-backend/` inside the monorepo.

---

## Three environments (read this first)

Every project uses the same three stages. **Always test in order: Local → DEV → PROD.**

```text
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   LOCAL     │ ──► │     DEV     │ ──► │    PROD     │
│  your Mac   │     │  cloud test │     │  live users │
└─────────────┘     └─────────────┘     └─────────────┘
```

| Environment | Guide | Who uses it | Deploy how |
|-------------|-------|-------------|------------|
| **Local** | [run-local.md](./run-local.md) | Developers on macOS | Run on your Mac — no cloud deploy |
| **DEV** | [run-dev.md](./run-dev.md) | Team testing, mobile QA | Auto-deploy on every merge to `main` |
| **PROD** | [run-prod.md](./run-prod.md) | Real users | Manual deploy only — after DEV passes |

**Golden rule:** Never skip DEV. Never deploy to PROD until DEV testing is complete.

---

## Which guide should I open?

| I want to… | Open |
|------------|------|
| Run the API on my Mac for the first time | [run-local.md](./run-local.md) → start with **Option A** (easiest) |
| Test the deployed cloud API used by the team | [run-dev.md](./run-dev.md) |
| Release to production after testing | [run-prod.md](./run-prod.md) |
| Run the mobile app too | [../starter-mobile/run/README.md](../starter-mobile/run/README.md) |

---

## Repository layout (where things live)

```text
starter/                          ← monorepo root (clone this repo)
├── starter-backend/              ← you are here
│   ├── run/                      ← these guides
│   ├── src/                      ← Java source code
│   ├── scripts/                  ← GCP setup scripts
│   └── Dockerfile                ← used for cloud deploys
└── starter-mobile/               ← Expo mobile app (separate run/ guides)
```

Clone the repo, then all commands in these guides assume you are inside `starter-backend/` unless stated otherwise.

---

## Accounts you may need

| Account | Needed for | Sign up |
|---------|------------|---------|
| None | Local (Option A — mocks) | — |
| Google Cloud | DEV and PROD cloud | [cloud.google.com](https://cloud.google.com) |
| Firebase | DEV and PROD auth | [console.firebase.google.com](https://console.firebase.google.com) |
| OpenRouter | Real AI chat (not mocks) | [openrouter.ai](https://openrouter.ai) |
| GitHub | CI/CD deploys | Your team's repo access |

---

## Typical first-day path for a new developer

1. **Clone the repo** and install Java 21 (see [run-local.md](./run-local.md))
2. **Run locally** with mocks — no cloud accounts needed
3. **Run the smoke test** to confirm everything works
4. Ask your team lead for the **DEV Cloud Run URL** and test against cloud
5. Read [run-dev.md](./run-dev.md) when you need to trigger or verify a cloud deploy
6. Read [run-prod.md](./run-prod.md) only when releasing — usually done by a lead

---

## Glossary

| Term | Meaning |
|------|---------|
| **Spring profile** | Configuration mode: `local`, `dev-local`, `dev`, or `prod` |
| **Cloud Run** | Google Cloud service that runs the API in the cloud |
| **Firestore** | Google Cloud database for user profiles |
| **Firebase Auth** | Login service — mobile signs in, backend verifies the token |
| **Bearer token** | Secret string sent in API requests to prove who you are |
| **OpenRouter** | External AI API used for chat responses |
| **WIF** | Workload Identity Federation — lets GitHub deploy to GCP securely |

---

## Related docs

- [../README.md](../README.md) — backend overview
- [../../docs/ARCHITECTURE_OVERVIEW.md](../../docs/ARCHITECTURE_OVERVIEW.md) — full system design
- [../../docs/ENVIRONMENT_MATRIX.md](../../docs/ENVIRONMENT_MATRIX.md) — all environment variables
