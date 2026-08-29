# Implementation Roadmap

This roadmap starts from the current prototype. Complete phases in order; later phases assume earlier boundaries are stable.

Checkbox legend: `[x]` complete · `[~]` partially complete (note explains the remainder) · `[ ]` not started. Status updated 2026-08-17 after Phase 3.

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
- [x] Remove monorepo path filters and `working-directory` assumptions — all `paths:` filters, `working-directory`, and the monorepo `cache-dependency-path` removed; mobile CI uses direct ESLint (`npm run lint`, Phase 4 made it fully deterministic with a pinned Node)
- [x] Fix all docs so standalone clones have no sibling-path dependency — verified: children contain no `../starter-backend`/`../starter-mobile` links; cross-repo coordination docs stay with the parent index
- [~] Add branch protection, CODEOWNERS, Dependabot/Renovate, and secret scanning — `/.github/CODEOWNERS` and `/.github/dependabot.yml` added to both repos plus repository-settings guidance in each README; branch protection, Dependabot alerts, and secret scanning/push protection are GitHub settings that activate only after each repo is pushed and configured
- [x] Verify clean clones on supported developer platforms — fresh clones verified: backend `./mvnw test -B` passes (14 tests, 0 failures); mobile `npm ci` + `npx tsc --noEmit` + `npx eslint .` pass

Exit: both repositories run their CI independently.

> Status note: extraction and local verification are complete. The remaining Phase 1 tail is hosting: create the two GitHub repositories, push `main`, enable the settings listed in each README ("Repository settings (enable after first push)"), and only after that does CI actually run on the platform.

## Phase 2 — contract and configuration safety

- [x] Add backend `openapi/openapi.yaml` and `/api/v1` routes — contract v1 at `openapi/openapi.yaml`; routes `/api/v1/me`, `/api/v1/ai/chat` replace the unversioned prototype paths
- [x] Standardize errors and correlation IDs — one envelope (`code`, `message`, `correlationId`) across validation, AI, auth (filter/entry point), and access-denied responses
- [x] Pin/validate the contract from mobile CI — `starter-mobile/contract/openapi.yaml` pinned copy + `npm run validate:contract` in mobile CI; backend `OpenApiContractTest` keeps spec and implementation in sync
- [x] Remove the backend's implicit `local` default profile — no default profile; cloud profiles fail startup when a required variable is missing
- [x] Validate required backend and mobile variables at startup/build time — strict `${VAR}` placeholders (backend) and `app.config.ts` pairing/fallback guards (mobile: PROD requires HTTPS + non-DEV project id; EAS preview requires HTTPS DEV URL)
- [x] Split public liveness/readiness from protected actuator diagnostics — public `/health/live` + `/health/ready`; `/actuator/**` requires ADMIN; actuator env info disabled

Exit: cross-repository integration is explicit and missing production configuration fails closed.

## Phase 3 — backend foundation hardening

- [x] Upgrade Spring Boot, Spring AI, Firebase Admin, and Google Cloud libraries to a supported baseline — Spring Boot 4.1.0, Spring AI 2.0.0 (`spring-ai-starter-model-openai`), spring-cloud-gcp 8.1.0, Firebase Admin 9.10.0, libraries-bom 26.86.0; milestone repo removed; Boot 4 migration completed (Jackson 3 `tools.jackson`, `@MockitoBean`, `spring-boot-starter-webmvc-test`)
- [x] Add AI timeout, rate limit, quota/budget, and safe telemetry — per-user sliding-window quota (`AI_MAX_REQUESTS_PER_USER`, `AI_RATE_LIMIT_WINDOW`) → `429 RATE_LIMITED` + `Retry-After`; input cap (`AI_MAX_INPUT_CHARS`) → `400 INPUT_LIMIT_EXCEEDED`; provider timeout (`AI_REQUEST_TIMEOUT`) → `502`; metrics `starter.ai.*` (outcome only, no prompts/PII); strict env placeholders in cloud profiles, guard-railed by `ConfigFailClosedTest`
- [x] Make the AI endpoint explicitly stateless; defer conversation memory — `sessionId` removed from the v1 contract (no tags released yet), `AiChatPort.complete(message)` has no session concept; contract and mobile client updated
- [x] Add integration tests for auth, errors, Firestore, and the OpenAPI contract — 40 tests green incl. rate-limit (429+Retry-After), input-cap, provider-timeout, auth-envelope, and a real Firestore emulator round trip (Testcontainers; opt-in `RUN_FIRESTORE_EMULATOR_TEST=true`, run in CI). Suite grown to 126 tests by 2026-08-29 (emulator guard, Basic scoping, billing/email) — see the caveat register.
- [x] Add container scanning, SBOM/provenance, and digest capture — CycloneDX SBOM at `target/bom.json` (artifact in CI, baked into the image), non-root container image with OCI labels, Trivy CRITICAL/HIGH gate in deploy-DEV (and on promoted images in PROD), immutable `image@sha256` digest captured and uploaded by deploy-DEV; PROD workflow accepts the digest for promotion (enforcement of digest-only is Phase 5)
- [x] Add repeatable infrastructure code for DEV and PROD — `infra/` Terraform (APIs, Firestore native, Artifact Registry, Secret Manager, least-privilege runtime SA, GitHub WIF pool/provider, deployment roles), per-env tfvars, plan/apply/set-secrets scripts, `terraform validate` PR check

Exit: a tagged backend candidate can be reproduced and safely deployed.

## Phase 4 — mobile foundation hardening

- [x] Reinstall dependencies from a clean lockfile state and make lint deterministic (`npm ci` + `.nvmrc` Node 22 + direct ESLint in CI; 32 Jest tests green from clean install; 105 tests by 2026-08-29 — see the caveat register)
- [x] Add unit/component tests for auth gating, API retry, and environment validation
- [x] Add generated or contract-checked API types (`validate-contract.mjs` now checks `src/api/types.ts` surface vs the pinned OpenAPI)
- [x] Add Firebase Auth persistence appropriate to React Native (AsyncStorage-backed via the SDK's RN entry)
- [x] Define app variants, bundle identifiers, schemes, and EAS environments (per-profile identifiers/schemes + profile/`APP_ENV` pairing guard)
- [x] Configure runtime versions (fingerprint) and record the EAS Update decision (not retained for v1; see caveat register)

Exit: preview and production candidates build from clean CI.

## Phase 5 — delivery and recovery

- [x] Gate DEV deployment on the same backend CI job/workflow — extracted a shared reusable `verify.yml` (mvn verify + SBOM) used by both `ci.yml` and the gating `verify` job in `deploy-dev.yml`; job chain `verify → build-and-push → deploy-dev → smoke-dev` with a `deploy-dev` concurrency group (cancel-in-progress). actionlint-clean; execution confirmed once repos are pushed (roadmap status 2026-08-19)
- [x] Deploy backend by immutable digest and store release metadata — deploy-DEV now deploys `image@sha256` (captured digest flows as a job output, never a mutable tag) and the post-smoke job writes/upload `release-metadata.json` (commit, digest, SBOM, Cloud Run revision, env, run URL, timestamp, smoke result, `promotable`). actionlint-clean; executes once pushed. Remaining: PROD legacy-rebuild removal + protected-env approval (Phase 5 A3)
- [~] Protect production with a GitHub environment and approval — `deploy-prod.yml` replaced by `promote-prod.yml`: digest-only promotion under `environment: production`, no build step, `confirm_deploy` removed, WIF PROD principal, registry-verify + Trivy gate before deploy. actionlint-clean; remaining is the GitHub-side step (create the `production` environment + reviewers in repo settings, pending until the repo is pushed)
- [~] Build store-signed mobile candidates and test through store tracks — workflows implemented (renamed to `build-preview` / new `build-release` / `submit-release`): `build-release` builds the store-signed `production` candidate per platform (tag/dispatch), records commit + EAS build IDs + contract/runtime version in `mobile-release-metadata.json`; `submit-release` (protected production env) uploads recorded IDs to TestFlight/Play internal, never preview. actionlint-clean. Remaining: EAS credentials + real store build and the device-track test (live setup)
- [x] Add post-deploy smoke tests and automated failure reporting — DEV `smoke-dev` (health fail-closed + optional authenticated `/api/v1/me`), new PROD `smoke-prod`; failure fails the workflow and leaves the digest unpromotable; `notify` jobs (DEV+PROD) annotate and fire a Slack alert when `SLACK_WEBHOOK` is set; previous healthy Cloud Run revision retained as rollback target. actionlint-clean
- [~] Perform one backend rollback drill and one mobile release/OTA rollback drill — runbooks codified (`starter-backend/docs/rollback_runbook.md`, `starter-mobile/docs/release_rollback_runbook.md`) with commands + drill checklists; execution is pending a live environment (none deployed yet)

Exit: the team can promote and recover without rebuilding or guessing.

## Phase 6 — template release and new-app trial

- [ ] Tag backend and mobile template `v1.0.0`
- [ ] Create a disposable sample product from both tags
- [ ] Provision DEV from scratch using only documented automation
- [ ] Complete login -> `/me` -> AI request on a real device
- [ ] Record setup time and every manual exception
- [ ] Fix the template until the process is repeatable

Exit: a new idea can reach a working DEV environment through the documented path.
