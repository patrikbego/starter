# Starter Mobile

Reusable Expo/React Native client foundation for new applications. It provides Firebase sign-in, an authenticated HTTP client, server-state management, environment validation, a small home screen, and a minimal AI interaction.

> Status: functional prototype. TypeScript passes, but the independent repository split, durable React Native auth persistence, versioned contract integration, clean-install lint verification, environment guardrails, tests, and corrected store-release workflow are still required before template v1.

## Prototype quick start

```bash
cp .env.example .env
npm ci
npx expo start
```

Configure the DEV backend URL and matching Firebase DEV project. A physical device cannot reach a backend at the computer's `localhost`; use an HTTPS DEV service or a deliberate local network/tunnel setup.

## Starter loop

```text
Firebase sign-in -> authenticated /me -> stateless AI request -> sign out
```

The backend is the source of truth for identity-derived user data, authorization, AI execution, and product rules. No server credential is included in the mobile bundle.

## Architecture

```text
app routes -> feature hooks/components -> ports -> Firebase/HTTP adapters
                                      -> TanStack Query
```

| Area | Responsibility |
|---|---|
| `app/` | Expo Router routes and composition |
| `src/features/` | Feature behavior and UI-facing hooks |
| `src/ports/` | Auth/API capabilities used by features |
| `src/adapters/` | Firebase and HTTP implementation details |
| `src/config/` | Validated build/runtime configuration |

## Build types

| Build | Configuration | Distribution |
|---|---|---|
| Development client | DEV/local | Developers |
| Preview | DEV | EAS internal distribution |
| Production release candidate | PROD | TestFlight / Play internal testing, then store release |

A preview/internal binary is not the production store artifact.

## Verify

```bash
npm ci
npm run lint
npx tsc --noEmit
```

Target v1 also has unit/component tests, Expo Doctor, contract validation, and clean EAS build checks.

## Documentation

Start with [docs/README.md](./docs/README.md), then read:

- [Architecture](./docs/mobile_architecture_plan.md)
- [Backend contract integration](./docs/BACKEND_INTEGRATION.md)
- [CI/CD and store release](./docs/mobile_cicd_deployment_plan.md)
- [Scope and readiness](./docs/mobile_mvp_scope_checklist.md)
- [Implementation plan](./docs/mobile_ui_integration_plan.md)

The backend is a separate repository. This client depends only on a published API contract and environment URL.
