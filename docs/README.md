# Starter Kit Documentation

Planning and architecture docs for the starter kit monorepo.

## Root docs (`docs/`)

| Document | Description |
|----------|-------------|
| [ARCHITECTURE_OVERVIEW.md](./ARCHITECTURE_OVERVIEW.md) | System design, principles, shared vs per-app concerns |
| [NEW_APP_WORKFLOW.md](./NEW_APP_WORKFLOW.md) | Step-by-step guide to fork the monorepo into a new product |
| [ENVIRONMENT_MATRIX.md](./ENVIRONMENT_MATRIX.md) | DEV/PROD variable mapping (GCP, Firebase, API URL, EAS) |

## Backend docs (`starter-backend/`)

| Document | Path |
|----------|------|
| Documentation index | [starter-backend/docs/README.md](../starter-backend/docs/README.md) |
| Architecture plan | `starter-backend/docs/backend_architecture_plan.md` |
| CI/CD deployment | `starter-backend/docs/cicd_deployment_plan.md` |
| MVP checklist | `starter-backend/docs/MVP_SCOPE_CHECKLIST.md` |
| Authentication | `starter-backend/docs/AUTHENTICATION.md` |
| Database | `starter-backend/docs/DATABASE.md` |
| AI integration | `starter-backend/docs/AI_INTEGRATION.md` |
| Security | `starter-backend/docs/SECURITY.md` |
| Storage extension (optional) | `starter-backend/docs/STORAGE_EXTENSION.md` |
| Actuator / health | `starter-backend/docs/operations/ACTUATOR.md` |
| Env config | `starter-backend/scripts/INTEGRATION_ENV_CONFIG.md` |
| GCP setup | `starter-backend/scripts/COMMON_GCP_SETUP.md`, `DEV_SETUP.md`, `PROD_SETUP.md` |
| Local dev | `starter-backend/scripts/DEV_LOCAL_SETUP.md` |

## Mobile docs (`starter-mobile/`)

| Document | Path |
|----------|------|
| Documentation index | [starter-mobile/docs/README.md](../starter-mobile/docs/README.md) |
| Architecture plan | `starter-mobile/docs/mobile_architecture_plan.md` |
| CI/CD deployment | `starter-mobile/docs/mobile_cicd_deployment_plan.md` |
| MVP checklist | `starter-mobile/docs/mobile_mvp_scope_checklist.md` |
| Backend integration | `starter-mobile/docs/BACKEND_INTEGRATION.md` |
| UI integration plan | `starter-mobile/docs/mobile_ui_integration_plan.md` |

## Quick alignment

```text
Mobile                          Backend
------                          -------
Firebase Auth (client)    <->   Firebase token verification
GET /api/me               <->   User auto-provision in Firestore
POST /api/chat            <->   Spring AI via OpenRouter
APP_ENV / EAS profile     <->   starter-dev / starter-prod GCP projects
```

Deployment model: **merge to `main` → DEV**; **manual approval → PROD**.

## Core integration loop

```text
Sign in (Firebase) → GET /api/me (user profile) → POST /api/chat (AI reply)
```

Backend owns auth verification, user persistence, and AI calls; mobile owns sign-in UI, API client, and chat display.
