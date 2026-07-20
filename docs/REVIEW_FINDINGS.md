# Project Review Findings

Reviewed on 2026-07-20 against the local `docsera` and `docsera-mobile` references and the current starter implementation.

## Summary

The prototype proves the core integration loop—Firebase identity, a Firestore user, a server-side AI call, and an Expo client—and the backend test suite passes. It is a useful seed, but it is not ready to be the source for many production apps. The main gaps are repository boundaries, fail-closed configuration, contract management, and release correctness.

## Priority findings

### P0 — resolve before template v1

1. **Repository model contradicts the requirement.** Both projects and all workflows are tracked by the parent Git repository. The target is two independent repositories with separate secrets, releases, and CI/CD.
2. **The default backend profile is unsafe.** `spring.profiles.default: local` means a missing cloud profile can activate mock authentication, mock persistence, and mock AI. Local mocks must require an explicit `local` profile; deployed startup must fail when the environment is incomplete.
3. **Deployments are not gated by CI.** Backend deploy and test workflows run independently on the same push, and deployment builds with tests skipped. A failing test can therefore coexist with a deployment attempt.
4. **Backend production is rebuilt.** The production workflow rebuilds the container, despite the docs claiming the exact DEV artifact is promoted. Production must deploy the immutable image digest already tested in DEV.
5. **Mobile promotion is technically incorrect.** The current preview profile produces an internal-distribution artifact with development configuration. That binary is not the production store artifact. A store-signed release candidate must be built separately, tested through TestFlight/Play internal testing, then released unchanged.
6. **There is no machine-readable API contract or route version.** Independent repositories need an OpenAPI source of truth, compatible error schemas, and `/api/v1` routes before they can evolve safely.

### P1 — required for a dependable starter

1. **AI sessions are misleading.** The API returns and reuses `sessionId`, but the provider adapter does not load history or preserve conversation memory. Document the endpoint as stateless or implement explicit conversation storage later.
2. **Documented `429` behavior is not implemented.** There is no per-user rate limit, quota, model budget, timeout policy, or `Retry-After` response. A generic AI starter should include cost and abuse controls.
3. **Public actuator information is broader than necessary.** `/actuator/info` exposes build metadata publicly. Prefer minimal dedicated liveness/readiness endpoints and keep actuator details private.
4. **Error responses are inconsistent.** Validation/provider errors are JSON, while some authentication paths return plain text. Use one versioned error envelope with a correlation ID.
5. **The baseline dependencies are old for a reusable 2026 template.** The prototype uses Spring Boot 3.2.3 and Spring AI `1.0.0-M1`. Upgrade and revalidate before declaring template v1; do not copy milestone dependencies into new apps by default.
6. **Cloud setup is manual.** Repeated app creation needs versioned infrastructure-as-code or an equally idempotent bootstrap, plus plan/apply controls and documented state ownership.
7. **Documentation contains broken sibling links and mixed “planned/implemented” language.** Each future repository must be readable from a standalone clone and clearly label current versus target state.
8. **The mobile toolchain is not deterministic locally.** `expo lint` fails in the current dual-architecture Node setup because its import resolver cannot load the optional `unrs-resolver` native binding, even after clean installs. Direct ESLint and TypeScript pass. Pin one supported Node version/architecture and make the repository script/CI reproduce it.
9. **Current logs include user email at account creation.** Treat email as personal data and avoid logging it in the generic platform baseline. Prefer a pseudonymous internal identifier only when operationally necessary.
10. **The mobile submit workflow accepts one build ID for both platforms.** iOS and Android builds have distinct EAS build records; release metadata/workflow inputs must track them separately and submit only store-signed production builds.

### P2 — hardening after the foundation

- Add dependency update automation, SBOM/provenance, container vulnerability policy, and secret scanning.
- Add API contract tests and mobile integration fixtures.
- Add structured release metadata, SLOs, alerts, and a rollback drill.
- Add Firebase App Check if abuse patterns justify the operational cost.
- Add database indexes, retention, export/restore, and deletion policies when product data expands beyond the starter user record.

## Verification performed

| Check | Result |
|---|---|
| Backend `./mvnw test -B` | Pass |
| Mobile `npx tsc --noEmit` | Pass |
| Mobile direct `eslint . --no-cache` | Pass under the matching installed Node architecture |
| Mobile `npm run lint` (`expo lint`) | Fails because the Expo wrapper/import resolver cannot load the optional `unrs-resolver` native binding in the current dual-architecture Node setup; clean installs under Node 20 x64 and Node 24 arm64 were both checked |
| Mobile `npm ci` audit summary | Installs successfully but reports 14 moderate dependency advisories; triage applicability during the dependency-baseline upgrade |
| Repository boundary check | Both child folders resolve to `/Users/pb/develop/starter` as their Git top-level |
| Documentation/code comparison | Found the release, profile, AI-session, status, and relative-link inconsistencies listed above |

## Evidence locations

| Finding | Current source |
|---|---|
| Local profile is the default | [`application.yml`](../starter-backend/src/main/resources/application.yml#L4) |
| DEV deploy skips tests and is independent of CI | [`deploy-dev-backend.yml`](../.github/workflows/deploy-dev-backend.yml#L26) |
| PROD checks out and rebuilds | [`deploy-prod-backend.yml`](../.github/workflows/deploy-prod-backend.yml#L30) |
| One EAS ID submitted as both platforms | [`eas-submit-prod.yml`](../.github/workflows/eas-submit-prod.yml#L3) |
| AI adapter ignores session history | [`SpringAiOpenRouterAdapter.java`](../starter-backend/src/main/java/com/starter/adapters/ai/SpringAiOpenRouterAdapter.java#L21) |
| User email is logged | [`UserService.java`](../starter-backend/src/main/java/com/starter/application/UserService.java#L24) |

## Recommended decision set

- Two template repositories, not a monorepo
- Backend-owned OpenAPI contract and mobile compatibility pin
- Explicit local mocks and fail-closed cloud configuration
- Firebase Auth + Firestore + provider-neutral AI port as the only required integrations
- Immutable backend image digest promotion
- DEV preview builds separate from store release-candidate builds
- Repeatable infrastructure provisioning owned by the backend repository
- Tagged template releases used to create products; no automatic upstream syncing
