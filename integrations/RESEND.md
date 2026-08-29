# RESEND.md — Transactional Email Integration (Resend)

**Status: designed, not yet implemented in the templates (2026-08-29).** This is the design +
activation checklist for transactional email on the Spring Boot backend (`starter-backend`),
sent through [Resend](https://resend.com). Same shape as [`STRIPE.md`](./STRIPE.md): what to
build, what to wire, exact paths. Unlike Stripe there is no code to activate yet — "What to
build" is the implementation spec; the per-environment sections are the runbook once it lands.

Positioning: **Firebase Auth already sends auth emails** (verification, password reset —
[`starter-backend/docs/AUTHENTICATION.md`](../starter-backend/docs/AUTHENTICATION.md)). Resend
covers *product* email the backend originates: welcome, notifications, receipts, digests. The
template ships only the wiring (port + fail-closed config + adapters); products add their own
templates and triggers. No new API routes, no OpenAPI change, nothing on mobile.

---

## Where it will live

| Piece | Path (to create unless noted) | Pattern to copy |
|---|---|---|
| Port | `starter-backend/src/main/java/com/starter/ports/EmailPort.java` | [`BillingPort.java`](../starter-backend/src/main/java/com/starter/ports/BillingPort.java) |
| Config binding (fail-closed) | `starter-backend/src/main/java/com/starter/config/EmailConfig.java` | [`BillingConfig.java`](../starter-backend/src/main/java/com/starter/config/BillingConfig.java) |
| Resend adapter (HTTP lives only here) | `starter-backend/src/main/java/com/starter/adapters/resend/ResendEmailAdapter.java` | [`StripeBillingAdapter.java`](../starter-backend/src/main/java/com/starter/adapters/stripe/StripeBillingAdapter.java) |
| Local mock adapter | `starter-backend/src/main/java/com/starter/adapters/resend/MockEmailAdapter.java` | `MockBillingAdapter` (billing `local` profile) |
| Default config keys | edit [`src/main/resources/application.yml`](../starter-backend/src/main/resources/application.yml) | existing `starter.billing.*` block |
| Terraform secret grant | edit [`infra/main.tf`](../starter-backend/infra/main.tf) → `local.secrets` | `stripe-secret-key` entries |
| Secret-creation flag | edit [`infra/scripts/set-secrets.sh`](../starter-backend/infra/scripts/set-secrets.sh) | `--stripe-secret-key` case |
| Deploy step (DEV) | edit [`.github/workflows/deploy-dev.yml`](../starter-backend/.github/workflows/deploy-dev.yml) | "Configure billing extension (DEV)" step |
| Deploy step (PROD) | edit [`.github/workflows/promote-prod.yml`](../starter-backend/.github/workflows/promote-prod.yml) | "Configure billing extension (PROD)" step |
| Design & security notes | `starter-backend/docs/EMAIL_EXTENSION.md` | [`BILLING_EXTENSION.md`](../starter-backend/docs/BILLING_EXTENSION.md) |

Routes: **none**. Email is a backend-internal capability behind the port; use cases call it as a
side effect of existing flows. If a product later needs delivery/bounce events, add an optional
webhook route mirroring the Stripe pattern (see §Optional webhooks).

---

## What to build (implementation spec)

### 1. Port — `EmailPort`

Deliberately minimal; implementations own all provider types:

```java
public interface EmailPort {

    /** Sends one email; returns the provider message id (for logging only). */
    String send(EmailMessage message);

    record EmailMessage(String to, String subject, String html, String text) {}
}
```

### 2. Config — `EmailConfig` (fail-closed, mirrors `BillingConfig`)

```java
@ConfigurationProperties(prefix = "starter.email")
public record EmailConfig(
        @DefaultValue("false") boolean enabled,
        String apiKey,
        String from,
        @DefaultValue("https://api.resend.com") String baseUrl
) {
    public EmailConfig {
        if (enabled) {
            require(apiKey, "starter.email.api-key");
            require(from, "starter.email.from");
        }
    }
    // require(...) identical to BillingConfig
}
```

`application.yml` block (disabled by default — same kill-switch philosophy as billing):

```yaml
starter:
  email:
    # Optional transactional email extension (Resend). Disabled by default.
    # Enabling requires every value below, otherwise startup fails closed
    # (EmailConfig). Secrets come from Secret Manager; never log them.
    enabled: ${EMAIL_ENABLED:false}
    api-key: ${RESEND_API_KEY:}
    from: ${EMAIL_FROM:}
    base-url: ${RESEND_BASE_URL:https://api.resend.com}
```

### 3. Adapter — `ResendEmailAdapter` via `RestClient` (no SDK dependency)

Resend's send API is a plain authenticated JSON POST, and there is **no official Java SDK** —
use Spring's `RestClient` (already on the classpath) instead of picking up a community SDK:

```java
POST {baseUrl}/emails
Authorization: Bearer {apiKey}
Content-Type: application/json

{"from": "Starter <no-reply@…>", "to": ["…"], "subject": "…", "html": "…"}
```

Response `{"id": "…"}` → returned as the message id. Any non-2xx (including `429`) maps to an
`EmailProviderException` carrying the status code — **the adapter never retries and never swallows**;
retry policy belongs to the calling use case. Class annotations mirror `StripeBillingAdapter`:

```java
@Service
@Profile("!local")
@ConditionalOnProperty(prefix = "starter.email", name = "enabled", havingValue = "true")
public class ResendEmailAdapter implements EmailPort { … }
```

Logging: **never** log `to`, `subject`, or body content (email addresses are PII). Log
`userId`/correlationId + template key + the provider message id only.

### 4. Mock — `MockEmailAdapter`

`@Profile("local")` only. No network, no secrets; logs the message to stdout for offline
inspection and returns a fake id. Same caveat as `MockBillingAdapter`: signature-free shortcuts
never leave a dev machine — `local` must never be deployed.

### 5. Wiring diffs

- **`infra/main.tf`** — add `"resend-api-key"` to `local.secrets` (next to the stripe entries),
  then `terraform plan/apply`. Without this, Cloud Run's runtime SA cannot read the secret and
  `set-secrets.sh` versions are invisible to the service.
- **`infra/scripts/set-secrets.sh`** — add a case mirroring `--stripe-secret-key`:

  ```bash
  --resend-api-key)
    name="resend-api-key"
    value="${2:?value required}"
    shift 2
    ;;
  ```

- **Deploy workflows** — a "Configure email extension" step in both `deploy-dev.yml` and
  `promote-prod.yml`, gated on the repo variable, and **always `--update-*`** (see Rules):

  ```yaml
  - name: Configure email extension (DEV)
    if: ${{ vars.EMAIL_ENABLED_DEV == 'true' }}
    run: |
      gcloud run services update ${{ env.SERVICE_NAME }} \
        --region ${{ env.REGION }} \
        --platform managed \
        --update-secrets "RESEND_API_KEY=resend-api-key:latest" \
        --update-env-vars "^|^EMAIL_ENABLED=true^EMAIL_FROM=${{ vars.EMAIL_FROM_DEV }}"
  ```

  ⚠️ **Delimiter is `^|^`, not the billing step's `^@^`** — `EMAIL_FROM` contains `@`
  (`no-reply@app.com`), so an `@`-delimited list splits mid-value exactly like the documented
  comma-vs-URL collision. Commas are equally forbidden (same `--set-env-vars` merging rule).

---

## What needs to be done (per environment)

### 1. Resend console

- [ ] Create the account and API keys — [resend.com/api-keys](https://resend.com/api-keys).
      **One key per environment** (DEV and PROD are separate `re_…` secrets; Resend has **no test
      mode** — every send is a real email).
- [ ] Add and verify the sending domain — [resend.com/domains](https://resend.com/domains):
      add the DNS records Resend displays (DKIM TXT, SPF TXT, MX return-path) in your DNS
      provider, then **Verify**. Recommended: `mail.<app>.dev` for DEV and `mail.<app>.com`
      for PROD (or one domain with distinct from-addresses).
- [ ] Before domain verification, Resend only allows sending from `onboarding@resend.dev` to
      your own account address — enough for the first smoke send, nothing more.

### 2. Backend secrets & variables (repo: `<app>-backend`)

- [ ] Secret → Secret Manager:
      ```bash
      cd infra && ./scripts/set-secrets.sh dev --resend-api-key 're_…'
      ```
- [ ] Non-secret values → GitHub Actions **repository variables** on the backend repo
      (Settings → Secrets and variables → Actions → Variables; or `gh variable set`).
      DEV names (PROD uses the `_PROD` suffix, e.g. `EMAIL_ENABLED_PROD`):
      ```bash
      gh variable set EMAIL_ENABLED_DEV -R <org>/<app>-backend --body true
      gh variable set EMAIL_FROM_DEV -R <org>/<app>-backend --body 'Starter <no-reply@dev.mail.<app>.dev>'
      ```
- [ ] Prereq check: `resend-api-key` present in `local.secrets` + terraform applied (§5 above),
      otherwise the `--update-secrets` binding points at a secret the service cannot read.

### 3. Deploy & verify (DEV)

- [ ] Local gate stays green: `./mvnw verify` then `docker build -t starter-backend:local .`
      (run from `starter-backend/`).
- [ ] Push/merge → deploy runs; the email step flips the Cloud Run env var + secret.
- [ ] Startup is itself a config proof: `EmailConfig` fails closed, so if the service starts
      with `EMAIL_ENABLED=true`, the binding is complete. Confirm the env survived:
      ```bash
      gcloud run services describe <svc> --format='yaml(spec.template.spec.containers[0].env)'
      ```
- [ ] There is **no route to trigger a send** — the template has none by design. First real
      send happens through the first product trigger; verify it in the Resend dashboard →
      **Logs** (every send appears there with delivered/bounced status).

### 4. Local development

- [ ] `local` profile serves `MockEmailAdapter` (no network, no Resend account needed).
      Nothing to do; `enabled: false` is the default.
- [ ] Against real DEV from a laptop: export `RESEND_API_KEY` + `EMAIL_FROM` and flip
      `starter.email.enabled=true` locally — sends are real (no test mode), keep recipients
      to a mailbox you own.

### 5. PROD (only after DEV is proven)

- [ ] Separate PROD API key → `set-secrets.sh prod --resend-api-key 're_…'`.
- [ ] `_PROD` repo variables: `EMAIL_ENABLED_PROD=true`, `EMAIL_FROM_PROD=…` (verified
      production from-address).
- [ ] CD/rollback rules unchanged
      ([`starter-backend/docs/cicd_deployment_plan.md`](../starter-backend/docs/cicd_deployment_plan.md)).

### 6. Mobile (`<app>-mobile`)

Nothing to configure — the app is contract-driven and email adds no contract surface.

- [ ] Only keep the pinned contract in sync after an unrelated backend contract change:
      ```bash
      npm run validate:contract
      ```

---

## Optional webhooks (delivery/bounce events)

Skip unless a product needs delivery state. When added, mirror the Stripe webhook pattern
exactly: a signature-exempt route (second entry in `SecurityConfig` next to
`/api/v1/billing/webhook`), signature verified over the raw payload before parsing, handlers
write resulting state so redeliveries are idempotent. Resend signs webhook deliveries with
Svix (`svix-id` / `svix-timestamp` / `svix-signature` headers) — verify the current details
against [Resend's webhook docs](https://resend.com/docs/dashboard/webhooks/introduction) when
implementing, and store the signing secret as a second Secret Manager entry
(`resend-webhook-secret`) granted through `local.secrets`.

## Rules that must not be broken

- **Email failures never fail the user flow.** Use cases catch `EmailProviderException`, log
  WARN with the correlationId, and continue — signup succeeds even if the welcome email fails.
  (No template flow treats the email itself as the feature.)
- **Resend has no test mode.** Every send is real. Separate API keys per environment, DEV sends
  only to owned mailboxes, never reuse the PROD key.
- **Fail-closed activation**: enabling without `RESEND_API_KEY`/`EMAIL_FROM` must fail startup
  (`EmailConfig`), never run half-configured.
- **No recipients, subjects, or bodies in logs** from the real adapter — email addresses are
  PII (backend security rule 4). `userId`/correlationId + template key + message id only.
- **Secrets only in Secret Manager**; never in `application*.yml` values, mobile
  `EXPO_PUBLIC_*`, or logs.
- **New secret needs terraform**: `resend-api-key` must be in `infra/main.tf` `local.secrets`
  and applied before `set-secrets.sh` versions are readable by Cloud Run.
- **Deploy step uses `--update-secrets`/`--update-env-vars` exclusively** — `--set-*` REPLACE
  the whole environment and wipes the base vars (observed Stripe incident: every authenticated
  endpoint 401'd while health stayed green).
- **Delimiter is `^|^` in the email step** — `@` appears in every `EMAIL_FROM` value and `,`
  collides with URLs; either inside a delimited list silently corrupts the remaining vars.
- **Mock adapter only under `local`; never deploy `local`.**

## Maintenance point

The send endpoint/response shape is isolated in `ResendEmailAdapter` — if Resend changes it,
that file is the only edit. If products need delivery events, add the optional webhook route
(§above) and extend `SecurityConfig` deliberately: any new Firebase-exempt route is a security
decision, not a wiring detail.
