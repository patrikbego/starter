# Security

Security configuration for the starter backend API.

## Security filter chain

Configured in `SecurityConfig` (planned).

| Path pattern | Access |
|--------------|--------|
| `/actuator/health`, `/actuator/info` | Public |
| `/actuator/**` (other) | `ROLE_ADMIN` + HTTP Basic |
| `/api/**` | Authenticated (Firebase Bearer token) |
| All other routes | Authenticated |

## Session and CSRF

- **Stateless** — `SessionCreationPolicy.STATELESS`
- **CSRF disabled** — stateless REST API; clients use Bearer tokens

## Firebase authentication

See [AUTHENTICATION.md](./AUTHENTICATION.md).

Filter order:

1. `CorrelationIdFilter` — MDC + response header
2. `FirebaseAuthenticationFilter` — token verification
3. Spring Security authorization

## Actuator protection

Non-public actuator endpoints require HTTP Basic auth:

- Username: `admin` (configurable)
- Password: `${ACTUATOR_PASSWORD}` from Secret Manager

Never expose `/actuator/env` or `/actuator/beans` publicly in production.

## CORS

Configured via `STARTER_CORS_ALLOWED_ORIGINS` (rename to `{APP}_CORS_ALLOWED_ORIGINS` when forking).

| Profile | Typical value |
|---------|---------------|
| `local`, `dev-local` | `http://localhost:8081`, Expo dev server origins |
| `dev` | `*` (acceptable for internal DEV) |
| `prod` | Specific origins only (mobile deep links, web admin if any) |

```java
// SecurityConfig pattern
configuration.setAllowedOrigins(corsAllowedOrigins);
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
```

## Secrets management

| Secret | Storage | Never in |
|--------|---------|----------|
| `OPENAI_API_KEY` | Secret Manager | YAML, git |
| `ACTUATOR_PASSWORD` | Secret Manager | YAML, git |
| Firebase service account | Cloud Run SA / local JSON file | git |

## Data access rules

When adding domain resources:

- Always scope by `userId` from `SecurityContext`
- Never expose resources by ID alone: `findByIdAndUserId(id, userId)`
- Validate ownership in `application/` services, not only in controllers

## Logging security

Do not log:

- Full Firebase ID tokens
- `ACTUATOR_PASSWORD` or API keys
- User passwords (Firebase handles auth — backend never sees passwords)

Correlation IDs are safe to log.

## Optional: suspended users

Extension pattern (not in MVP):

- `SuspendedUserFilter` blocks suspended users except `GET /api/me`
- `User.suspended` flag in Firestore

## Optional: Firebase App Check

Post-MVP abuse prevention for mobile clients. Verify App Check tokens in addition to Firebase Auth.

## Cloud Run

- `--allow-unauthenticated` at Cloud Run level — application enforces auth
- Dedicated service account per environment with least-privilege IAM
- No public Firestore or GCS access from client

## Security checklist (deploy)

- [ ] `ACTUATOR_PASSWORD` set in Secret Manager
- [ ] CORS restricted in PROD
- [ ] Service account has minimum required roles
- [ ] No secrets in `application-prod.yml`
- [ ] Firebase projects separated (DEV vs PROD)

## Related docs

- [AUTHENTICATION.md](./AUTHENTICATION.md)
- [cicd_deployment_plan.md](./cicd_deployment_plan.md)
- [operations/ACTUATOR.md](./operations/ACTUATOR.md)
