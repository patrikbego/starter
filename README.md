# Application Starter — Workspace Index

Coordination workspace for two **independent**, reusable template repositories, extracted into standalone Git repositories during Phase 1. Each child directory is now its own repository and is no longer tracked by this workspace.

| Repository | Content | Branch |
|---|---|---|
| [`starter-backend`](./starter-backend/) | Spring Boot API template | `main` |
| [`starter-mobile`](./starter-mobile/) | Expo / React Native client template | `main` |

The two repositories have independent histories, CI workflows, and (after first push) protection, secrets, and environments. They integrate only through a versioned HTTP contract; neither reaches into the other's source tree.

## What this parent repo is

This repository is intentionally reduced to an index:

- `docs/` retains the cross-repository design and roadmap ([start here](./docs/README.md)). It is historical coordination material, not the source of truth for either template.
- Both child directories are git-ignored here. All code lives and evolves in their own repositories.
- The original monorepo workflows were relocated into each child's `/.github/workflows/`.

## Where to go next

- Open `starter-backend` and `starter-mobile` as their own checkouts. Each README lists the **repository settings to enable after first push** (branch protection, Dependabot, secret scanning, environments).
- Phase 1 (this extraction) is complete. The next coordination step is Phase 2 of the roadmap: version the HTTP contract and harden fail-closed configuration.
- Eventually this workspace can be retired once both templates are published to GitHub and products are created from tagged releases.