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
| affordability | Not yet | AI rate/quota/budget/timeout controls are roadmap Phase 3; GCP budgets are configured manually. |
| agility | Covered | Independent repos, CI on every push, DEV preview builds, short release loop. |
| analyzability | Covered | Structured logs, request/response logging filter, correlation IDs, health endpoints. |
| auditability | Covered | GitHub Actions history plus release records (commit, digest, approver — see `NEW_APP_WORKFLOW.md`). |
| autonomy | Covered | Separate repositories with independent CI/CD, secrets, and release cycles; no cross-repo imports. |
| availability | Covered (foundation) | Cloud Run managed scaling and restarts; multi-region availability is not designed yet. |
| compatibility | Covered (foundation) | Provider-neutral OpenAI-compatible AI port, pinned Expo/Spring versions; versioned HTTP contract is Phase 2. |
| composability | Covered | Backend ports/adapters compose cleanly; mobile feature modules sit on a thin API client. |
| confidentiality | Covered | No server secrets in the app; Secret Manager + Workload Identity; no tokens, prompts, or PII logged. |
| configurability | Covered | Environment-driven spring profiles; build-time-validated app config; fail-closed startup is the Phase 2 target. |
| convenience | Covered | One-command quick starts, `.env.example`, wrapper scripts, guided run docs. |
| correctness | Covered | 14 backend tests, TypeScript, and direct ESLint gated in CI; DTO validation annotations. |
| credibility | Covered | Evidence-based `REVIEW_FINDINGS.md` (file/line references); every doc labels prototype vs. target v1. |
| customizability | Covered | Rename/branding checklist and extension-point docs in `NEW_APP_WORKFLOW.md`. |
| debuggability | Covered | Explicit `local` mock profile with DEBUG logging, correlation IDs, IDE/CLI run guides. |
| degradability | Partially | AI provider failure degrades to a safe `502`; richer degraded-operation modes are not designed. |
| determinability | Covered | Deterministic local mocks behind an explicit `local` profile; in-memory persistence for tests. |
| demonstrability | Covered | Run guides walk the full starter loop (login → `/me` → AI chat) end to end. |
| dependability | Covered | Aggregated from subsets below: availability, fault-tolerance, reliability, recoverability, resilience. |
| deployability | Covered (foundation) | Docker + Cloud Run + EAS Build; "build once, promote" digest promotion is roadmap Phase 5. |
| discoverability | Not yet | No published OpenAPI document or `/api/v1` routes until Phase 2; consumers read code/docs today. |
| distributability | Covered | Stateless backend scales horizontally on Cloud Run; mobile is distributed via app stores. |
| durability | Covered | Firestore (managed) for user data; retention/export/restore/deletion policies not yet defined. |
| effectiveness | Covered | The full starter loop (sign-in → `/api/me` → `/api/chat`) works end to end. |
| efficiency | Not yet | No latency/load baselines; AI timeout, input caps, and quota are roadmap Phase 3. |
| elasticity | Covered | Cloud Run scales on demand with per-environment `min`/`max` instances. |
| evolvability | Covered | Layered architecture, versioned contract plan, documented extension paths. |
| extensibility | Covered | Auth, Firestore, and AI sit behind ports; product entities/storage/search are documented extensions. |
| failure transparency | Not yet | Fail-safe messages exist but are inconsistent (some auth paths are plain text); one unified error envelope with `correlationId` is Phase 2. |
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
| predictability | Covered (foundation) | Explicit profiles and deterministic mocks; removing the `local` default profile is Phase 2. |
| process capabilities | Covered | Merge-gated CI, DEV deploy, PROD approval input, release-record template. |
| proactivity | Not yet | Alerting/SLOs and automated failure reporting are roadmap Phase 5. |
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
| securability | Covered | Confidentiality + integrity + safety rows; hardened identity (App Check) is a P2 consideration. |
| simplicity | Covered | Flat layering, no microservices/Kubernetes, explicit non-goals in the architecture overview. |
| stability | Covered (foundation) | Pinned versions and lockfiles; dependency baseline upgrade is Phase 3. |
| standards compliance | Covered | HTTP/JSON, JWTs, OpenAI-compatible surface; publishing the OpenAPI contract is Phase 2. |
| survivability | Not yet | No multi-region/DR topology; disaster-recovery drills are Phase 6. |
| sustainability | NA | Energy/organizational sustainability is outside the template contract. |
| tailorability | Covered | Tailoring per product = customizability path (rename + env config in `NEW_APP_WORKFLOW.md`). |
| testability | Covered | 14 backend tests, mobile `tsc`/ESLint gates; contract tests/fixtures are Phase 3/4. |
| timeliness | Not yet | No latency or freshness SLOs/budgets; acceptable at prototype scale, revisit before v1. |
| traceability | Covered | Correlation IDs per request; release records pin commit/digest/approver; template provenance recorded. |
| transparency | Covered | Open error mapping, honest docs, public review findings; unified error envelope is Phase 2. |
| ubiquity | NA | Only iOS/Android/web-dev targets; ubiquitous reach is not a goal. |
| understandability | Covered | Small codebase, layered docs, README as a single reading path. |
| upgradability | Not yet | Milestone dependencies (Spring AI, Boot) upgraded in Phase 3; Dependabot config is ready. |
| usability | Covered | Simple consistent interaction patterns; formal usability tests are beyond the template. |
| vulnerability | Not yet | Scanning (dependency/container), SBOM, and secret scanning are Phase 2/3 + repo settings; 14 moderate advisories tracked in `REVIEW_FINDINGS.md`. |

## Keeping this table current

- Rows describe the state after the Phase 1 repository extraction; re-validate at every roadmap exit.
- When a phase completes, flip the corresponding "Not yet"/"Partially" rows to "Covered" with a reference before closing the phase.
- A term with no implementation and no planned roadmap item must not stay "Covered" — the whole point of the table is to make those gaps visible.