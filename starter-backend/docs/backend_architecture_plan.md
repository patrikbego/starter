# Starter Backend Architecture Plan

## 1. Product Vision

The starter backend is a **generic API foundation** for mobile-first applications. It is not tied to a specific product domain — you fork it, rename placeholders, and add your business logic.

Out of the box it provides:

- Firebase Authentication verification on every protected request
- User auto-provisioning in Firestore on first authenticated call
- A minimal AI chat endpoint via Spring AI + OpenRouter
- Health checks for Cloud Run and client reachability
- Production-ready deployment on Google Cloud Run with DEV/PROD environments

The **mobile client** (Expo) is documented in [starter-mobile](../starter-mobile/) — `docs/mobile_architecture_plan.md`. This document defines the backend APIs and infrastructure those apps call.

### 1.1 Implementation Status

| Component | Status |
|-----------|--------|
| Documentation | Complete (docs phase) |
| API: auth, `/api/me`, `/api/chat` | Planned |
| Firestore user repository | Planned |
| Spring AI + OpenRouter adapter | Planned |
| GitHub Actions deploy workflows | Planned |
| File upload / GCS | Extension only — see `STORAGE_EXTENSION.md` |

---

## 2. Key Architectural Principles

### 2.1 Google Cloud First, But Portable

The first version uses Google Cloud services because they fit well with mobile-first, serverless, AI-enabled applications. However, the backend avoids deep vendor lock-in by using a ports-and-adapters architecture.

Cloud-specific technologies are hidden behind interfaces:

| Port | Purpose |
|------|---------|
| `UserRepositoryPort` | User profile persistence |
| `AiChatPort` | AI completion / chat |
| `AuthService` (implicit via filter) | Firebase token verification |

Future adapters can target PostgreSQL, AWS S3, Anthropic API, etc. without changing `application/` services.

### 2.2 Pay-Per-Usage / Scale-to-Zero

Preferred deployment: **Google Cloud Run** — scales to zero when idle.

Avoid during MVP:

- Kubernetes clusters
- Always-running VMs
- Self-hosted databases or vector stores

### 2.3 Simple MVP First

The starter focuses on:

- Secure API with Firebase auth
- User profile persistence
- One AI endpoint to prove integration
- Production-ready security basics (CORS, actuator protection, structured logging)

Advanced features (file upload, subscriptions, search, async workers) are **extension paths** documented separately.

---

## 3. Recommended Technology Stack

### 3.1 Backend

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Security
- Spring AI (OpenAI-compatible starter)
- Firebase Admin SDK
- Google Cloud Java SDKs (Firestore)
- Lombok
- Logback + logstash-logback-encoder (JSON in cloud profiles)

### 3.2 Google Cloud Services

| Service | MVP use |
|---------|---------|
| Cloud Run | API hosting |
| Firestore | User profiles |
| Secret Manager | API keys, actuator password |
| Artifact Registry | Docker images |
| Cloud Logging | Structured logs from Cloud Run |

Optional (extensions): Cloud Storage, Cloud Tasks, Document AI.

### 3.3 Authentication

- Firebase Authentication (client-side sign-in)
- Firebase Admin SDK in backend for token verification
- Firebase App Check (post-MVP, abuse prevention)

### 3.4 AI

- Spring AI with OpenAI-compatible API
- OpenRouter as provider (`https://openrouter.ai/api/v1`)
- API key in Secret Manager (`openai-api-key`)

---

## 4. Package Structure

```text
com.starter/
├── StarterApplication.java
├── api/
│   ├── MeController.java            # GET /api/me
│   ├── ChatController.java          # POST /api/chat
│   ├── dto/                         # Request/response DTOs
│   └── GlobalExceptionHandler.java
├── application/
│   ├── UserService.java             # getOrCreateUser, getCurrentUser
│   └── ChatService.java             # sendMessage, session handling
├── domain/
│   ├── User.java
│   └── ChatMessage.java
├── ports/
│   ├── UserRepositoryPort.java
│   └── AiChatPort.java
├── adapters/
│   ├── gcp/
│   │   └── FirestoreUserRepositoryAdapter.java
│   ├── ai/
│   │   ├── SpringAiOpenRouterAdapter.java
│   │   └── MockAiChatAdapter.java       # @Profile("local")
│   └── firebase/
│       ├── FirebaseAuthServiceImpl.java
│       └── MockFirebaseAuthService.java # @Profile("local")
├── config/
│   ├── SecurityConfig.java
│   ├── FirebaseConfig.java
│   └── AiConfig.java
├── security/
│   ├── FirebaseAuthenticationFilter.java
│   ├── FirebaseAuthenticationToken.java
│   └── FirebaseUser.java
└── logging/
    ├── CorrelationIdFilter.java
    └── RequestResponseLoggingFilter.java
```

---

## 5. API Contract (MVP)

### 5.1 Health

```http
GET /actuator/health
```

Public. Returns Spring Actuator health response. Used by Cloud Run and mobile reachability check.

### 5.2 Current User

```http
GET /api/me
Authorization: Bearer <firebase_id_token>
```

Response:

```json
{
  "id": "firebase-uid",
  "email": "user@example.com",
  "displayName": "Jane Doe",
  "createdAt": "2026-01-15T10:00:00Z"
}
```

On first call, the backend creates the user record in Firestore if it does not exist.

### 5.3 AI Chat

```http
POST /api/chat
Authorization: Bearer <firebase_id_token>
Content-Type: application/json

{
  "message": "Hello, what can you do?",
  "sessionId": "optional-uuid-for-multi-turn"
}
```

Response:

```json
{
  "reply": "I'm a starter kit assistant...",
  "sessionId": "uuid"
}
```

Errors: `400` invalid body, `401` unauthenticated, `429` rate limited, `502` AI provider failure.

---

## 6. Data Model (MVP)

### Collection: `users`

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Firebase UID (document ID) |
| `email` | string | From Firebase token |
| `displayName` | string | Optional |
| `createdAt` | timestamp | First provision time |
| `updatedAt` | timestamp | Last update |

No other collections in the starter MVP. Add domain collections when forking.

See [DATABASE.md](./DATABASE.md) for repository port details.

---

## 7. Spring Profiles

| Profile | Use case | Adapters |
|---------|----------|----------|
| `local` | Offline dev | All mocks |
| `dev-local` | IDE → real DEV GCP | Real Firestore + Firebase; optional auth emulator |
| `dev` | Cloud Run DEV | All real |
| `prod` | Cloud Run PROD | All real |

Configuration files: `application-{profile}.yml`.

---

## 8. Security Summary

- Stateless sessions (`SessionCreationPolicy.STATELESS`)
- CSRF disabled (stateless API)
- Public: `/actuator/health`, `/actuator/info`
- Admin (HTTP Basic): other `/actuator/**` endpoints
- All `/api/**` require valid Firebase Bearer token

See [SECURITY.md](./SECURITY.md) and [AUTHENTICATION.md](./AUTHENTICATION.md).

---

## 9. Build Phases

### Phase 1: Foundation (starter MVP)

- [ ] Project scaffold (Maven, Spring Boot, profiles)
- [ ] Firebase auth filter + SecurityConfig
- [ ] UserService + FirestoreUserRepositoryAdapter
- [ ] MeController (`GET /api/me`)
- [ ] AiChatPort + SpringAiOpenRouterAdapter + MockAiChatAdapter
- [ ] ChatController (`POST /api/chat`)
- [ ] Correlation ID + structured logging
- [ ] Dockerfile + GitHub Actions deploy-dev / deploy-prod
- [ ] Local profile with mocks

### Phase 2: Extensions (per product)

- [ ] File upload (GCS signed URLs) — `STORAGE_EXTENSION.md`
- [ ] Subscriptions (RevenueCat webhooks)
- [ ] Async processing (Cloud Tasks)
- [ ] Search / RAG (embeddings, vector search)

---

## 10. Future Extensions

When forking for a product that needs more than the starter MVP:

| Extension | Suggested approach |
|-----------|-------------------|
| File storage | `ObjectStoragePort` + GCS adapter — see `STORAGE_EXTENSION.md` |
| Subscriptions | `SubscriptionPort` + RevenueCat adapter + webhook controller |
| Background jobs | Cloud Tasks + worker endpoints in same Cloud Run service |
| Vector search | `EmbeddingPort` + `VectorSearchPort`; Firestore vector or dedicated DB |
| Multi-tenant admin | Separate `ROLE_ADMIN` users, admin-only controllers |

Reference implementation patterns: [docsera](https://github.com/patrikbego/docsera) repository.

---

## Related docs

- [cicd_deployment_plan.md](./cicd_deployment_plan.md)
- [MVP_SCOPE_CHECKLIST.md](./MVP_SCOPE_CHECKLIST.md)
- [AI_INTEGRATION.md](./AI_INTEGRATION.md)
- [../docs/NEW_APP_WORKFLOW.md](../../docs/NEW_APP_WORKFLOW.md)
