# Project Review Findings

Reviewed on 2026-07-20 against the local `docsera` and `docsera-mobile` references and the current starter implementation. Status layer re-verified on 2026-08-17 after roadmap Phase 3 (backend foundation hardening) and again for Phase 4 (mobile foundation hardening). Re-verified on 2026-08-29 after the cross-repo review and hardening pass (contract digest pin, emulator fail-closed gate, HTTP Basic scoping, mobile auth fixes) — see [the 2026-08-29 section](#2026-08-29-cross-repo-review-and-hardening-pass).

Status legend:

- **Done** — implemented and verified by tests/builds in this workspace;
- **Partially** — implementation exists, with a documented remainder (see the Caveats section) or a later-phase item;
- **Open** — not addressed (explicitly out of Phase 3 scope or pending an external step).

## Summary

The prototype proves the core integration loop — Firebase identity, a Firestore user, a server-side AI call, and an Expo client — and the backend suite is green (126 tests, 0 failures). After Phase 3 the backend foundation is materially stronger: fail-closed cloud configuration with explicit `local` mocks, a versioned OpenAPI contract with one error envelope, a supported dependency baseline (Spring Boot 4.1, Spring AI 2.0), bounded AI calls (`429`+`Retry-After`, input caps, timeout), an SBOM-producing build, and repeatable DEV/PROD infrastructure code. After Phase 4 the mobile client is deterministic from a clean install: pinned Node 22, direct ESLint, strict typecheck, 105 Jest tests (env validation, auth gate, HTTP retry/refresh incl. shared singleflight refresh and sign-out on forced-refresh failure, chat errors, contract digest pin, billing, profile, email), contract-checked DTOs with an enforced byte-identity digest pin, AsyncStorage-backed Firebase persistence, per-variant app identifiers/schemes with a profile/`APP_ENV` pairing guard, and a fingerprint runtime version (EAS Update deliberately not retained for v1). What remains is largely external or later-phase: tags and tagged releases, live infrastructure apply, mobile delivery (EAS credentials, store submission), and the documented caveats below. On 2026-08-29 the contract boundary became mechanically enforceable (mobile digest pin), the Firebase emulator was restricted to `dev-local` by a fail-closed startup check, HTTP Basic was scoped to actuator, and the two mobile auth gaps (cache clear on user change, sign-out on refresh failure) were closed.

## Priority findings

### P0 — resolve before template v1

1. **Repository model contradicts the requirement.** [Partially] Both projects live in independent child Git repositories with `origin` remotes (`patrikbego/starter-backend`, `patrikbego/starter-mobile`) and pushed branches (verified 2026-08-29); tags and tagged releases are still missing — publishing/tagging remains the external step.
2. **The default backend profile is unsafe.** [Done — Phase 1] The implicit `local` default was removed: running without a profile fails startup, mocks require an explicit `local` profile, and cloud profiles fail closed on missing variables (`ConfigFailClosedTest` guards this).
3. **Deployments are not gated by CI.** [Done — Phase 5, verified 2026-08-29] `deploy-dev.yml` gates deployment on the CI job (`needs: verify`, line 52). The build-and-push step still packages with `-DskipTests` — acceptable because tests already ran in the same workflow's `verify` job, so the image is never built from unverified source.
4. **Backend production is rebuilt.** [Done — Phase 5, verified 2026-08-29] Only `promote-prod.yml` remains (renamed from `deploy-prod.yml`): digest-only `image@sha256` promotion with no build step and no source checkout; the legacy rebuild path is removed.
5. **Mobile promotion is technically incorrect.** [Open — mobile template] Unchanged: PROM store release candidates still need a separate store-signed build + internal testing, distinct from preview builds.
6. **There is no machine-readable API contract or route version.** [Done — Phase 2] OpenAPI `openapi/openapi.yaml` (v1), `/api/v1/*` routes, one error envelope, and `OpenApiContractTest` keeping spec and implementation in sync; the mobile contract copy is pinned and byte-identity is machine-enforced against `contract/openapi.yaml.sha256` (added 2026-08-29 — a stale copy now fails mobile CI).

## Priority findings (P1 — required for a dependable starter)

1. **AI sessions are misleading.** [Done — Phase 3] The endpoint is explicitly stateless: `sessionId` was removed from the contract, `AiChatPort.complete(message)` has no session concept, and deferred memory is documented.
2. **Documented `429` behavior was not implemented.** [Done — Phase 3, budget caveat] Per-user sliding-window quota → `429 RATE_LIMITED` + `Retry-After`; input cap → `400`; provider timeout → `502`; safe outcome-only `starter.ai.*` telemetry. The environment cost budget remains caveat C2.
3. **Public actuator information is broader than necessary.** [Done — Phase 1; Basic scoped 2026-08-29] Only `/health/live` and `/health/ready` are public; `/actuator/**` requires the ADMIN role via HTTP Basic only — Basic is never valid on API routes — and health details are `when_authorized`.
4. **Error responses are inconsistent.** [Done — Phase 2] One versioned envelope `{code, message, correlationId}` for validation, auth, authorization, rate-limit, and provider failures.
5. **The baseline dependencies are old for a reusable 2026 template.** [Done — Phase 3] Spring Boot 4.1.0, Spring AI 2.0.0 (`spring-ai-starter-model-openai`), spring-cloud-gcp 8.1.0, Firebase Admin 9.10.0, libraries-bom 26.86.0; milestone repository removed; Boot 4 migration completed (Jackson 3, `@MockitoBean`, `spring-boot-starter-webmvc-test`, Testcontainers 1.21.4).
6. **Cloud setup is manual.** [Done — Phase 3, apply caveat] `infra/` Terraform provisions APIs, Firestore native, Artifact Registry, secrets, the runtime service account, WIF, and deployment roles per environment; caveat C4 notes it is written/validated but not yet applied to live GCP.
7. **Documentation contains broken sibling links and mixed “planned/implemented” language.** [Partially — Phase 3] Statuses are now labeled current-vs-target across docs; `run-prod.md` stale workflow names fixed; standalone-clone link hygiene (backend docs referencing parent-workspace files) remains open.
8. **The mobile toolchain is not deterministic locally.** [Done — Phase 4] Lint now runs ESLint directly (`npm run lint`, no `expo lint` wrapper), the Node version is pinned by `.nvmrc` (22) and read by CI via `node-version-file`, and the Jest suite (105 tests as of 2026-08-29) passes from a clean `npm ci`. Expo Doctor and EAS builds now run in GitHub Actions (see C10).
9. **Current logs include user email at account creation.** [Open] `UserService.java` still logs the user's email on creation. Email is personal data; prefer a pseudonymous identifier.
10. **The mobile submit workflow accepts one build ID for both platforms.** [Open — mobile] Unchanged; iOS/Android EAS records must be tracked separately.

### P2 — hardening after the foundation

- [Partially — Phase 3] Dependency automation + SBOM/provenance + container scan + secret scanning: Dependabot configs in place; SBOM (`target/bom.json`, artifact + baked into image); Trivy CRITICAL/HIGH SARIF gate in `deploy-dev.yml`; secret-scanning patterns in `.gitignore`/repo settings. True scanning execution and SLSA attestation are caveats C3/C5.
- [Partially — Phase 3/4] API contract tests: backend `OpenApiContractTest` done; mobile contract validation (`npm run validate:contract`, incl. DTO surface in `src/api/types.ts`) done; backend ↔ mobile integration fixtures remain.
- [Done — Phase 4; expanded 2026-08-29] Mobile test suite: 105 Jest tests over env validation (incl. EAS profile/`APP_ENV` pairing), auth-gate unit + routing component, HTTP adapter (401 refresh/retry, shared singleflight refresh, sign-out on forced-refresh failure, error envelope decoding, network failure), and chat error UX, plus the contract digest-pin guard.
- [Open] Structured release metadata, SLOs, alerts, rollback drill — digest capture exists; SLOs/alerts are Phase 5.
- [Open] Firebase App Check — out of scope until abuse patterns justify the cost.
- [Open] Database indexes/retention/export/restore and deletion policies — documented; product-data territory.

## Caveats (roadmap Phase 3 — implemented but with documented limits, not fixed)

Documented here and in the repo docs (`AI_INTEGRATION.md`, `infra/README.md`, `ENVIRONMENT_MATRIX.md`, `IMPLEMENTATION_ROADMAP.md`) so the template carries its limits honestly.

- **C1 — Rate limit is per instance.** The default `InMemoryAiRateLimitStore` enforces the per-user quota inside one process; a hard global quota on multi-instance Cloud Run needs a shared store behind the `AiRateLimitStore` port (Redis etc.).
- **C2 — No environment cost budget.** Timeout, input cap, and per-user quota exist; the daily/monthly $ budget cap is recorded `not yet` and remains a product decision.
- **C3 — Provenance is lightweight.** SBOM + OCI labels + git SHA are captured; there are no SLSA attestations or cosign signatures.
- **C4 — Infrastructure is written, not yet applied.** No Terraform CLI/GCP credentials here; only `terraform validate` is wired as a PR check. First `plan`/`apply` must run against real projects; Firestore location is immutable once chosen; provider version constraint is a range to re-pin after the first clean plan/apply pair; secret values are populated via `scripts/set-secrets.sh`, never committed.
- **C5 — Trivy gate and digest capture are configured, not executed.** They run in GitHub Actions/deploy workflows; execution needs pushed repositories, runners, and GCP projects.
- **C6 — No git tags or releases.** [Updated 2026-08-29: remotes exist and branches are pushed — `patrikbego/starter-*`; `git tag` count is still 0 in both repos.] The exit criterion “a tagged backend candidate can be reproduced and safely deployed” is reproducible-but-untagged: clean verify + deterministic SBOM + captured digest, but tag + deploy-live smoke tests await the first tagged release.
- **C7 — Deployment still rebuilds in DEV and can rebuild in PROD.** [Resolved 2026-08-29] DEV deployment is gated on the CI job (`deploy-dev.yml:52` `needs: verify`); PROD is digest-only via `promote-prod.yml` with no build step and no source checkout. The legacy rebuild branch no longer exists.
- **C8 — No inbound concurrency cap or safe-retry policy on AI calls.** Timeout bounds latency, but in-flight provider-call concurrency limiting and a provider auto-retry policy are deferred.
- **C9 — Mobile advisory count at re-check.** `npm audit` re-run 2026-08-29 reports **99 vulnerabilities (9 moderate, 90 high)** (was 30 after Phase 4). The earlier triage attributed the 30 to transitive build/dev tooling chains (`brace-expansion`/`minimatch`/`glob` via Expo, React Native codegen, Metro, Jest) — build-time paths, not app runtime; the grown set has not been re-triaged item-by-item yet. Triage during the mobile dependency-baseline pass and re-audit when upstream pins land.
- **C10 — Mobile toolchain, resolved.** The `expo lint`/`unrs-resolver` non-determinism is closed by running ESLint directly with a pinned Node (`.nvmrc` = 22) in CI and scripts. [Updated 2026-08-29: Expo Doctor now runs in CI (`build-release.yml:59`), and EAS builds run in Actions (`build-preview.yml:45`, `build-release.yml:65`) — the toolchain checks are no longer local-only.]
- **C11 — EAS Update not retained for v1.** `runtimeVersion` policy `fingerprint` is configured as forward-prep, but expo-updates is not installed and no channels/branches exist; OTA stays opt-in (decision + drill work are roadmap Phase 5/6 and the mobile delivery checklist).
- **C12 — Local typing shim for Firebase RN persistence.** `getReactNativePersistence` is exported only by the SDK's react-native entry, which the shared public types omit; `src/types/firebase-auth.d.ts` augments it (native-only, never called on web). Revisit when Firebase publishes the export in the shared types.
- **C13 — Unroutable requests answer 401, not 404.** [Found 2026-08-29 by the new push e2e (`e2e/push.spec.ts`), verified against DEV.] Any path/method combination with no controller — unknown route or wrong method on a known path — returns the security entry-point envelope `{"code":"UNAUTHORIZED",...,"Authentication is required..."}` even with a valid bearer, because the `NoResourceFoundException` → `/error` dispatch re-enters the stateless security chain with an empty context (`SecurityConfig` re-authorizes the ERROR dispatch). Fail-closed (never leaks route existence) but makes 401 ambiguous for API consumers and e2e probes; a fix would permit the ERROR dispatch to reuse the authenticated context. Not changed during the push integration.

## Verification performed (updated 2026-08-29 — hardening-pass re-verify; earlier: 2026-08-17 Phase 4)

| Check | Result |
|---|---|
| Backend `./mvnw verify` | Pass — 126 tests, 0 failures/errors, 1 opt-in skip (2026-08-29) |
| Firestore emulator integration (Testcontainers, opt-in) | Pass — ran with `RUN_FIRESTORE_EMULATOR_TEST=true` |
| Backend container build | Pass — image boots; liveness 200; stateless AI echo works |
| Backend SBOM | `bom.json` at `starter-backend/target/bom.json` (CycloneDX 1.6) |
| Mobile clean install `npm ci` (Node 22 per `.nvmrc`, lockfile) | Pass |
| Mobile `validate:contract` | Pass — pinned backend contract v1 + `src/api/types.ts` DTO surface + byte-identity digest pin (2026-08-29) |
| Mobile `typecheck` | Pass (strict) |
| Mobile `lint` (`npm run lint`, direct ESLint) | Pass |
| Mobile `test:ci` (Jest 105 tests) | Pass — env validation, auth gate, HTTP adapter (401 refresh/retry + shared singleflight + refresh-failure sign-out), chat errors, digest pin (2026-08-29) |
| App config variants (`expo config` eval) | Pass — dev/preview/production identifiers, schemes, fingerprint runtime version; profile/`APP_ENV` mismatches rejected |
| Mobile `npm audit` | 99 vulnerabilities (9 moderate, 90 high) — see C9 (2026-08-29) |
| Repository boundary check | Child folders are their own Git top-levels with origin remotes and pushed branches; no tags (2026-08-29) |
| Contract drift check | Backend `openapi.yaml` and mobile `contract/openapi.yaml` byte-identical after re-sync and enforced by the digest pin; no `sessionId` residue |
| Backend emulator guard | `firebase.emulator.host` outside `dev-local` fails startup (`FirebaseConfigTest`: prod and dev cases) |
| Backend Basic scoping | Basic rejected on `/api/v1/me`, accepted on `/actuator/health` (`SecurityConfigTest`) |

## 2026-08-29 cross-repo review and hardening pass

Full review of backend, mobile, and the contract boundary; all fixes applied and verified the same day (backend `./mvnw verify` 126 tests; mobile Jest 105 tests, lint/typecheck/contract clean):

- **Contract drift (blocker, fixed).** The backend had added the verified-email `403 EMAIL_NOT_VERIFIED` response; the mobile pinned copy was one commit behind, and `validate:contract` could not see it (version-regex + DTO-surface checks only). Fixed: copy re-synced and byte-identity now enforced via `contract/openapi.yaml.sha256` (`validateWithDigest` in `scripts/validate-contract-core.js`); a one-line drift into the pinned copy fails with exit 1 and a remediation message.
- **Spec prose bug (fixed).** Backend `openapi/openapi.yaml` said "input size cap (400)" while code and schema say 4000; prose corrected.
- **Emulator fail-closed (fixed).** `FirebaseConfig` accepted emulator token verification in any `!local` profile when `firebase.emulator.host` was set (one stray env var on a deployed service = emulator-signed tokens accepted). Now startup fails unless the profile is `dev-local`; `FirebaseConfigTest` covers the prod and dev refusals and the dev-local allowance.
- **HTTP Basic scoping (fixed).** Basic authenticated every protected route; now a dedicated `/actuator/**` chain requires the admin credential and API routes are Firebase-bearer only (`SecurityConfigTest`: Basic rejected on `/api/v1/me`, accepted on `/actuator/health`).
- **Mobile auth gaps (fixed).** The query cache now clears on any user change (in-place account switch previously rendered the previous user's cached `/me`/billing), and a failed forced refresh now signs out and clears instead of surfacing a raw Firebase error; the retry reuses the refreshed token instead of re-reading (and clobbering) it.
- **Register hygiene (this update).** P0#1/#3/#4/#6, P1#3/#8, C6/C7/C9/C10 and the verification/evidence tables brought to current reality.

## Evidence locations

| Finding | Current source |
|---|---|
| No default profile; fail-closed config | [`application.yml`](../starter-backend/src/main/resources/application.yml) + [`ConfigFailClosedTest.java`](../starter-backend/src/test/java/com/starter/ConfigFailClosedTest.java) |
| DEV deploy is gated on the CI verify job (packaging runs `-DskipTests` only after `verify` passed in the same workflow) | [`deploy-dev.yml`](../starter-backend/.github/workflows/deploy-dev.yml) |
| PROD digest promotion (workflow renamed from `deploy-prod.yml`; legacy rebuild path since removed) | [`promote-prod.yml`](../starter-backend/.github/workflows/promote-prod.yml) |
| AI adapter is stateless | [`SpringAiOpenRouterAdapter.java`](../starter-backend/src/main/java/com/starter/adapters/ai/SpringAiOpenRouterAdapter.java) |
| AI quota/input/timeout enforcement | [`AiRequestGuard.java`](../starter-backend/src/main/java/com/starter/application/AiRequestGuard.java), [`AiGuardConfig.java`](../starter-backend/src/main/java/com/starter/config/AiGuardConfig.java) and tests |
| User email is logged | [`UserService.java`](../starter-backend/src/main/java/com/starter/application/UserService.java#L25) |
| Infrastructure is code | [`infra/README.md`](../starter-backend/infra/README.md) |

## Recommended decision set (status at Phase 4)

- Two template repositories — Partially: independent repos with origin remotes and pushed branches; tags/releases pending
- Backend-owned OpenAPI contract and mobile binding — Done
- Explicit local mocks and fail-closed cloud configuration — Done
- Firebase Auth + Firestore + provider-neutral AI port as the only required integrations — Done
- Immutable backend image digest promotion — Done: digest-only `promote-prod.yml`; DEV deploy gated on CI (verified 2026-08-29)
- DEV preview builds separate from store release-candidate builds — Partially: distinct identifiers/schemes + pairing guard in place; EAS credentials/store submission pending (external)
- Repeatable infrastructure provisioning owned by the backend repository — Done (written): live apply pending first GCP bootstrap
- Deterministic mobile toolchain and tests — Done (Phase 4)
- EAS Update (OTA) — Not retained for v1; fingerprint runtime version configured (C11)
- Tagged template releases used to create products; no automatic upstream syncing — Open (external)