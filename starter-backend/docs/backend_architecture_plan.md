# Backend Architecture

## 1. Purpose

The backend template is the server-side foundation for many unrelated applications. It standardizes platform concerns and contains almost no product domain.

Included in starter core:

- Firebase ID-token verification
- current-user provisioning behind a persistence port
- a minimal, stateless AI request behind a provider port
- standard validation/errors and correlation IDs
- liveness/readiness and protected diagnostics
- explicit local/DEV/PROD configuration
- repeatable Cloud Run infrastructure and delivery
- a versioned OpenAPI contract

Excluded from starter core: documents, files, OCR, subscriptions, billing, background workflows, search, RAG, organizations, and admin portals.

## 2. Current prototype versus target v1

| Concern | Prototype | Target v1 |
|---|---|---|
| Git | Child of parent repository | Independent repository |
| API | `/api/me`, `/api/chat` | OpenAPI-owned `/api/v1/me`, `/api/v1/ai/chat` |
| Health | Public actuator health/info | Minimal live/ready; diagnostics protected |
| Profiles | `local` is the default | `local` explicit; cloud config fails closed |
| AI | Provider call plus unused `sessionId` | Explicitly stateless plus rate/cost/timeout controls |
| CI/CD | Separate concurrent test/deploy workflows | Verify -> build -> deploy dependency; immutable digest promotion |
| Infrastructure | Manual GCP guides | Versioned, idempotent infrastructure code |
| Dependencies | Older Spring Boot and milestone Spring AI | Supported, tested stable baseline |

## 3. Design principles

### Purposeful ports and adapters

Abstract provider capabilities that product code may need to replace. Do not wrap every Spring class.

```text
HTTP adapter -> application use case -> domain
                         |
                         v
                 outbound port <- infrastructure adapter
```

Starter ports:

| Port | Responsibility |
|---|---|
| `UserRepositoryPort` | Load/create the starter user record |
| `AiChatPort` | Request a bounded text completion |

Firebase token verification is a security adapter. If it becomes a replaceable product choice, formalize an identity-verification port; until then, keep its types out of application/domain code.

### Security in the use case

Controllers receive the verified principal. Application services decide which user/resource may be accessed. Repositories support scoped lookups such as `(resourceId, ownerId)`; a controller-supplied user ID is never authoritative.

### Serverless, not cloud-entangled

Cloud Run and Firestore provide an inexpensive default. Their SDKs remain in adapters/configuration. Domain and application tests run without Google Cloud.

### Fail closed

Mocks require the explicit `local` profile. `dev`, `prod`, and an unset profile never create a mock verifier. Missing required variables, credentials, or invalid environment pairings stop startup.

## 4. Package layout

```text
src/main/java/com/starter/
├── api/             # HTTP controllers, DTOs, exception mapping
├── application/     # use cases and authorization decisions
├── domain/          # plain domain types
├── ports/           # outbound capability interfaces
├── adapters/
│   ├── ai/          # provider implementation and local mock
│   ├── firebase/    # identity verification adapter
│   └── gcp/         # Firestore adapter
├── config/          # bean/config validation
├── security/        # principal and filter chain
├── logging/         # correlation and safe request metadata
└── health/          # target live/ready indicators

openapi/openapi.yaml # target source of truth for public HTTP contract
infra/               # target repeatable DEV/PROD resources
```

Rules:

- `domain` imports only JDK types.
- `application` imports domain and ports, not cloud SDKs or web DTOs.
- Adapters map provider failures into application-defined failures.
- API DTOs do not expose persistence models directly.
- No prompt or response content is logged by platform filters.

## 5. Target API contract

### `GET /api/v1/me`

```http
Authorization: Bearer <firebase-id-token>
```

```json
{
  "id": "firebase-uid",
  "email": "user@example.com",
  "displayName": "Jane Doe",
  "createdAt": "2026-07-20T10:00:00Z"
}
```

The first valid call creates the record atomically/idempotently. Later token changes require an explicit policy for synchronizing email/display name.

### `POST /api/v1/ai/chat`

```json
{
  "message": "Explain this feature in one sentence"
}
```

```json
{
  "reply": "...",
  "requestId": "correlation-or-ai-request-id"
}
```

This endpoint is stateless. Conversation IDs/history are a product extension that require persistence, retention, authorization, deletion, and token-budget rules.

### Standard error

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Message must not be empty",
  "correlationId": "01K..."
}
```

Every `4xx`/`5xx`, including authentication failures, uses the same envelope. Do not leak provider or stack details.

## 6. Data

Starter core stores one `users/{firebaseUid}` document. Product collections are added with explicit ownership and lifecycle rules.

Required data concerns before adding a collection:

- authorization key and scoped query pattern;
- validation and maximum size;
- indexes and expected access paths;
- idempotency/concurrency behavior;
- retention, export/restore, and deletion;
- sensitive-field logging policy.

## 7. Profiles

| Profile | Adapters | Credentials |
|---|---|---|
| `local` | deterministic mocks or explicit emulators | none |
| `dev-local` | real DEV adapters | developer ADC/DEV provider key |
| `dev` | real DEV adapters on Cloud Run | runtime identity + DEV secrets |
| `prod` | real PROD adapters on Cloud Run | runtime identity + PROD secrets |

Use configuration validation and profile tests to prove that `dev`/`prod` cannot start with local mocks.

## 8. AI boundary

`AiChatPort` accepts the smallest provider-neutral request needed by the use case. Application code owns input validation, user quota, timeout policy, and safe metrics. The adapter owns provider request mapping and provider-specific error translation.

Model identifiers are managed configuration, not hardcoded domain decisions. A production rollout of a new model follows normal release/change control because model behavior can change product behavior.

## 9. Testing strategy

| Layer | Test |
|---|---|
| Domain/application | Fast unit tests with fake ports |
| API/security | Spring integration tests for auth, validation, errors, correlation |
| Adapters | Emulator/provider-contract tests separated from unit tests |
| Contract | OpenAPI validation plus implementation compatibility |
| Container | Build/start/health smoke test |
| Deployment | DEV smoke test using immutable digest |

No real AI provider call runs in the default PR test suite.

## 10. Extension rule

Add an extension only when a product needs it. Start with its use case and ownership/security model, then introduce the smallest necessary port and adapter. The Docsera backend is a reference for storage and subscription patterns, not code that belongs automatically in this template.
