# Application Starter Workspace

Documentation-first workspace for two reusable application templates:

```text
starter/
├── starter-backend/   # intended independent Git repository
├── starter-mobile/    # intended independent Git repository
└── docs/              # temporary cross-repository design and bootstrap guides
```

The backend and mobile templates have independent release cycles and CI/CD. A product created from them normally becomes two repositories such as `myapp-backend` and `myapp-mobile`. They integrate through a versioned HTTP contract; neither repository reaches into the other's source tree.

> Current workspace status: both prototypes are still tracked by the parent `starter` Git repository and the workflows still live in the parent `.github/` directory. The documentation defines the target two-repository design. Repository extraction and workflow relocation are the next implementation phase; they are intentionally not performed during this docs-first phase.

## What the templates provide

| Capability | Backend template | Mobile template |
|---|---|---|
| Identity | Firebase ID-token verification | Firebase sign-in and token refresh |
| User data | Firestore user profile behind a repository port | Typed `/me` query and authenticated UI |
| AI | Server-side provider adapter and a minimal chat use case | Thin chat UI; no provider key in the app |
| Security | Fail-closed profiles, authorization, validation, safe errors | Secure session handling and no server secrets |
| Operations | Health/readiness, structured logs, correlation IDs | Environment diagnostics and recoverable errors |
| Delivery | Immutable container promotion to Cloud Run | EAS preview builds plus store release-candidate builds |

Product-specific entities, workflows, prompts, screens, billing, storage, search, and background jobs are extensions, not starter-core features.

## Documentation-first entry points

1. Read [the project review](./docs/REVIEW_FINDINGS.md) for the current gaps and recommended priorities.
2. Read [the architecture overview](./docs/ARCHITECTURE_OVERVIEW.md) for system boundaries.
3. Read [the repository strategy](./docs/REPOSITORY_STRATEGY.md) before splitting the workspace.
4. Use [the implementation roadmap](./docs/IMPLEMENTATION_ROADMAP.md) to move from prototype to template v1.
5. Use [the new-app workflow](./docs/NEW_APP_WORKFLOW.md) after both templates have a tagged release.

Repository-specific docs:

- [Backend documentation](./starter-backend/docs/README.md)
- [Mobile documentation](./starter-mobile/docs/README.md)

## Target release model

```text
backend main -> CI -> one immutable image digest -> DEV -> approved PROD promotion
mobile main  -> CI -> DEV preview build
mobile tag   -> store-signed release candidate -> TestFlight/Play internal -> release same binary
```

“Build once, promote” applies within one releasable artifact type. An EAS internal preview binary is not a store binary and is never presented as one.

## Definition of ready for template v1

- Two independent Git repositories with their own workflows and ownership rules
- Versioned OpenAPI contract owned by the backend and consumed by the mobile app
- Explicit profiles; a missing cloud profile cannot activate mock authentication
- CI gates deployment and production uses the exact tested backend image digest
- Mobile preview and store-release flows are separate and documented accurately
- Security, AI cost controls, observability, rollback, and bootstrap steps are tested
- A clean new product can be created from tagged template releases without editing starter infrastructure by hand
