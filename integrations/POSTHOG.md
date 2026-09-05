# POSTHOG.md — Product Analytics + Feature Flags (PostHog)

**Status: implemented in both templates (2026-09-04), opt-in at runtime.** This runbook is the
activation checklist for product analytics and remote feature flags across the template:
[PostHog](https://posthog.com) captures mobile product events and serves the backend its flags
via server-side *local evaluation* (free tier ≈ 1M analytics events + 1M feature-flag requests
per month, usage hard-stops at the limit — snapshot 2026, re-verify on the
[pricing page](https://posthog.com/pricing)). The code ships but is inert — nothing is sent and
no flag is consulted until a product enables it (console + keys below).

One **PostHog project per product app**; mobile and backend share it with different key types.

### Current starter-project status (2026-09-04)

- Project created: **US cloud**, project id `594242` (https://us.posthog.com/project/594242) —
  region is fixed per project; the template default host `eu.i.posthog.com` is overridden via
  `POSTHOG_HOST` / `EXPO_PUBLIC_POSTHOG_HOST` (`https://us.i.posthog.com`).
- Mobile: wired for local dev — `EXPO_PUBLIC_POSTHOG_*` set in gitignored
  `starter-mobile/.env` (`EXPO_PUBLIC_POSTHOG_ENABLED=true`, project key, US host).
- Backend: keys stored in gitignored `starter-backend/.env` for the `dev-local` smoke
  (`FLAGS_ENABLED=true`, project secret API key with `feature_flag:read`, project key, US
  host) — endpoint verified live: `HTTP 200`, flag `ai-chat-enabled` active at 100%.
  Cloud wiring (Secret Manager + deploy-workflow steps) **landed 2026-09-05** (§2); the
  per-project cloud activation run itself is still pending.
- Pending: create flag `ai-chat-enabled` (boolean, 100%) in the PostHog UI; DEV kill-switch
  drill (§4).

```text
starter-mobile  ──events (capture, public project key)──────────▶ PostHog <app> project
starter-backend ──definitions poll (secure API key, Secret Manager)──▶ same project
     └─ FeatureFlagPort ◀── in-JVM evaluation of simple boolean flags
```

## Decisions (recorded 2026-09-04, see also `docs/integrations-plan.md` §5)

- **PostHog over Firebase Remote Config.** The plan's title offered "PostHog or Firebase
  Remote Config", but only PostHog can deliver *both* halves in this template: Firebase GA4's
  JS SDK is browser-only, and native analytics would need `@react-native-firebase` — the
  native-module stack the template deliberately avoided (web is first-class; same precedent as
  the App Check web provider). PostHog covers analytics + flags from one SDK on web and native
  (`posthog-react-native`, official [Expo guide](https://docs.expo.dev/guides/using-posthog/)),
  with EU (Frankfurt) or US cloud hosting and an MIT-licensed self-host escape hatch.
  Firebase Remote Config stays the documented **flags-only fallback**: `FeatureFlagPort` +
  `AnalyticsPort` are vendor-neutral, so swapping means writing one adapter.
- **Backend evaluates flags in-JVM via local evaluation — no SDK.** The adapter polls
  `GET {host}/flags/definitions?token=<project-key>` with `Authorization: Bearer <server key>`
  — the exact request shape of posthog-node's poller (verified against posthog-node 5.51.6) —
  and evaluates simple boolean flags itself. `RestClient` + `JsonMapper`, matching the Resend
  precedent; no community Java SDK in the dependency tree. The server key is a PostHog
  **project secret API key** with the `feature_flag:read` scope (the "Local feature flag
  evaluation" preset, `phs_...`): project-scoped, rotatable, hashed at rest. The legacy
  Feature Flags Secure API Key and personal API keys also authenticate on the same endpoint
  (PostHog deprecated both for new integrations). Verified live against a US-cloud project
  2026-09-04: boolean flags ship with `filter_type` omitted, so the adapter treats
  `filter_type=null` + 100% rollout + no targeting properties as a simple boolean.
- **Simple boolean flags only at v1.** Multivariate and partial-rollout flags evaluate to
  *absent* (static-config fallback). Re-implementing PostHog's hashed per-user rollout in Java
  is explicitly out of scope until a product needs per-user experiments.
- **Kill-switch semantics are non-breaking.** The static `starter.ai.enabled` config stays
  authoritative as the fallback; the remote flag `ai-chat-enabled` only ever **kills**:
  - static off → rejected (`reason=disabled`) before the provider is ever contacted;
  - flag present `false` → rejected (`reason=disabled_remote`) instantly, no redeploy;
  - flag absent / unknown key / unsupported type / provider outage → static config decides.
  Failed refreshes keep serving the last known snapshot (a kill-switch must be sticky); a
  provider outage surfaces as the `starter.flags.refresh{outcome=error}` metric, never as an
  exception.
- **Mobile web is included** (unlike Sentry at v1): `posthog-react-native` resolves to a
  web-compatible build under react-native-web. Caveat: the Expo guide's tested path is
  Android/iOS — verify the web export in a browser smoke during activation, and gate web off
  Sentry-style if it misbehaves.
- **No fingerprint change.** The SDK's native/expo helpers are optional requires (verified in
  `posthog-react-native@4.67.0`: `dist/optional/*` wrap `require` in try/catch); v1 installs
  the JS SDK only, so no config plugin, no native rebuild. Session replay would change that
  (see Follow-ups).
- **Privacy bar = Sentry's.** No autocapture (the RN SDK default), uid-only `identify`,
  outcome-only events, never prompt/reply content, emails, or tokens (backend security rule 4,
  mobile security rule 6).
- **`local` never touches the network** — `DisabledFeatureFlagAdapter` (flags off) or
  `MockFeatureFlagAdapter` (flags on, offline "no opinion" for every key).

## Where the implementation lives (implemented)

### Backend (`starter-backend`)

| Piece | Path | Mirrors |
|---|---|---|
| Port (tri-state) | `src/main/java/com/starter/ports/FeatureFlagPort.java` | [`ErrorReporter.java`](../starter-backend/src/main/java/com/starter/ports/ErrorReporter.java) |
| Config (fail-closed) | `src/main/java/com/starter/config/FeatureFlagsConfig.java` | [`EmailConfig.java`](../starter-backend/src/main/java/com/starter/config/EmailConfig.java) |
| Real adapter (`!local` + enabled) | `src/main/java/com/starter/adapters/posthog/PostHogFlagAdapter.java` | `ResendEmailAdapter` (`RestClient`, no SDK) |
| Local mock (`local` + enabled) | `src/main/java/com/starter/adapters/posthog/MockFeatureFlagAdapter.java` | `NoOpErrorReporter` |
| Disabled default (any profile) | `src/main/java/com/starter/adapters/flags/DisabledFeatureFlagAdapter.java` | — (guarantees a port bean exists when off) |
| Hook point | [`AiRequestGuard.java`](../starter-backend/src/main/java/com/starter/application/AiRequestGuard.java) → `AI_CHAT_ENABLED_FLAG` | none — the guard is the single funnel |
| Default config keys | `src/main/resources/application.yml` → `starter.flags.*` block | `starter.sentry.*`, `starter.email.*` |
| Tests | `PostHogFlagAdapterTest`, `FeatureFlagsConfigTest`, `AiRequestGuardTest` | `ResendEmailAdapterTest` pattern |

Config keys (all `starter.flags.*`, env-tunable):

```yaml
starter:
  flags:
    enabled: ${FLAGS_ENABLED:false}          # opt-in, off by default
    api-key: ${POSTHOG_API_KEY:}             # project secret API key (feature_flag:read) — SECRET
    project-api-key: ${POSTHOG_PROJECT_API_KEY:}     # public project key (phc_...)
    host: ${POSTHOG_HOST:https://eu.i.posthog.com}   # or https://us.i.posthog.com
    poll-interval: ${FLAGS_POLL_INTERVAL:30s}        # lazy snapshot refresh cadence
    timeout: ${FLAGS_TIMEOUT:3s}             # per-request provider timeout
```

- **Enabled requires `api-key`** — startup fails otherwise (fail closed, same as
  billing/email/sentry).
- **`local` always serves the mock** — enabling locally only exercises wiring (dummy key OK).
- **Metrics:** `starter.flags.refresh{outcome=ok|error}` (snapshot health) and the existing
  `starter.ai.rejected{reason=disabled_remote}` (remote kill in action).

Flag evaluation contract (`isEnabled` → effect on `POST /api/v1/ai/chat`):

| PostHog state of `ai-chat-enabled` | `isEnabled` returns | Effect |
|---|---|---|
| active, simple, 100% rollout | `true` | chat unaffected |
| inactive (any filters) | `false` | `503 AI_DISABLED` instantly, `reason=disabled_remote` |
| multivariate / partial rollout | empty | static `starter.ai.enabled` decides |
| unknown key / outage / flags off | empty | static `starter.ai.enabled` decides |

### Mobile (`starter-mobile`)

| Piece | Path |
|---|---|
| Port | `src/ports/AnalyticsPort.ts` (`capture` / `identify` / `reset`, never throws) |
| Adapter | `src/adapters/PostHogAnalyticsAdapter.ts` — `getAnalytics()`, no-op until init |
| Init | `src/telemetry/posthog.ts` — `initPostHogIfEnabled()`, called in `app/_layout.tsx` next to `initSentryIfEnabled()` |
| Opt-in toggle + key | `EXPO_PUBLIC_POSTHOG_ENABLED`, `EXPO_PUBLIC_POSTHOG_API_KEY` (public by design), `EXPO_PUBLIC_POSTHOG_HOST` — `app.config.ts` extra + `src/config/env.ts` |
| Base events | `app_opened {appEnv}` (init) · `user_signed_in` / `user_signed_out` (+ uid `identify`/`reset`) in `AuthProvider` · `chat_message_sent {outcome}` in `useChat` |
| Test isolation | global `posthog-react-native` module mock in `jest.setup.ts` (per-file overrides in the analytics tests) |

Events at v1 are deliberately minimal and PII-free; products add their own via
`getAnalytics().capture(...)` — the no-op default means event call sites never need opt-in
checks.

## What needs to be done (per environment)

### 1. PostHog console (per product app)

- [ ] Create the org/account → [posthog.com](https://posthog.com). **Region (EU/US) is chosen
      per project — pick once**; the template defaults to EU (`https://eu.i.posthog.com`).
- [ ] Create one project `<app>` (analytics + flags share it). Environments distinguish
      themselves by `appEnv`/distinct-id properties, not by separate projects (same reasoning
      as the single Sentry project per app).
- [ ] Copy the **project API key** (`phc_...`, public) for mobile + the backend URL token.
- [ ] Create the backend server key: Project settings → *Feature flags* tab → **"Create a
      project secret API key"** with the **"Local feature flag evaluation" preset**
      (`feature_flag:read`). Shown once (hashed at rest) — copy it immediately. Do NOT use a
      personal API key, and delete any key whose `Feature flag` scope is NA.

### 2. Backend secrets & variables (repo: `<app>-backend`)

- [x] **Wired in the template (2026-09-05)**: `posthog-api-key` is in `infra/main.tf`
      `local.secrets` (Terraform creates the secret + grants the runtime SA) and
      `infra/scripts/set-secrets.sh` takes `--posthog-api-key 'phs_...'`; both workflows have
      a "Configure feature flags extension" step (gated on `FLAGS_ENABLED_DEV`/`_PROD`
      repo variables) that binds `POSTHOG_API_KEY=posthog-api-key:latest` +
      `FLAGS_ENABLED=true` and optionally `POSTHOG_HOST` — `--update-*` only, never
      `--set-*`, and an empty host variable is deliberately not passed (blank host fails
      startup).
- [ ] Per-project activation: `./infra/scripts/set-secrets.sh dev --posthog-api-key 'phs_...'`
      (then `terraform apply` once so the secret resource exists), set the
      `FLAGS_ENABLED_DEV=true` repo variable, redeploy.
- [ ] Non-secret variable: `POSTHOG_PROJECT_API_KEY` = the public `phc_...` project key
      (same value the mobile app uses; it identifies the project in the definitions URL) —
      add via a repo variable + the same workflow step when a product needs it.
- [ ] Non-secret variables: `FLAGS_ENABLED_DEV=true` (later `FLAGS_ENABLED_PROD=true`),
      optionally `POSTHOG_HOST_DEV`/`POSTHOG_HOST_PROD` for US projects.
- [ ] Create the flag **`ai-chat-enabled`** in the PostHog UI (boolean, 100% rollout) —
      matching [`AiRequestGuard.AI_CHAT_ENABLED_FLAG`](../starter-backend/src/main/java/com/starter/application/AiRequestGuard.java).
      With flags enabled but the flag absent, the static config decides, so creating it is
      safe but required before the remote switch works.

### 3. Mobile variables (repo: `<app>-mobile`)

- [ ] `EXPO_PUBLIC_POSTHOG_API_KEY` (project key) + `EXPO_PUBLIC_POSTHOG_ENABLED=true` in the
      **dual location** the Firebase trio uses (GitHub repo variables AND EAS environment
      variables — see [`guides/STEP6_NEW_APP_FROM_STARTER.md`](../guides/STEP6_NEW_APP_FROM_STARTER.md) §5).
- [ ] DEV and PROD share the project key; `app_opened {appEnv}` distinguishes environments
      (same reasoning as the single Sentry DSN). Use separate projects only if DEV noise
      becomes a problem.

### 4. Verify (DEV)

- [ ] Backend local gate: `./mvnw verify` green — no flag beans touch the network; the
      fail-closed test (`FeatureFlagsConfigTest`), adapter tests (`PostHogFlagAdapterTest`)
      and the Spring wiring test (`PostHogFlagAdapterContextTest`) prove shape, availability
      rules, and bean selection.
- [ ] **Remote kill drill on DEV** — full walkthrough below (executed successfully
      2026-09-04 against a local `dev-local` JVM + real DEV Firebase Auth + live US-cloud
      PostHog):

<details>
<summary><strong>Step 1 — start the backend with flags enabled</strong></summary>

```bash
cd starter-backend
set -a; source .env; set +a          # FLAGS_ENABLED=true, POSTHOG_* (gitignored, see §2)
export SPRING_PROFILES_ACTIVE=dev-local
export GCP_PROJECT_ID=starter-demo-dev             # your {app}-dev Firebase/GCP project
export OPENAI_API_KEY=drill-not-used-placeholder   # never reached: guards fire first
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Expect `Started StarterApplication`. The PostHog snapshot is fetched **lazily on the first
flag check** — the refresh log appears after the first chat request, not at startup.

</details>

<details>
<summary><strong>Step 2 — mint a DEV Firebase user + token (no mobile app needed)</strong></summary>

```bash
API_KEY=$(grep "^EXPO_PUBLIC_FIREBASE_API_KEY" ../starter-mobile/.env | cut -d= -f2)
curl -s "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{"email":"flagdrill-<ts>@starter-drill.test","password":"DrillPass!2026x","returnSecureToken":true}'
```

Grab `idToken` from the response. An unverified email is fine: the flag check runs *before*
the email-verified gate, so flag-on vs flag-off are distinguishable (`403` vs `503`)
without verifying anything and without touching the AI provider.

</details>

<details>
<summary><strong>Step 3 — flag ON: request passes the flag, fails the next gate</strong></summary>

```bash
curl -s -w "\nHTTP %{http_code}\n" http://localhost:8081/api/v1/ai/chat \
  -H "Authorization: Bearer $ID_TOKEN" -H "Content-Type: application/json" \
  -d '{"message":"flag drill"}'
```

Expected (captured 2026-09-04):

```text
PostHog flag snapshot refreshed (1 flag(s))     <- backend log, first request
{"code":"EMAIL_NOT_VERIFIED",...} HTTP 403      <- flag did NOT kill; next guard ran
```

</details>

<details>
<summary><strong>Step 4 — flip the flag off, expect 503 within one poll (captured 2026-09-05)</strong></summary>

UI path (needs a browser session): https://us.posthog.com/project/<id>/feature_flags →
`ai-chat-enabled` → toggle off. The drill key is read-only (`feature_flag:read`) by design,
so the live off/on transition below was executed against a **local mock PostHog** serving
the byte-identical response shape (same endpoint, same poll loop, same adapter code path —
only the JSON payload differs, and that shape is verified live in §3). To reproduce:

```bash
python3 /tmp/mock-posthog.py &                      # serves /flags/definitions on :18087
export POSTHOG_HOST=http://127.0.0.1:18087          # overrides the .env host for this run
echo off > /tmp/flag-state                          # = toggling the flag off in the UI
echo on  > /tmp/flag-state                          # = toggling it back on
```

Captured timeline (6s probes, `poll-interval=30s`):

```text
08:59:55  flag flipped OFF    403 EMAIL_NOT_VERIFIED × 4  <- sticky snapshot, not yet polled
09:00:20  503 AI_DISABLED     25s after flip (within one poll-interval)  <- remote kill
09:00:20  flag flipped ON     503 AI_DISABLED × 5        <- sticky in this direction too
09:00:50  403 EMAIL_NOT_VERIFIED  recovered, 30s after flip
```

The guard is fail-safe in both directions and the flag only ever kills; static config
stays the primary gate.

</details>

<details>
<summary><strong>Step 5 — outage drill (sticky last-known values)</strong></summary>

Block egress to `*.i.posthog.com` (Little Snitch / firewall) → the adapter cannot refresh →
chat keeps following the **last known** flag value (sticky); a blocked *first* fetch leaves
no snapshot and requests fall back to static config. `starter.flags.refresh{outcome=error}`
increments. Unblocking restores refreshes on the next poll.

</details>

- [ ] Mobile: `npm run lint`, `npx tsc --noEmit`, `npm test` — analytics no-ops without the
      toggle; `npm run validate:contract` untouched (zero contract surface added).
- [ ] **Live smoke:** with `EXPO_PUBLIC_POSTHOG_ENABLED=true` + project key, sign in and send
      a chat message → events (`app_opened`, `user_signed_in`, `chat_message_sent`) appear in
      the PostHog project within seconds. **On web too** (browser smoke) — web support in the
      RN SDK is newer than its native path; if it misbehaves, gate web off Sentry-style.

Runtime findings baked into the implementation (2026-09-04 drill):

- The drill exposed a **real wiring bug**: two constructors (primary + test seam) made Spring
  fail with `No default constructor found` once `FLAGS_ENABLED=true` — unit tests never boot
  the bean. Fixed with `@Autowired` on the primary constructor + `PostHogFlagAdapterContextTest`
  locking bean selection for all three adapters (enabled/dev, enabled/local, disabled).
- PostHog now omits `filter_type` on plain booleans; the evaluator treats
  `filter_type=null` + 100% rollout + no targeting properties as a supported simple boolean,
  and treats group `properties` (per-user targeting) as unsupported → absent.

### 5. PROD (only after DEV is proven)

- [ ] `FLAGS_ENABLED_PROD=true` repo variable; the PROD "Configure feature flags extension"
      step (promote-prod.yml) binds the same Secret Manager key (set
      `POSTHOG_HOST_PROD=https://us.i.posthog.com` for US projects).
- [ ] Keep `ai-chat-enabled` **on** with the static config also on — the flag is an
      emergency brake, not the primary gate; `starter.ai.enabled` remains the deployment-level
      switch.

## Follow-ups (not v1 core)

- **Client-side flags in mobile UI** (`useFeatureFlag` over the same PostHog client) — the SDK
  already fetches them; product UI only needs the hook. Backend stays authoritative for
  authorization/business rules; client flags are presentation-only.
- **PostHog AI/LLM analytics (`$ai` events)** for OpenRouter chat quality/cost tracking —
  natural next step, no architecture change.
- **Session replay / error tracking** — needs the `posthog-react-native/expo` config plugin
  and native modules → **fingerprint change → new dev builds**; defer until a product asks.
- **Deploy-workflow activation steps** (see §2) — copy the Sentry pattern when a product
  enables flags/analytics for real.

## Alternatives considered

| Option | Verdict |
|---|---|
| Firebase Remote Config (+GA4) | Flags-only fallback documented above; GA4 cannot serve this template's native target without `@react-native-firebase` (breaks web) |
| Togglz / FF4J (Spring flag libs) | In-JVM only — no remote switch, no analytics; rejected |
| LaunchDarkly / Unleash | Flags without analytics; paid or self-hosted ops; port makes swapping trivial if a product needs it |
| Self-hosted PostHog (MIT) | Supported escape hatch; `host` is already config — swap without code changes |

## Rules that must not be broken

- **The remote flag only ever kills.** It can never enable AI while `starter.ai.enabled=false`.
- **Absent/unknown/outage ⇒ static config decides.** The integration must not be able to take
  AI down by itself; refresh failures keep last-known values and never throw.
- **Two key types, two trust levels:** the backend *project secret API key*
  (`feature_flag:read`) is a Secret Manager secret (never `EXPO_PUBLIC_*`, never logs); the
  mobile *project* key is public by design.
- **Simple boolean flags only on the backend at v1** — multivariate/partial-rollout flags are
  treated as absent, never approximated.
- **No PII, no user content in events** — uid + coarse outcomes only (backend rule 4, mobile
  rule 6); never prompt/reply bodies, emails, or tokens.
- **`local` always no-ops**; the real adapter exists only outside `local` when enabled.
- **Fail-closed activation**: `FLAGS_ENABLED=true` without `POSTHOG_API_KEY` fails startup
  (`FeatureFlagsConfig`), never runs half-configured.
