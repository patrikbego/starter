# STRIPE.md — Stripe Subscription Billing Integration

**Status: implemented in the templates (2026-08-24), opt-in at runtime. Template's own
integration fully wired + E2E-verified in DEV and PROD — Stripe TEST mode (2026-08-29);
live mode pending.** Ticks below record the template's own integration; start a new app
with blank boxes. This runbook is the
activation checklist for **one Stripe subscription price per environment** on the Spring Boot
backend (`starter-backend`), consumed by the Expo client (`starter-mobile`) through the pinned
OpenAPI contract. Follow it top-to-bottom per environment; DEV in test mode first, PROD second.

Other backend integrations land as sibling docs in this folder (same shape: what exists, what to
wire, exact paths).

---

## Where the implementation lives

| Piece | Path |
|---|---|
| Extension design & security model | [`starter-backend/docs/BILLING_EXTENSION.md`](../starter-backend/docs/BILLING_EXTENSION.md) |
| API contract (v1) | [`starter-backend/openapi/openapi.yaml`](../starter-backend/openapi/openapi.yaml) — `paths: /api/v1/billing/*` |
| Config binding (fail-closed) | [`starter-backend/src/main/java/com/starter/config/BillingConfig.java`](../starter-backend/src/main/java/com/starter/config/BillingConfig.java) |
| Use cases / policy | [`starter-backend/src/main/java/com/starter/application/BillingService.java`](../starter-backend/src/main/java/com/starter/application/BillingService.java) |
| Stripe adapter (SDK lives only here) | [`starter-backend/src/main/java/com/starter/adapters/stripe/StripeBillingAdapter.java`](../starter-backend/src/main/java/com/starter/adapters/stripe/StripeBillingAdapter.java) |
| Webhook signature verification | same adapter, `parseWebhook(...)` + [`StripeWebhookJson.java`](../starter-backend/src/main/java/com/starter/adapters/stripe/StripeWebhookJson.java) |
| State persistence | `users/{uid}.subscription` via [`UserRepositoryPort`](../starter-backend/src/main/java/com/starter/ports/UserRepositoryPort.java) (nested doc field) |
| Mobile feature | [`starter-mobile/src/features/billing/useBilling.ts`](../starter-mobile/src/features/billing/useBilling.ts) + tab screen [`app/(tabs)/billing.tsx`](<../starter-mobile/app/(tabs)/billing.tsx>) |
| Mobile contract notes | [`starter-mobile/docs/BACKEND_INTEGRATION.md`](../starter-mobile/docs/BACKEND_INTEGRATION.md) → "Billing (optional backend extension)" |

### Routes (contract v1)

| Method + path | Auth | Purpose |
|---|---|---|
| `POST /api/v1/billing/checkout-session` | Firebase Bearer | Create hosted Checkout session → `{url}` |
| `POST /api/v1/billing/portal-session` | Firebase Bearer | Create hosted portal session (400 if no customer) |
| `GET /api/v1/billing/me` | Firebase Bearer | Local subscription projection |
| `POST /api/v1/billing/webhook` | Stripe signature | Lifecycle events; **only Firebase-exempt API route** (`SecurityConfig`) |

Error codes: `BILLING_DISABLED` (503), `BILLING_PROVIDER_ERROR` (502), `NO_STRIPE_CUSTOMER` (400),
`INVALID_WEBHOOK_SIGNATURE` (400) — all on the standard `{code, message, correlationId}`
envelope.

---

## What needs to be done (per environment)

### 1. Stripe console (test mode first)

- [x] Log in and switch to **TEST mode** (toggle bottom-left):
      Stripe dashboard — <https://dashboard.stripe.com> / test view <https://dashboard.stripe.com/test>.
- [x] Create the subscription product in **TEST mode** (starter: DEV `price_1U9RPhBX76CeluqMtOURLc9T`, PROD `price_1U9iShBX76CeluqMnna2gBlb`):
      [Dashboard → Product catalog → Add product](https://dashboard.stripe.com/test/products)
      → Recurring price (e.g. monthly). Copy the **price id** (`price_...`).
- [x] Create the webhook endpoint (starter: DEV + PROD endpoints, the PROD one 2026-08-29):
      [Dashboard → Developers → Webhooks → Add endpoint](https://dashboard.stripe.com/test/webhooks) →
      **Endpoint URL** = `https://<app>-api-<env>.run.app/api/v1/billing/webhook` — select
      exactly: `checkout.session.completed`, `customer.subscription.created`,
      `customer.subscription.updated`, `customer.subscription.deleted`.
      Copy the **signing secret** (`whsec_...`).

**The Endpoint URL must be the real DEV Cloud Run URL** — the same one you put in
`API_BASE_URL_DEV` for mobile (from the deploy output / `curl https://<url>/health/ready`).
Template pattern from STEP6: `https://myapp-api-dev-<hash>-…run.app/api/v1/billing/webhook`.

- `API_BASE_URL_DEV` is **not a file** — it is a GitHub Actions **repository variable** on the
  mobile repo (`<app>-mobile` → Settings → Secrets and variables → Actions → Variables) and is
  duplicated in the EAS `preview` environment. Mobile code reads it at
  `starter-mobile/app.config.ts` (fallback `http://localhost:8080`).
- Template's own value as a concrete example:
  `https://starter-api-dev-906316354955.europe-west2.run.app` (see
  `guides/STEP5_FIRST_PUSH_CHECKLIST.md` §repo-variables).
- For a **new** app the variable does not exist yet — you create it in
  `guides/STEP6_NEW_APP_FROM_STARTER.md` §4b *after* the backend deploy, from the same deploy
  output. Until then there is nothing to find; the deploy output line and
  `curl https://<url>/health/ready` are the source of truth.
- [x] Note the **secret key** ([API keys page](https://dashboard.stripe.com/test/apikeys)):
      `sk_test_...` for DEV, `sk_live_...` only for PROD (live keys from
      <https://dashboard.stripe.com/apikeys>).

### 2. Backend config & secrets (repo: `<app>-backend`, files here are the template's)

The extension reads `starter.billing.*` — defaults live in
[`src/main/resources/application.yml`](../starter-backend/src/main/resources/application.yml)
(disabled by default: `enabled: ${BILLING_ENABLED:false}`). **Enabling without every value below
fails startup on purpose** (fail-closed, `BillingConfig`).

The deploy workflows already carry the wiring — [`deploy-dev.yml`](../starter-backend/.github/workflows/deploy-dev.yml)
and [`promote-prod.yml`](../starter-backend/.github/workflows/promote-prod.yml) run a
**conditional "Configure billing extension" step** that is skipped unless the matching repo
variable is `true`. You only provide the values.

- [x] Secrets → Secret Manager (starter: dev + prod done). [`infra/scripts/set-secrets.sh`](../starter-backend/infra/scripts/set-secrets.sh)
      already supports the two Stripe flags:
      ```bash
      cd infra && ./scripts/set-secrets.sh dev \
        --stripe-secret-key 'sk_test_…' \
        --stripe-webhook-secret 'whsec_…'
      ```
- [x] Non-secret values → GitHub Actions **repository variables** on the backend repo (starter: both envs)
      (Settings → Secrets and variables → Actions → Variables; or `gh variable set`).
      DEV names (PROD uses the `_PROD` suffix, e.g. `BILLING_ENABLED_PROD`):
      ```bash
      gh variable set BILLING_ENABLED_DEV -R <org>/<app>-backend --body true
      gh variable set STRIPE_PRICE_ID_DEV -R <org>/<app>-backend --body 'price_…'
      gh variable set BILLING_SUCCESS_URL_DEV -R <org>/<app>-backend \
        --body 'https://<app>-api-dev-….run.app/billing/return?status=success'
      gh variable set BILLING_CANCEL_URL_DEV -R <org>/<app>-backend \
        --body 'https://<app>-api-dev-….run.app/billing/return?status=canceled'
      gh variable set BILLING_PORTAL_RETURN_URL_DEV -R <org>/<app>-backend \
        --body 'https://<app>-api-dev-….run.app/billing'
      ```
      ⚠️ enabling without `STRIPE_PRICE_ID_*` / `*_URL_*` set makes the step **fail loudly**
      (deploy fails closed) — that is intentional.
- [x] Push/merge → deploy runs; the billing step flips the Cloud Run env vars + secrets (via `--update-*`, never `--set-*`).
- [ ] (Optional, template-only) local-run defaults if you want the app to answer 503 cleanly
      without a Stripe account — already the default; nothing to do.

### 3. Deploy & verify (DEV, test mode)

- [x] Local gate stays green: `./mvnw verify` then `docker build -t starter-backend:local .`
      (run from `starter-backend/`).
- [x] Deploy; smoke:
      ```bash
      curl -s -o /dev/null -w '%{http_code}\n' https://<app>-api-dev.run.app/api/v1/billing/me
      # 401 without token; with a DEV Firebase token: 200 {"status":"none",…}
      ```
- [x] **Automated E2E runs in CI** — `deploy-dev.yml` smoke job executes
      [`scripts/billing-e2e.sh`](../starter-backend/scripts/billing-e2e.sh) whenever
      `BILLING_ENABLED_DEV=true` (Stripe test mode; keys pulled from Secret Manager). It proves
      the full server-side chain: anonymous sign-in → `billing/me` `none` → hosted Checkout URL →
      Stripe customer (metadata `userId`) → real test subscription → **signed webhooks** →
      `billing/me` `active` with `planId`/`currentPeriodEnd` → portal URL → subscription cleanup.
      Prerequisites: `FIREBASE_WEB_API_KEY` secret, anonymous sign-in enabled, workflow SA may
      read the two Stripe secrets. The E2E uses `tok_visa` (raw card numbers via API are
      rejected); it validates the *backend* chain — it does not click through Checkout.
- [ ] End-to-end with a **test card** (`4242 4242 4242 4242`, any future date/CVC) — the manual
      click-through the CI E2E cannot do:
      1. App → Billing tab → **Upgrade** → hosted Checkout opens → pay.
      2. On return the app refetches; backend webhook set `status: active`.
      3. `GET /api/v1/billing/me` now returns `{"status":"active",…}`.
- [ ] Portal: Billing tab → **Manage subscription** → portal opens; cancel-at-period-end reflects
      back in `GET /api/v1/billing/me`.

### 4. Local development (no Stripe account needed)

- [x] `local` profile serves `MockBillingAdapter` (no network, no secrets; webhook parsing skips
      signature checks **by design — never run `local` outside a dev machine**).
- [ ] Against real DEV, forward test-mode webhooks:
      ```bash
      stripe listen --forward-to localhost:8080/api/v1/billing/webhook
      ```

### 5. Environments vs Stripe modes — what "done" actually means

The `dev`/`prod` split is **infrastructure only** (projects, promotion, webhook endpoints).
"PROD" here is a production-*shaped* environment. Real production billing = Stripe **live
mode** — none of the checks below involve real money:

| Environment | Stripe test mode | Stripe live mode |
|---|---|---|
| `dev` (starter-demo-dev) | ✅ wired, E2E green | n/a — dev stays test |
| `prod` (starter-demo-prod) | ✅ wired, E2E green — **still test mode** | ❌ go-live checklist below |

### 5.1 PROD-shaped env, Stripe TEST mode (wired 2026-08-29)

- [x] **Wired in test mode (no real money possible)**: terraform applied to `starter-demo-prod` (stripe secrets +
      Firebase Auth project config as code), `set-secrets.sh prod` with the test key + a **new
      PROD webhook endpoint** (own `whsec_…`, pointing at
      `starter-api-prod-599289271429.europe-west2.run.app`), new PROD product + price
      (`price_1U9iShBX76CeluqMnna2gBlb`, gbp 15/mo — one price per environment), the five
      `_PROD` repo variables, `promote-prod` green, and the full `billing-e2e.sh` chain
      verified against the PROD URL.
- [x] **PROD Firebase initialized** (was GCP-only — authed endpoints would have 401'd on
      `CONFIGURATION_NOT_FOUND`): project linked to Firebase, web app registered
      (`1:599289271429:web:1b0b90cf12627925c5c5ec`, API key + authDomain provisioned), anonymous
      + email/password sign-in enabled via `google_identity_platform_config` in `infra/main.tf`.
      The mobile **PROD build** config uses these values (app.config.ts / EAS env).
- [ ] **Real production = live mode** (the only step that touches real money): repeat §1 in live mode (new product/price/webhook endpoint,
      `sk_live_…`/`whsec_live_…`), re-run `set-secrets.sh prod`, update `STRIPE_PRICE_ID_PROD`.
      Everything else already works per environment.
- [ ] Smoke with a real (small) payment before announcing; CD/rollback rules unchanged
      (see [`starter-backend/docs/cicd_deployment_plan.md`](../starter-backend/docs/cicd_deployment_plan.md)).

### 6. Web pricing table (web export only, optional)

The billing screen's native Checkout path works without this — the pricing
table is an **extra** marketing/signup surface on the web export.

- [ ] Create the pricing table in the Stripe dashboard:
      [Developers → Pricing tables → Create pricing table](https://dashboard.stripe.com/test/pricing-tables)
      + your product/price. Copy the **pricing-table id** (`prctbl_...`).
- [ ] Add two **build-time env vars** to the mobile app's web export
      (`app.config.ts` reads them; they are public client config):
      ```bash
      STRIPE_PRICING_TABLE_ID=prctbl_...
      STRIPE_PUBLISHABLE_KEY=pk_test_...
      ```
      Set them in the EAS/web build env (e.g. `--set-env-vars` for the
      static-export step) or locally before `npx expo export`.
- [x] The webhook path already handles these checkouts: pricing-table sessions
      carry `client_reference_id` (the signed-in user's uid, set by
      [`app/pricing.tsx`](<../starter-mobile/app/pricing.tsx>)), and the adapter
      prefers that session hint when resolving the owning user.
- [x] The Billing tab links to `/pricing` on web only; native builds keep the
      backend-created hosted Checkout and render nothing extra.

### 7. Mobile (`<app>-mobile`)

Nothing to configure on native — the app is contract-driven.

- [x] Only keep the pinned contract in sync after a backend contract change:
      ```bash
      npm run validate:contract   # passes when types match contract/openapi.yaml
      ```
- [ ] If the tab is unwanted, remove the screen entry from [`app/(tabs)/_layout.tsx`](<../starter-mobile/app/(tabs)/_layout.tsx>) and the route file
      `app/(tabs)/billing.tsx` — no other wiring references it.

---

## Rules that must not be broken

- **Card data never touches this API** — collection happens on Stripe-hosted pages; the app only
  opens URLs.
- **`/api/v1/billing/webhook` stays the only Firebase-exempt route**; it authenticates via the
  `Stripe-Signature` HMAC over the raw payload, verified before parsing.
- **No delta effects on webhooks**: handlers write resulting state, so redeliveries are
  idempotent; events for unknown customers are acked `200` and dropped.
- **Secrets only in Secret Manager**; never in `application*.yml` values, mobile
  `EXPO_PUBLIC_*`, or logs.
- **Billing deploy is opt-in**: the deploy workflows' billing step only runs when
  `BILLING_ENABLED_<ENV>=true` is a repo variable; otherwise billing stays disabled (503).
- **New Stripe secrets need terraform**: `infra/main.tf` `local.secrets` lists which secrets
  Cloud Run can access — add `stripe-secret-key`/`stripe-webhook-secret` there (done in the
  template) and `terraform plan/apply` before `set-secrets.sh` can create versions.
- **`--set-env-vars` delimiter is `@` in `deploy-dev.yml`** (`^@^`): commas collide with URLs
  (`http://…`), so a **comma inside an `@`-delimited string silently merges the next var's
  `NAME=value` into the previous value** — e.g. `AI_ENABLED=true,AI_CHAT_MODEL=…` binds
  `AI_ENABLED` to `true,AI_CHAT_MODEL=…`, startup fails with `failed to convert ... to boolean`,
  and Cloud Run reports the unhelpful "container failed to start". Never mix separators in one
  `--set-env-vars`; `promote-prod.yml` stays comma-delimited (no URLs there).
- **`--set-env-vars`/`--set-secrets` REPLACE the whole environment on `gcloud run services
  update`; `--update-env-vars`/`--update-secrets` merge.** A billing step that ran
  `services update --set-env-vars "BILLING_…"` after the deploy step wiped the base env
  (`SPRING_PROFILES_ACTIVE`, `GCP_PROJECT_ID`, `AI_*`, CORS) and base secrets — the service
  came up but **every `verifyIdToken` rejected**, so all authenticated endpoints returned 401
  while health checks stayed green. This is why the billing steps in `deploy-dev.yml` and
  `promote-prod.yml` use `--update-*` exclusively. Corollary: after any env change, verify the
  live env survived (`gcloud run services describe <svc> --format='yaml(spec.template.spec.containers[0].env,spec.template.spec.containers[0].resources)'`), not just that the deploy succeeded.

## Maintenance point

When Stripe introduces a new subscription `status`, map it in
[`SubscriptionStatus.java`](../starter-backend/src/main/java/com/starter/domain/SubscriptionStatus.java)
and the OpenAPI `SubscriptionStatus` enum — unknown statuses currently degrade to `none` with a
WARN log (intentional, total webhook handling).
