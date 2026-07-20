# AI Integration

## Scope

The starter proves one secure, server-side AI call. It does not provide conversation memory, RAG, agents, tools, embeddings, or product prompts.

```text
AI controller -> application service -> AiChatPort -> provider adapter
                                      -> local deterministic adapter
```

The application owns policy. The adapter owns provider protocol.

## Target API

```http
POST /api/v1/ai/chat
Authorization: Bearer <firebase-id-token>
Content-Type: application/json

{"message":"Give me one concise idea"}
```

```json
{
  "reply": "...",
  "requestId": "01K..."
}
```

Constraints:

- authenticated user required;
- non-blank, bounded Unicode input;
- one provider call per accepted request;
- stateless response;
- provider details do not leak through errors;
- prompt/reply content is not logged by default.

The prototype's `sessionId` is only echoed/generated; it does not load history and must not be documented as multi-turn memory.

## Port design

Use provider-neutral input/output that can grow without importing Spring AI/provider types into application code:

```java
public interface AiChatPort {
    AiReply complete(AiPrompt prompt);
}
```

`AiPrompt` can contain the validated message and policy-selected options. End users do not choose unrestricted provider/model identifiers.

## Required controls before template v1

| Control | Behavior |
|---|---|
| Authentication | Derive quota key from verified UID |
| Input limit | Reject before provider call |
| Rate limit | Per UID plus coarse IP/instance protection |
| Usage budget | Daily/monthly user and environment cost cap |
| Timeout | Bound provider latency and release server resources |
| Retry | Only safe transient failures; short capped backoff |
| Concurrency | Limit in-flight provider calls |
| Error mapping | Stable `429`, `502`, `503`, `504` application errors |
| Telemetry | Duration, outcome, model/config version, token/cost counts when available |
| Privacy | No prompt/reply logging; define provider retention policy per product |

Return `Retry-After` when the client can safely retry. Do not claim `429` support until these controls and tests exist.

## Configuration

Configuration is environment-managed:

```text
OPENAI_API_KEY        # Secret Manager
AI_MODEL              # approved model identifier
AI_REQUEST_TIMEOUT    # bounded duration
AI_MAX_INPUT_CHARS    # server-side validation
AI_USER_RATE_LIMIT    # policy value
AI_USER_BUDGET        # policy value
```

The local profile uses a deterministic fake and no provider key. DEV uses a low-limit key/account where possible. PROD has separate budgets and alerts.

OpenRouter is the initial OpenAI-compatible provider. Spring AI/provider classes stay inside the adapter/configuration boundary so a product can swap providers without changing application use cases.

## Model changes

A model identifier is behavioral configuration, not a harmless secret rotation. Change it through a reviewed release with:

- representative evaluation cases;
- latency/cost comparison;
- safety and output-format checks;
- rollback to the prior configuration;
- recorded configuration/model version in metrics.

## Conversation memory extension

Add only when a product requires it. It needs:

- authenticated ownership of conversations;
- a persisted ordered message model;
- truncation/token-budget rules;
- concurrency/idempotency semantics;
- retention, export, and deletion;
- prompt-injection and tool authorization policy;
- tests proving one user cannot access another user's history.

A client-provided `sessionId` alone is not conversation memory or an authorization boundary.

## RAG/tool extension

Keep retrieval and tools as separate ports/use cases. Treat retrieved text and tool output as untrusted input. Enforce tool authorization outside the model and include citations/traceability in the product contract.
