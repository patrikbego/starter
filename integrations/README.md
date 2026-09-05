# Integrations Index

One runbook per external integration. Each doc is the single home for *how to activate and
configure that integration for a product app*; `guides/` keeps the *when* (derivation order,
buildout history), and the backend/mobile repositories own the implementation docs.

> Status and priorities for every integration (done, missing, cost, Spring coverage) live in
> [`docs/integrations-plan.md`](../docs/integrations-plan.md).
>
> Need the *exact console URLs and identifiers* for the template's own DEV instance (Firebase
> project, Cloud Run URL, PostHog project, Mailtrap inbox, …)? See
> [`LINKS.md](./LINKS.md).

## Product-facing integrations (runtime)

| Integration | Status | Runbook | Implementation doc |
|---|---|---|---|
| Firebase Authentication | ✅ implemented | [FIREBASE_AUTH.md](./FIREBASE_AUTH.md) | [starter-backend/docs/AUTHENTICATION.md](../starter-backend/docs/AUTHENTICATION.md) |
| Social login (Google/Apple) | ✅ web implemented (2026-09-05); native pending | [SOCIAL_LOGIN.md](./SOCIAL_LOGIN.md) | — |
| Firebase App Check | ✅ implemented (web); native wired in code — activation per app | [APP_CHECK.md](./APP_CHECK.md) | [starter-backend/docs/SECURITY.md](../starter-backend/docs/SECURITY.md) |
| Sign-up abuse gate | 🟡 A + C implemented in code (App Check + reCAPTCHA Enterprise gate, both default-off); console + per-app activation pending | [SIGNUP_ABUSE_GATE.md](./SIGNUP_ABUSE_GATE.md) | [starter-backend/docs/SECURITY.md](../starter-backend/docs/SECURITY.md) |
| AI chat via OpenRouter | ✅ implemented | [OPENROUTER_AI.md](./OPENROUTER_AI.md) | [starter-backend/docs/AI_INTEGRATION.md](../starter-backend/docs/AI_INTEGRATION.md) |
| Firestore (database) | ✅ implemented | (provisioned by Terraform — no separate runbook) | [starter-backend/docs/DATABASE.md](../starter-backend/docs/DATABASE.md) |
| Stripe billing | ✅ implemented, opt-in | [STRIPE.md](./STRIPE.md) | [starter-backend/docs/BILLING_EXTENSION.md](../starter-backend/docs/BILLING_EXTENSION.md) |
| Resend transactional email | ✅ implemented, opt-in | [RESEND.md](./RESEND.md) | [starter-backend/docs/EMAIL_EXTENSION.md](../starter-backend/docs/EMAIL_EXTENSION.md) |
| Push notifications (Expo/FCM/APNs) | ✅ implemented, opt-in | [PUSH.md](./PUSH.md) | [starter-backend/docs/PUSH_EXTENSION.md](../starter-backend/docs/PUSH_EXTENSION.md) |
| Sentry error/crash monitoring | ✅ implemented, opt-in (backend + native mobile; web at v1) | [SENTRY.md](./SENTRY.md) | [starter-backend/docs/SENTRY_EXTENSION.md](../starter-backend/docs/SENTRY_EXTENSION.md) · mobile `src/telemetry/sentry.ts` |
| PostHog analytics + feature flags | ✅ implemented, opt-in (backend local evaluation + AI kill-switch; mobile analytics, web included) | [POSTHOG.md](./POSTHOG.md) | [starter-backend/docs/FEATURE_FLAGS.md](../starter-backend/docs/FEATURE_FLAGS.md) · mobile `src/telemetry/posthog.ts` |
| GCS file storage (signed-URL uploads) | 🟡 pattern doc only | (see implementation doc) | [starter-backend/docs/STORAGE_EXTENSION.md](../starter-backend/docs/STORAGE_EXTENSION.md) |
| Media upload (GCS + validation + variants + vision AI) | 🟢 implemented, on by default (opt-out `MEDIA_ENABLED=false`; analysis opt-in) | [MEDIA_UPLOAD.md](./MEDIA_UPLOAD.md) | [starter-backend/docs/MEDIA_UPLOAD_EXTENSION.md](../starter-backend/docs/MEDIA_UPLOAD_EXTENSION.md) |
| Playwright browser E2E vs DEV backend | ✅ P0–P6 implemented | [BROWSER_E2E.md](./BROWSER_E2E.md) | — |
| Firebase Hosting (permanent web URL) | ✅ implemented (DEV) | [FIREBASE_HOSTING.md](./FIREBASE_HOSTING.md) | — |
| Mailtrap email sandbox (auth-email capture for E2E) | ✅ code + runbook ready, relay + API token active on DEV; **spec is manual-run only, excluded from the PR gate** (quota-sensitive) | [MAILTRAP.md](./MAILTRAP.md) | — |
| Search (Typesense; Algolia = swap) | 🟡 pattern doc + runbook, vendor decided; **not implemented** | [SEARCH.md](./SEARCH.md) | [starter-backend/docs/SEARCH_EXTENSION_NOT_IMPLEMENTED.md](../starter-backend/docs/SEARCH_EXTENSION_NOT_IMPLEMENTED.md) |
| Deep links (custom scheme ✅ shipped; universal links = runbook) | 🟡 scheme works; https half per-product domain | [DEEP_LINKS.md](./DEEP_LINKS.md) | — |
| LLM observability (Langfuse) | 🟡 Spring AI ships OTel tracing by default; Langfuse = OTLP config, not wired | [LANGUFUSE.md](./LANGUFUSE.md) | [starter-backend/docs/AI_INTEGRATION.md](../starter-backend/docs/AI_INTEGRATION.md) |

## Operational integrations

| Integration | Status | Runbook |
|---|---|---|
| Slack deploy-failure alerts | ✅ optional, backend | [SLACK_ALERTS.md](./SLACK_ALERTS.md) |
| Background jobs (Cloud Scheduler/Tasks) | 🟡 mechanism shipped (opt-in, off by default); scheduler/tasks activation per app | [BACKGROUND_JOBS.md](./BACKGROUND_JOBS.md) |
| SonarQube local quality gate | ✅ shared server, per-app project keys | [../shared-infra/sonar/README.md](../shared-infra/sonar/README.md) |
| Trivy container scan + CodeQL/SBOM | ✅ in CI, no activation | (part of the workflows; see `.github/workflows/`) |

## Platform delivery (not per-app integrations)

Cloud Run/Terraform/WIF provisioning, GitHub Actions pipelines, Expo EAS delivery, and the
OpenAPI contract are the delivery system, not integrations to activate. They are documented in
`guides/STEP5*` (template bring-up), `guides/STEP6_NEW_APP_FROM_STARTER.md` (per-app
derivation), and [docs/UPSTREAM_SYNC.md](../docs/UPSTREAM_SYNC.md) (propagation).

## Per-app vs shared resources

What can be reused across all your apps vs. what must be created fresh per app is tabulated in
`guides/STEP6_NEW_APP_FROM_STARTER.md` §2. Rule of thumb: accounts and billing are shared;
projects, keys, webhooks, and identifiers are per-app.
