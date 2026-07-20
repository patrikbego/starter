# Starter Backend

Reusable Spring Boot backend foundation for new applications. It provides identity verification, a minimal user record, a provider-neutral AI use case, operational health, and a Cloud Run delivery pattern. Product domains are added after creating a product repository from a tagged template release.

> Status: functional prototype. The local API and tests work, but the repository split, versioned OpenAPI contract, fail-closed profile change, dependency upgrade, AI limits, infrastructure-as-code, and corrected promotion workflow are still required before template v1.

## Prototype quick start

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

```bash
curl http://localhost:8080/actuator/health
curl -H 'Authorization: Bearer local-test-token' http://localhost:8080/api/me
curl -X POST http://localhost:8080/api/chat \
  -H 'Authorization: Bearer local-test-token' \
  -H 'Content-Type: application/json' \
  -d '{"message":"hello"}'
```

The explicit `local` profile uses mock auth, persistence, and AI. Never deploy it.

## Target template contract

| Method | Path | Access | Purpose |
|---|---|---|---|
| `GET` | `/health/live` | Public | Minimal liveness |
| `GET` | `/health/ready` | Deployment checks | Dependency readiness |
| `GET` | `/api/v1/me` | Firebase Bearer token | Current user |
| `POST` | `/api/v1/ai/chat` | Firebase Bearer token | Stateless AI request |

The current prototype routes are unversioned. The OpenAPI contract and `/api/v1` migration are part of the v1 roadmap.

## Architecture

```text
api -> application -> domain
             |          ^
             v          |
            ports <- adapters
```

- Domain and application code have no Firestore, Firebase, or AI-provider types.
- Authenticated identity comes from the verified backend principal, never from request data.
- External systems sit behind purposeful ports.
- Local mocks are explicit, deterministic adapters.

## Profiles

| Profile | Use |
|---|---|
| `local` | Explicit offline mocks/emulators |
| `dev-local` | Local JVM connected to DEV services |
| `dev` | DEV Cloud Run |
| `prod` | Production Cloud Run |

Target v1 has no default `local` profile. Missing cloud configuration must fail startup.

## Verify

```bash
./mvnw verify
docker build -t starter-backend:local .
```

## Documentation

Start with [docs/README.md](./docs/README.md), then read:

- [Architecture](./docs/backend_architecture_plan.md)
- [API and authentication](./docs/AUTHENTICATION.md)
- [Database](./docs/DATABASE.md)
- [AI integration](./docs/AI_INTEGRATION.md)
- [Security](./docs/SECURITY.md)
- [CI/CD and rollback](./docs/cicd_deployment_plan.md)
- [Scope and readiness](./docs/MVP_SCOPE_CHECKLIST.md)

The companion mobile template is a separate repository and integrates only through the published API contract.
