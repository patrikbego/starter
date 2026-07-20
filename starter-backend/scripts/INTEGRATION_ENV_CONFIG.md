# Starter Integration & Environment Configuration Guide

Environment variables and configuration for each Spring Boot profile.

---

## Environment Variables by Profile

### `local` Profile (Full Offline Development)

**No secrets required.** Mocks and emulators with defaults.

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `FIRESTORE_EMULATOR_HOST` | No | `localhost:8080` | Firestore emulator |
| `FIREBASE_AUTH_EMULATOR_HOST` | No | `localhost:9099` | Firebase Auth emulator |
| `LOCAL_AI_URL` | No | `http://localhost:8081/v1` | Mock AI endpoint (unused if MockAiChatAdapter) |
| `GOOGLE_CLOUD_PROJECT` | No | `starter-local` | Emulator project ID |

---

### `dev-local` Profile (Real Cloud from IntelliJ)

| Variable | Required | Description |
|----------|----------|-------------|
| `GOOGLE_APPLICATION_CREDENTIALS` | Yes | Path to service account JSON |
| `GCP_PROJECT_ID` | No | Defaults to `starter-dev` |
| `OPENAI_API_KEY` | Yes | OpenRouter API key |
| `FIREBASE_AUTH_EMULATOR_HOST` | No | Set to `localhost:9099` to use auth emulator |

Optional:

| Variable | Default | Description |
|----------|---------|-------------|
| `ACTUATOR_PASSWORD` | (empty) | Admin actuator password |

---

### `dev` Profile (Cloud Run DEV)

| Variable | Required | Description |
|----------|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | Yes | `dev` |
| `GCP_PROJECT_ID` | Yes | `starter-dev` |
| `STARTER_CORS_ALLOWED_ORIGINS` | Yes | `*` or specific origins |

**Secrets (Secret Manager):**

| Secret name | Env var |
|-------------|---------|
| `openai-api-key` | `OPENAI_API_KEY` |
| `actuator-password` | `ACTUATOR_PASSWORD` |

---

### `prod` Profile (Cloud Run PROD)

| Variable | Required | Description |
|----------|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | Yes | `prod` |
| `GCP_PROJECT_ID` | Yes | `starter-prod` |
| `STARTER_CORS_ALLOWED_ORIGINS` | Yes | Specific allowed origins |

**Secrets:** Same as `dev` with production values.

---

## Integration by Profile

### `local`

| Integration | Implementation |
|-------------|----------------|
| Firebase Auth | `MockFirebaseAuthService` |
| Firestore | `MockUserRepositoryAdapter` (in-memory) |
| AI | `MockAiChatAdapter` |
| CORS | Permissive (`localhost`) |
| Logging | Console, plain text |

### `dev-local`

| Integration | Implementation |
|-------------|----------------|
| Firebase Auth | Real or emulator |
| Firestore | Real `starter-dev` |
| AI | Real OpenRouter |
| Logging | Console, plain text |

### `dev` / `prod`

| Integration | Implementation |
|-------------|----------------|
| Container | Cloud Run |
| Firebase Auth | Real |
| Firestore | Real |
| AI | OpenRouter via Secret Manager |
| CORS | Configurable |
| Actuator | HTTP Basic (`admin` / password) |
| Logging | JSON (Cloud Logging) |

---

## Custom configuration namespace

Planned `starter.*` properties in YAML:

```yaml
starter:
  cors:
    allowed-origins: ${STARTER_CORS_ALLOWED_ORIGINS:*}
  chat:
    max-message-length: 4000
```

---

## Related docs

- [DEV_LOCAL_SETUP.md](./DEV_LOCAL_SETUP.md)
- [COMMON_GCP_SETUP.md](./COMMON_GCP_SETUP.md)
- [../../docs/ENVIRONMENT_MATRIX.md](../../docs/ENVIRONMENT_MATRIX.md)
