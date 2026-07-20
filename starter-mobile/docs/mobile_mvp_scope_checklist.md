# Mobile Template v1 Scope

The prototype implements the visible starter loop. Template v1 is not complete until the repository, contract, persistence, tests, and release flow meet the criteria below.

## Prototype evidence

| Capability | Current status |
|---|---|
| Expo Router app and TypeScript | Implemented |
| Login/sign-up/sign-out UI | Implemented |
| Auth gate/provider | Implemented; durable native persistence needs verification |
| Bearer-token API adapter and one 401 retry | Implemented |
| TanStack Query provider, `/me`, health | Implemented |
| Minimal chat screen | Implemented |
| `app.config.ts` and `eas.json` | Present; target validation/release model pending |
| TypeScript check | Passing on 2026-07-20 |
| Direct ESLint | Passing with `eslint . --no-cache` under matching Node architecture |
| `npm run lint` / `expo lint` | Fails on the current dual-architecture local setup because `unrs-resolver` native binding loading is inconsistent; toolchain pin/fix required |
| Parent-workspace GitHub workflows | Present, not independent-repository ready |

## Required before template v1

### Repository and contract

- [ ] Independent mobile Git repository with repository-owned workflows
- [ ] Pinned backend OpenAPI contract/version
- [ ] Generated or contract-validated API types/fixtures
- [ ] Client paths moved to `/api/v1`
- [ ] Standard backend error decoding and correlation ID support

### Authentication and data safety

- [ ] Durable Firebase auth persistence configured for the pinned React Native stack
- [ ] Stable loading gate while session restores
- [ ] Concurrent `401`s share one refresh; retry occurs once
- [ ] Protected query data clears when the user changes/signs out
- [ ] No tokens, prompts, or sensitive payloads in logs/analytics

### Configuration

- [ ] One validated API URL variable per EAS environment
- [ ] Production build rejects DEV/localhost/missing values
- [ ] Preview build rejects PROD Firebase/API mismatch
- [ ] DEV and PROD app identifiers/variants documented
- [ ] No server secrets use `EXPO_PUBLIC_*`

### UX and tests

- [ ] Unit/component tests for auth gate, API adapter, `/me`, and AI errors
- [ ] Clean `npm ci`, lint, typecheck, tests, and Expo Doctor in CI
- [ ] Loading, empty, offline, timeout, rate-limit, and retry states
- [ ] Accessibility, safe-area, keyboard, dynamic-text, and contrast checks
- [ ] Real-device DEV smoke flow passes

### Delivery

- [ ] DEV preview workflow uses internal distribution and DEV config
- [ ] Production workflow produces store artifacts with PROD config
- [ ] Submission reuses recorded production build IDs without rebuilding
- [ ] TestFlight/Play internal testing precedes public release
- [ ] Runtime-version/update-channel strategy documented if EAS Update enabled
- [ ] Store and OTA rollback drills completed

## Explicit non-goals

- Product branding system beyond replaceable placeholders
- Camera/file upload
- Payments/subscriptions
- Push notifications
- Analytics vendor
- Offline-first sync
- Persisted conversations/RAG UI
- Admin or tablet-specific product experiences

## End-to-end acceptance test

From a clean clone:

1. CI passes with a clean dependency install.
2. Preview build validates DEV API/Firebase pairing.
3. App restores a DEV Firebase session without route flicker.
4. `/api/v1/me` displays the authenticated profile.
5. AI request returns a stateless reply and handles limit/provider failures.
6. Invalid token causes one refresh/retry, then sign-out if still invalid.
7. Sign-out clears protected server-state cache.
8. Production candidate contains only PROD configuration.
9. The same store candidate tested internally is released without rebuilding.
