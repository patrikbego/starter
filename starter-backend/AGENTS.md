# Starter Backend Agent Context

## Purpose

This is a generic backend template, not a product. Keep identity, API, data, AI, observability, and delivery foundations reusable; add no Docsera-specific domain behavior.

## Status

The code is a functional prototype still tracked by a parent workspace repository. Target v1 is an independent repository with its own workflows, OpenAPI contract, fail-closed profiles, stable dependency baseline, AI limits, infrastructure code, and immutable promotion. Read `docs/README.md` and `docs/MVP_SCOPE_CHECKLIST.md` before claiming completion.

## Stack

- Java 21 and Spring Boot/Security
- Firebase Authentication
- Firestore behind a repository port
- Spring AI/OpenAI-compatible provider behind `AiChatPort`
- Cloud Run, Secret Manager, Artifact Registry
- GitHub Actions with OIDC/WIF

## Boundaries

```text
api -> application -> domain
             |          ^
             v          |
            ports <- adapters
```

- No cloud/provider/web types in `domain`.
- Application services own authorization and policy.
- Adapters own SDK mapping and provider failures.
- The backend owns `openapi/openapi.yaml` in target v1.

## Security rules

1. Derive identity/ownership only from the verified principal.
2. Scope protected lookups by resource plus owner/tenant.
3. Local mocks require explicit `local`; never deploy them.
4. Never log tokens, secrets, AI prompt/reply content, or sensitive bodies.
5. Keep secrets in Secret Manager and CI cloud access on short-lived OIDC.
6. Add rate, budget, timeout, and concurrency controls before real AI production use.

## Current and target routes

Prototype: `/actuator/health`, `/api/me`, `/api/chat`.

Target v1: `/health/live`, `/health/ready`, `/api/v1/me`, `/api/v1/ai/chat` with one standard error envelope.

## Verification

```bash
./mvnw verify
docker build -t starter-backend:local .
```

Deployment must be gated on verification and PROD must use the exact DEV-tested image digest without rebuilding.
