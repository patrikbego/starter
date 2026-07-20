# Starter Mobile Agent Context

## Purpose

This is a generic Expo client template. Keep it a thin authenticated client over a versioned backend contract. Product screens, branding, analytics, payments, files, notifications, and offline sync are extensions.

## Status

The code is a functional prototype still tracked by a parent workspace repository. Target v1 is an independent repository with its own workflows, pinned backend contract, durable auth persistence, deterministic tests, validated environments, and correct preview/store release flows. Read `docs/README.md` and `docs/mobile_mvp_scope_checklist.md` before claiming completion.

## Stack

- Expo/React Native/TypeScript (use the exact SDK pinned in `package.json`)
- Expo Router
- Firebase Authentication
- TanStack Query
- Typed HTTP adapter
- EAS Build/Submit and optional EAS Update

## Boundaries

```text
app routes -> feature hooks/components -> ports -> adapters
                                      -> TanStack Query
```

- Routes compose UI; feature behavior stays under `src/features/`.
- Firebase and HTTP details stay in adapters.
- Backend is authoritative for authorization, user data, business rules, and AI.
- Client integration uses the pinned OpenAPI contract, not backend source files.

## Security and configuration rules

1. Every protected request carries a Firebase ID token.
2. On `401`, force-refresh once, retry once, then sign out; avoid concurrent refresh storms.
3. Clear protected query data on user change/sign-out.
4. Production build rejects DEV/localhost/missing config.
5. Never place server secrets in `EXPO_PUBLIC_*` or the app bundle.
6. Never log tokens, prompts/replies, or sensitive payloads.

## Build rule

- `preview`: DEV config and internal distribution; not store-promotable.
- `production`: PROD config and store signing; test via TestFlight/Play internal, then release the same binary.

## Verification

```bash
npm ci
npm run lint
npx tsc --noEmit
```

Use the exact versioned Expo documentation for the SDK in `package.json`.
