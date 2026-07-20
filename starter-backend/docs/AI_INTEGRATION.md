# AI Integration

The starter backend integrates AI via **Spring AI** with an **OpenAI-compatible API** (OpenRouter). The MVP exposes a single `POST /api/chat` endpoint.

## Architecture

```text
ChatController → ChatService → AiChatPort
                                  ├── SpringAiOpenRouterAdapter (@Profile !local)
                                  └── MockAiChatAdapter (@Profile local)
```

Business logic in `ChatService` — adapters only handle provider communication.

## AiChatPort

```java
public interface AiChatPort {
    String complete(String userMessage, Optional<String> sessionId);
}
```

Extend to multi-turn when needed:

```java
String complete(List<ChatMessage> history, String userMessage);
```

## API contract

### Request

```http
POST /api/chat
Authorization: Bearer <firebase_id_token>
Content-Type: application/json

{
  "message": "Hello!",
  "sessionId": "optional-uuid"
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `message` | Yes | User message (max length enforced in validation) |
| `sessionId` | No | For multi-turn; server generates if omitted |

### Response

```json
{
  "reply": "Hello! I'm the starter kit assistant.",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Errors

| Status | Cause |
|--------|-------|
| `400` | Empty or invalid message |
| `401` | Missing/invalid token |
| `429` | Rate limit (future) |
| `502` | OpenRouter / AI provider failure |

## Spring AI configuration (planned)

### application.yml (base)

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: https://openrouter.ai/api/v1
      chat:
        options:
          model: openai/gpt-4o-mini
          temperature: 0.7
```

### Local profile

```yaml
spring:
  ai:
    openai:
      api-key: mock-key
      base-url: ${LOCAL_AI_URL:http://localhost:8081/v1}
```

With `local` profile, `MockAiChatAdapter` returns deterministic echo responses without calling OpenRouter.

### Cloud profiles (dev / prod)

API key from Secret Manager:

```bash
# Cloud Run deploy
--set-secrets=OPENAI_API_KEY=openai-api-key:latest
```

## OpenRouter setup

1. Create account at [openrouter.ai](https://openrouter.ai)
2. Generate API key (`sk-or-v1-...`)
3. Store in GCP Secret Manager as `openai-api-key`
4. Set locally: `export OPENAI_API_KEY=sk-or-v1-...`

## Mock adapter (local profile)

`MockAiChatAdapter` behavior:

- Returns `"Echo: {message}"` or a fixed greeting
- No network calls
- Enables offline development and fast tests

## Session handling (MVP)

MVP can be **stateless** (each request independent) or store session ID in memory/Firestore for multi-turn.

Recommended MVP: generate `sessionId` per request if not provided; optional Firestore `chat_sessions` collection in Phase 2.

## Rate limiting (future)

When adding limits:

- Enforce in `ChatService` before calling `AiChatPort`
- Return `429` with `Retry-After` header
- Per-user counters in Firestore or Redis

## Model selection

Default: `openai/gpt-4o-mini` (cost-effective for starter).

Change via `spring.ai.openai.chat.options.model` per profile. OpenRouter supports many models — see [openrouter.ai/models](https://openrouter.ai/models).

## Extension: RAG

Not in starter MVP. When needed:

1. Add `EmbeddingPort` and `VectorSearchPort`
2. Retrieve context in `ChatService` before calling `AiChatPort`
3. Include citations in response DTO

Reference: docsera architecture plan (RAG section).

## Related docs

- [backend_architecture_plan.md](./backend_architecture_plan.md)
- [MVP_SCOPE_CHECKLIST.md](./MVP_SCOPE_CHECKLIST.md)
- [scripts/INTEGRATION_ENV_CONFIG.md](../scripts/INTEGRATION_ENV_CONFIG.md)
