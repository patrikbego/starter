# Starter Backend

Generic Spring Boot API boilerplate for mobile-first products. Deployed on Google Cloud Run with Firebase Auth, Firestore, and Spring AI.

**Status:** Docs phase — application code and CI workflows will follow.

## Quick start (after implementation)

```bash
# Local offline development
firebase emulators:start --only auth,firestore
export FIRESTORE_EMULATOR_HOST=localhost:8080
export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Minimal API

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/actuator/health` | Public | Health check |
| GET | `/api/me` | Bearer token | Current user profile |
| POST | `/api/chat` | Bearer token | AI chat message |

## Documentation

See [docs/README.md](./docs/README.md).

## Creating a new app

Follow the monorepo guide: [../docs/NEW_APP_WORKFLOW.md](../docs/NEW_APP_WORKFLOW.md).

## Tech stack

- Java 21, Spring Boot 3.x, Spring Security, Spring AI
- Google Cloud Run, Firestore, Secret Manager, Artifact Registry
- Firebase Authentication
- OpenRouter (OpenAI-compatible API via Spring AI)
