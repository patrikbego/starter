# Mobile Architecture

## 1. Purpose

The mobile template is a thin, secure client for many different application domains. It standardizes navigation composition, authentication, API access, server-state handling, environment configuration, errors, and delivery. Branding and product features are added only after creating a product repository.

## 2. Current prototype versus target v1

| Concern | Prototype | Target v1 |
|---|---|---|
| Git | Child of parent repository | Independent repository |
| API | Handwritten types, unversioned paths | Pinned/validated OpenAPI contract and `/api/v1` |
| Auth | Firebase JS adapter | Explicit React Native persistence and tested restore/refresh |
| Config | DEV localhost fallback | Build-type validation; production fails on missing/mixed config |
| Tests | TypeScript and lint config | Unit/component/integration contract checks |
| Delivery | Parent workflows | Repository-owned preview and store-release workflows |
| Release | Preview build described as promotable | Separate store candidate tested then released unchanged |

## 3. Principles

### Backend is authoritative

Firebase supplies a client token; the backend verifies identity and owns user provisioning, authorization, domain rules, and AI calls. Client-side checks improve UX but are never the only enforcement.

### Feature-oriented UI

Routes compose features. Feature modules contain hooks/components and depend on small ports. Adapters own Firebase/HTTP details.

### Server state is server state

Use TanStack Query for remote data, cache invalidation, loading/error/retry state. Use component/context state for short-lived UI/session composition. Do not duplicate `/me` or domain records into ad hoc global state.

### Configuration fails early

`app.config.ts` validates environment pairing during build. Production cannot default to DEV/localhost, and DEV/PROD Firebase project IDs cannot be crossed.

## 4. Structure

```text
app/
├── _layout.tsx
├── (auth)/
│   └── login.tsx
└── (tabs)/
    ├── index.tsx
    └── chat.tsx

src/
├── api/             # generated/validated contract types and fixtures
├── adapters/        # FirebaseAuthAdapter, HttpApiClient
├── config/          # validated environment and build metadata
├── features/
│   ├── auth/
│   ├── profile/
│   └── chat/
├── ports/           # AuthPort, ApiPort
└── providers/       # query/auth/app composition
```

Product features get their own directory and route(s). Avoid generic `utils/` dumping grounds; place behavior next to its owner.

## 5. Authentication

The auth adapter provides:

- sign in/sign up/sign out;
- current ID token with optional forced refresh;
- auth-state subscription;
- explicit durable persistence supported by the pinned Firebase/React Native stack.

The auth gate renders a stable loading state while persistence restores. It never briefly exposes authenticated routes before identity is known.

On API `401`: force-refresh once, retry once, then clear session and return to login. Concurrent `401`s should share one refresh operation rather than triggering a refresh storm.

## 6. API and errors

The backend OpenAPI contract is pinned by release/version. The HTTP adapter:

- joins a validated HTTPS base URL and contract path;
- injects the Firebase token for protected endpoints;
- sets/propagates a bounded correlation ID when useful;
- applies a request timeout/abort policy;
- decodes the standard error envelope;
- retries only when policy says the operation is safe;
- never logs tokens or full sensitive payloads.

See [BACKEND_INTEGRATION.md](./BACKEND_INTEGRATION.md).

## 7. State and UX

Starter query keys:

```text
['me']
['health']
```

The AI mutation maintains only on-screen transient messages. It does not imply server conversation memory. Products that persist conversations need a versioned backend contract and local privacy/cache policy.

Every screen includes loading, empty, error, and retry behavior. Accessibility labels, dynamic text, keyboard handling, reduced motion, color contrast, and safe-area behavior are baseline acceptance criteria, not a branding phase.

## 8. Configuration

Use one public variable name per concept with environment-specific EAS values:

```text
APP_ENV
EXPO_PUBLIC_API_BASE_URL
EXPO_PUBLIC_FIREBASE_API_KEY
EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN
EXPO_PUBLIC_FIREBASE_PROJECT_ID
EAS_PROJECT_ID
```

Build validation:

- production requires HTTPS and a PROD project ID;
- preview requires a configured DEV HTTPS URL;
- localhost is allowed only for deliberate local development;
- required values are never replaced with production defaults;
- app identifiers/schemes match the selected variant.

Public Expo/Firebase configuration is visible in the binary. Server secrets never use `EXPO_PUBLIC_*`.

## 9. Testing

| Layer | Coverage |
|---|---|
| Pure feature logic | Unit tests |
| Auth/query providers | Component tests with fake ports |
| HTTP adapter | Token injection, single refresh, errors, timeout |
| Contract | Pinned OpenAPI/generated type validation and fixtures |
| Navigation | Auth restoration and route gating |
| Build config | DEV/PROD pairing and missing-value failures |
| Device smoke | Login -> `/me` -> AI -> sign out |

## 10. Extensions

Add camera/files, subscriptions, push notifications, analytics, deep links, biometrics, or offline synchronization only for a product that needs them. Each extension must define permissions, privacy, failure states, environment configuration, and backend-contract ownership.
