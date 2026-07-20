# Run Backend Locally

The explicit `local` profile uses deterministic in-process adapters and does not require Google Cloud or an AI key.

## Prerequisites

- JDK 21 available to Maven
- `curl`

## Start

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Expected base URL: `http://localhost:8080`.

## Smoke test

```bash
curl -i http://localhost:8080/actuator/health
```

```bash
curl -i http://localhost:8080/api/me \
  -H 'Authorization: Bearer local-test-token'
```

```bash
curl -i -X POST http://localhost:8080/api/chat \
  -H 'Authorization: Bearer local-test-token' \
  -H 'Content-Type: application/json' \
  -d '{"message":"hello"}'
```

The local AI adapter returns a deterministic echo. A returned `sessionId` in the prototype does not imply conversation memory.

## Automated verification

```bash
./mvnw verify
./scripts/test/test-local.sh
```

## Optional emulators

The current local implementation uses an in-memory user repository. Setting Firestore emulator variables alone does not switch that repository. Treat Firebase/Firestore emulator coverage as a separate adapter-integration mode to implement before template v1.

## Troubleshooting

| Symptom | Check |
|---|---|
| Port already used | Stop the old process or set a deliberate local port |
| `/api/me` returns `401` | Include any non-empty Bearer token under `local` |
| Real provider/cloud contacted | Confirm the active profile is exactly `local` |
| Java mismatch | Run `java -version`; build target is Java 21 |
