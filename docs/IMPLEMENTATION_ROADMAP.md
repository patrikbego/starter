# Implementation Roadmap

This roadmap starts from the current prototype. Complete phases in order; later phases assume earlier boundaries are stable.

Checkbox legend: `[x]` complete · `[~]` partially complete (note explains the remainder) · `[ ]` not started. Status updated 2026-08-17 after Phase 1.

## Phase 0 — documentation baseline

- [x] Define two-repository ownership
- [x] Define target architecture and environment model
- [x] Correct backend and mobile promotion concepts
- [x] Record the prototype gaps
- [~] Approve repository names, GitHub organization, and history strategy — history strategy settled (per-repository `git subtree split` preserving full history); repository names and GitHub organization still to approve before pushing

Exit: architecture and ownership are accepted before code is moved.

## Phase 1 — repository extraction

- [x] Create independent backend and mobile Git repositories — done locally as nested repos (`starter-backend`, `starter-mobile`, own `.git` on `main`) with full history preserved via `git subtree split`
- [x] Move each workflow to its owning repository root — backend: `ci.yml`, `deploy-dev.yml`, `deploy-prod.yml`; mobile: `ci.yml`, `eas-build-dev.yml`, `eas-submit-prod.yml`
- [x] Remove monorepo path filters and `working-directory` assumptions — all `paths:` filters, `working-directory`, and the monorepo `cache-dependency-path` removed; mobile CI switched from `npx expo lint` (broken on this toolchain) to `npx eslint .`
- [x] Fix all docs so standalone clones have no sibling-path dependency — verified: children contain no `../starter-backend`/`../starter-mobile` links; cross-repo coordination docs stay with the parent index
- [~] Add branch protection, CODEOWNERS, Dependabot/Renovate, and secret scanning — `/.github/CODEOWNERS` and `/.github/dependabot.yml` added to both repos plus repository-settings guidance in each README; branch protection, Dependabot alerts, and secret scanning/push protection are GitHub settings that activate only after each repo is pushed and configured
- [x] Verify clean clones on supported developer platforms — fresh clones verified: backend `./mvnw test -B` passes (14 tests, 0 failures); mobile `npm ci` + `npx tsc --noEmit` + `npx eslint .` pass

Exit: both repositories run their CI independently.

> Status note: extraction and local verification are complete. The remaining Phase 1 tail is hosting: create the two GitHub repositories, push `main`, enable the settings listed in each README ("Repository settings (enable after first push)"), and only after that does CI actually run on the platform.

## Phase 2 — contract and configuration safety

- [ ] Add backend `openapi/openapi.yaml` and `/api/v1` routes
- [ ] Standardize errors and correlation IDs
- [ ] Pin/validate the contract from mobile CI
- [ ] Remove the backend's implicit `local` default profile
- [ ] Validate required backend and mobile variables at startup/build time
- [ ] Split public liveness/readiness from protected actuator diagnostics

Exit: cross-repository integration is explicit and missing production configuration fails closed.

## Phase 3 — backend foundation hardening

- [ ] Upgrade Spring Boot, Spring AI, Firebase Admin, and Google Cloud libraries to a supported baseline
- [ ] Add AI timeout, rate limit, quota/budget, and safe telemetry
- [ ] Make the AI endpoint explicitly stateless; defer conversation memory
- [ ] Add integration tests for auth, errors, Firestore, and the OpenAPI contract
- [ ] Add container scanning, SBOM/provenance, and digest capture
- [ ] Add repeatable infrastructure code for DEV and PROD

Exit: a tagged backend candidate can be reproduced and safely deployed.

## Phase 4 — mobile foundation hardening

- [ ] Reinstall dependencies from a clean lockfile state and make lint deterministic
- [ ] Add unit/component tests for auth gating, API retry, and environment validation
- [ ] Add generated or contract-checked API types
- [ ] Add Firebase Auth persistence appropriate to React Native
- [ ] Define app variants, bundle identifiers, schemes, and EAS environments
- [ ] Configure runtime versions and update channels if EAS Update is retained

Exit: preview and production candidates build from clean CI.

## Phase 5 — delivery and recovery

- [ ] Gate DEV deployment on the same backend CI job/workflow
- [ ] Deploy backend by immutable digest and store release metadata
- [ ] Protect production with a GitHub environment and approval
- [ ] Build store-signed mobile candidates and test through store tracks
- [ ] Add post-deploy smoke tests and automated failure reporting
- [ ] Perform one backend rollback drill and one mobile release/OTA rollback drill

Exit: the team can promote and recover without rebuilding or guessing.

## Phase 6 — template release and new-app trial

- [ ] Tag backend and mobile template `v1.0.0`
- [ ] Create a disposable sample product from both tags
- [ ] Provision DEV from scratch using only documented automation
- [ ] Complete login -> `/me` -> AI request on a real device
- [ ] Record setup time and every manual exception
- [ ] Fix the template until the process is repeatable

Exit: a new idea can reach a working DEV environment through the documented path.
