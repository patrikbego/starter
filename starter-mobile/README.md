# Starter Mobile

Generic Expo SDK 54 client boilerplate for mobile-first products. Connects to the [starter-backend](../starter-backend/) API with Firebase Authentication.

## Quick start

**New to the project?** Start with the step-by-step run guides: [run/README.md](./run/README.md)

```bash
cd starter-mobile
cp .env.example .env
# Fill in API_BASE_URL_DEV and EXPO_PUBLIC_FIREBASE_* values
npm install
npx expo start
```

For physical devices, use your machine's LAN IP instead of `localhost` for `API_BASE_URL_DEV` when testing against a local backend.

## Minimal screens

| Screen | Backend |
|--------|---------|
| Login | Firebase Auth (email/password) |
| Home | `GET /api/me`, `GET /actuator/health` |
| Chat | `POST /api/chat` |

## Environment variables

Set in `.env` for local dev or via EAS env vars for builds. See [`.env.example`](./.env.example) and [../docs/ENVIRONMENT_MATRIX.md](../docs/ENVIRONMENT_MATRIX.md).

| Variable | Purpose |
|----------|---------|
| `APP_ENV` | `development` or `production` |
| `API_BASE_URL_DEV` | DEV Cloud Run URL (defaults to `http://localhost:8080`) |
| `API_BASE_URL_PROD` | PROD Cloud Run URL |
| `EXPO_PUBLIC_FIREBASE_*` | Firebase web config for the matching environment |

**Rule:** DEV builds must never default to the PROD API URL.

## Project structure

```text
app/                    # expo-router screens
  (auth)/login.tsx
  (tabs)/index.tsx      # Home
  (tabs)/chat.tsx       # AI chat
src/
  config/env.ts         # Runtime config from app.config.ts
  ports/                # AuthPort, ApiPort
  adapters/             # FirebaseAuthAdapter, HttpApiClient
  features/             # auth, profile, chat hooks
```

## CI/CD

| Workflow | Trigger |
|----------|---------|
| `ci-mobile.yml` | PR + push to `main` — lint + typecheck |
| `eas-build-dev.yml` | Push to `main` — EAS preview build |
| `eas-submit-prod.yml` | Manual — submit tested build to stores |

Requires `EXPO_TOKEN` GitHub secret and EAS env vars. See [docs/mobile_cicd_deployment_plan.md](./docs/mobile_cicd_deployment_plan.md).

## Creating a new app

Follow the monorepo guide: [../docs/NEW_APP_WORKFLOW.md](../docs/NEW_APP_WORKFLOW.md).

## Tech stack

- Expo SDK 54, React Native, TypeScript
- expo-router (file-based navigation)
- Firebase JS SDK (Authentication)
- TanStack Query (server state)
- EAS Build / Submit for CI/CD

## Documentation

See [docs/README.md](./docs/README.md).
