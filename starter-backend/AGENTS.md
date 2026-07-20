# Starter Backend - Agent Context

## What is this?

A generic backend starter kit for mobile-first applications. It provides authentication, user persistence, AI chat, and Cloud Run deployment patterns that you fork and extend per product.

## Core Technology Stack

- **Backend**: Java 21, Spring Boot 3.x, Spring Security, Spring AI
- **Cloud**: Google Cloud Run (serverless), Firestore, Secret Manager, Artifact Registry
- **Auth**: Firebase Authentication
- **AI**: Spring AI + OpenRouter (OpenAI-compatible API)
- **CI/CD**: GitHub Actions, Docker, Artifact Registry

## Architecture Principles

1. **Portable Design** - Use ports-and-adapters pattern. Cloud services hide behind interfaces so Google Cloud can be swapped for AWS, Azure, or self-hosted alternatives later.

2. **Scale-to-Zero** - Serverless first (Cloud Run). No always-on infrastructure during MVP.

3. **Security by Default** - Every protected request verifies Firebase tokens. Scope data by `userId` when adding domain resources.

## Project Structure

```
src/main/java/com/starter/
  application/     - Service layer (UserService, ChatService)
  domain/          - Domain models (User, ChatMessage)
  ports/           - Interface definitions (UserRepositoryPort, AiChatPort)
  adapters/        - Implementation of ports
    gcp/           - Firestore user repository
    ai/            - Spring AI + OpenRouter adapter (+ mock for local)
    firebase/      - Firebase Auth adapter (+ mock for local profile)
  api/             - REST controllers (MeController, ChatController)
  config/          - Spring configuration (SecurityConfig, FirebaseConfig, AiConfig)
  security/        - Security filters (FirebaseAuthenticationFilter)
  logging/         - CorrelationIdFilter, request logging

src/main/resources/
  application.yml           - Base shared configuration
  application-local.yml     - Local development with mocks (@Profile: local)
  application-dev-local.yml - IntelliJ with real DEV services (@Profile: dev-local)
  application-dev.yml       - Cloud Run DEV deployment (@Profile: dev)
  application-prod.yml        - Cloud Run PROD deployment (@Profile: prod)
```

### Profile-Specific Beans

- `@Profile("local")` - Mocks: `MockFirebaseAuthService`, `MockUserRepositoryAdapter`, `MockAiChatAdapter`
- `@Profile("!local")` - Real: `FirebaseAuthServiceImpl`, `FirestoreUserRepositoryAdapter`, `SpringAiOpenRouterAdapter`

## How to Run

### Local Profile (`local`) - Full Offline Development

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Dev-Local Profile (`dev-local`) - Real DEV Cloud from IntelliJ

See `scripts/DEV_LOCAL_SETUP.md`.

### Dev / Prod Profiles

Deployed via GitHub Actions to Cloud Run. See `docs/cicd_deployment_plan.md`.

## Rules for Agents

1. Follow hexagonal architecture — business logic in `application/`, no GCP imports in `domain/`
2. Never commit secrets to YAML or git — use env vars and Secret Manager
3. All protected endpoints require Firebase Bearer token except `/actuator/health` and `/actuator/info`
4. Use `@Profile` for mock vs real adapter switching
5. Keep MVP scope minimal — extend via new ports/adapters, don't bloat the starter

## Documentation

- Architecture: `docs/backend_architecture_plan.md`
- CI/CD: `docs/cicd_deployment_plan.md`
- Auth: `docs/AUTHENTICATION.md`
- Database: `docs/DATABASE.md`
- AI: `docs/AI_INTEGRATION.md`
- New app workflow: `../docs/NEW_APP_WORKFLOW.md`
