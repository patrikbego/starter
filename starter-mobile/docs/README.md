# Starter Mobile Documentation

Planning and architecture docs for the starter mobile client (Expo SDK 54). Connects to the [starter-backend](../starter-backend/) Spring Boot API on Google Cloud Run.

## Mobile docs (`starter-mobile/docs/`)

| Document | Description |
|----------|-------------|
| [mobile_architecture_plan.md](./mobile_architecture_plan.md) | Client architecture, ports/adapters, flows, build phases |
| [mobile_cicd_deployment_plan.md](./mobile_cicd_deployment_plan.md) | EAS Build/Submit, GitHub Actions, DEV/PROD environments |
| [mobile_mvp_scope_checklist.md](./mobile_mvp_scope_checklist.md) | MVP screens, acceptance criteria, phased delivery |
| [BACKEND_INTEGRATION.md](./BACKEND_INTEGRATION.md) | API client, auth header, error handling, endpoint mapping |
| [mobile_ui_integration_plan.md](./mobile_ui_integration_plan.md) | Task list for wiring auth → me → chat |

## Backend docs (starter-backend repo)

| Document | Path in starter-backend |
|----------|-------------------------|
| Documentation index | `docs/README.md` |
| Backend architecture | `docs/backend_architecture_plan.md` |
| Backend CI/CD | `docs/cicd_deployment_plan.md` |
| Authentication | `docs/AUTHENTICATION.md` |
| AI integration | `docs/AI_INTEGRATION.md` |
| MVP checklist | `docs/MVP_SCOPE_CHECKLIST.md` |

## Root docs (`docs/` at monorepo root)

| Document | Path |
|----------|------|
| New app workflow | `../../docs/NEW_APP_WORKFLOW.md` |
| Architecture overview | `../../docs/ARCHITECTURE_OVERVIEW.md` |
| Environment matrix | `../../docs/ENVIRONMENT_MATRIX.md` |

## Backend docs (`starter-backend/docs/`)

## Expo

Target **Expo SDK 54**. Use versioned docs: https://docs.expo.dev/versions/v54.0.0/

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

Mobile owns sign-in UI, API client, and chat display; backend owns auth verification, persistence, and AI.
