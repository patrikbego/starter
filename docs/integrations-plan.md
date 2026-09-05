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
| Firebase App Check | ✅ web / 🟡 native wired in code (opt-in; console registration + EAS live smoke pending) | Backend verifies `X-Firebase-AppCheck` (manual RS256 JWT/JWKS — the Java Admin SDK has no verifyToken API); filter requires a valid token on `/api/v1/ai/chat` when enabled (default off, per-env repo-var `APP_CHECK_ENABLED_*` gate); web provider ships; native = `@react-native-firebase/app-check` 23.8.8 + JS bridge for the JS-SDK auth path. Local mock via `MockAppCheckVerifier` |
| Firestore (database) | ✅ | Repository-port boundary; provisioned by Terraform; `local` uses in-memory repo |
| AI chat (OpenRouter) | ✅ | Spring AI behind `AiChatPort`; fail-closed; per-user quota + rate limit (429/Retry-After), input cap, provider timeout, outcome-only metrics |
| Stripe billing | ✅ opt-in | `BillingPort`/`StripeBillingAdapter`; checkout + portal + signature-authenticated webhook (the only bearer-exempt `permitAll` besides pre-auth sign-up); `stripe-java` already in pom |
| Resend transactional email | ✅ opt-in | `EmailPort`/`ResendEmailAdapter` (RestClient, no SDK); fail-closed `EmailConfig`; `MockEmailAdapter` in `local`; welcome email on signup |
| Push notifications | ✅ opt-in | `PushPort`/`PushTokenPort` + Expo adapter (FCM/APNs); `MockPushAdapter` + in-memory token repo in `local` |
| Error/crash monitoring (Sentry) | ✅ opt-in | backend `ErrorReporter` port + `SentryErrorReporter` (core SDK, manual init — Boot-4 starter unverified) / `NoOpErrorReporter` (local), 5xx handlers capture; mobile `@sentry/react-native` native-only (web excluded at v1) |
| Analytics + feature flags (PostHog) | ✅ opt-in | backend `FeatureFlagPort` + `PostHogFlagAdapter` (RestClient local evaluation, no SDK) + AI kill-switch flag `ai-chat-enabled` (non-breaking: static config wins, flag only kills); mobile `AnalyticsPort` + PostHog adapter, web included, no fingerprint change. Runbook: [`POSTHOG.md`](../integrations/POSTHOG.md); backend doc: [`FEATURE_FLAGS.md`](../starter-backend/docs/FEATURE_FLAGS.md) |
| Background jobs (Cloud Scheduler/Tasks) | ✅ opt-in (mechanism) | `starter.jobs` off by default — no scheduler when disabled; `local` demo job proves the mechanism offline. Durable path = Cloud Scheduler/Tasks → authenticated HTTP job endpoint per product ([`BACKGROUND_JOBS.md`](../integrations/BACKGROUND_JOBS.md)); in-process `@Scheduled` is not durable on scale-to-zero Cloud Run |
| Playwright browser E2E | ✅ | vs DEV backend |
| Slack deploy alerts | ✅ optional | DEV + PROD notify jobs when `SLACK_WEBHOOK` is set |
| SonarQube local quality gate | ✅ | shared server, per-app project keys |
| Trivy + CodeQL + SBOM | ✅ | in CI, no activation |

## 2. Integration register (implemented ✅; remaining 🟡/⛔)

| Integration | Status / blockers | Priority | Effort | Free at template scale? | Spring can do it? | Notes |
|---|---|---|---|---|---|---|
| Sign-up abuse gate (reCAPTCHA Enterprise / App Check enforcement / blocking functions) | 🟡 phased A→C→B — A: native App Check wired, console+smoke pending; **C: backend + web client implemented** (route + gate, default-off; native deliberately stays client-direct); B: evidence-triggered ([runbook](../integrations/SIGNUP_ABUSE_GATE.md)) | High | A: console-only · C: ✅ done · B: 2–3 days | 🟡 10k assessments/mo free but **org-wide, shared with App Check minting**; Essentials hard-stops with 429 | ✅ backend done (`RecaptchaAssessorPort` + `RestClient` adapter) | Blocking functions cannot receive a reCAPTCHA token (no client channel; `recaptchaScore` is SMS-only) — C is Spring-mediated sign-up; native stays on A (no assessment SDK) |
| Analytics + feature flags — **PostHog chosen** (Firebase Remote Config = documented flags-only fallback) | ✅ implemented, opt-in (2026-09-04; runbook [`POSTHOG.md`](../integrations/POSTHOG.md)) | Medium | ~1 day | ✅ 1M events + 1M flag requests/mo free, hard stop at limit | ✅ `RestClient` adapter like Resend | One tool covers analytics + flags + the AI kill-switch (decision in §5); deploy-workflow activation steps wired in both deploy workflows (`deploy-dev.yml` / `promote-prod.yml`); per-project flag + drill pending |
| Firebase Hosting web + security headers | ✅ implemented | DEV site `https://starter-demo-dev.web.app` (Expo export, CORS origin in DEV backend); security headers in `firebase.json` `headers` (CSP `script-src 'self'`, HSTS via Hosting, nosniff, `X-Frame-Options`, Referrer-Policy, Permissions-Policy, immutable asset caching) — runbook [`FIREBASE_HOSTING.md`](../integrations/FIREBASE_HOSTING.md); PROD site pending Phase 5 |
| GCS file storage (signed URL) | 🟡 **half shipped in code** — signed-URL *download* proven by the media adapter (`GcsMediaObjectStorageAdapter.signedGetUrl`, 302s); the standalone signed-PUT *upload* pattern stays doc-only ([`STORAGE_EXTENSION.md`](../starter-backend/docs/STORAGE_EXTENSION.md)) | Medium | 1–2 days (for the signed-PUT half) | ✅ GCS free tier (5 GB) | ✅ `spring-cloud-gcp-storage` | Extension by design; see [`MEDIA_UPLOAD_EXTENSION.md`](../starter-backend/docs/MEDIA_UPLOAD_EXTENSION.md) (implemented API-proxied upload + signed download) |
| Media upload (GCS storage + validation + variants + vision AI) | ✅ implemented, opt-in (backend: `MediaService` + GCS/Firestore/OpenRouter adapters, local in-memory mocks; spec contract v1) | Medium | ✅ done | ✅ GCS free tier (5 GB) | ✅ `spring-cloud-gcp-storage`, ImageIO/webp | Combines the storage pattern with the secure-upload reference (magic-byte validation, variant pipeline, opt-in OpenRouter vision delivered over `PushPort`); API-proxied upload by default, signed-URL download; gated `503` when disabled |
| Social login (Google/Apple) | ✅ web implemented (2026-09-05) — Google + Apple popup sign-in shipped; native pending | Medium (Apple if Google added) | code ~1 day, console fiddly | ✅ Firebase providers are free | 🟡 mostly client + console; backend unchanged (claims ride the token) | **Sign in with Apple is mandatory on iOS if any other social login exists** (Guideline 4.8) — web-only Google is fine today ([`SOCIAL_LOGIN.md`](../integrations/SOCIAL_LOGIN.md)) |
| Search | 🟡 pattern doc + runbook, **no code — not implemented** ([`SEARCH.md`](../integrations/SEARCH.md), [`SEARCH_EXTENSION_NOT_IMPLEMENTED.md`](../starter-backend/docs/SEARCH_EXTENSION_NOT_IMPLEMENTED.md)); vendor decided: **Typesense** (Algolia = adapter swap) | Low (product) | 1–3 days | 🟡 **engine free (Apache-2.0) but hosting is not** — self-host VM ≈$7–16/mo, Typesense Cloud from ≈$26/mo (free dev cluster is dev-only, sleeps); Algolia free tier real but caps at 10k records/10k searches/100k ops per mo | ✅ official Java clients (or `RestClient` — house precedent); Spring Data Elasticsearch exists but ES is stateful + ops-heavy — rejected | Add when a product needs it; **stateful** — no scale-to-zero, so an always-on VM or managed Cloud is required, and that is a fixed cost from day one |
| SMS OTP / phone sign-in | 🟡 **no vendor needed** — Firebase phone auth is the built-in path (web: JS SDK + reCAPTCHA verifier works today; native: needs the avoided `@react-native-firebase` stack → pending, same posture as social login/App Check); Twilio only for **arbitrary** SMS (transactional, not OTP) | Low (product) | ~1 day | 🟡 per-SMS via Firebase: $0.01 US/CA … $0.46 worst region, **first 10 SMS/day free**, Blaze required; Twilio Verify $0.05/verification; raw Twilio SMS needs 10DLC + ≈$1/mo number | ✅ none — Firebase handles SMS; backend unchanged (phone users arrive as normal Firebase ID tokens) | Pick when a product wants phone login; OTP-abuse already covered by the sign-up gate (`beforeSmsSent` reCAPTCHA) ([`SIGNUP_ABUSE_GATE.md`](../integrations/SIGNUP_ABUSE_GATE.md)) |
| Deep links / universal links | ✅ **custom-scheme deep links shipped** (per-variant `scheme` + Expo Router auto-routes); 🟡 universal links (https) = runbook only, no config — needs per-env domain + 2 `.well-known` files ([`DEEP_LINKS.md`](../integrations/DEEP_LINKS.md)) | Low (mobile) | ~1 day | ✅ free (static files; DEV Hosting already exists) | ❌ mobile-only (Expo Router + `expo-linking`; no backend surface) | Standard for product apps; custom scheme works today, https half needs a real domain per product |
| LLM observability (Langfuse) | 🟡 **Spring AI already ships built-in OTel tracing** (metrics + spans; prompt/completion content OFF by default — matches the outcome-only rule); Langfuse = OTLP export config (bridge + exporter deps), **no SDK**, no port ([`LANGUFUSE.md`](../integrations/LANGUFUSE.md)); not wired — opt-in per product | Low | ~half day (config only) | ✅ OSS self-host free / cloud free hobby tier (observation cap — re-verify) | ✅ native — Spring AI observability + Micrometer→OTel bridge + OTLP exporter; no `RestClient` shim needed | Not needed at template scale; content capture is a product/privacy decision (see §5) |
| Apple IAP / RevenueCat | ❌ product decision | Low (decision) | 3–5 days | 🟡 RevenueCat free ≤ $2.5k MTR/mo then 1% (confirmed 2026); **Apple takes 15–30%** whatever the tool | ❌ mobile SDK — and a **native RN module** (collides with the avoid-native-SDK default; App Check is the only opted-in exception) | Only for digital goods inside the iOS app; Stripe already covers web; native posture makes this doubly "add when a product needs it" (see §5) |

## 3. Cost summary — what is actually payable

- **Free at template scale:** every row above except the four below. Free-tier numbers are a
  snapshot (2026); re-verify before relying on them.
- **Genuinely payable:** Stripe ~2.9% + $0.30 per transaction · Apple/Google IAP 15–30% of
  digital-goods revenue (unavoidable for in-app purchases) + RevenueCat 1% of MTR past
  $2.5k/mo when adopted · SMS OTP via Firebase phone auth, per SMS sent ($0.01 US/CA … $0.46
  worst region; first 10/day free; Blaze required — no number fee; the old plan's "Twilio
  number fee" applied only to raw Twilio SMS 10DLC).
- **Payable but fixed, not per-use:** Search — the engine is free (Apache-2.0) but stateful:
  no scale-to-zero. Production needs an always-on VM (≈$7–16/mo) or Typesense Cloud (from
  ≈$26/mo); Algolia's free tier is the only true-$0 option and caps at 10k searches/mo.
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
- **Media upload is a combined extension on top of GCS storage** — implemented in the backend (see [`MEDIA_UPLOAD_EXTENSION.md`](../starter-backend/docs/MEDIA_UPLOAD_EXTENSION.md), runbook [`MEDIA_UPLOAD.md`](../integrations/MEDIA_UPLOAD.md)). API-proxied upload by default (validation + variant re-encoding need server byte access), signed-URL download (API never proxies reads in cloud). Vision analysis opt-in, delivered over the existing `PushPort` (no WebSocket).
- **App Check** is implemented for v1 (web provider shipped, native stubbed). The backend verifies
  tokens with a manual RS256 JWT/JWKS check — the Java Admin SDK has no `AppCheck.verifyToken`, so
  the earlier "firebase-admin v12 bump" path was a Node-SDK error. Enforcement is off by default
  and toggled per environment via repo variables (`APP_CHECK_ENABLED_DEV`/`…_PROD`); PROD-on
  once a live smoke passes. Runbook: [`../integrations/APP_CHECK.md`](../integrations/APP_CHECK.md).
- **EAS Update deliberately not retained** for mobile v1 (fingerprint runtime version instead).
- **Sentry uses the core SDK + manual `Sentry.init`, not the Spring Boot starter** — the template
  runs Spring Boot 4.1.1 and the starter's Boot-4 support is unverified; core SDK is
  version-agnostic and matches the manual-JWT/JWKS precedent. Mobile uses `@sentry/react-native`
  (not deprecated `sentry-expo`); web is excluded at v1 (the RN SDK has no react-native-web
  support). Activation is off by default and needs a console/DSN per app.
  Runbook: [`../integrations/SENTRY.md`](../integrations/SENTRY.md).
- **PostHog is the analytics + feature flags vendor**; Firebase Remote Config stays the
  documented flags-only fallback. Firebase GA4 cannot deliver the analytics half in this
  template — its JS SDK is browser-only, and native would require `@react-native-firebase`,
  a native-module stack the template deliberately avoided (web is first-class; see the App
  Check web-provider precedent). PostHog covers analytics + flags from one SDK on web and
  native (`posthog-react-native`, official Expo guide), with server-side local evaluation
  for backend flags. v1 shape: vendor-neutral `FeatureFlagPort` (backend) + `AnalyticsPort`
  (mobile), PostHog adapters, simple boolean flags only (no Java re-implementation of hashed
  rollout evaluation). AI kill-switch is **non-breaking**: static `starter.ai.enabled` stays
  as the fallback; the remote flag `ai-chat-enabled` only ever kills — provider down ⇒
  static config decides. Free tier: 1M analytics events + 1M flag requests/mo, usage
  hard-stops at the limit (snapshot 2026; re-verify). Caveats: verify
  `posthog-react-native` on react-native-web during implementation (gate web off
  Sentry-style if it misbehaves); keys are per-app/per-env client values, the backend
  `local_evaluation` personal key is a Secret Manager secret.
- **Typesense is the search vendor; Algolia is an adapter swap.** Backend-proxied search only
  (engine key in Secret Manager, owner ANDed at the adapter — never client-direct keys).
  The engine is free (Apache-2.0) but **stateful**: Cloud Run doesn't fit, so hosting means a
  GCE VM or Typesense Cloud at a fixed monthly cost (free dev cluster is dev-only/sleeps).
  Elasticsearch rejected (ops-heavy). **Not implemented and deferred until an app has live
  traffic** — the only integration with no production free tier, so a fixed hosting bill is
  unjustified without real users
  ([`SEARCH.md`](../integrations/SEARCH.md), [`SEARCH_EXTENSION_NOT_IMPLEMENTED.md`](../starter-backend/docs/SEARCH_EXTENSION_NOT_IMPLEMENTED.md)).
- **SMS OTP needs no Twilio.** Firebase phone sign-in is the built-in path — web (JS SDK +
  reCAPTCHA verifier) works today; native is pending on the deliberately avoided
  `@react-native-firebase` stack (same posture as native social login / native App Check).
  Backend unchanged: phone users arrive as regular Firebase ID tokens, so no new port,
  no custom-token minting, no second identity path. Cost is per-SMS through Firebase
  ($0.01 US/CA … $0.46 worst region, first 10 SMS/day free, Blaze required — re-verify;
  the plan's old "number fee from day one" applied only to raw Twilio SMS 10DLC).
  OTP-abuse controls already exist: App Check + `beforeSmsSent` reCAPTCHA scoring
  ([`SIGNUP_ABUSE_GATE.md`](../integrations/SIGNUP_ABUSE_GATE.md)). Twilio earns a slot only
  for **transactional SMS** (alerts/notifications — the SMS twin of Resend), which is a
  different integration entirely.
- **Deep links: scheme half ships, universal links wait for a domain.** Expo Router
  auto-links every route to the per-variant scheme (`startermobile-dev/-preview/-mobile`),
  so custom-scheme deep links already work with zero backend surface. The https half (iOS
  Universal Links / Android App Links) needs a per-environment domain + hosted
  `apple-app-site-association` / `assetlinks.json` — static files, free, and a native
  fingerprint change (new EAS build per variant). Deferred because the domain is
  product-specific; DEV can reuse Firebase Hosting (`starter-demo-dev.web.app`).
  Runbook: [`DEEP_LINKS.md`](../integrations/DEEP_LINKS.md).
- **LLM observability: Spring AI has it by default; Langfuse is a config, not an
  integration.** The user's instinct checked out: `spring-ai-starter-model-openai` (v2.0.1
  here) observes every model call via Micrometer → OTel spans; prompt/completion content is
  **off by default for privacy** — exactly the template's outcome-only posture. Langfuse =
  two deps (`micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`) + an OTLP
  endpoint pointing at Langfuse's `/api/public/otel` ingestion, no Langfuse SDK, no new
  port. Enabling `log-prompt`/`log-completion` sends user content off-server and overrides
  backend rule 4 — a product/privacy decision, never the starter default
  ([`LANGUFUSE.md`](../integrations/LANGUFUSE.md)).
- **GCS signed-URL storage: the download half is already in code.** The media extension's
  adapter (`GcsMediaObjectStorageAdapter.signedGetUrl`) implements short-TTL signed-URL
  downloads (302s) today; what remains doc-only is the standalone **client-direct signed-PUT
  upload** pattern (no confirm route, `upload-mode=signed` config-validated but unwired —
  see [`MEDIA_UPLOAD_EXTENSION.md`](../starter-backend/docs/MEDIA_UPLOAD_EXTENSION.md)).
  A product needing large files the API should never touch implements that one path from
  [`STORAGE_EXTENSION.md`](../starter-backend/docs/STORAGE_EXTENSION.md).
- **Apple IAP is a native-SDK exception, not a template feature.** RevenueCat is an RN native
  module — like `@react-native-firebase`, it collides with the avoid-native-SDK default (App
  Check is the one opted-in exception). Only products selling digital goods *inside* the iOS
  app need it; Stripe already covers web, and no IAP tool reduces Apple's 15–30%. Free to
  $2.5k MTR/mo then 1% (2026 numbers). Decide per product; the starter ships nothing.
- **Background jobs ship as a mechanism, not product jobs** — `starter.jobs` is off by default
  (no scheduler when disabled); `local` has a demo job only. In-process `@Scheduled` is not
  durable on scale-to-zero Cloud Run, so the durable path is Cloud Scheduler/Tasks →
  authenticated HTTP job endpoint per product. No Quartz at v1.
  Runbook: [`../integrations/BACKGROUND_JOBS.md`](../integrations/BACKGROUND_JOBS.md).

## 6. Suggested order of work

1. **Firebase App Check** — ✅ done (security; backend JWT/JWKS + web provider, per-env `APP_CHECK_ENABLED_*` gate)
2. **Sentry** — ✅ done (observability; backend 5xx handler capture + mobile native, opt-in, web at v1)
3. **Background jobs** — ✅ done (mechanism opt-in; Cloud Scheduler/Tasks activation = per app)
4. **Analytics + feature flags** — ✅ done (PostHog, §5): backend `FeatureFlagPort` + AI kill-switch, mobile `AnalyticsPort` + base events; runbook [`POSTHOG.md`](../integrations/POSTHOG.md)
5. Everything else when a product fork needs it (extension rule)

## 7. Delivery tail (not integrations, but blocks v1)

Live infra apply · `production` environment + approval · store-signed builds and store tracks ·
rollback drills · `v1.0.0` tag + clean new-app trial. Statuses `[~]`/`[ ]` in
[`IMPLEMENTATION_ROADMAP.md`](./IMPLEMENTATION_ROADMAP.md) Phases 5–6.