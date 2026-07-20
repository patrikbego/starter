# Starter Kit

A documentation-first **monorepo** boilerplate for quickly spinning up new mobile + backend products. Based on patterns proven in [docsera](https://github.com/patrikbego/docsera) and [docsera-mobile](https://github.com/patrikbego/docsera-mobile).

## Monorepo layout

```text
starter/                          # single git repository
├── README.md
├── docs/                           # cross-cutting guides (new-app workflow, env matrix)
├── starter-backend/                # Spring Boot API (Java 21, Cloud Run)
└── starter-mobile/                 # Expo SDK 54 client
```

| Package | Purpose |
|---------|---------|
| `docs/` | Architecture overview, new-app workflow, environment matrix |
| `starter-backend/` | Spring Boot API on Cloud Run (Firebase, Firestore, Spring AI) |
| `starter-mobile/` | Expo SDK 54 client (Firebase Auth, typed REST client) |

One repository, one `git push` — CI/CD workflows use path filters to deploy backend and mobile independently.

## Minimal integration surface

| Layer | Backend | Mobile |
|-------|---------|--------|
| Health | `GET /actuator/health` | Home screen reachability badge |
| Auth | Firebase token verification → `GET /api/me` | Login + auth gate |
| AI | `POST /api/chat` (Spring AI + OpenRouter) | Simple chat screen |
| DB | Firestore user profile (behind a port) | Display user from `/api/me` |

Product-specific logic is added **after** forking — see [docs/NEW_APP_WORKFLOW.md](./docs/NEW_APP_WORKFLOW.md).

## Quick start

1. Read [docs/ARCHITECTURE_OVERVIEW.md](./docs/ARCHITECTURE_OVERVIEW.md) for the full stack.
2. Follow [docs/NEW_APP_WORKFLOW.md](./docs/NEW_APP_WORKFLOW.md) to fork this monorepo into a new product.
3. Use [docs/ENVIRONMENT_MATRIX.md](./docs/ENVIRONMENT_MATRIX.md) when configuring DEV/PROD.

## Deployment model

```text
merge to main  →  auto-deploy DEV (backend Cloud Run + mobile EAS internal build)
manual approval  →  PROD (same Docker image / same EAS build ID)
```

## Documentation index

See [docs/README.md](./docs/README.md).

## Status

**Backend MVP implemented** — Spring Boot API with auth, `/api/me`, `/api/chat`, Docker, and GitHub Actions CI/CD in `starter-backend/`.

**Mobile MVP implemented** — Expo SDK 54 app with Firebase Auth, home (`/api/me` + health), chat (`/api/chat`), and GitHub Actions + EAS CI/CD in `starter-mobile/`.
