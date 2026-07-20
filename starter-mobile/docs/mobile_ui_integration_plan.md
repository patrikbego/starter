# Mobile UI Integration Plan

Task checklist for wiring the starter mobile app to the backend. Expand per task during implementation.

**Status:** Docs phase — tasks planned.

---

## Phase 1: Project foundation

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 1.1 | Create Expo project with expo-router, TypeScript | — | Planned |
| 1.2 | Add `app.config.ts` with `apiBaseUrl`, `appEnv`, Firebase extra | — | Planned |
| 1.3 | Add TanStack Query, Firebase SDK dependencies | — | Planned |
| 1.4 | Configure `src/config/env.ts` reading from Constants | — | Planned |
| 1.5 | Set up eslint + `tsc --noEmit` in CI | — | Planned |

---

## Phase 2: Auth layer

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 2.1 | Define `AuthPort` interface | — | Planned |
| 2.2 | Implement `FirebaseAuthAdapter` | Firebase DEV project | Planned |
| 2.3 | Create `AuthProvider` context | — | Planned |
| 2.4 | Build login screen (email/password) | Firebase Auth enabled | Planned |
| 2.5 | Auth gate in `app/_layout.tsx` | — | Planned |
| 2.6 | Sign out on home screen | — | Planned |

---

## Phase 3: API client

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 3.1 | Define `ApiPort` / implement `HttpApiClient` | — | Planned |
| 3.2 | Bearer token injection | — | Planned |
| 3.3 | 401 refresh + retry + sign-out | Backend `/api/me` returns 401 for bad token | Planned |
| 3.4 | Error types (`ApiError`) | — | Planned |

---

## Phase 4: Home screen

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 4.1 | `useMe` hook → `GET /api/me` | `GET /api/me` implemented | Planned |
| 4.2 | Display user email, displayName | — | Planned |
| 4.3 | `useHealth` hook → `GET /actuator/health` | Health endpoint live | Planned |
| 4.4 | Health status badge (UP / down) | — | Planned |
| 4.5 | Pull-to-refresh | — | Planned |
| 4.6 | Loading and error states | — | Planned |

---

## Phase 5: Chat screen

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 5.1 | Chat UI (input + message list) | — | Planned |
| 5.2 | `useSendChat` → `POST /api/chat` | Chat endpoint implemented | Planned |
| 5.3 | Display reply in message list | — | Planned |
| 5.4 | Loading indicator while waiting | — | Planned |
| 5.5 | Error handling (502, network) | — | Planned |
| 5.6 | Optional: persist `sessionId` for multi-turn | — | Planned |

---

## Phase 6: CI/CD

| # | Task | Backend dependency | Status |
|---|------|-------------------|--------|
| 6.1 | Create `eas.json` profiles | — | Planned |
| 6.2 | Set EAS env vars (`API_BASE_URL_DEV`, Firebase keys) | DEV Cloud Run URL known | Planned |
| 6.3 | GitHub Actions `ci.yml` | — | Planned |
| 6.4 | GitHub Actions `eas-build-dev.yml` | — | Planned |
| 6.5 | Manual `eas-submit-prod.yml` | PROD backend deployed | Planned |
| 6.6 | End-to-end test on DEV build | Backend DEV live | Planned |

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
