# SEARCH.md — Search Extension (Typesense)

**Status: pattern doc + runbook, no code.** Vendor decided (2026-09-05):
**Typesense**, with Algolia kept as an adapter swap. Design doc:
[`SEARCH_EXTENSION_NOT_IMPLEMENTED.md`](../starter-backend/docs/SEARCH_EXTENSION_NOT_IMPLEMENTED.md). This runbook is the activation guide
for a product fork that needs typo-tolerant search over user content — enable
the extension, provision the engine, run the local proof, verify the API flow.

## Decision — Typesense over Algolia

| | **Typesense** | **Algolia** |
|---|---|---|
| Model | Apache-2.0 OSS — self-host or Typesense Cloud | SaaS only |
| Cost at template scale | Engine **$0** (Apache-2.0), **hosting is not** — self-host needs an always-on VM (≈$7–16/mo); Cloud paid from ≈$26/mo; the Cloud free *dev* cluster is dev-only (sleeps) | Free tier **is real**: 10k records / 10k searches / 100k ops **per month** — then per-operation |
| Operations | Single lightweight binary, Docker, easy GCE VM | Zero ops — but per-op meter running |
| Fit with template | REST API mirrors the Resend/PostHog `RestClient` precedent; official Java client | Official Java client; same port shape |
| Typo tolerance / facets / synonyms | Built-in | Built-in |

Snapshot 2026 — re-verify numbers at activation (house rule: free-tier figures
are never trusted from memory). Sources at decision time:
[BuildMVPFast search pricing comparison (2026)](https://www.buildmvpfast.com/api-costs/search),
[Typesense Cloud free tier](https://cloud-help.typesense.org/article/how-does-the-free-tier-work),
[Typesense install guide](https://typesense.org/docs/guide/install-typesense.html),
[Algolia 10k free tier](https://dev.to/0012303/algolia-has-a-free-tier-add-instant-search-to-any-app-with-10k-records-and-10k-requestsmonth-3f96).

**The deployment caveat that drives the choice:** Typesense is **stateful** —
it cannot live on scale-to-zero Cloud Run (ephemeral disk). Self-host means a
small GCE VM (or Docker locally / Typesense Cloud managed). **That VM is a
fixed cost from day one** — the engine is free, the hosting is not. Algolia
buys zero stateful infra with per-operation billing; its free tier caps at
10k searches/mo and is the only true-$0 production option at template scale.
Rule of thumb: self-host Typesense when the product controls infra and pays a
small fixed VM bill; pick Typesense Cloud or Algolia when the product wants a
managed meter to pay.

```text
client ──GET /api/v1/search──▶ Cloud Run (owner from principal)
                                  └─ SearchPort ─▶ Typesense (filter_by owner)   [cloud]
                                                  InMemorySearchAdapter          [local]
```

## Decisions (recorded 2026-09-05)

- **Backend-proxied search only.** The backend holds the engine key and ANDs
  every query with `ownerUid` from the verified principal. The mobile client
  never sees a search key and search UI stays product territory — the template
  ships no search screen.
- **Write-through indexing.** Product code calls `SearchService.index(...)` on
  its own write paths; delete on entity delete. A durable reindex job
  (Cloud Scheduler/Tasks → `POST /api/v1/search/jobs/reindex`, same mechanism
  as [`BACKGROUND_JOBS.md`](./BACKGROUND_JOBS.md)) covers backfills.
- **`RestClient`, not the SDK, is the default adapter shape** — the house
  precedent (Resend, PostHog, reCAPTCHA all use `RestClient` + `JsonMapper`,
  no community SDK in the dependency tree). If the official `typesense-java`
  client is adopted instead, it stays inside the adapter.
- **Elasticsearch rejected.** Spring Data Elasticsearch is a real wrapper but
  ES is stateful and ops-heavy; both real vendors ship official Java clients
  (or plain REST) that fit the port without a wrapper. See
  [`integrations-plan.md`](../docs/integrations-plan.md) §4.
- **Search terms are user content** — never logged, never telemetry beyond
  outcome-only counters (backend security rule 4 / mobile rule 6 extension).

## Prerequisites (per product app)

- Firebase Auth enabled (identity from the verified principal).
- An engine, one of:
  - **Typesense Cloud** — create the dev cluster, copy the API key + host URL.
  - **Self-hosted Typesense** — GCE VM (e.g. `e2-small`, 2 GB RAM),
    `docker run -p 8108:8108 typesense/typesense --data-dir /data
    --api-key <admin-key> --enable-cors`; open 8108 to Cloud Run egress only.
    Verify the API key is a **search+write key for the collection**, not the
    master, once collections exist — the master key in Secret Manager is
    acceptable at template scale (same posture as the OpenRouter key).
- Firestore security rules unchanged — search state lives in the engine, not
  Firestore; the index is engine-side.

## Activation steps

1. **Engine** — provision Typesense (Cloud or VM above). Notes the collection
   will be created by the adapter on first startup (idempotent upsert).
2. **Env** (service env vars, cloud profiles) — the full `SEARCH_*` table lives in
   [`docs/ENVIRONMENT_MATRIX.md`](../docs/ENVIRONMENT_MATRIX.md) §Search; this runbook does not
   restate it. Minimum activation:

   ```text
   SEARCH_ENABLED=true
   SEARCH_HOST=https://<typesense-cluster>.typesense.net   # or http://<vm>:8108
   SEARCH_API_KEY=<search+write key>                       # Secret Manager
   ```

   **Fail-closed:** `SEARCH_ENABLED=true` without `host` or `api-key` fails
   startup — there is no half-configured run.
3. **Reindex job (cloud, durable)** — Cloud Scheduler (cron) or Cloud Tasks →
   `POST /api/v1/search/jobs/reindex`, authenticated via Cloud Scheduler OIDC /
   the runtime service account. The extension answers only that caller; users
   never reach it.
4. **Verify locally** — `local` profile needs no network or credentials
   (deterministic `InMemorySearchAdapter`):

   ```bash
   SPRING_PROFILES_ACTIVE=local SEARCH_ENABLED=true ./mvnw spring-boot:run
   # register/login, index a document through the write-through route, then:
   curl -s "http://localhost:8080/api/v1/search?q=<term>" -H "Authorization: Bearer $TOKEN"
   ```

   Optionally run real Typesense via Docker for a contract-fidelity smoke
   (`docker run -p 8108:8108 typesense/typesense --data-dir /data
   --api-key <dev-key> --enable-cors`).
5. **PROD** — `SEARCH_ENABLED_PROD=true` repo variable + the PROD secret binding
   (same Secret Manager key pattern as PostHog/OpenRouter; see
   [`guides/STEP6_NEW_APP_FROM_STARTER.md`](../guides/STEP6_NEW_APP_FROM_STARTER.md) §5).

## Verification

- `./mvnw verify` green with the extension **off** (routes answer
  `503 SEARCH_DISABLED`) and **on** (unit adapter + service tests: owner filter
  always ANDed, pagination, write-through delete, provider timeout, leak-free
  failures, config fail-closed).
- Live smoke: index a document, search for it with a typo (typo tolerance
  proves the engine is really serving), confirm another user's document never
  appears (owner scoping proof).
- OpenAPI contract test (`OpenApiContractTest`) keeps the spec and routes in
  sync once the surface lands.

## Rules that must not be broken

- **Search results are always owner-filtered at the adapter** — the engine
  credential can never be used without an owner filter; scoping is not the
  controller's job to remember.
- **The engine key stays in Secret Manager** — never mobile, never logs, never
  `EXPO_PUBLIC_*`.
- **Search terms are never logged or telemetried** beyond outcome-only
  counters.
- **Extension disabled ⇒ routes 503** — search must be kill-switched without a
  redeploy (`SEARCH_ENABLED=false`); the contract route stays but the gate
  answers `SEARCH_DISABLED`.
- **No product-specific search UI in the template** — the port + routes are the
  capability; screens belong to the product fork.

## Per-app vs shared

The Typesense/Cloud account and billing are shared across your apps; the
cluster (or VM), collection, keys, and search routes are per app, per
environment — same rule as every runtime integration
([`guides/STEP6_NEW_APP_FROM_STARTER.md`](../guides/STEP6_NEW_APP_FROM_STARTER.md) §2).