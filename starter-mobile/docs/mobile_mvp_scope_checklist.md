# Mobile MVP Scope Checklist

Minimal screens and integrations for the starter mobile app.

**Status:** MVP implemented.

---

## 1. Screens

### 1.1 Login (`app/(auth)/login.tsx`)

| Criterion | Status |
|-----------|--------|
| Email/password sign-in via Firebase | Implemented |
| Show error on invalid credentials | Implemented |
| Navigate to tabs on success | Implemented |
| Link to sign-up (optional MVP) | Implemented |

### 1.2 Home (`app/(tabs)/index.tsx`)

| Criterion | Status |
|-----------|--------|
| Display user email/name from `GET /api/me` | Implemented |
| Show loading state while fetching | Implemented |
| Backend health badge from `GET /actuator/health` | Implemented |
| Pull-to-refresh refetches me + health | Implemented |
| Sign out button | Implemented |

### 1.3 Chat (`app/(tabs)/chat.tsx`)

| Criterion | Status |
|-----------|--------|
| Text input for message | Implemented |
| Send calls `POST /api/chat` | Implemented |
| Display AI reply | Implemented |
| Show loading while waiting | Implemented |
| Show error on failure | Implemented |
| Maintain `sessionId` across messages (optional MVP) | Implemented |

### 1.4 Auth gate (`app/_layout.tsx`)

| Criterion | Status |
|-----------|--------|
| Unauthenticated users see only `(auth)` routes | Implemented |
| Authenticated users see `(tabs)` | Implemented |
| Session restored on app launch | Implemented |

---

## 2. Infrastructure

| Item | Status |
|------|--------|
| Expo SDK 54 project | Implemented |
| TypeScript strict | Implemented |
| `app.config.ts` with `apiBaseUrl` | Implemented |
| Firebase Auth adapter | Implemented |
| HttpApiClient with Bearer injection | Implemented |
| TanStack Query provider | Implemented |
| `eas.json` | Implemented |
| GitHub Actions `ci-mobile.yml` | Implemented |
| GitHub Actions `eas-build-dev.yml` | Implemented |

---

## 3. Explicitly out of scope (starter MVP)

| Feature | Notes |
|---------|-------|
| RevenueCat / paywall | Extension |
| File upload / camera | Extension — backend `STORAGE_EXTENSION.md` |
| Search / RAG UI beyond simple chat | Extension |
| Offline mode | Post-MVP |
| Biometric lock | Post-MVP |
| Push notifications | Post-MVP |

---

## 4. End-to-end test flow

1. Install DEV build (EAS internal)
2. Sign in with Firebase (DEV project)
3. Home shows user profile from `/api/me`
4. Health badge shows green when backend reachable
5. Chat sends "hello" → receives AI reply
6. Sign out → returns to login

---

## 5. Backend coordination

Align with [starter-backend/docs/MVP_SCOPE_CHECKLIST.md](../starter-backend/docs/MVP_SCOPE_CHECKLIST.md):

- [ ] DEV mobile uses DEV API URL
- [ ] DEV mobile uses DEV Firebase project
- [ ] Bearer token accepted by backend
- [ ] Chat works against DEV OpenRouter key

---

## Related docs

- [mobile_architecture_plan.md](./mobile_architecture_plan.md)
- [BACKEND_INTEGRATION.md](./BACKEND_INTEGRATION.md)
- [mobile_ui_integration_plan.md](./mobile_ui_integration_plan.md)
