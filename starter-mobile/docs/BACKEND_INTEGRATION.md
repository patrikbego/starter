# Backend Integration

## Contract boundary

The mobile repository consumes a published, versioned backend OpenAPI contract. It does not copy backend DTOs by hand indefinitely and does not require a sibling backend checkout.

Target starter endpoints:

| Method | Path | Auth | Mobile use |
|---|---|---|---|
| `GET` | `/health/live` | Public | Coarse reachability only |
| `GET` | `/api/v1/me` | Bearer | User profile/bootstrap |
| `POST` | `/api/v1/ai/chat` | Bearer | Stateless AI interaction |

The prototype currently calls `/actuator/health`, `/api/me`, and `/api/chat`. Migrate together with the versioned contract before template v1.

## Contract versioning

- Backend owns `openapi/openapi.yaml` and publishes a tagged artifact/release URL.
- Mobile pins a known contract version or generated-client package checksum.
- CI verifies generated types/fixtures are current.
- Additive backend changes remain compatible within `v1`.
- Breaking changes introduce `v2` and coexist until supported store clients migrate.

## Environment URL

Read `EXPO_PUBLIC_API_BASE_URL` through validated config. Rules:

- production: required HTTPS PROD URL;
- preview: required DEV HTTPS URL;
- local: localhost/LAN/tunnel only with deliberate local mode;
- trim trailing slash once and reject unexpected schemes/embedded credentials;
- never hardcode a production fallback.

## Authentication

Protected request:

```http
Authorization: Bearer <firebase-id-token>
```

On `401`:

```text
request -> 401 -> one shared forced token refresh -> retry once
                                              -> second 401 -> sign out
```

The adapter prevents refresh storms across concurrent requests and never logs the token. `403` means the session may still be valid; show a permission error rather than refreshing repeatedly.

## Request behavior

The HTTP adapter must:

- apply a finite timeout with `AbortController`;
- encode JSON only when a body exists;
- parse the standard error envelope;
- distinguish offline/timeout/server/validation/rate-limit errors;
- retry automatically only when idempotent and policy permits;
- accept/return correlation IDs for support without exposing payloads.

## Standard error

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Message must not be empty",
  "correlationId": "01K..."
}
```

Recommended UX mapping:

| Status | Client behavior |
|---|---|
| `400` | Show actionable validation message |
| `401` | Refresh once, retry once, then sign out |
| `403` | Permission message; do not refresh loop |
| `404` | Resource unavailable/navigate safely |
| `409` | Refresh conflicting state |
| `429` | Respect `Retry-After`; disable immediate resend |
| `502/503/504` | Temporary service message and deliberate retry |
| Network/timeout | Offline/timeout state; preserve user input |

## Current user

```http
GET /api/v1/me
Authorization: Bearer <token>
```

```json
{
  "id": "firebase-uid",
  "email": "user@example.com",
  "displayName": "Jane Doe",
  "createdAt": "2026-07-20T10:00:00Z"
}
```

Cache under `['me']`; clear protected query data on sign-out or user change.

## AI request

```http
POST /api/v1/ai/chat
Authorization: Bearer <token>
Content-Type: application/json

{"message":"Hello"}
```

```json
{
  "reply": "Hello!",
  "requestId": "01K..."
}
```

The starter interaction is stateless. The app may display a transient list of messages, but it must not tell users the server remembers a conversation. On failure, preserve the typed message and prevent duplicate sends while one request is pending.

## Environment pairing

| Build | Firebase | Backend |
|---|---|---|
| Local/preview | DEV Firebase | local/DEV API |
| Production | PROD Firebase | PROD API |

Build configuration validates the pairing. A mismatch commonly causes token audience/project failures and must not be discovered only after store upload.

## Integration test fixture

Mobile CI should test the client against contract fixtures/mocks. Real DEV device smoke tests cover:

1. auth restoration;
2. valid `/me`;
3. invalid token and single refresh;
4. stateless AI success;
5. validation/rate-limit/provider failure mapping;
6. sign-out clearing protected cache.
