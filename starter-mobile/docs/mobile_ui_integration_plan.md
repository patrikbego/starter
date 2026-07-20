# Mobile UI Integration Plan

Task checklist for wiring the starter mobile app to the backend.

**Status:** MVP implemented.

---

## Phase 1: Project foundation

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 1.1 | Create Expo project with expo-router, TypeScript | — | Implemented |
| 1.2 | Add `app.config.ts` with `apiBaseUrl`, `appEnv`, Firebase extra | — | Implemented |
| 1.3 | Add TanStack Query, Firebase SDK dependencies | — | Implemented |
| 1.4 | Configure `src/config/env.ts` reading from Constants | — | Implemented |
| 1.5 | Set up eslint + `tsc --noEmit` in CI | — | Implemented |

---

## Phase 2: Auth layer

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 2.1 | Define `AuthPort` interface | — | Implemented |
| 2.2 | Implement `FirebaseAuthAdapter` | Firebase DEV project | Implemented |
| 2.3 | Create `AuthProvider` context | — | Implemented |
| 2.4 | Build login screen (email/password) | Firebase Auth enabled | Implemented |
| 2.5 | Auth gate in `app/_layout.tsx` | — | Implemented |
| 2.6 | Sign out on home screen | — | Implemented |

---

## Phase 3: API client

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 3.1 | Define `ApiPort` / implement `HttpApiClient` | — | Implemented |
| 3.2 | Bearer token injection | — | Implemented |
| 3.3 | 401 refresh + retry + sign-out | Backend `/api/me` returns 401 for bad token | Implemented |
| 3.4 | Error types (`ApiError`) | — | Implemented |

---

## Phase 4: Home screen

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 4.1 | `useMe` hook → `GET /api/me` | `GET /api/me` implemented | Implemented |
| 4.2 | Display user email, displayName | — | Implemented |
| 4.3 | `useHealth` hook → `GET /actuator/health` | Health endpoint live | Implemented |
| 4.4 | Health status badge (UP / down) | — | Implemented |
| 4.5 | Pull-to-refresh | — | Implemented |
| 4.6 | Loading and error states | — | Implemented |

---

## Phase 5: Chat screen

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 5.1 | Chat UI (input + message list) | — | Implemented |
| 5.2 | `useSendChat` → `POST /api/chat` | Chat endpoint implemented | Implemented |
| 5.3 | Display reply in message list | — | Implemented |
| 5.4 | Loading indicator while waiting | — | Implemented |
| 5.5 | Error handling (502, network) | — | Implemented |
| 5.6 | Optional: persist `sessionId` for multi-turn | — | Implemented |

---

## Phase 6: CI/CD

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 6.1 | Create `eas.json` profiles | — | Implemented |
| 6.2 | Set EAS env vars (`API_BASE_URL_DEV`, Firebase keys) | DEV Cloud Run URL known | Pending (manual setup) |
| 6.3 | GitHub Actions `ci-mobile.yml` | — | Implemented |
| 6.4 | GitHub Actions `eas-build-dev.yml` | — | Implemented |
| 6.5 | Manual `eas-submit-prod.yml` | PROD backend deployed | Implemented |
| 6.6 | End-to-end test on DEV build | Backend DEV live | Pending (manual verification) |

---

## Verification checklist

After Phase 6:

- [ ] Login with DEV Firebase user succeeds
- [ ] Home shows correct user from `/api/me`
- [ ] Health badge green when backend up
- [ ] Chat message returns AI reply
- [ ] Sign out returns to login
- [ ] 401 after token invalidation signs user out

---

## Related docs

- [mobile_mvp_scope_checklist.md](./mobile_mvp_scope_checklist.md)
- [BACKEND_INTEGRATION.md](./BACKEND_INTEGRATION.md)
- [starter-backend/docs/MVP_SCOPE_CHECKLIST.md](../starter-backend/docs/MVP_SCOPE_CHECKLIST.md)
