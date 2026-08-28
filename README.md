# Application Starter — Workspace Index

Coordination workspace for two **independent**, reusable template repositories, extracted into standalone Git repositories during Phase 1. Each child directory is now its own repository and is no longer tracked by this workspace.

| Repository | Content | Branch |
|---|---|---|
| [`starter-backend`](./starter-backend/) | Spring Boot API template | `main` |
| [`starter-mobile`](./starter-mobile/) | Expo / React Native client template | `main` |

The two repositories have independent histories, CI workflows, and (after first push) protection, secrets, and environments. They integrate only through a versioned HTTP contract; neither reaches into the other's source tree.

## Technology stack (short)

| Layer | Backend (`starter-backend`) | Mobile (`starter-mobile`) |
|---|---|---|
| Language / framework | Java 21, Spring Boot + Security | Expo / React Native / TypeScript |
| Identity | Firebase Auth (Bearer ID tokens) | Firebase Auth client |
| Data | Firestore behind a repository port | TanStack Query (server state) |
| AI | Spring AI / OpenAI-compatible behind `AiChatPort` | Typed HTTP adapter |
| API | Versioned OpenAPI v1 REST contract | Pinned contract copy, validated in CI |
| Cloud / infra | GCP: Cloud Run, Firestore, Secret Manager, Artifact Registry; Terraform `infra/` | EAS Build/Submit; optional EAS Update |
| Deployment target | Cloud Run (`{app}-api-{env}`), image in Artifact Registry, deployed via GitHub Actions OIDC/WIF | EAS → TestFlight / Play internal, then App Store / Play Store |

Full per-repo lists: [`starter-backend/AGENTS.md`](./starter-backend/AGENTS.md) and [`starter-mobile/AGENTS.md`](./starter-mobile/AGENTS.md). Environments (`local` → `dev-local` → `dev` → `prod`) and where each runs per environment: [`docs/ENVIRONMENT_MATRIX.md`](./docs/ENVIRONMENT_MATRIX.md).

## What this parent repo is

This repository is intentionally reduced to an index:

- `docs/` retains the cross-repository design and roadmap ([start here](./docs/README.md)). It is historical coordination material, not the source of truth for either template.
- Both child directories are git-ignored here. All code lives and evolves in their own repositories.
- The original monorepo workflows were relocated into each child's `/.github/workflows/`.

## Where to go next

- **Integrations that need activation wiring** (Stripe subscriptions on Spring): [`integrations/STRIPE.md`](./integrations/STRIPE.md).
- Open `starter-backend` and `starter-mobile` as their own checkouts. Each README lists the **repository settings to enable after first push** (branch protection, Dependabot, secret scanning, environments).
- Phase 1 (this extraction) is complete. The next coordination step is Phase 2 of the roadmap: version the HTTP contract and harden fail-closed configuration.
- Eventually this workspace can be retired once both templates are published to GitHub and products are created from tagged releases.