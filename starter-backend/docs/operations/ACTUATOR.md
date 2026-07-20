# Spring Boot Actuator Documentation

Actuator endpoints for the starter API.

## Endpoints

### Public (no authentication)

- **`GET /actuator/health`**
  - Application health for Cloud Run liveness probes and mobile reachability checks.
  - `200 OK` — healthy
  - `503 Service Unavailable` — unhealthy

- **`GET /actuator/info`**
  - Build and git metadata (when configured).

### Secured (`ROLE_ADMIN`, HTTP Basic)

- **`GET /actuator/metrics`** (if exposed)
- **`GET /actuator/env`** (if exposed)
- **`GET /actuator/loggers`** (if exposed)

Access with:

```bash
curl -u admin:$ACTUATOR_PASSWORD https://starter-api-dev-XXX.run.app/actuator/metrics
```

## Configuration (planned)

In `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized
  info:
    build:
      enabled: true
    git:
      mode: full
```

Cloud profiles (`dev`, `prod`): set `ACTUATOR_PASSWORD` from Secret Manager.

## Build information

Generated during Maven build:

- `spring-boot-maven-plugin` — `build-info`
- `git-commit-id-maven-plugin` — git commit in `/actuator/info`

## Cloud Run health check

Cloud Run automatically probes the service. Ensure:

- App listens on `$PORT` (default 8080)
- `/actuator/health` responds within startup timeout
- Firestore/Firebase failures may mark health down if configured in custom `HealthIndicator`

## Related docs

- [SECURITY.md](../SECURITY.md)
- [MVP_SCOPE_CHECKLIST.md](../MVP_SCOPE_CHECKLIST.md)
