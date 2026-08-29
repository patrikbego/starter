# Integrations Index

One runbook per external integration. Each doc is the single home for *how to activate and
configure that integration for a product app*; `guides/` keeps the *when* (derivation order,
buildout history), and the backend/mobile repositories own the implementation docs.

## Product-facing integrations (runtime)

| Integration | Status | Runbook | Implementation doc |
|---|---|---|---|
| Firebase Authentication | ✅ implemented | [FIREBASE_AUTH.md](./FIREBASE_AUTH.md) | [starter-backend/docs/AUTHENTICATION.md](../starter-backend/docs/AUTHENTICATION.md) |
| AI chat via OpenRouter | ✅ implemented | [OPENROUTER_AI.md](./OPENROUTER_AI.md) | [starter-backend/docs/AI_INTEGRATION.md](../starter-backend/docs/AI_INTEGRATION.md) |
| Firestore (database) | ✅ implemented | (provisioned by Terraform — no separate runbook) | [starter-backend/docs/DATABASE.md](../starter-backend/docs/DATABASE.md) |
| Stripe billing | ✅ implemented, opt-in | [STRIPE.md](./STRIPE.md) | [starter-backend/docs/BILLING_EXTENSION.md](../starter-backend/docs/BILLING_EXTENSION.md) |
| Resend transactional email | ✅ implemented, opt-in | [RESEND.md](./RESEND.md) | [starter-backend/docs/EMAIL_EXTENSION.md](../starter-backend/docs/EMAIL_EXTENSION.md) |
| Push notifications (Expo/FCM/APNs) | ✅ implemented, opt-in | [PUSH.md](./PUSH.md) | [starter-backend/docs/PUSH_EXTENSION.md](../starter-backend/docs/PUSH_EXTENSION.md) |
| GCS file storage (signed-URL uploads) | 🟡 pattern doc only | (see implementation doc) | [starter-backend/docs/STORAGE_EXTENSION.md](../starter-backend/docs/STORAGE_EXTENSION.md) |
| Playwright browser E2E vs DEV backend | ✅ P0–P4 implemented | [BROWSER_E2E.md](./BROWSER_E2E.md) | — |

## Operational integrations

| Integration | Status | Runbook |
|---|---|---|
| Slack deploy-failure alerts | ✅ optional, backend | [SLACK_ALERTS.md](./SLACK_ALERTS.md) |
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
