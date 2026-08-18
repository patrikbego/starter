# Project Review Findings

Reviewed on 2026-07-20 against the local `docsera` and `docsera-mobile` references and the current starter implementation. Status layer re-verified on 2026-08-17 after roadmap Phase 3 (backend foundation hardening) and again for Phase 4 (mobile foundation hardening).

Status legend:

- **Done** — implemented and verified by tests/builds in this workspace;
- **Partially** — implementation exists, with a documented remainder (see the Caveats section) or a later-phase item;
- **Open** — not addressed (explicitly out of Phase 3 scope or pending an external step).

## Summary

The prototype proves the core integration loop — Firebase identity, a Firestore user, a server-side AI call, and an Expo client — and the backend suite is green (40 tests, 0 failures). After Phase 3 the backend foundation is materially stronger: fail-closed cloud configuration with explicit `local` mocks, a versioned OpenAPI contract with one error envelope, a supported dependency baseline (Spring Boot 4.1, Spring AI 2.0), bounded AI calls (`429`+`Retry-After`, input caps, timeout), an SBOM-producing build, and repeatable DEV/PROD infrastructure code. After Phase 4 the mobile client is deterministic from a clean install: pinned Node 22, direct ESLint, strict typecheck, 32 Jest tests (env validation, auth gate, HTTP retry/refresh incl. shared singleflight refresh, chat errors), contract-checked DTOs, AsyncStorage-backed Firebase persistence, per-variant app identifiers/schemes with a profile/`APP_ENV` pairing guard, and a fingerprint runtime version (EAS Update deliberately not retained for v1). What remains is largely external or later-phase: real repository separation with tags/remotes, CI-gated and digest-only deployment, mobile delivery (EAS credentials, store submission), and the documented caveats below.

## Priority findings

### P0 — resolve before template v1

1. **Repository model contradicts the requirement.** [Partially] Both projects now live in independent child Git repositories (each is its own top-level), but they have no remotes, tags, or releases yet — splitting/publishing is an external step.
2. **The default backend profile is unsafe.** [Done — Phase 1] The implicit `local` default was removed: running without a profile fails startup, mocks require an explicit `local` profile, and cloud profiles fail closed on missing variables (`ConfigFailClosedTest` guards this).
3. **Deployments are not gated by CI.** [Open — Phase 5] `deploy-dev.yml` still runs `mvn clean package -DskipTests` and is not gated on the CI job (Phase 1 note in the file). Roadmap Phase 5 gates DEV deployment on the same backend CI job.
4. **Backend production is rebuilt.** [Partially — Phase 5] `deploy-prod.yml` now accepts an `image_digest` input and deploys `image@sha256` without rebuilding when supplied; the legacy rebuild path remains until Phase 5 removes it (`run-prod.md` policy text updated).
5. **Mobile promotion is technically incorrect.** [Open — mobile template] Unchanged: PROM store release candidates still need a separate store-signed build + internal testing, distinct from preview builds.
6. **There is no machine-readable API contract or route version.** [Done — Phase 2] OpenAPI `openapi/openapi.yaml` (v1), `/api/v1/*` routes, one error envelope, and `OpenApiContractTest` keeping spec and implementation in sync; the mobile contract copy is pinned and byte-identical.

## Priority findings (P1 — required for a dependable starter)

1. **AI sessions are misleading.** [Done — Phase 3] The endpoint is explicitly stateless: `sessionId` was removed from the contract, `AiChatPort.complete(message)` has no session concept, and deferred memory is documented.
2. **Documented `429` behavior was not implemented.** [Done — Phase 3, budget caveat] Per-user sliding-window quota → `429 RATE_LIMITED` + `Retry-After`; input cap → `400`; provider timeout → `502`; safe outcome-only `starter.ai.*` telemetry. The environment cost budget remains caveat C2.
3. **Public actuator information is broader than necessary.** [Done — Phase 1] Only `/health/live` and `/health/ready` are public; `/actuator/**` requires the ADMIN role and health details are `when_authorized`.
4. **Error responses are inconsistent.** [Done — Phase 2] One versioned envelope `{code, message, correlationId}` for validation, auth, authorization, rate-limit, and provider failures.
5. **The baseline dependencies are old for a reusable 2026 template.** [Done — Phase 3] Spring Boot 4.1.0, Spring AI 2.0.0 (`spring-ai-starter-model-openai`), spring-cloud-gcp 8.1.0, Firebase Admin 9.10.0, libraries-bom 26.86.0; milestone repository removed; Boot 4 migration completed (Jackson 3, `@MockitoBean`, `spring-boot-starter-webmvc-test`, Testcontainers 1.21.4).
6. **Cloud setup is manual.** [Done — Phase 3, apply caveat] `infra/` Terraform provisions APIs, Firestore native, Artifact Registry, secrets, the runtime service account, WIF, and deployment roles per environment; caveat C4 notes it is written/validated but not yet applied to live GCP.
7. **Documentation contains broken sibling links and mixed “planned/implemented” language.** [Partially — Phase 3] Statuses are now labeled current-vs-target across docs; `run-prod.md` stale workflow names fixed; standalone-clone link hygiene (backend docs referencing parent-workspace files) remains open.
8. **The mobile toolchain is not deterministic locally.** [Done — Phase 4] Lint now runs ESLint directly (`npm run lint`, no `expo lint` wrapper), the Node version is pinned by `.nvmrc` (22) and read by CI via `node-version-file`, and the Jest suite (32 tests) passes from a clean `npm ci`. See caveats C10/C11 for the pre-push checks that remain outside CI.
9. **Current logs include user email at account creation.** [Open] `UserService.java` still logs the user's email on creation. Email is personal data; prefer a pseudonymous identifier.
10. **The mobile submit workflow accepts one build ID for both platforms.** [Open — mobile] Unchanged; iOS/Android EAS records must be tracked separately.

### P2 — hardening after the foundation

- [Partially — Phase 3] Dependency automation + SBOM/provenance + container scan + secret scanning: Dependabot configs in place; SBOM (`target/bom.json`, artifact + baked into image); Trivy CRITICAL/HIGH SARIF gate in `deploy-dev.yml`; secret-scanning patterns in `.gitignore`/repo settings. True scanning execution and SLSA attestation are caveats C3/C5.
- [Partially — Phase 3/4] API contract tests: backend `OpenApiContractTest` done; mobile contract validation (`npm run validate:contract`, incl. DTO surface in `src/api/types.ts`) done; backend ↔ mobile integration fixtures remain.
- [Done — Phase 4] Mobile test suite: 32 Jest tests over env validation (incl. EAS profile/`APP_ENV` pairing), auth-gate unit + routing component, HTTP adapter (401 refresh/retry, shared singleflight refresh, error envelope decoding, network failure), and chat error UX.
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
- **C6 — No git tags or remotes.** The exit criterion “a tagged backend candidate can be reproduced and safely deployed” is reproducible-but-untagged: clean verify + deterministic SBOM + captured digest, but tag + deploy-live smoke tests await the repo split/push (external).
- **C7 — Deployment still rebuilds in DEV and can rebuild in PROD.** Deploy-to-DEV is not gated on the CI job; PROD keeps a legacy rebuild branch until Phase 5 enforces digest-only promotion.
- **C8 — No inbound concurrency cap or safe-retry policy on AI calls.** Timeout bounds latency, but in-flight provider-call concurrency limiting and a provider auto-retry policy are deferred.
- **C9 — Mobile advisory count at re-check.** `npm audit` (after Phase 4 dependency additions) reports **30 vulnerabilities (12 moderate, 18 high)**, exclusively `brace-expansion`/`minimatch`/`glob` chains in build/dev tooling (Expo, React Native codegen, Metro, Jest, glob/rimraf) — no fix available upstream at check time; these are build-time paths, not app runtime. Triage during the mobile dependency-baseline pass and re-audit when upstream pins land.
- **C10 — Mobile toolchain, resolved.** The `expo lint`/`unrs-resolver` non-determinism is closed by running ESLint directly with a pinned Node (`.nvmrc` = 22) in CI and scripts; Expo Doctor and EAS build checks are NOT in CI — they run pre-push/pre-release and require environment values (they fail closed when the Firebase env vars are absent, which is the intended guard).
- **C11 — EAS Update not retained for v1.** `runtimeVersion` policy `fingerprint` is configured as forward-prep, but expo-updates is not installed and no channels/branches exist; OTA stays opt-in (decision + drill work are roadmap Phase 5/6 and the mobile delivery checklist).
- **C12 — Local typing shim for Firebase RN persistence.** `getReactNativePersistence` is exported only by the SDK's react-native entry, which the shared public types omit; `src/types/firebase-auth.d.ts` augments it (native-only, never called on web). Revisit when Firebase publishes the export in the shared types.

## Verification performed (updated 2026-08-17 — Phase 4 mobile re-verify)

| Check | Result |
|---|---|
| Backend `./mvnw clean verify -B` | Pass — 40 tests, 0 failures/errors, 1 opt-in skip |
| Firestore emulator integration (Testcontainers, opt-in) | Pass — ran with `RUN_FIRESTORE_EMULATOR_TEST=true` |
| Backend container build | Pass — image boots; liveness 200; stateless AI echo works |
| Backend SBOM | `bom.json` at `starter-backend/target/bom.json` (CycloneDX 1.6) |
| Mobile clean install `npm ci` (Node 22 per `.nvmrc`, lockfile) | Pass |
| Mobile `validate:contract` | Pass — pinned backend contract v1 + `src/api/types.ts` DTO surface (required fields, `correlationId`, no `sessionId`) |
| Mobile `typecheck` | Pass (strict) |
| Mobile `lint` (`npm run lint`, direct ESLint) | Pass |
| Mobile `test:ci` (Jest 32 tests) | Pass — env validation, auth gate, HTTP adapter (401 refresh/retry + shared singleflight), chat errors |
| App config variants (`expo config` eval) | Pass — dev/preview/production identifiers, schemes, fingerprint runtime version; profile/`APP_ENV` mismatches rejected |
| Mobile `npm audit` | 30 vulnerabilities (12 moderate, 18 high) — see C9 |
| Repository boundary check | Child folders are now their own Git top-levels; no remotes/tags |
| Contract drift check | Backend `openapi.yaml` and mobile `contract/openapi.yaml` byte-identical; no `sessionId` residue |

## Evidence locations

| Finding | Current source |
|---|---|
| No default profile; fail-closed config | [`application.yml`](../starter-backend/src/main/resources/application.yml) + [`ConfigFailClosedTest.java`](../starter-backend/src/test/java/com/starter/ConfigFailClosedTest.java) |
| DEV deploy builds with tests skipped and is independent of CI | [`deploy-dev.yml`](../starter-backend/.github/workflows/deploy-dev.yml) |
| PROD digest promotion + legacy rebuild path | [`deploy-prod.yml`](../starter-backend/.github/workflows/deploy-prod.yml) |
| AI adapter is stateless | [`SpringAiOpenRouterAdapter.java`](../starter-backend/src/main/java/com/starter/adapters/ai/SpringAiOpenRouterAdapter.java) |
| AI quota/input/timeout enforcement | [`AiRequestGuard.java`](../starter-backend/src/main/java/com/starter/application/AiRequestGuard.java), [`AiGuardConfig.java`](../starter-backend/src/main/java/com/starter/config/AiGuardConfig.java) and tests |
| User email is logged | [`UserService.java`](../starter-backend/src/main/java/com/starter/application/UserService.java#L25) |
| Infrastructure is code | [`infra/README.md`](../starter-backend/infra/README.md) |

## Recommended decision set (status at Phase 4)

- Two template repositories — Partially: independent local repos; split/push pending the external step
- Backend-owned OpenAPI contract and mobile binding — Done
- Explicit local mocks and fail-closed cloud configuration — Done
- Firebase Auth + Firestore + provider-neutral AI port as the only required integrations — Done
- Immutable backend image digest promotion — Partially: digest captured and promotable; legacy rebuild path removed in Phase 5
- DEV preview builds separate from store release-candidate builds — Partially: distinct identifiers/schemes + pairing guard in place; EAS credentials/store submission pending (external)
- Repeatable infrastructure provisioning owned by the backend repository — Done (written): live apply pending first GCP bootstrap
- Deterministic mobile toolchain and tests — Done (Phase 4)
- EAS Update (OTA) — Not retained for v1; fingerprint runtime version configured (C11)
- Tagged template releases used to create products; no automatic upstream syncing — Open (external)