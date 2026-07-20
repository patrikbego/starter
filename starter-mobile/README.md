# Starter Mobile

Generic Expo SDK 54 client boilerplate for mobile-first products. Connects to the [starter-backend](../starter-backend/) API with Firebase Authentication.

**Status:** Docs phase — application code and EAS/CI workflows will follow.

## Quick start (after implementation)

```bash
npm install
npx expo start
```

Firebase and native modules require a **development build** — Expo Go alone is insufficient for production features.

## Minimal screens

| Screen | Backend |
|--------|---------|
| Login | Firebase Auth |
| Home | `GET /api/me`, `GET /actuator/health` |
| Chat | `POST /api/chat` |

## Documentation

See [docs/README.md](./docs/README.md).

## Creating a new app

Follow the monorepo guide: [../docs/NEW_APP_WORKFLOW.md](../docs/NEW_APP_WORKFLOW.md).

## Tech stack

- Expo SDK 54, React Native, TypeScript
- expo-router (file-based navigation)
- Firebase Authentication
- TanStack Query (server state)
- EAS Build / Submit for CI/CD
