# Backend Template v1 Scope

The prototype implements the basic loop, but template v1 is not yet complete. “Done” means verified from a clean independent repository, not merely present in the current parent workspace.

## Prototype evidence

| Capability | Current status |
|---|---|
| Spring Boot application and local profile | Implemented |
| Firebase Bearer-token filter and security integration tests | Implemented |
| Firestore user repository behind a port | Implemented |
| `GET /api/me` and current-user provisioning | Implemented |
| Spring AI/OpenRouter adapter plus local fake | Implemented |
| `POST /api/chat` with input validation | Implemented |
| Correlation/logging filters | Implemented |
| Maven test suite | Passing on 2026-07-20 |
| Dockerfile and prototype workflows | Present, not production-ready |

## Required before template v1

### Repository and contract

- [ ] Independent backend Git repository
- [ ] Workflows moved to its `.github/workflows/`
- [ ] `openapi/openapi.yaml` is the HTTP source of truth
- [ ] Routes versioned under `/api/v1`
- [ ] One error envelope for all failures
- [ ] Contract validation runs in CI

### Configuration and security

- [ ] Remove implicit `local` default; mocks require explicit opt-in
- [ ] `dev`/`prod` configuration fails closed
- [ ] Minimal public liveness; readiness/diagnostics appropriately restricted
- [ ] Production CORS rejects wildcard and invalid origins
- [ ] Cross-user authorization pattern and tests documented for extensions
- [ ] Supported stable dependency baseline selected and verified

### AI

- [ ] Endpoint documented and implemented as stateless
- [ ] Remove misleading session semantics or implement full authorized memory outside starter core
- [ ] Per-user rate limit and environment budget
- [ ] Provider timeout/concurrency/retry policy
- [ ] Stable `429`/provider error mapping with `Retry-After` where appropriate
- [ ] Safe metrics without prompt/reply content

### Data and operations

- [ ] Atomic/idempotent current-user creation behavior tested
- [ ] DEV/PROD data isolation verified
- [ ] Backup/export and retention responsibility documented
- [ ] Structured cloud logs and request IDs verified
- [ ] Smoke tests exercise readiness, auth rejection, `/me`, and mocked/stubbed AI behavior

### Delivery

- [ ] CI is a hard dependency of deployment
- [ ] Container built once and deployed to DEV by digest
- [ ] PROD workflow deploys the same eligible digest and contains no build step
- [ ] OIDC/WIF replaces long-lived cloud keys
- [ ] Protected production environment/approval configured
- [ ] Release metadata and rollback drill completed
- [ ] Repeatable infrastructure code provisions a clean DEV project

## Explicit non-goals

- File/object storage
- OCR/document processing
- Subscriptions/payments
- Async workers
- Search, embeddings, RAG, tools, or agents
- Organizations/admin console
- Multi-region/high-availability architecture

These are product extensions. Adding them to the template without a product need makes every future app harder to understand and secure.

## End-to-end acceptance test

From clean clones and a freshly provisioned DEV environment:

1. CI passes and publishes one image digest.
2. DEV deploys that digest.
3. Public liveness is minimal and readiness succeeds.
4. An unauthenticated `/api/v1/me` request receives standard `401` JSON.
5. A valid DEV Firebase token provisions/returns the correct user.
6. A valid AI request returns a bounded stateless reply.
7. The next over-limit request receives the documented rate-limit response.
8. The eligible digest can be approved for PROD without rebuilding.
9. The service can roll back to the previous revision.
