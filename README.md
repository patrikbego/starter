# Application Starter — Workspace Index

Coordination workspace for two **independent**, reusable template repositories, extracted into standalone Git repositories during Phase 1. Each child directory is now its own repository and is no longer tracked by this workspace.

| Repository | Remote | Content |
|---|---|---|
| [`starter-backend`](./starter-backend/) | `patrikbego/starter-backend` | Spring Boot API template |
| [`starter-mobile`](./starter-mobile/) | `patrikbego/starter-mobile` | Expo / React Native client template |

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

## How to read this documentation (newcomer path)

Reading order for someone new to the workspace. Each step assumes the previous one;
skip ahead once you know the shape.

**Phase 0 — Orient (~10 min):**
1. [`README.md`](./README.md) (this file) — the three-repo map.
2. Root + child `AGENTS.md` (skim) — rules of the road; near-identical by design.

**Phase 1 — Understand the shape (~30 min):**
3. [`docs/README.md`](./docs/README.md) — index of the cross-repo policy docs.
4. [`docs/ARCHITECTURE_OVERVIEW.md`](./docs/ARCHITECTURE_OVERVIEW.md) — target boundaries, principles, deliberate non-goals.
5. [`docs/REPOSITORY_STRATEGY.md`](./docs/REPOSITORY_STRATEGY.md) — why two independent repos, how they integrate via contract, not files.
6. [`docs/ENVIRONMENT_MATRIX.md`](./docs/ENVIRONMENT_MATRIX.md) — the `local` → `dev-local` → `dev` → `prod` model; **single source for every env var, including all extension vars. Bookmark it; you will return constantly.**

**Phase 2 — Know the current state (~20 min):**
7. [`docs/REVIEW_FINDINGS.md`](./docs/REVIEW_FINDINGS.md) — evidence-based status (Done / Partially / Open), the caveat register (C1–C14). Read this before trusting any "implemented" label elsewhere.
8. [`docs/IMPLEMENTATION_ROADMAP.md`](./docs/IMPLEMENTATION_ROADMAP.md) — phases 0–6 and where the project sits.

**Phase 3 — Backend (1–2 h):**
9. [`starter-backend/README.md`](./starter-backend/README.md) — quickstart, profiles, verification.
10. [`starter-backend/docs/README.md`](./starter-backend/docs/README.md) — backend doc index; use it to pick which docs below you need.
11. [`starter-backend/docs/MVP_SCOPE_CHECKLIST.md`](./starter-backend/docs/MVP_SCOPE_CHECKLIST.md) — prototype evidence + what is genuinely left before v1.
12. Core story (in order): [`AUTHENTICATION.md`](./starter-backend/docs/AUTHENTICATION.md) → [`DATABASE.md`](./starter-backend/docs/DATABASE.md) → [`AI_INTEGRATION.md`](./starter-backend/docs/AI_INTEGRATION.md) → [`SECURITY.md`](./starter-backend/docs/SECURITY.md); delivery/ops: [`cicd_deployment_plan.md`](./starter-backend/docs/cicd_deployment_plan.md) + [`docs/operations/COST_CONTROLS.md`](./starter-backend/docs/operations/COST_CONTROLS.md).
13. [`starter-backend/openapi/openapi.yaml`](./starter-backend/openapi/openapi.yaml) — the contract; it is the integration boundary, so skim it even if you never read backend code.

**Phase 4 — Mobile (1–2 h):**
14. [`starter-mobile/README.md`](./starter-mobile/README.md) — quickstart, including the live DEV web app you can try today.
15. [`starter-mobile/docs/README.md`](./starter-mobile/docs/README.md) — mobile doc index.
16. [`starter-mobile/docs/mobile_mvp_scope_checklist.md`](./starter-mobile/docs/mobile_mvp_scope_checklist.md) — same evidence / exit-criteria pattern as the backend.
17. [`mobile_architecture_plan.md`](./starter-mobile/docs/mobile_architecture_plan.md) (structure/rules) → [`BACKEND_INTEGRATION.md`](./starter-mobile/docs/BACKEND_INTEGRATION.md) (how the client consumes the contract — read carefully) → [`CAVEATS.md`](./starter-mobile/docs/CAVEATS.md) (M1–M5).

**Phase 5 — Integrations (on demand, ~15 min each):**
18. [`integrations/README.md`](./integrations/README.md) — the status table; read once to know what exists.
19. [`docs/integrations-plan.md`](./docs/integrations-plan.md) — the register: decisions, costs, effort.
20. **Only the runbook for the integration you are activating** — each is self-contained (`STRIPE.md`, `POSTHOG.md`, `MEDIA_UPLOAD.md`, …). These are reference cards, not linear reading.

**Phase 6 — Run & recover (when you actually run things):**
21. [`starter-backend/run/README.md`](./starter-backend/run/README.md) + `run-local.md` / `run-dev.md` / `run-prod.md`; same set under [`starter-mobile/run/`](./starter-mobile/run/).
22. Rollback runbooks — [`starter-backend/docs/rollback_runbook.md`](./starter-backend/docs/rollback_runbook.md) + [`starter-mobile/docs/release_rollback_runbook.md`](./starter-mobile/docs/release_rollback_runbook.md) — only once you are near a release.

**Phase 7 — When you create a product from the template:**
23. [`docs/NEW_APP_WORKFLOW.md`](./docs/NEW_APP_WORKFLOW.md), then [`docs/UPSTREAM_SYNC.md`](./docs/UPSTREAM_SYNC.md).
24. [`guides/STEP6_NEW_APP_FROM_STARTER.md`](./guides/STEP6_NEW_APP_FROM_STARTER.md) + [`guides/STEP5_DELIVERY_AND_RECOVERY.md`](./guides/STEP5_DELIVERY_AND_RECOVERY.md).

**Shortcuts:** 20 minutes only → read 1, 4, 6, 7, 10, 15. One rule to remember: **the environment matrix and the OpenAPI contract are the ground truth; every other doc is commentary that can drift.**

## What this parent repo is

This repository is intentionally reduced to an index:

- `docs/` retains the cross-repository design and roadmap ([start here](./docs/README.md)). It is historical coordination material, not the source of truth for either template.
- Both child directories are git-ignored here. All code lives and evolves in their own repositories.
- The original monorepo workflows were relocated into each child's `/.github/workflows/`.
- **Three repos, three remotes**: this index (`patrikbego/starter`), [`patrikbego/starter-backend`](https://github.com/patrikbego/starter-backend), [`patrikbego/starter-mobile`](https://github.com/patrikbego/starter-mobile). Git at the workspace root touches only the index — scope git commands per repository (see [Repository strategy](./docs/REPOSITORY_STRATEGY.md)).

## Where to go next

- **Integrations**: one runbook per external service in [`integrations/README.md`](./integrations/README.md) — Firebase Auth, OpenRouter AI, Stripe billing and Resend email (both implemented, opt-in), Slack alerts, browser E2E.
- Open `starter-backend` and `starter-mobile` as their own checkouts. Each README lists the **repository settings to enable after first push** (branch protection, Dependabot, secret scanning, environments).
- Roadmap Phases 1–4 are complete: contract v1 is backend-owned with a digest-pinned mobile copy, profiles fail closed, and AI guardrails are in. The remaining v1 exit criteria are the unchecked items in each repo's scope checklist: tags/releases, live infra apply, EAS/store delivery.
- Eventually this workspace can be retired once both templates are published to GitHub and products are created from tagged releases.