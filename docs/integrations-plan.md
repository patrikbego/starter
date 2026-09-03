# Integration Plan

Single status + plan for external integrations across the template (backend + mobile).
Complements [`../integrations/README.md`](../integrations/README.md) (runbook index) and
[`IMPLEMENTATION_ROADMAP.md`](./IMPLEMENTATION_ROADMAP.md) (delivery). Statuses follow the
backend doc labels: **Prototype** (code exists) · **Target v1** (required before this is a
production template) · **Extension** (product-specific, excluded from starter core).

Status legend: ✅ implemented · 🟡 partial (pattern doc only / pending live setup) ·
❌ missing · ⛔ open (documented, blocked or deferred).

## 1. Done — implemented integrations

| Integration | Status | Where / notes |
|---|---|---|
| Firebase Authentication | ✅ | Email/Password; mobile signs in with Firebase Web SDK, backend verifies each ID token with Admin SDK; local mock without network |
| Firebase App Check | ✅ web / 🟡 native | Backend verifies `X-Firebase-AppCheck` (manual RS256 JWT/JWKS — the Java Admin SDK has no verifyToken API); filter requires a valid token on `/api/v1/ai/chat` when enabled (default off, PROD-on); web provider ships, native (Play Integrity/App Attest) stubbed. Local mock via `MockAppCheckVerifier` |
| Firestore (database) | ✅ | Repository-port boundary; provisioned by Terraform; `local` uses in-memory repo |
| AI chat (OpenRouter) | ✅ | Spring AI behind `AiChatPort`; fail-closed; per-user quota + rate limit (429/Retry-After), input cap, provider timeout, outcome-only metrics |
| Stripe billing | ✅ opt-in | `BillingPort`/`StripeBillingAdapter`; checkout + portal + webhook (only Firebase-exempt route); `stripe-java` already in pom |
| Resend transactional email | ✅ opt-in | `EmailPort`/`ResendEmailAdapter` (RestClient, no SDK); fail-closed `EmailConfig`; `MockEmailAdapter` in `local`; welcome email on signup |
| Push notifications | ✅ opt-in | `PushPort`/`PushTokenPort` + Expo adapter (FCM/APNs); `MockPushAdapter` + in-memory token repo in `local` |
| Error/crash monitoring (Sentry) | ✅ opt-in | backend `ErrorReporter` port + `SentryErrorReporter` (core SDK, manual init — Boot-4 starter unverified) / `NoOpErrorReporter` (local), 5xx handlers capture; mobile `@sentry/react-native` native-only (web excluded at v1) |
| Background jobs (Cloud Scheduler/Tasks) | ✅ opt-in (mechanism) | `starter.jobs` off by default — no scheduler when disabled; `local` demo job proves the mechanism offline. Durable path = Cloud Scheduler/Tasks → authenticated HTTP job endpoint per product ([`BACKGROUND_JOBS.md`](../integrations/BACKGROUND_JOBS.md)); in-process `@Scheduled` is not durable on scale-to-zero Cloud Run |
| Playwright browser E2E | ✅ | vs DEV backend |
| Slack deploy alerts | ✅ optional | DEV + PROD notify jobs when `SLACK_WEBHOOK` is set |
| SonarQube local quality gate | ✅ | shared server, per-app project keys |
| Trivy + CodeQL + SBOM | ✅ | in CI, no activation |

## 2. To-do — missing integrations (ranked)

| Integration | Status / blockers | Priority | Effort | Free at template scale? | Spring can do it? | Notes |
|---|---|---|---|---|---|---|
| Sign-up abuse gate (reCAPTCHA Enterprise / blocking functions) | ⛔ blocked on deployment | High | ~1 day | ✅ 10k free assessments/mo | 🟡 verification is a Google API call via `RestClient` | Cheap uid minting lets attackers rotate past AI quotas |
| Analytics + feature flags (PostHog or Firebase Remote Config) | ❌ | Medium | ~1 day | ✅ ~1M events/mo free | 🟡 `RestClient` adapter like Resend | Gives an AI kill-switch |
| Firebase Hosting web + security headers | ⛔ blocked on deployment | Medium | ~half day | ✅ free tier (10 GB) | ❌ CDN layer, not Spring | Expo web delivery + CORS/headers |
| GCS file storage (signed-URL uploads) | 🟡 `ObjectStoragePort` pattern doc only, no code | Medium | 1–2 days | ✅ GCS free tier (5 GB) | ✅ `spring-cloud-gcp-storage` | Extension by design ([`STORAGE_EXTENSION.md`](../starter-backend/docs/STORAGE_EXTENSION.md)) |
| Social login (Google/Apple) | ❌ Email/Password only today | Low (Apple → Medium if Google is added) | ~1 day | ✅ Firebase providers are free | 🟡 mostly client + console; backend unchanged (claims ride the token) | Sign in with Apple is **mandatory on iOS** if any other social login exists |
| Search (Typesense / Algolia) | ❌ | Low (product) | 1–3 days | ✅ Typesense self-host free; Algolia paid beyond quota | 🟡 own SDKs; Spring Data Elasticsearch is the only Spring wrapper | Add when a product needs it |
| SMS OTP (Twilio) | ❌ | Low (product) | ~1 day | ❌ per-SMS + number fee from day one | 🟡 `RestClient` call | Only per-use cost in the list besides payment fees |
| Deep links / universal links | ❌ | Low (mobile) | ~1 day | ✅ free (manifest/apple-app-site-association files) | ❌ mobile-only | Standard for product apps |
| LLM observability (Langfuse) | ❌ | Low | ~half day | ✅ OSS self-host free / cloud hobby | 🟡 HTTP via `RestClient` | Fits the AI port; not needed at template scale |
| Apple IAP / RevenueCat | ❌ product decision | Low (decision) | 3–5 days | 🟡 RevenueCat free ≤ $2.5k MTR/mo then 1%; **Apple takes 15–30%** | ❌ mobile SDK | Only relevant for digital goods in the iOS app; separate from Stripe (web payments) |

## 3. Cost summary — what is actually payable

- **Free at template scale:** every row above except the three below. Free-tier numbers are a
  snapshot (2026); re-verify before relying on them.
- **Genuinely payable:** Stripe ~2.9% + $0.30 per transaction · Apple/Google IAP 15–30% of
  digital-goods revenue · Twilio per-SMS from the first message.
- **GCP usage** beyond free allowances is already the template's baseline cost; new
  integrations add marginal usage, not new bills.

## 4. Spring coverage rule

Spring covers the **mechanism**, never the **vendor**. Already in pom: Spring AI, Spring
Security, Actuator, spring-cloud-gcp Firestore. Available when needed: `spring-boot-starter-mail`,
`@Scheduled`/Quartz, WebSocket/STOMP, Spring Data Elasticsearch, Resilience4j/Bucket4j,
OAuth2 client, Togglz/FF4J (flags), spring-cloud-gcp Storage/Tasks, `sentry-spring-boot`.
Every missing integration fits the existing **port + adapter** boundary: `XxxPort` + adapter
(`RestClient` or vendor SDK) + fail-closed config + `local` mock — same shape as
`EmailPort`/`ResendEmailAdapter` and `BillingPort`/`StripeBillingAdapter`. Nothing here bends
the architecture.

## 5. Decisions recorded

- **Resend is the real sender and has no test mode** — every send is a real email; one API key
  per environment; DEV recipients limited to mailboxes you own ([`RESEND.md`](../integrations/RESEND.md)).
- **Email confirmation is Firebase-native** (`sendEmailVerification`), not Resend. Resend covers
  product email only — welcome, notifications, receipts.
- **Mailtrap: not needed.** SMTP connectivity was verified (sandbox credentials work), but the
  Resend adapter is HTTP JSON, not SMTP, and `local` already serves `MockEmailAdapter`. Mailtrap
  only becomes relevant if `spring-boot-starter-mail` is adopted later.
- **GCS storage is an extension**, not starter core ([`STORAGE_EXTENSION.md`](../starter-backend/docs/STORAGE_EXTENSION.md)).
- **App Check** is implemented for v1 (web provider shipped, native stubbed). The backend verifies
  tokens with a manual RS256 JWT/JWKS check — the Java Admin SDK has no `AppCheck.verifyToken`, so
  the earlier "firebase-admin v12 bump" path was a Node-SDK error. Enforcement is off by default
  and PROD-on once a live smoke passes. Runbook: [`../integrations/APP_CHECK.md`](../integrations/APP_CHECK.md).
- **EAS Update deliberately not retained** for mobile v1 (fingerprint runtime version instead).
- **Sentry uses the core SDK + manual `Sentry.init`, not the Spring Boot starter** — the template
  runs Spring Boot 4.1.1 and the starter's Boot-4 support is unverified; core SDK is
  version-agnostic and matches the manual-JWT/JWKS precedent. Mobile uses `@sentry/react-native`
  (not deprecated `sentry-expo`); web is excluded at v1 (the RN SDK has no react-native-web
  support). Activation is off by default and needs a console/DSN per app.
  Runbook: [`../integrations/SENTRY.md`](../integrations/SENTRY.md).
- **Background jobs ship as a mechanism, not product jobs** — `starter.jobs` is off by default
  (no scheduler when disabled); `local` has a demo job only. In-process `@Scheduled` is not
  durable on scale-to-zero Cloud Run, so the durable path is Cloud Scheduler/Tasks →
  authenticated HTTP job endpoint per product. No Quartz at v1.
  Runbook: [`../integrations/BACKGROUND_JOBS.md`](../integrations/BACKGROUND_JOBS.md).

## 6. Suggested order of work

1. **Firebase App Check** — ✅ done (security; backend JWT/JWKS + web provider, PROD-on)
2. **Sentry** — ✅ done (observability; backend 5xx handler capture + mobile native, opt-in, web at v1)
3. **Background jobs** — ✅ done (mechanism opt-in; Cloud Scheduler/Tasks activation = per app)
4. **Analytics + feature flags** (~1 day) — AI kill-switch
5. Everything else when a product fork needs it (extension rule)

## 7. Delivery tail (not integrations, but blocks v1)

Live infra apply · `production` environment + approval · store-signed builds and store tracks ·
rollback drills · `v1.0.0` tag + clean new-app trial. Statuses `[~]`/`[ ]` in
[`IMPLEMENTATION_ROADMAP.md`](./IMPLEMENTATION_ROADMAP.md) Phases 5–6.