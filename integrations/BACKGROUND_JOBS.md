# BACKGROUND_JOBS.md — Background Jobs (Cloud Scheduler / Cloud Tasks)

**Status: mechanism implemented in the template (opt-in, OFF by default); Cloud
activation is per-app user action.** This runbook is the activation guide for
scheduled/recurring backend work: retries, digests, cleanups, fan-out. The
template ships the *capability* (a config block + a `local` demo job proving
the mechanism) — products add the real jobs.

```text
prod product job (durable)      Cloud Scheduler/Cloud Tasks ──authenticated HTTP──▶ your job endpoint on Cloud Run
local/dev  (verifiable offline) @Scheduled demo job under the `local` profile
```

Why two paths: on Cloud Run with **`min-instances=0`** (this template's scale-to-zero
baseline), in-process `@Scheduled` is unreliable — an instance that is asleep
never fires. Scheduler/Tasks wake the service by calling it. In-process
scheduling is right for local/dev only.

## Decisions (recorded 2026-09-03)

- **v1 ships the mechanism, not product jobs.** `starter.jobs.enabled=false`
  means *no scheduler exists at all* — nothing is silently scheduled. Local
  `@Scheduled` beans are the local/dev path; the durable cloud path is
  Scheduler/Tasks → an **authenticated HTTP job endpoint** each product adds
  (mirroring the Stripe webhook's signature-exempt route shape, but
  authenticated via Cloud Scheduler OIDC / app-default service account, never
  public).
- **No Quartz at v1.** Spring's `@Scheduled` covers in-process needs; Quartz is
  only worth it when a product needs pauses/calendars/JDBC store — an
  extension, not starter core. Same reasoning as every "Spring can do it" row
  in [`docs/integrations-plan.md`](../docs/integrations-plan.md).
- **Cloud Tasks vs Scheduler**: Scheduler = cron push (3 free jobs, retries
  with configurable backoff); Tasks = queues for retry/fan-out per item
  (≈pennies). If a job is "run this every N" → Scheduler. If it is "process a
  backlog / retry failed webhook deliveries" → Tasks.

## What exists in the template (backend, `starter-backend`)

| Piece | Path |
|---|---|
| Config (`starter.jobs.*`, opt-in, no secret → nothing to fail closed on) | [`src/main/java/com/starter/config/JobsConfig.java`](../starter-backend/src/main/java/com/starter/config/JobsConfig.java) |
| Scheduling switch (off by default: `@EnableScheduling` only when enabled) | [`src/main/java/com/starter/jobs/JobSchedulingConfiguration.java`](../starter-backend/src/main/java/com/starter/jobs/JobSchedulingConfiguration.java) |
| Local demo job (logs one line per tick; `local` profile only) | [`src/main/java/com/starter/jobs/LocalDemoJob.java`](../starter-backend/src/main/java/com/starter/jobs/LocalDemoJob.java) |
| Default config keys | edit [`src/main/resources/application.yml`](../starter-backend/src/main/resources/application.yml) → `starter.jobs.*` |
| Tests | `JobsConfigTest`, `JobsContextTest` (no demo-job bean when disabled; wired when enabled under `local`), `ConfigFailClosedTest` (cloud profiles inherit, must not redefine) |

Config keys (all `starter.jobs.*`, env-tunable):

```yaml
starter:
  jobs:
    # Optional background-jobs capability (Cloud Scheduler/Tasks). Disabled by
    # default: no scheduler, nothing runs.
    enabled: ${JOBS_ENABLED:false}
    demo-interval: ${JOBS_DEMO_INTERVAL:PT1M}   # local demo job period
```

- **`local` verify (no Cloud needed):** run with `--spring.profiles.active=local
  -DJOBS_ENABLED=true`, expect one
  `Background-jobs demo tick (local profile; interval=...)` line per minute
  (tune with `JOBS_DEMO_INTERVAL=PT5S` for an instant smoke).
- **Cloud profiles inherit the block** (enforced by `ConfigFailClosedTest`);
  enable per environment with `JOBS_ENABLED=true` only if a product actually
  has in-process jobs worth keeping on a non-zero-minimum instance.

## What needs to be done (per product job) — durably

A product adds two pieces; the template does not ship them because they are
product-specific:

### 1. An authenticated job endpoint (backend)

Add a route the scheduler calls, e.g. `POST /api/v1/jobs/stripe-retry`, that:
- is protected, not public — Cloud Scheduler can authenticate with
  `OIDC token / service account` (same project) or, for Tasks, the
  `OIDC service account email` + `OIDC token audience` of your service;
  a shared-secret header is the fallback. **Never a public job route.**
- is **idempotent** and safe to hit twice (Scheduler/Tasks re-deliver);
- returns fast (Cloud Run request budget) and does its work asynchronously or
  in bounded batches; enqueue heavy tails via Cloud Tasks instead of blocking.

The webhook route (`/api/v1/billing/webhook`) is the shape reference for an
exempt-but-authenticated route; every additional exempt route is a security
decision (`SecurityConfig`), not a wiring detail.

### 2. The schedule (GCP, per environment)

- **Cloud Scheduler** (cron-style): create a job in the app's project
  (`{app}-dev` / `{app}-prod`), target = the job endpoint URL, HTTP method +
  OIDC auth as above. 3 free jobs; more are pennies. Terraform `infra/` can own
  it: add a `google_cloud_scheduler_job` resource per environment.
- **Cloud Tasks** (queue/retry): `google_cloud_tasks_queue` + enqueue from the
  adapter (spring-cloud-gcp Tasks, or plain HTTP to the queue endpoint).

### 3. Verify

- [ ] Local: demo tick appears (above) on `local` + `JOBS_ENABLED=true`.
- [ ] DEV: create the Scheduler job → watch the target endpoint receive the
      authenticated call; confirm idempotent double-delivery is harmless.
- [ ] PROD only after DEV proves the round-trip (same rule as every extension).

## Example jobs (per the integration plan's note: "Stripe retries / digests will force this")

| Job | Trigger | Notes |
|---|---|---|
| Stripe webhook re-delivery retry | Cloud Tasks queue | Stale/failed webhook deliveries retried with backoff; handler stays idempotent |
| Daily digest (Resend) | Cloud Scheduler daily | Reuses `EmailPort`; recipients/bodies never logged |
| Token / stale-data cleanup | Cloud Scheduler hourly | Keep batches bounded |

## Rules that must not be broken

- **Jobs are off by default; nothing is silently scheduled.** Add a product job
  only by enabling the extension and adding the bean/route.
- **The demo job is `local`-profile only — never deployed** (identical rule to
  every mock adapter).
- **Job endpoints are authenticated, never public**; idempotent; bounded-time.
- **No secrets in scheduled code or logs** (backend security rule 4); jobs that
  touch PII follow the same logging rules as the request path.
- **Scale-to-zero reality**: an in-process `@Scheduled` job in a product is
  `@Scheduled` on a sleeping instance. Prefer Scheduler/Tasks for anything a
  product depends on; keep `@Scheduled` for dev-only and always-on instances.
- **Cloud profiles inherit** `starter.jobs` from the base file — never redefine
  the block in `application-dev/prod.yml`.

## Maintenance point

The mechanism is tiny and lives entirely in `config/JobsConfig.java` +
`jobs/JobSchedulingConfiguration.java` + `jobs/LocalDemoJob.java`. Products own
their job beans and endpoints; this workspace only evolves the switch and the
runbook (e.g. adding a ready-made `google_cloud_scheduler_job` Terraform module
when the first product applies it live).