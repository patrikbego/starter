# Starter Backend Documentation

Planning and architecture docs for the starter backend (Spring Boot on Google Cloud Run, Firebase, Firestore, Spring AI). The primary client is the [starter-mobile](../starter-mobile/) Expo app.

## Backend docs (`starter-backend/docs/`)

| Document | Description |
|----------|-------------|
| [backend_architecture_plan.md](./backend_architecture_plan.md) | Server architecture, package layout, ports, build phases |
| [cicd_deployment_plan.md](./cicd_deployment_plan.md) | Cloud Run deploy, GitHub Actions, DEV/PROD, mobile coordination |
| [MVP_SCOPE_CHECKLIST.md](./MVP_SCOPE_CHECKLIST.md) | Minimal APIs, acceptance criteria, implementation status |
| [AUTHENTICATION.md](./AUTHENTICATION.md) | Firebase ID tokens, protected endpoints |
| [DATABASE.md](./DATABASE.md) | Firestore user collection, repository port, emulator setup |
| [AI_INTEGRATION.md](./AI_INTEGRATION.md) | Spring AI + OpenRouter, `POST /api/chat` contract |
| [SECURITY.md](./SECURITY.md) | SecurityConfig, CORS, actuator protection |
| [STORAGE_EXTENSION.md](./STORAGE_EXTENSION.md) | Optional: GCS signed-URL upload pattern |
| [operations/ACTUATOR.md](./operations/ACTUATOR.md) | Health and actuator endpoints |

## Setup and integration (scripts)

| Document | Path |
|----------|------|
| Integration / env config | `scripts/INTEGRATION_ENV_CONFIG.md` |
| Local dev setup | `scripts/DEV_LOCAL_SETUP.md` |
| DEV / PROD setup | `scripts/DEV_SETUP.md`, `scripts/PROD_SETUP.md` |
| GCP shared setup | `scripts/COMMON_GCP_SETUP.md` |

## Root docs (`docs/` at monorepo root)

| Document | Path |
|----------|------|
| New app workflow | `../../docs/NEW_APP_WORKFLOW.md` |
| Architecture overview | `../../docs/ARCHITECTURE_OVERVIEW.md` |
| Environment matrix | `../../docs/ENVIRONMENT_MATRIX.md` |

## Mobile docs (`starter-mobile/docs/`)

| Document | Path in starter-mobile |
|----------|------------------------|
| Mobile architecture | `docs/mobile_architecture_plan.md` |
| Mobile CI/CD | `docs/mobile_cicd_deployment_plan.md` |
| Backend integration | `docs/BACKEND_INTEGRATION.md` |
| Mobile MVP checklist | `docs/mobile_mvp_scope_checklist.md` |

## Quick alignment

```text
Mobile                          Backend
------                          -------
Firebase Auth (client)    <->   Firebase token verification
GET /api/me               <->   User auto-provision in Firestore
POST /api/chat            <->   Spring AI via OpenRouter
APP_ENV / EAS profile     <->   starter-dev / starter-prod GCP projects
```

Deployment model (monorepo): **merge to `main` → DEV**; **manual approval → PROD**.

## Core integration loop

```text
Sign in (Firebase) → GET /api/me (user profile) → POST /api/chat (AI reply)
```

Backend owns auth verification, user persistence, and AI calls; mobile owns sign-in UI, API client, and chat display.
