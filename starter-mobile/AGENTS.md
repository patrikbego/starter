# Starter Mobile - Agent Context

## What is this?

A generic Expo mobile starter kit. Thin authenticated client over the starter-backend API. Fork and customize screens and features per product.

## Core Technology Stack

- **Framework**: Expo SDK 54, React Native, React 19
- **Navigation**: expo-router (file-based)
- **Auth**: Firebase Authentication
- **Server state**: TanStack Query
- **API**: Typed REST client with Bearer token injection
- **CI/CD**: GitHub Actions + EAS Build/Submit

## Architecture Principles

1. **Backend is source of truth** — never trust local state for user data or AI responses
2. **Ports and adapters** — abstract Firebase and HTTP behind interfaces
3. **Simple MVP** — login, home (me + health), chat only in starter
4. **Security by default** — Bearer tokens, no backend secrets in app binary

## Project Structure (planned)

```
app/
  (auth)/login.tsx
  (tabs)/
    index.tsx       # Home: /api/me + health
    chat.tsx        # AI chat
  _layout.tsx       # Auth gate + providers

src/
  config/env.ts
  ports/            # AuthPort, ApiPort
  adapters/         # FirebaseAuthAdapter, HttpApiClient
  features/
    auth/
    profile/
    chat/
```

## Environment

- `APP_ENV=development` → DEV API + DEV Firebase
- `APP_ENV=production` → PROD API + PROD Firebase

Config via `app.config.ts` + `expo-constants` (planned).

## Rules for Agents

1. Use [Expo SDK 54 docs](https://docs.expo.dev/versions/v54.0.0/) for all Expo APIs
2. Screens in `app/`; business logic in `src/features/`
3. Every protected API call includes `Authorization: Bearer <token>`
4. On 401: refresh token once → retry → sign out
5. Keep MVP minimal — add features in `src/features/`, don't bloat starter

## Documentation

- Architecture: `docs/mobile_architecture_plan.md`
- Backend integration: `docs/BACKEND_INTEGRATION.md`
- CI/CD: `docs/mobile_cicd_deployment_plan.md`
- New app workflow: `../docs/NEW_APP_WORKFLOW.md`
