# Architecture Checks

Cross-repository quality-attribute checklist for the starter templates (`starter-backend` + `starter-mobile`). One row per term, in the source list order, with a short note on how the project covers it.

Status vocabulary:

- **Covered** — present and exercised today; the note names the concrete implementation or document.
- **Not yet** — known gap, mapped to a roadmap phase (`IMPLEMENTATION_ROADMAP.md`) or the findings (`REVIEW_FINDINGS.md`).
- **Partially** — foundation present, full capability pending.
- **NA** — deliberately out of scope of the generic template (see non-goals in `ARCHITECTURE_OVERVIEW.md`).

Common subsets referenced below: **agility**, **dependability**, and **securability** are aggregates of the rows marked in their notes.

| Term | Status | Short note |
|---|---|---|
| accessibility | Not yet | Screens use default React Native/Expo components; no explicit accessibility labels, roles, or review yet. |
| accountability | Covered | Review gates (CODEOWNERS, branch protection) and per-request correlation IDs tie actions to a person and commit. |
| accuracy | Covered | Backend integration tests assert exact status codes and response bodies; AI reply quality is deliberately out of scope. |
| adaptability | Covered | Templates are designed to be renamed/reshaped per product via `NEW_APP_WORKFLOW.md`. |
| administrability | Covered | Per-environment run guides, GCP setup scripts, protected actuator endpoints, structured logs; no admin UI by design. |
| affordability | Covered | AI timeout, per-user quota with `429`+`Retry-After`, and input caps enforced before provider calls (Phase 3); GCP budgets are configured manually. |
| agility | Covered | Independent repos, CI on every push, DEV preview builds, short release loop. |
| analyzability | Covered | Structured logs, request/response logging filter, correlation IDs, health endpoints. |
| auditability | Covered | GitHub Actions history plus release records (commit, digest, approver — see `NEW_APP_WORKFLOW.md`). |
| autonomy | Covered | Separate repositories with independent CI/CD, secrets, and release cycles; no cross-repo imports. |
| availability | Covered (foundation) | Cloud Run managed scaling and restarts; multi-region availability is not designed yet. |
| compatibility | Covered | Versioned HTTP contract (v1) owned by the backend and pinned+validated by mobile CI; provider-neutral OpenAI-compatible AI port; pinned Expo/Spring versions. |
| composability | Covered | Backend ports/adapters compose cleanly; mobile feature modules sit on a thin API client. |
| confidentiality | Covered | No server secrets in the app; Secret Manager + Workload Identity; no tokens or prompts logged. One caveat: `UserService` still logs the user's email at account creation (REVIEW_FINDINGS P1#9 — personal data, prefer pseudonymous id). |
| configurability | Covered | Environment-driven spring profiles; build-time-validated app config; fail-closed startup is the Phase 2 target. |
| convenience | Covered | One-command quick starts, `.env.example`, wrapper scripts, guided run docs. |
| correctness | Covered | 287 backend tests (incl. JWT/config/contract guards), strict TypeScript, and direct ESLint gated in CI; DTO validation annotations. |
| credibility | Covered | Evidence-based `REVIEW_FINDINGS.md` (file/line references); every doc labels prototype vs. target v1. |
| customizability | Covered | Rename/branding checklist and extension-point docs in `NEW_APP_WORKFLOW.md`. |
| debuggability | Covered | Explicit `local` mock profile with DEBUG logging, correlation IDs, IDE/CLI run guides. |
| degradability | Partially | AI provider failure degrades to a safe `502`; richer degraded-operation modes are not designed. |
| determinability | Covered | Deterministic local mocks behind an explicit `local` profile; in-memory persistence for tests. |
| demonstrability | Covered | Run guides walk the full starter loop (login → `/me` → AI chat) end to end. |
| dependability | Covered | Aggregated from subsets below: availability, fault-tolerance, reliability, recoverability, resilience. |
| deployability | Covered | Docker + Cloud Run + EAS Build; "build once, promote" digest promotion implemented (`deploy-dev.yml` gated, `promote-prod.yml` digest-only, verified 2026-08-29). |
| discoverability | Covered | Contract v1 published at `openapi/openapi.yaml`, pinned into the mobile repo, and validated in both repos' CI. |
| distributability | Covered | Stateless backend scales horizontally on Cloud Run; mobile is distributed via app stores. |
| durability | Covered | Firestore (managed) for user data; retention/export/restore/deletion policies not yet defined. |
| effectiveness | Covered | The full starter loop (sign-in → `/api/me` → `/api/chat`) works end to end. |
| efficiency | Partially | AI provider calls are timeout-bound and pre-filtered (input cap, quota) so no wasted provider work; latency/load baselines are still an exercise for product teams. |
| elasticity | Covered | Cloud Run scales on demand with per-environment `min`/`max` instances. |
| evolvability | Covered | Layered architecture, versioned contract plan, documented extension paths. |
| extensibility | Covered | Auth, Firestore, and AI sit behind ports; product entities/storage/search are documented extensions. |
| failure transparency | Covered | One error envelope (`code`, `message`, `correlationId`) on every failure path including auth (filter and entry point) and access-denied responses. |
| familiarity | Covered | Standard stack (Spring Boot, Firebase, Expo, React) with conventional structure and a small dependency surface. |
| fault-tolerance | Covered (foundation) | Stateless server; client 401 force-refresh-and-retry-once; typed `NetworkError`. |
| fidelity | NA | Data/behavior fidelity is product-domain — not part of the generic template contract. |
| flexibility | Covered | Environment-driven configuration; provider-neutral adapters; per-environment CORS. |
| inspectability | Covered | Actuator (health/info) plus structured logs; reducing public actuator detail is Phase 3. |
| installability | Covered | `mvnw` wrapper, `npm ci`, Docker build, documented run guides — no manual install steps. |
| integrity | Covered | Firebase ID tokens verified server-side; ownership derived from the verified principal; input validation. |
| interactivity | Covered | The app ships functional login, profile, and chat screens wired to the API. |
| interchangeability | Covered | AI provider and repositories are pluggable behind ports without touching domain code. |
| interoperability | Covered | Standard HTTP/JSON routes, Firebase ID tokens, OpenAI-compatible provider API. |
| intuitiveness | Covered | Two-tab app (home + chat) plus auth gate; minimal surface to learn. |
| learnability | Covered | Docs-first repository layout, guided run docs, small codebase; formal usability testing not performed. |
| localizability | Not yet | No i18n/l10n; UI strings are hardcoded English. |
| maintainability | Covered | Small cohesive modules, documented boundaries, CI quality gates. |
| manageability | Covered | Per-environment scripts and docs, actuator endpoints, environment matrix in docs. |
| mobility | Covered | Expo targets iOS and Android (web preview available for development). |
| modifiability | Covered | Product logic is added through documented extension checkpoints without editing platform core. |
| modularity | Covered | Backend layered `api → application → domain → ports/adapters`; mobile `src/features` modules. |
| observability | Covered | Correlation IDs, structured logs, health/readiness, no sensitive payloads logged. |
| operability | Covered | Deployment run-cards, environment scripts, structured logs for fast failure identification. |
| orthogonality | Covered | Auth, persistence, AI, and delivery are independent concerns with no horizontal coupling. |
| portability | Covered | Backend: single Docker image with profile-driven config; mobile: one codebase, two platforms. |
| precision | Covered | Integration tests assert exact HTTP status/body; explicit DTOs. |
| predictability | Covered | No default profile; explicit profiles and deterministic mocks; `ConfigFailClosedTest` guards against reintroducing a default or permissive fallbacks. |
| process capabilities | Covered | Merge-gated CI, DEV deploy, PROD approval input, release-record template. |
| proactivity | Partially | Alerting and automated failure reporting shipped (`smoke-dev`/`smoke-prod` + Slack `notify` jobs); SLOs and alert review are roadmap Phase 5/6. |
| producibility | Covered | Products are created from tagged releases by design; end-to-end proof is Phase 6. |
| provability | Covered (tests) | Automated tests prove API contracts; formal verification is out of scope. |
| recoverability | Covered (foundation) | Cloud Run revision rollback; formal backend/store rollback drills are Phase 6. |
| redundancy | Covered (foundation) | Cloud Run multi-instance; single-region, no hot standby by design. |
| relevance | Covered | Feature scope is deliberately minimal and justified; non-goals are explicit in the architecture doc. |
| reliability | Covered (foundation) | Tests, error mapping, client retry, health probes; smoke tests/SLOs are Phase 5. |
| repairability | Not yet | Rollback docs exist; formal rollback drills are Phase 5/6. |
| repeatability | Covered | Clean-clone verification, version-pinned dependencies; mobile toolchain determinism is Phase 4. |
| reproducibility | Covered | Lockfiles + Maven wrapper + clean clones verified; immutable digests are the Phase 5 target. |
| resilience | Covered (foundation) | Stateless service, bounded client retry, safe AI failure mapping (502). |
| responsiveness | Partially | UI is snappy by scale alone; no latency SLOs or load tests yet. |
| reusability | Covered | The template premise: tagged releases create new products without re-deriving plumbing. |
| robustness | Covered | Input validation, error mapping, client retry, tests for happy paths and primary failures. |
| safety | Covered | Fail-closed routing, no secrets/tokens/prompts logged, per-environment CORS, least-privilege identity. |
| scalability | Covered | Stateless Cloud Run autoscaling, serverless Firestore, AI quotas act as the natural throttle. |
| seamlessness | Covered | Automatic session refresh with one retry; sign-out clears cached data. |
| self-sustainability | Not yet | Runtime relies on platform recycling (Cloud Run); custom self-healing/alerting not automated. |
| serviceability (a.k.a. supportability) | Covered | Structured logs, health endpoints, rollback-by-revision, run guides per environment. |
| securability | Covered | Confidentiality + integrity + safety rows; hardened identity (App Check) implemented opt-in (web live, native wired — activation per app). |
| simplicity | Covered | Flat layering, no microservices/Kubernetes, explicit non-goals in the architecture overview. |
| stability | Covered | Pinned versions and lockfiles; Spring Boot 4.1 / Spring AI 2.0 supported baseline (Phase 3) with Dependabot schedules ready. |
| standards compliance | Covered | HTTP/JSON, JWTs, OpenAI-compatible surface, and a published OpenAPI 3 contract that CI keeps in sync with the implementation. |
| survivability | Not yet | No multi-region/DR topology; disaster-recovery drills are Phase 6. |
| sustainability | NA | Energy/organizational sustainability is outside the template contract. |
| tailorability | Covered | Tailoring per product = customizability path (rename + env config in `NEW_APP_WORKFLOW.md`). |
| testability | Covered | 287 backend unit/integration tests incl. Firestore emulator round trip, rate-limit/input/timeout behavior, and the OpenAPI contract guard; mobile 201 Jest tests + `tsc`/ESLint gates. |
| timeliness | Not yet | No latency or freshness SLOs/budgets; acceptable at prototype scale, revisit before v1. |
| traceability | Covered | Correlation IDs per request; release records pin commit/digest/approver; template provenance recorded. |
| transparency | Covered | Open error mapping, honest docs, public review findings; unified error envelope is Phase 2. |
| ubiquity | NA | Only iOS/Android/web-dev targets; ubiquitous reach is not a goal. |
| understandability | Covered | Small codebase, layered docs, README as a single reading path. |
| upgradability | Covered | Supported baseline (Spring Boot 4.1, Spring AI 2.0, spring-cloud-gcp 8.1, Firebase Admin 9.10); Dependabot config is ready. |
| usability | Covered | Simple consistent interaction patterns; formal usability tests are beyond the template. |
| vulnerability | Partially | CycloneDX SBOM + Trivy CRITICAL/HIGH container gate in CI/CD and secret scanning/push protection configured as repo settings; execution and dependency-scanning Dependabot coverage are caveats C3/C5 in the caveats register. |

## Keeping this table current

- Rows describe the state after the Phase 1 repository extraction; re-validate at every roadmap exit.
- When a phase completes, flip the corresponding "Not yet"/"Partially" rows to "Covered" with a reference before closing the phase.
- A term with no implementation and no planned roadmap item must not stay "Covered" — the whole point of the table is to make those gaps visible.