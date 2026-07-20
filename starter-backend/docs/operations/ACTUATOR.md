# Health and Diagnostics

## Prototype

- `/actuator/health` is public.
- `/actuator/info` is public and may contain build/git metadata.
- Other actuator routes require the configured admin role, though only `health,info` are currently exposed.

## Target v1

Expose purpose-specific endpoints:

| Endpoint | Audience | Content |
|---|---|---|
| `/health/live` | Public/platform | Process can respond; no dependency/build detail |
| `/health/ready` | Cloud Run/deployment checks | Whether instance should receive traffic |
| Actuator diagnostics | Operators only | Detailed health/build/metrics as explicitly enabled |

Do not expose environment variables, beans, config properties, heap data, or secrets. Avoid public build metadata unless a concrete client need outweighs fingerprinting risk.

## Semantics

- Liveness must not depend on Firestore or the AI provider; dependency outages should not cause restart loops.
- Readiness may include only dependencies required to serve normal traffic and must use short timeouts.
- AI provider health is usually better represented by request metrics/circuit state than an active provider call from health checks.

## Deployment smoke check

After deploying a revision:

1. Verify the deployed digest/revision metadata.
2. Check liveness/readiness.
3. Verify a protected endpoint rejects missing auth with standard JSON.
4. Use a controlled test identity for a minimal authenticated request when allowed.
5. Do not make an unbounded real AI call merely to prove process health.
