# Workspace Documentation

These documents coordinate the backend and mobile templates while they are developed as sibling folders. They define shared policy; implementation details belong in the repository that owns them.

| Document | Purpose |
|---|---|
| [REVIEW_FINDINGS.md](./REVIEW_FINDINGS.md) | Evidence-based review of the current prototype and priority gaps |
| [ARCHITECTURE_CHECKS.md](./ARCHITECTURE_CHECKS.md) | Quality-attribute checklist (94 NFR terms) with coverage status per template |
| [ARCHITECTURE_OVERVIEW.md](./ARCHITECTURE_OVERVIEW.md) | Target system boundaries and shared design principles |
| [REPOSITORY_STRATEGY.md](./REPOSITORY_STRATEGY.md) | Why there are two repositories and how they evolve safely |
| [ENVIRONMENT_MATRIX.md](./ENVIRONMENT_MATRIX.md) | Local, DEV, and PROD configuration across both repositories |
| [IMPLEMENTATION_ROADMAP.md](./IMPLEMENTATION_ROADMAP.md) | Ordered path from the current prototype to template v1 |
| [NEW_APP_WORKFLOW.md](./NEW_APP_WORKFLOW.md) | How to create a product from tagged template releases |

Repository-owned documentation:

- [Backend docs](../starter-backend/docs/README.md)
- [Mobile docs](../starter-mobile/docs/README.md)

## Ownership rule

The backend repository owns the HTTP API contract, cloud runtime, database policy, backend security, and backend delivery. The mobile repository owns client behavior, app identity, native build configuration, store delivery, and supported backend-contract versions.

Cross-repository documents describe coordination only. They must not become a substitute for a versioned API specification or executable CI checks.
