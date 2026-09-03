# Security Architecture Plan

Target for template v1: a production architecture that is **near-$0 when idle** while keeping real security in place. This document records the agreed architecture, what changes **now** (v1), and what changes **later** with an explicit trigger. Items we have decided will never be done are omitted by design.

Companion documents: [ARCHITECTURE_OVERVIEW.md](./ARCHITECTURE_OVERVIEW.md) (boundaries) and `starter-backend/docs/SECURITY.md` (baseline policy). This plan is the phased change list on top of that baseline.

## Agreed architecture (decided in review)

```text
                          INTERNET
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
   Firebase Hosting (Expo Web)         Native app (iOS/Android)
            │                                 │
            └───────────────┬─────────────────┘
                            │
                     Firebase Auth
                     Firebase App Check
                            │
                            ▼
                   ┌─────────────────┐
                   │     CLOUD RUN    │
                   │   min = 0        │
                   │   max = N        │
                   │                  │
                   │  Spring Boot 21  │
                   │  Spring Security │
                   │  Auth + AppCheck │
                   │  per-user quotas │
                   └────────┬────────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
          Firestore     Secret Manager   AI provider
          SERVER-ONLY    (runtime SA)     (via AiChatPort)
```

Decisions that shape everything else:

- **No external load balancer** (~$18/mo fixed) and **no Cloud Armor** for v1 — Cloud Run's managed endpoint and routing cover single-region needs, and the service scales to zero.
- **No WAF sidecar (Caddy/Coraza/CRS) in v1** — a JSON API with parameterized Firestore queries and server-side authorization does not have the attack surface CRS defends; CRS on an AI chat endpoint mostly produces false positives on legitimate user text. Revisit only on evidence (see Later).
- **Firestore is server-only.** Clients never talk to Firestore; TanStack Query talks to the REST API. Deny-all rules are belt-and-braces; authorization happens in Spring.
- **Identity is Firebase Auth; authorization is Spring.** The `uid` always comes from the verified ID token, never from a client-supplied field.
- **The money path (AI) gets its own controls**: per-user quotas, per-request caps, timeouts, budget alerts.

## Cost profile

| State | Cost |
|---|---|
| 0 traffic | ~$0 (Cloud Run min=0 + request billing; Firestore/Secret Manager/Artifact Registry within free allowances; Firebase Hosting 10 GB storage + 10 GB/mo transfer free) |
| Small traffic | A few dollars or still $0 on free tiers |
| Attack flood | **Capped by `max-instances`** — worst case ≈ `max_instances × vCPU-seconds/day × rate`. This is the one bill an attacker can force; see Now §3. |

---

## NOW — changes for v1

> **Implementation status (updated after this work):** items marked ✅ are implemented in the repos; items marked ⛔ are **blocked on Phase 5/6** (deployment) or on a deliberate baseline change, and are noted as such rather than left half-done.

| # | Now item | Status |
|---|---|---|
| 1 | Firestore deny-all rules + IAM | ✅ rules file, `firebase.json` wiring, deploy script added; IAM already least-privilege. Deploying rules = Phase 5/6 |
| 2 | App Check (client + backend verify) | ✅ done (web) / 🟡 native wired in code (opt-in, pending console registration + EAS live smoke) — backend verifies RS256 JWT/JWKS manually (the Java Admin SDK has no AppCheck API, so no SDK bump unlocks it); requires a valid token on `/api/v1/ai/chat` when enabled (default off, PROD-on); web provider shipped; native = `@react-native-firebase/app-check` behind the same toggle |
| 3 | Cloud Run cost containment | ✅ `min=0` / `concurrency` encoded in deploy workflows; `max-instances` = attack budget. Live application = Phase 5/6 |
| 4 | AI cost controls | ✅ kill-switch `AI_ENABLED` added; quota/caps/timeout already baseline. Budgets-as-code added to `infra/` + `COST_CONTROLS.md` runbook; live apply/wiring = Phase 5/6 |
| 5 | Firebase Hosting web + CORS/headers | ⛔ blocked on Phase 5/6 (deployment) |
| 6 | Sign-up abuse gate | 🟡 phased A→C→B ([runbook](../integrations/SIGNUP_ABUSE_GATE.md)) — A: native App Check wired, console+smoke pending; C: backend implemented (route in contract, gate default-off), mobile adoption open; B needs Cloud Functions (Phase 5/6) |
| 7 | Observability & errors | ✅ baseline; added `starter.ai.rejected/reason=disabled` metric |
| 8 | Secrets/CI + supply chain | ✅ baseline (WIF, Dependabot, Trivy gate); image-scanning budget decision still open |

#### Implemented now (this change)

- **AI kill-switch** (`starter.ai.enabled` / `AI_ENABLED`): `ChatService` fails closed with `503 AI_DISABLED` before input/quota/provider work; `rejected(reason=disabled)` metric; wired as a strict placeholder in dev/prod profiles and set in both deploy workflows; documented in the OpenAPI (new `AiDisabled` response) and backend README. Tests: `ChatKillSwitchIntegrationTest`, kill-switch cases in `AiRequestGuardTest` and `ChatServiceTest`. Flip the env var on the running Cloud Run service to stop the money path without a redeploy.
- **Firestore deny-all rules**: `starter-backend/firestore/firestore.rules` (deny all client read/write), referenced from `firebase.json`, deployable via `./scripts/deploy-firestore-rules.sh <project-id>`.
- **Cloud Run cost containment**: `--min-instances 0` and `--concurrency` added to deploy-dev/prod so the pinned values (min=0, and `max-instances` as the attack-budget ceiling) are captured in source with an explanatory comment.

#### Blocked on Phase 5/6 (deployment) — deferred, do not force

- **App Check** (client init + Spring `verifyToken`): **implemented** — because the Java Admin
  SDK has no `AppCheck.verifyToken` (verified in `firebase-admin:9.10.0`), the backend verifies
  the RS256 token manually against the project's public keys (Nimbus JWT/JWKS). Web provider
  shipped; native is wired in code (`@react-native-firebase/app-check` behind the opt-in toggle)
  pending console registration + EAS live smoke. Enforcement off by default, PROD-on after a
  live smoke.
- **Firebase Hosting** web app + CORS origin + security headers on the served app.
- **Budget/spend alerts applied**: the budget resources (50/90/100% + forecast, Pub/Sub + optional email) are written in `infra/main.tf` but only materialize once `billing_account_id` is set in the tfvars and `terraform apply` runs against the live billing account; automating the stop actions (Pub/Sub → Cloud Function) is also Phase 5/6.
- **Sign-up abuse gate**: phased A→C→B per [integrations/SIGNUP_ABUSE_GATE.md](../integrations/SIGNUP_ABUSE_GATE.md) — Phase A (App Check enforcement on Identity Platform) is console-only with native App Check now wired; Phase C (reCAPTCHA assessment on Spring-mediated sign-up) is **backend-implemented** (route in contract, gate default-off), mobile adoption open; Phase B (blocking functions) is evidence-triggered. Full plan verified against live Firebase/Google docs; the old "blocking functions + reCAPTCHA" single-mechanism reading was wrong — a `beforeUserCreated` event carries no reCAPTCHA token channel.
- **Deploying the Firestore rules** to the DEV/PROD projects.

---

Legend: ✅ baseline already in code · 🟡 partial, finish it · ⬜ new work

### 1. Firestore locked server-only (✅ added)

- ⬜ Add Firestore rules denying all client access and deploy them per environment:

  ```javascript
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      match /{document=**} {
        allow read, write: if false;
      }
    }
  }
  ```

  Server SDKs bypass rules via IAM, so Spring is unaffected.
- ✅ Runtime service account already least-privilege: `roles/datastore.user` (Firestore), `roles/secretmanager.secretAccessor`, `roles/monitoring.metricWriter`, `roles/logging.logWriter` — no Owner/Editor (`starter-backend/infra/main.tf`).
- 🟡 Verify no client-side Firestore SDK usage anywhere in `starter-mobile`; add a lint/CI check if needed. The mobile repo must stay TanStack Query → REST only.

### 2. Firebase App Check (authentication + client authenticity)

- ✅ **Backend** verifies the App Check token (`X-Firebase-AppCheck`) with a manual RS256
  JWT/JWKS check (Nimbus) — the Java Admin SDK has no `AppCheck.verifyToken`, so no dependency
  bump unlocks it. `AppCheckVerifier` port + `JwksAppCheckVerifier` adapter (`!local`) + `MockAppCheckVerifier`
  (local) + `AppCheckFilter` (require on `/api/v1/ai/chat`, verify-if-present elsewhere).
- ✅ **Web client**: `@firebase/app-check` reCAPTCHA Enterprise provider; header sent on every
  request (omitted when unavailable). See [`integrations/APP_CHECK.md`](../integrations/APP_CHECK.md).
- ⬜ **Native client** (Play Integrity / App Attest): stubbed to `null` on native — the native
  module + console registration are a follow-up. Until then, native devices send no token, which
  the backend accepts except where required.
- Policy: reject invalid tokens regardless of route; **require** a valid token on `/api/v1/ai/chat`
  from v1; treat as an auxiliary signal elsewhere, never as an authentication replacement.
- Note: App Check tokens are replayable until expiry and web attestation is bypassable by headless
  clients — it raises the bar for casual scripters; per-user quotas are the real abuse control.

### 3. Cloud Run cost containment (the flood ceiling)

- ⬜ Pin in the deployment definition (workflow manifest + Terraform where it owns resources), not just a README:
  - `min-instances = 0` (request-based billing, CPU throttled)
  - `max-instances = N` where **N is chosen by cost math, not ambition**: worst-case attack cost/day ≈ `N × vCPU-seconds/day × rate`. Start conservative (10–20); N is your daily "attack budget".
  - `concurrency = 40–80` — benchmark against Spring before fixing.
- ⬜ Keep ingress public (no LB in front), but accept that an unauthenticated HTTP flood can still start instances — `max-instances` is the only ceiling in this architecture.
- ⬜ Add a verification step that prod settings match the pinned values (fail-closed test or workflow assertion).

### 4. AI cost and abuse controls

- ✅ Per-user quota guard exists (`AiRequestGuard`, in-memory sliding window, 429 `RATE_LIMITED` + `Retry-After`).
- 🟡 **Set explicit production limits via env/config, never permissive defaults**: requests per window, max prompt size, `max_tokens`, provider timeout. In-memory quota is per-instance (documented caveat) — acceptable for v1; hard global quota is Later §1.
- ⬜ **Budget alerts**: GCP budget alerts on the project **and** spend alerts on the AI provider account; on breach, quota endpoints return 429s automatically and a kill-switch (per-provider flag) exists.
- ✅ Provider keys live in Secret Manager (`openai-api-key`), injected to the runtime SA — never in the app or `EXPO_PUBLIC_*`.

### 5. Expo Web hosting + transport hardening

- ⬜ Host the Expo web build on **Firebase Hosting** (free tier, global CDN, custom domain, SSL). Route non-AI endpoints through Hosting rewrites → Cloud Run.
- ⬜ **Keep `/ai/chat` calling Cloud Run directly** (Hosting → Cloud Run rewrites have a 60 s timeout; AI generation + streaming exceeds it).
- 🟡 CORS already fail-closed per profile; add the Hosting domain to prod `STARTER_CORS_ALLOWED_ORIGINS` (no wildcard).
- ⬜ Web security headers: CSP, HSTS, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy` on the web app.
- ✅ Firebase web config is public-by-design (identifier, not credential); no backend keys ever enter the client.

### 6. Sign-up abuse gate (makes per-uid quotas meaningful)

Phased A→C→B — details, click-paths, cost traps: [integrations/SIGNUP_ABUSE_GATE.md](../integrations/SIGNUP_ABUSE_GATE.md).

- ⬜ **Phase A**: App Check enforcement on Identity Platform (console toggle) — stops scripted account minting; native App Check is **wired in code**, console registration + EAS live smoke remain.
- 🟡 **Phase C**: reCAPTCHA Enterprise assessment on **Spring-mediated sign-up** — **backend implemented** (`POST /api/v1/auth/sign-up`, `starter.recaptcha.*`, default off; a `beforeUserCreated` blocking function cannot do it — no client token channel; `recaptchaScore` is SMS-only on `beforeSmsSent`). Mobile adoption open; native stays on Phase A until a product needs per-attempt scoring there.
- ⬜ **Phase B**: `beforeUserCreated` blocking function (Identity Platform upgrade + Blaze + first Cloud Functions deploy) — only on evidence of uid-mint velocity.
- ⬜ Disable anonymous auth unless a feature explicitly needs it (Phase 0, console-only).
- Rationale: per-uid quotas are the strongest abuse control in this design — but only as strong as account creation being hard to automate.

### 7. Observability and errors

- ✅ Safe-telemetry logging already in place (no tokens, prompts, or bodies by default; quota rejections counted).
- 🟡 Add abuse-oriented metrics + alert hooks: quota-rejection rate by reason, single-uid burst detection, per-endpoint 4xx spikes.
- ✅ One error envelope (`code`, `message`, `correlationId`), generic messages, no stack traces.

### 8. Secrets, CI/CD, supply chain

- ✅ WIF (OIDC) from GitHub Actions with repository attribute condition; no service-account JSON in CI (`infra/main.tf`).
- ✅ Dependabot enabled in both repos.
- 🟡 Container image scanning: Artifact Registry scanning is now a paid feature — make an explicit budget/owner decision for v1 (alternatives: local `trivy` in CI).
- ⬜ EAS Update **code signing** + separate dev/preview/prod channels — only when EAS Update is actually enabled (see Later §6).

---

## LATER — changes with explicit triggers

| # | Change | Trigger | Notes |
|---|---|---|---|
| 1 | Hard global quota store (Memorystore/Redis or batched counters) | Multiple instances routinely serving, or a hard per-user limit is a product requirement | In-memory per-instance approximation becomes too lenient; do **not** use per-request Firestore writes for counters (they cost money) |
| 2 | Cloud Armor Standard + HTTPS load balancer, ingress `internal-and-cloud-load-balancing` | Public launch at meaningful traffic, or observed flood/attack traffic | ~$25–30/mo fixed; blocks floods at the edge before instances start |
| 3 | In-pod WAF (Caddy and/or Coraza; CRS only with per-route exclusions for `/ai/*`) | Evidence of actual attack traffic; default is never | v1 stays Spring-only; add only when logs show a WAF-shaped gap |
| 4 | Session revocation (`checkRevoked`) | Paid tiers / canceled accounts must lose access immediately | ID tokens live ~1 h; acceptable until then |
| 5 | Cold-start mitigation (`min-instances=1` at discounted standby rate, or CRaC/AppCDS) | First-request-after-idle latency complaints | Costs a few $/mo; explicitly trades away "essentially $0" |
| 6 | EAS Update with code signing | EAS Update is enabled | OTA pipeline = production attack surface; sign updates end-to-end |
| 7 | Anomaly alerting tuned (uid hitting 90% of daily cap early, new-user bursts) | Daily AI volume justifies thresholds | Otherwise alert noise |
| 8 | Optional Cloudflare free tier in front of a custom domain | Want flood absorption before paying for Armor | Verify ToS fit for API proxying first |

---

## Open parameters to set (owners: backend + infra)

- Exact quota numbers (requests/window, requests/day, tokens/day) — product decision, start from current `AiGuardConfig` defaults and tighten for prod.
- `max-instances` and `concurrency` after a load benchmark of the Spring service.
- Production web origin list for CORS once the Hosting domain exists.
- Whether anonymous auth is ever needed (default: no).
- Image scanning tool + budget for v1.

## Deliberately not in v1 (revisit only via Later triggers)

- External load balancer, Cloud Armor, per-IP edge rate limiting.
- WAF sidecar / OWASP CRS.
- Client-side Firestore access or Firebase Security Rules authoring for clients (server-only is the point).
- Any client-held provider/backend secrets.
