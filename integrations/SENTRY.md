# SENTRY.md — Error / Crash Monitoring (Sentry)

**Status: planned — runbook drafted, implementation pending.** This is the activation guide for
error/crash monitoring across the template: [Sentry](https://sentry.io) captures backend 5xx
failures and mobile native crashes (Free plan ≈ 5k errors/mo, snapshot 2026 — re-verify on the
[pricing page](https://sentry.io/pricing/)). It is the top item in
[`docs/integrations-plan.md`](../docs/integrations-plan.md) ("cheapest win — turns deployed into
*know it broke*").

Two independent **Sentry projects** (one per repo): `<app>-backend` (server errors) and
`<app>-mobile` (app crashes). They share an account/org but never mix.

```text
starter-backend ──5xx failures──▶ Sentry <app>-backend project
starter-mobile  ──native crashes──▶ Sentry <app>-mobile project   (web: excluded at v1, see below)
```

## Decisions (recorded 2026-09-03)

- **Backend uses the Sentry *core* SDK + manual `Sentry.init(dsn)`, not the Spring Boot starter.**
  Reason: the template runs Spring Boot **4.1.1**, and
  [sentry-java's Spring Boot starter support](https://docs.sentry.io/platforms/java/guides/spring-boot/)
  is verified against Boot 2/3 — Boot 4 compat is unconfirmed at this writing. Core SDK is
  version-agnostic, matches the minimal-dependency philosophy of the manual App Check JWT/JWKS
  fix, and cannot surprise us with auto-configuration. The starter stays a documented alternative
  (gains request-tracing/filter-chain instrumentation for free) if a later product upgrades onto
  a verified Boot-4 starter line. Re-check before choosing.
- **Mobile uses `@sentry/react-native` (the current SDK), NOT `sentry-expo`** — the latter is
  [deprecated with a migration path](https://docs.sentry.io/platforms/react-native/guides/expo/migration/sentry-expo/)
  and does not track new Expo SDKs. Install with `npx expo install @sentry/react-native` so the
  config plugin tracks the pinned SDK (~54).
- **Mobile web is excluded at v1.** The React Native SDK does not support react-native-web
  ([upstream issue](https://github.com/getsentry/sentry-react-native/issues/3823)), and the
  template's web target is an Expo static export. Decided: native (iOS/Android) only at v1;
  revisit web with `@sentry/browser` bundling only when a product actually ships web.
- **Capture policy = 5xx-class handlers only** (see [Capture policy](#capture-policy)).
  No logback appender, no breadcrumb-based tracing. Sentry is *errors*, not logs — Micrometer
  already owns outcome-only metrics (`starter.ai.*`).
- **Sentry must never break a request.** Every capture is fire-and-forget inside a try/catch;
  no retries, no blocking, SDK disabled (no-op) when unconfigured.

## Where the implementation will live (planned)

### Backend (`starter-backend`)

| Piece | Path | Mirrors |
|---|---|---|
| Port | `src/main/java/com/starter/ports/ErrorReporter.java` | [`EmailPort.java`](../starter-backend/src/main/java/com/starter/ports/EmailPort.java) |
| Config (fail-closed) | `src/main/java/com/starter/config/SentryConfig.java` | [`EmailConfig.java`](../starter-backend/src/main/java/com/starter/config/EmailConfig.java) |
| Real reporter (`!local` + enabled) | `src/main/java/com/starter/adapters/sentry/SentryErrorReporter.java` | `ResendEmailAdapter` (adapter owns the provider) |
| No-op (default / `local`) | `src/main/java/com/starter/adapters/sentry/NoOpErrorReporter.java` | `MockEmailAdapter` (local profile) |
| Hook points (already funnelled) | [`GlobalExceptionHandler.java`](../starter-backend/src/main/java/com/starter/api/GlobalExceptionHandler.java) — the 5 error-logging handlers | none — the handler is the single funnel |
| Dependency | `io.sentry:sentry` core, pinned property in `pom.xml` | `nimbus-jose-jwt` pattern |
| Default config keys | `src/main/resources/application.yml` → `starter.sentry.*` block | `starter.app-check.*`, `starter.email.*` |
| Terraform secret grant | `infra/main.tf` → `local.secrets` (+ `infra/scripts/set-secrets.sh` flag) | `resend-api-key` entries |
| Deploy steps (opt-in) | `.github/workflows/deploy-dev.yml` + `promote-prod.yml` → "Configure Sentry extension" | App Check / email steps |

Config keys (all `starter.sentry.*`, env-tunable):

```yaml
starter:
  sentry:
    # Optional error reporting extension (Sentry). Disabled by default; enabling
    # without a DSN fails startup on purpose (SentryConfig). The DSN is a
    # backend secret: Secret Manager, never env/yml/logs.
    enabled: ${SENTRY_ENABLED:false}
    dsn: ${SENTRY_DSN:}
```

- **Enabled requires `dsn`** — startup fails otherwise (fail closed, same as billing/email/app-check).
- **`local` profile always serves the no-op** — no network, no DSN needed.
- **Capture points** — the four handlers that already `log.error`:
  `RuntimeException` (500), `AiProviderException` (502), `BillingProviderException` (502),
  `PushProviderException` (502). They keep logging AND call `ErrorReporter.capture` with the
  `correlationId` as context. 4xx stays uncaptured (no 429/400/403 noise by design).
- **DSN in Secret Manager** (`sentry-dsn`), never in `application*.yml` values, logs, or mobile.

```java
// planned shape
public interface ErrorReporter {
    /** Fire-and-forget. Implementations must never throw. */
    void capture(Throwable t, String correlationId);
}
```

### Mobile (`starter-mobile`)

| Piece | Path |
|---|---|
| Dependency + plugin | `npx expo install @sentry/react-native`; plugin entry in `app.config.ts` `plugins` |
| Optional DSN + toggle | `EXPO_PUBLIC_SENTRY_DSN` (public by design — a DSN is not a secret), `EXPO_PUBLIC_SENTRY_ENABLED` (opt-in, off by default) — `src/config/env.ts` + `envValidation.ts` (missing DSN while enabled → build/setup no-ops, never fails) |
| Init | bootstrap in `src/providers/AppProviders.tsx` (or `_layout`): `Sentry.init({ dsn, environment: config.appEnv, release })` when configured; otherwise no-op |
| Native crash handling | on (native-only; see decisions) |

- Release tag = app version from `Constants.expoConfig` (crash → version correlation out of the box).
- The SDK's Expo config plugin wires the native crash handler; verify with `expo doctor` after install
  (Expo ~54 is new — plugin/prebuild compat must be checked at install time).

## Capture policy

| Outcome | Captured? | Why |
|---|---|---|
| 500 `INTERNAL_ERROR` (`RuntimeException` handler) | ✅ | Unexpected = what monitoring is for |
| 502 `AI_PROVIDER_ERROR` / `BILLING_PROVIDER_ERROR` / `PUSH_PROVIDER_ERROR` | ✅ | Provider outages are the "know it broke" signal |
| 4xx (validation, quota, auth, disabled, not-verified) | ❌ | Expected client states; quota noise would flood the free tier |
| Startup failures | ⚠️ manual-init caveat | Only captured if `Sentry.init` ran first (before context). Documented limit at v1 |

**Privacy (backend security rule 4):** `sendDefaultPii=false`, `tracesSampleRate=0` until a
product opts into tracing, and the DSN never travels in `Authorization`/`X-Firebase-AppCheck`
headers or logs. The handler already sends only `{code, message, correlationId}` to the client —
Sentry gets the throwable + correlationId, not request bodies.

## What needs to be done (per environment)

### 1. Sentry console (per product app)

- [ ] Create the Sentry org/account → [sentry.io](https://sentry.io). One account per product,
      reused across its DEV/PROD apps like the Stripe account.
- [ ] Create **two projects**: `<app>-backend` (Platform: Spring Boot / Java) and `<app>-mobile`
      (Platform: React Native). Separate DSNs per project, shared org.
- [ ] (Recommended) Enable alert rules: email/Slack on `issue` created for `error` level. Slack
      alerts can reuse the existing deploy-alert webhook pattern
      ([`SLACK_ALERTS.md`](./SLACK_ALERTS.md)).

### 2. Backend secrets & variables (repo: `<app>-backend`)

- [ ] Store `SENTRY_DSN` (backend project DSN) via Secret Manager:
      ```bash
      cd infra && ./scripts/set-secrets.sh dev --sentry-dsn 'https://<key>@o<org>.ingest.sentry.io/<project>'
      ```
- [ ] Ensure `sentry-dsn` is listed in `infra/main.tf` `local.secrets` and terraform applied
      before the secret version is readable (same rule as `resend-api-key`).
- [ ] Repo variable (non-secret): `SENTRY_ENABLED_DEV=true` / `SENTRY_ENABLED_PROD=true`
      (GitHub Actions → Settings → Secrets and variables → Actions → Variables).
- [ ] Deploy: the "Configure Sentry extension" step in both workflows flips
      `SENTRY_ENABLED=true` and binds the `SENTRY_DSN` secret — **always `--update-secrets` /
      `--update-env-vars`, never `--set-*`** (replaces the whole env; see RESEND.md Rules).

### 3. Mobile variables (repo: `<app>-mobile`)

- [ ] `EXPO_PUBLIC_SENTRY_DSN` (mobile project DSN) + `EXPO_PUBLIC_SENTRY_ENABLED=true` in the
      **dual location** the Firebase trio uses (GitHub repo variables AND EAS environment vars —
      see [`guides/STEP6_NEW_APP_FROM_STARTER.md`](../guides/STEP6_NEW_APP_FROM_STARTER.md) §5).
- [ ] DEV and PROD each get their own DSN? **No — same project DSN, different `environment`
      tag** (`dev`/`prod` from `APP_ENV`). One project per app, environments distinguish
      DEV vs PROD. Cheaper on the free tier than two projects per app.

### 4. Verify (DEV)

- [ ] Backend local gate: `./mvnw verify` stays green — no-op reporter under `local`, nothing
      sent.
- [ ] Backend fail-closed test: `SENTRY_ENABLED=true` without DSN fails startup
      (`SentryConfigTest`), mirroring `EmailConfigTest`.
- [ ] Backend adapter test: `SentryErrorReporterTest` asserts capture is called + never throws
      when the SDK hiccups (mock at the port boundary, no network).
- [ ] Mobile: `npm run lint`, `npx tsc --noEmit`, `npm test` — init no-ops without a DSN.
- [ ] **Live smoke:** force a 500 on DEV (e.g. temporarily disable ai/chat provider), confirm the
      issue appears in the Sentry `<app>-backend` project within ~1 min. Then a native crash
      (or `Sentry.captureMessage` smoke) lands in `<app>-mobile`.
- [ ] No OpenAPI change — error monitoring adds zero contract surface; `npm run validate:contract`
      must still pass untouched.

### 5. PROD (only after DEV is proven)

- [ ] `SENTRY_ENABLED_PROD=true` repo variable; the PROD deploy step binds the same DSN with
      `environment: prod`.
- [ ] CD/rollback rules unchanged
      ([`starter-backend/docs/cicd_deployment_plan.md`](../starter-backend/docs/cicd_deployment_plan.md)).

## Follow-ups (not v1 core)

- **Source maps / release tagging for mobile** — needs `SENTRY_AUTH_TOKEN` (a real secret: EAS
  environment secret + `@sentry/cli` upload step in `build-preview.yml` / `build-release.yml`)
  so stack traces de-minify. Skipped at v1 for the same reason EAS Update was: keep the release
  path minimal until a product ships.
- **Web monitoring** (`@sentry/browser` on the Expo web export) — only when a product ships web.
- **Backend Spring Boot starter** — revisit when sentry-java documents Boot 4 support.

## Alternatives considered

| Option | Verdict |
|---|---|
| Self-hosted Sentry / [GlitchTip](https://glitchtip.com) | The port boundary makes swapping trivial (same SDK, different DSN host). Not chosen because the free SaaS tier covers template scale with zero ops |
| Other commercial monitors | Same port applies; no template decision needed |
| Logs-only (no error tracker) | Rejected — logs are passive; Sentry is the "know it broke" alert |

## Rules that must not be broken

- **Sentry never fails or slows a request** — capture is fire-and-forget, wrapped, no retries.
- **Fail-closed activation**: `SENTRY_ENABLED=true` without `SENTRY_DSN` fails startup
  (`SentryConfig`), never runs half-configured.
- **Backend DSN is a secret** → Secret Manager only. **Mobile DSN is public by design** (it is
  not a secret; it lives in `EXPO_PUBLIC_*` in the bundle) — never put a *backend* DSN in a
  mobile bundle.
- **No PII breadcrumbs**: `sendDefaultPii=false`; never send request bodies, `Authorization`,
  `X-Firebase-AppCheck`, user emails, prompt/reply content (backend security rule 4).
- **Capture policy is fixed**: the four 5xx handlers only. No logback appender, no 4xx noise.
- **`local` always no-ops**; the real reporter exists only outside `local` when enabled.
- **Deploy steps use `--update-*` exclusively** (same incident history as billing/email).

## Maintenance point

The provider dependency is the only coupling: `io.sentry:sentry` (backend) and
`@sentry/react-native` (mobile). If Sentry's SDK surface changes, `SentryErrorReporter` is the
single backend edit. Re-verify Spring Boot-4 starter support and Expo-SDK plugin compat each
template baseline bump — that is where this integration ages fastest.