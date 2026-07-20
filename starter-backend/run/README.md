# Run the Backend

Choose one environment:

| Guide | Process | Services |
|---|---|---|
| [run-local.md](./run-local.md) | Local JVM | Mock auth, user repository, and AI |
| [run-dev.md](./run-dev.md) | Local JVM or Cloud Run | Real DEV Firebase/Firestore/AI |
| [run-prod.md](./run-prod.md) | Cloud Run | Approved immutable production artifact |

This guide assumes `starter-backend` is the repository root. During the current docs-first workspace phase it is still nested under a parent Git repository; CI instructions become executable after repository extraction.

## Fastest path

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Then:

```bash
curl http://localhost:8080/actuator/health
curl -H 'Authorization: Bearer local-test-token' http://localhost:8080/api/me
```

Prototype routes are unversioned. Target v1 routes are documented in [the architecture](../docs/backend_architecture_plan.md).

## Verify before pushing

```bash
./mvnw verify
docker build -t starter-backend:local .
```

## Safety rules

- Always select `local`, `dev-local`, `dev`, or `prod` explicitly.
- Never use the local mock profile in Cloud Run.
- Never commit credentials or `.env` values.
- Do not use the current prototype production workflow as evidence of immutable promotion; see [CI/CD](../docs/cicd_deployment_plan.md).
