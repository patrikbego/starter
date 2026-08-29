# OPENROUTER_AI.md — AI Chat Integration per Product App

**Status: implemented.** The backend exposes `POST /api/v1/ai/chat`, backed by an
OpenAI-compatible `/v1/chat/completions` endpoint through Spring AI. The template is wired for
[OpenRouter](https://openrouter.ai) (key prefix `sk-or-v1-`); any OpenAI-compatible provider
works by changing the base URL/model. Implementation details live in
[starter-backend/docs/AI_INTEGRATION.md](../starter-backend/docs/AI_INTEGRATION.md).

## Where the implementation lives

| Piece | Path |
|---|---|
| Port (adapter boundary) | [`starter-backend/src/main/java/com/starter/ports/AiChatPort.java`](../starter-backend/src/main/java/com/starter/ports/AiChatPort.java) |
| OpenRouter adapter (SDK/HTTP lives only here) | [`starter-backend/src/main/java/com/starter/adapters/ai/SpringAiOpenRouterAdapter.java`](../starter-backend/src/main/java/com/starter/adapters/ai/SpringAiOpenRouterAdapter.java) |
| Local mock (no network) | [`starter-backend/src/main/java/com/starter/adapters/ai/MockAiChatAdapter.java`](../starter-backend/src/main/java/com/starter/adapters/ai/MockAiChatAdapter.java) |
| Use case + abuse guard | [`starter-backend/src/main/java/com/starter/application/ChatService.java`](../starter-backend/src/main/java/com/starter/application/ChatService.java), [`AiRequestGuard.java`](../starter-backend/src/main/java/com/starter/application/AiRequestGuard.java) |
| Guardrail config binding | [`starter-backend/src/main/java/com/starter/config/AiGuardConfig.java`](../starter-backend/src/main/java/com/starter/config/AiGuardConfig.java) |
| API route | `POST /api/v1/ai/chat` ([ChatController.java](../starter-backend/src/main/java/com/starter/api/ChatController.java)) |
| Provider/model config | `starter-backend/src/main/resources/application.yml` → `spring.ai.openai.*` + `starter.ai.*` |

## Verify (per environment)

1. Deploy with the key mounted; then, with a Firebase token from that environment:

```bash
curl -s -X POST https://<app>-api-<env>.run.app/api/v1/ai/chat \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"message":"Say hi in five words"}'
```

   Unauthenticated → `401`; over-quota → `429 RATE_LIMITED` + `Retry-After`; over-long input →
   `400 INPUT_LIMIT_EXCEEDED`; provider unreachable/bad key → `502 AI_PROVIDER_ERROR`; feature
   off → `503 AI_DISABLED` (codes from `GlobalExceptionHandler`). A truncated key surfaces as
   `AI_PROVIDER_ERROR` (provider 401) — re-run `set-secrets.sh` with the full key and redeploy.
2. Bound the reply: responses are size-capped and stateless; nothing about the prompt or reply
   may appear in logs.

## Rules that must not be broken

- **One API key per app per environment** — never shared; spend tracking depends on it.
- **Keys live in Secret Manager only** — never in the repo, GitHub variables, EAS config, or
  client config; the mobile app never sees a provider key.
- **Never log prompt or reply content** — telemetry is outcome-only (backend security rules).
- **Limits are template-owned** — tune `AI_*` defaults in the starter and sync down
  ([docs/UPSTREAM_SYNC.md](../docs/UPSTREAM_SYNC.md)); do not fork the guard logic app-side.

## Per-app key rule

✅ Reuse the OpenRouter **account** across all your apps. ❌ Never share a key: **create a new
API key per app** so spend is tracked per product, and feed it only to that app's Secret
Manager (DEV and PROD get separate keys/values).

## Get a key (click-path)

1. Sign up at [openrouter.ai](https://openrouter.ai).
2. Keys → **Create API key** → copy the `sk-or-v1-…` string (shown once).
3. Add a small credit balance if needed (Chat Completions cost fractions of a cent per call).

## Where the key lives

GCP **Secret Manager**, not GitHub and not the repo:

```bash
cd starter-backend/infra
./scripts/set-secrets.sh dev --openai-api-key 'sk-or-v1-…' --actuator-password 'change-me'
```

Repeat with `prod` (and the PROD key) before promoting. The deploy workflows mount it via
`--set-secrets "OPENAI_API_KEY=openai-api-key:latest,…"`. `set-secrets.sh` reads `project_id`
from `<env>.tfvars` — do not hand-point it at another app's project.

⚠️ **Truncated-key trap** (hit 2026-08-22): a key captured as `sk-or-v1-cae7…` produces `401`
from `/api/v1/ai/chat`. If AI calls 401, re-run `set-secrets.sh` with the full key and
redeploy — the secret version must be `latest`-mounted at deploy time.

## Runtime knobs (deploy workflows)

Set in the `--set-env-vars` of the deploy workflow (template defaults; apps tune via
template changes or variables, not hand-edits — see [docs/UPSTREAM_SYNC.md](../docs/UPSTREAM_SYNC.md)):

| Variable | Template default | Meaning |
|---|---|---|
| `AI_ENABLED` | `true` | master switch (`false` → `503 AI_DISABLED`) |
| `AI_CHAT_MODEL` | `deepseek/deepseek-v4-flash-0731` | OpenRouter model id |
| `AI_REQUEST_TIMEOUT` | `30s` | upstream call timeout |
| `AI_MAX_INPUT_CHARS` | `4000` | input cap per request |
| `AI_MAX_REQUESTS_PER_USER` | `120` | quota per window |
| `AI_RATE_LIMIT_WINDOW` | `1h` | quota window |

## Behavior guarantees

- Fail closed: provider errors, quota, and timeouts surface as the standard error envelope
  `{code, message, correlationId}` — safe outcome-only telemetry, no provider internals.
- Never log prompt or reply content (backend security rules).
- Rate limiting uses a per-user store (`adapters/ratelimit`); local dev uses
  `MockAiChatAdapter` (no external calls) behind the `AiChatPort`.
