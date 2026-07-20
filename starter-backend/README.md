# Starter Backend

Generic Spring Boot API boilerplate for mobile-first products. Deployed on Google Cloud Run with Firebase Auth, Firestore, and Spring AI.

## Quick start

**New to the project?** Start with the step-by-step run guides: [run/README.md](./run/README.md)

### Local offline development (mocks)

```bash
cd starter-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Optional: start Firebase emulators for auth/firestore testing:

```bash
firebase emulators:start --only auth,firestore
export FIRESTORE_EMULATOR_HOST=localhost:8080
export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
export GOOGLE_CLOUD_PROJECT=starter-local
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Smoke test

```bash
./scripts/test/test-local.sh
```

## Minimal API

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/actuator/health` | Public | Health check |
| GET | `/actuator/info` | Public | Build info |
| GET | `/api/me` | Bearer token | Current user profile (auto-provisioned) |
| POST | `/api/chat` | Bearer token | AI chat message |

### Example requests (local profile)

```bash
# Health (no auth)
curl http://localhost:8080/actuator/health

# Me (mock accepts any token)
curl -H "Authorization: Bearer test-token" http://localhost:8080/api/me

# Chat
curl -X POST -H "Authorization: Bearer test-token" \
  -H "Content-Type: application/json" \
  -d '{"message":"hello"}' \
  http://localhost:8080/api/chat
```

## Profiles

| Profile | Use case |
|---------|----------|
| `local` | Offline dev with mocks |
| `dev-local` | IDE connected to real DEV GCP |
| `dev` | Cloud Run DEV |
| `prod` | Cloud Run PROD |

See [scripts/DEV_LOCAL_SETUP.md](./scripts/DEV_LOCAL_SETUP.md) for dev-local setup.

## Documentation

See [docs/README.md](./docs/README.md).

## Creating a new app

Follow the monorepo guide: [../docs/NEW_APP_WORKFLOW.md](../docs/NEW_APP_WORKFLOW.md).

## Tech stack

- Java 21, Spring Boot 3.x, Spring Security, Spring AI
- Google Cloud Run, Firestore, Secret Manager, Artifact Registry
- Firebase Authentication
- OpenRouter (OpenAI-compatible API via Spring AI)

## CI/CD

GitHub Actions workflows at the monorepo root:

- `ci-backend.yml` — runs tests on PR/push
- `deploy-dev-backend.yml` — auto-deploy to Cloud Run DEV on push to `main`
- `deploy-prod-backend.yml` — manual promote to PROD

See [docs/cicd_deployment_plan.md](./docs/cicd_deployment_plan.md).
