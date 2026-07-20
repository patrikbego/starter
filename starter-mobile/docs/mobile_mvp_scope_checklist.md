# Mobile MVP Scope Checklist

Minimal screens and integrations for the starter mobile app.

**Status:** Docs phase — all items planned.

---

## 1. Screens

### 1.1 Login (`app/(auth)/login.tsx`)

| Criterion | Status |
|-----------|--------|
| Email/password sign-in via Firebase | Planned |
| Show error on invalid credentials | Planned |
| Navigate to tabs on success | Planned |
| Link to sign-up (optional MVP) | Planned |

### 1.2 Home (`app/(tabs)/index.tsx`)

| Criterion | Status |
|-----------|--------|
| Display user email/name from `GET /api/me` | Planned |
| Show loading state while fetching | Planned |
| Backend health badge from `GET /actuator/health` | Planned |
| Pull-to-refresh refetches me + health | Planned |
| Sign out button | Planned |

### 1.3 Chat (`app/(tabs)/chat.tsx`)

| Criterion | Status |
|-----------|--------|
| Text input for message | Planned |
| Send calls `POST /api/chat` | Planned |
| Display AI reply | Planned |
| Show loading while waiting | Planned |
| Show error on failure | Planned |
| Maintain `sessionId` across messages (optional MVP) | Planned |

### 1.4 Auth gate (`app/_layout.tsx`)

| Criterion | Status |
|-----------|--------|
| Unauthenticated users see only `(auth)` routes | Planned |
| Authenticated users see `(tabs)` | Planned |
| Session restored on app launch | Planned |

---

## 2. Infrastructure

| Item | Status |
|------|--------|
| Expo SDK 54 project | Planned |
| TypeScript strict | Planned |
| `app.config.ts` with `apiBaseUrl` | Planned |
| Firebase Auth adapter | Planned |
| HttpApiClient with Bearer injection | Planned |
| TanStack Query provider | Planned |
| `eas.json` | Planned |
| GitHub Actions `ci.yml` | Planned |
| GitHub Actions `eas-build-dev.yml` | Planned |

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
