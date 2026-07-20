# Backend Documentation

This directory is self-contained within the backend repository.

| Document | Purpose |
|---|---|
| [backend_architecture_plan.md](./backend_architecture_plan.md) | Target architecture, boundaries, contract, profiles, and extension rules |
| [MVP_SCOPE_CHECKLIST.md](./MVP_SCOPE_CHECKLIST.md) | Prototype status and template-v1 exit criteria |
| [AUTHENTICATION.md](./AUTHENTICATION.md) | Firebase token flow and authorization rules |
| [DATABASE.md](./DATABASE.md) | Firestore user model and repository boundary |
| [AI_INTEGRATION.md](./AI_INTEGRATION.md) | Stateless AI use case, provider port, limits, and telemetry |
| [SECURITY.md](./SECURITY.md) | Threat boundaries and deploy checklist |
| [cicd_deployment_plan.md](./cicd_deployment_plan.md) | Independent-repository CI, immutable promotion, and rollback |
| [STORAGE_EXTENSION.md](./STORAGE_EXTENSION.md) | Optional signed-upload extension; not starter core |
| [operations/ACTUATOR.md](./operations/ACTUATOR.md) | Current actuator behavior and target health split |

Setup guides under `scripts/` and `run/` describe the current prototype. Where they conflict with target-v1 requirements, the architecture, security, and CI/CD documents in this index take precedence until implementation catches up.

## Documentation labels

- **Prototype** means code present today.
- **Target v1** means required before this repository should be used as a production template.
- **Extension** means product-specific and excluded from starter core.

Do not mark a capability implemented merely because a plan or workflow filename exists. It must pass from a clean clone and meet the acceptance criteria in the scope checklist.
