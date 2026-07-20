# Starter Mobile Architecture Plan

## 1. Product Vision

The starter mobile app is a **generic Expo client foundation** for mobile-first products. It is not tied to a specific domain — you fork it, rebrand, and add screens and features.

Out of the box it provides:

- Firebase Authentication (sign-in / sign-out)
- Auth gate routing (unauthenticated → login)
- Home screen showing user profile from `GET /api/me` and backend health status
- Simple AI chat screen calling `POST /api/chat`
- Environment-aware API base URL (DEV vs PROD)

The backend APIs and infrastructure are defined in [starter-backend](../starter-backend/) — `docs/backend_architecture_plan.md`.

### 1.1 Implementation Status

| Component | Status |
|-----------|--------|
| Documentation | Complete (docs phase) |
| Expo project scaffold | Planned |
| Firebase Auth | Planned |
| API client + TanStack Query | Planned |
| Auth gate + login screen | Planned |
| Home + chat screens | Planned |
| `app.config.ts` / EAS profiles | Planned |
| GitHub Actions + `eas.json` | Planned |

---

## 2. Key Architectural Principles

### 2.1 Backend Is Source of Truth

The mobile app is a **thin, authenticated client**.

Never treat local state as authoritative for:

- User profile data (fetch from `GET /api/me`)
- AI responses (from `POST /api/chat`)
- Business rules (enforce on API when adding features)

### 2.2 Portable Client Boundaries

Use ports-and-adapters:

| Port | Responsibility |
|------|----------------|
| `AuthPort` | Sign-in, sign-out, current user, ID token |
| `ApiPort` | HTTP with auth header injection |

Implementations in `src/adapters/`; screens depend on ports or feature hooks.

### 2.3 Simple MVP First

Avoid in starter MVP:

- Offline-first sync
- Client-side AI
- Local SQLite vault
- File upload UI (extension — see backend `STORAGE_EXTENSION.md`)

Use TanStack Query cache + refetch for server state.

### 2.4 Security by Default

- Firebase ID token on every protected API call
- `expo-secure-store` for token persistence where needed
- No GCP service account keys on device
- No backend API secrets in app (only public Firebase keys via build-time env)
- No logging of tokens in production

---

## 3. Technology Stack

### 3.1 Core

| Area | Choice |
|------|--------|
| Runtime | Expo ~54, React Native, React 19 |
| Navigation | expo-router (file-based) |
| Language | TypeScript (strict) |
| Linting | eslint-config-expo |

### 3.2 Authentication

- Firebase Authentication (email/password minimum)
- Expo **development build** for native Firebase modules
- Config via Expo config plugin + `google-services.json` / `GoogleService-Info.plist` per env

### 3.3 API and State

- Typed REST client (fetch) with interceptors
- TanStack Query for `/api/me`, health check, chat
- React context for auth session only

### 3.4 Planned dependencies (implementation phase)

```json
{
  "@tanstack/react-query": "...",
  "firebase": "...",
  "expo-secure-store": "...",
  "expo-constants": "..."
}
```

---

## 4. Navigation Structure

```text
app/
  _layout.tsx              # Root: providers + auth gate
  (auth)/
    login.tsx              # Email/password sign-in
  (tabs)/
    _layout.tsx            # Tab navigator
    index.tsx              # Home: user + health
    chat.tsx               # AI chat
```

### Auth gate flow

```text
App launch
  → AuthProvider checks Firebase session
  → If no session: redirect to /(auth)/login
  → If session: load (tabs)
```

---

## 5. Feature Modules

```text
src/
  config/
    env.ts                 # apiBaseUrl, firebase config from Constants
  ports/
    AuthPort.ts
    ApiPort.ts
  adapters/
    FirebaseAuthAdapter.ts
    HttpApiClient.ts
  features/
    auth/
      useAuth.ts
      AuthProvider.tsx
    profile/
      useMe.ts             # TanStack Query → GET /api/me
      useHealth.ts         # GET /actuator/health
    chat/
      useChat.ts           # POST /api/chat mutation
```

---

## 6. API Integration

See [BACKEND_INTEGRATION.md](./BACKEND_INTEGRATION.md).

| Endpoint | Mobile use |
|----------|------------|
| `GET /actuator/health` | Home screen backend status badge |
| `GET /api/me` | Home screen user profile |
| `POST /api/chat` | Chat screen send/receive |

Auth header on protected calls:

```http
Authorization: Bearer <firebase_id_token>
```

---

## 7. Error Handling

| HTTP | Client behavior |
|------|-----------------|
| 401 | Refresh token once → retry → sign out |
| 403 | Show permission message |
| 404 | Navigate back |
| 429 | Rate limit message |
| 5xx | Generic retry message |

---

## 8. Environment Configuration

### app.config.ts (planned)

```typescript
const appEnv = process.env.APP_ENV ?? 'development';
const apiBaseUrl =
  appEnv === 'production'
    ? process.env.API_BASE_URL_PROD
    : process.env.API_BASE_URL_DEV;

export default {
  expo: {
    extra: {
      appEnv,
      apiBaseUrl,
      firebase: {
        apiKey: process.env.EXPO_PUBLIC_FIREBASE_API_KEY,
        authDomain: process.env.EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN,
        projectId: process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID,
      },
    },
  },
};
```

**Rule:** DEV builds must never default to PROD API URL.

---

## 9. Build Phases

### Phase 1: Foundation (starter MVP)

- [ ] Expo project with expo-router
- [ ] `app.config.ts` with env-driven `apiBaseUrl`
- [ ] Firebase Auth adapter + AuthProvider
- [ ] HttpApiClient with Bearer injection
- [ ] TanStack Query provider
- [ ] Login screen
- [ ] Home screen (`/api/me` + health)
- [ ] Chat screen (`/api/chat`)
- [ ] Auth gate in root layout
- [ ] `eas.json` + GitHub Actions CI

### Phase 2: Extensions (per product)

- [ ] File upload UI (signed URL flow)
- [ ] RevenueCat subscriptions
- [ ] Additional tab screens
- [ ] Push notifications
- [ ] Firebase App Check

---

## 10. Future Extensions

| Extension | Approach |
|-----------|----------|
| File upload | `DocumentUploadPort` + image/document picker |
| Subscriptions | RevenueCat + `SubscriptionPort` |
| Offline cache | TanStack Query `staleTime` / persistence (not full sync) |
| Deep linking | expo-router linking config |

Reference: [docsera-mobile](https://github.com/patrikbego/docsera-mobile) for document-vault patterns.

---

## Related docs

- [BACKEND_INTEGRATION.md](./BACKEND_INTEGRATION.md)
- [mobile_mvp_scope_checklist.md](./mobile_mvp_scope_checklist.md)
- [mobile_cicd_deployment_plan.md](./mobile_cicd_deployment_plan.md)
- [../docs/NEW_APP_WORKFLOW.md](../../docs/NEW_APP_WORKFLOW.md)
