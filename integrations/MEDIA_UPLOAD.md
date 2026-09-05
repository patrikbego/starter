# MEDIA_UPLOAD.md — Media Upload Extension (GCS storage + validation + variants + vision AI)

**Status: implemented, opt-in (backend; spec contract v1).** Design doc:
`starter-backend/docs/MEDIA_UPLOAD_EXTENSION.md`; local mocks + cloud adapters
(in-memory storage/metadata, GCS, Firestore, OpenRouter), full test coverage
(unit + enabled/disabled integration + OpenAPI contract). This runbook is the
activation guide: enable the extension, provision GCS, run the local proof, and
verify the API flow. It mirrors `PUSH.md` / `STRIPE.md` in shape.

```text
client ──POST /api/v1/media──▶ Cloud Run (validate + variants) ──▶ GCS bucket
                                 └─analysis enabled?─▶ OpenRouter vision ─▶ PushPort
```

## Decisions (recorded)

- **API-proxied upload by default; signed URLs for download.** Content
  validation and variant re-encoding force the API to see bytes at least once,
  so `starter.media.upload-mode=proxy` is default. Downloads are `302` to
  short-TTL signed GCS URLs — the API never proxies read bytes in cloud
  profiles. `upload-mode=signed` is config-validated but **not wired in v1**
  (the storage port issues no signed-PUT URLs and there is no confirm route);
  it is the future path for large files that need no server-side processing.
- **Analysis is off by default.** `starter.media.analysis.enabled=false`: no
  bytes leave the server unless you enable it with a key. Only the re-encoded
  `web` variant is ever sent to the provider, never the original.
- **Completion is delivered over the existing push channel, not WebSocket.**
  If push is disabled, the metadata endpoint is the fallback (analysis status is
  always there).
- **The durable analysis drain is Cloud Scheduler/Tasks → the authenticated job
  endpoint.** In-process `@Scheduled` exists only under `local`. Same posture as
  [`BACKGROUND_JOBS.md`](./BACKGROUND_JOBS.md).

## Prerequisites

- Firebase Auth enabled (the extension derives identity from the verified
  principal — no separate auth setup).
- GCS bucket (e.g. `gs://{project-id}-media`) with uniform bucket-level
  access; Cloud Run's runtime service account gets
  `roles/storage.objectUser` (write + read + delete) and, for signed URLs,
  `roles/iam.serviceAccountTokenCreator`.
- Firestore enabled (media metadata lives under `users/{uid}/media/{mediaId}`;
  server-only access via the runtime service account, no rules change).
- Vision analysis (optional): an OpenRouter API key in Secret Manager
  (`OPENROUTER_API_KEY` can be shared with the AI-chat key).

## Activation steps

1. **GCS** — create the bucket, set IAM as above. Uniform bucket-level access,
   public access prevention enforced, no public URLs (signed URLs only).
   Local dev can use [fake-gcs-server](https://github.com/fsouza/fake-gcs-server).
2. **Env** (service env vars, cloud profiles) — the full `MEDIA_*` name/default table lives in
   [`docs/ENVIRONMENT_MATRIX.md`](../docs/ENVIRONMENT_MATRIX.md) §Media upload; this runbook
   deliberately does not restate it. Minimum activation:

   ```text
   MEDIA_ENABLED=true
   MEDIA_STORAGE_BUCKET=gs://{project-id}-media
   MEDIA_ANALYSIS_ENABLED=false            # or true + OPENROUTER_API_KEY (Secret Manager)
   ```

   **Fail-closed:** `MEDIA_ENABLED=true` without a bucket, or
   `MEDIA_ANALYSIS_ENABLED=true` without a key, fails startup — there is no
   half-configured run.
3. **Background drain (cloud, durable)** — Cloud Scheduler (cron, e.g.
   `*/5 * * * *`) or Cloud Tasks → `POST /api/v1/media/jobs/analyze`,
   authenticated via Cloud Scheduler OIDC / the runtime service account.
   **Caveat:** the route currently accepts any Firebase bearer (no job
   token/owner scoping) and drains all owners' pending rows — a jobs-only
   credential is recommended hardening before production (see the
   `MEDIA_UPLOAD_EXTENSION.md` Surface section in the backend repo).
4. **Verify locally** — `local` profile needs no network or credentials
   (in-memory storage + deterministic vision fake + mock push):

   ```bash
   SPRING_PROFILES_ACTIVE=local MEDIA_ENABLED=true ./mvnw spring-boot:run
   # register/login, then:
   curl -s -X POST http://localhost:8080/api/v1/media \
     -H "Authorization: Bearer $TOKEN" -F 'file=@photo.png'   # expect 201 + variants + analysis
   curl -s http://localhost:8080/api/v1/media -H "Authorization: Bearer $TOKEN"
   curl -sI http://localhost:8080/api/v1/media/1/file -H "Authorization: Bearer $TOKEN"   # 302 → signed URL (cloud) / bytes (local)
   ```

5. **Contract** — `MEDIA_UPLOAD_EXTENSION.md` defines the routes; the OpenAPI
   contract (`openapi/openapi.yaml`) and `OpenApiContractTest` keep spec and
   implementation in sync. Any product that forks the template pins that
   contract, not the backend source.

## Gotchas

- **Signed URLs carry `X-Goog-*` headers** — clients fetch them with
  `fetch(url)` (no Authorization header — the signature is in the query).
- **Rate limiting is per-UID sliding window in memory** — same single-instance
  caveat as AI: a hard global quota needs a shared `AiRateLimitStore`
  implementation.
- **Analysis cost is per image and per provider** — a global spend cap on the
  OpenRouter key is the product's responsibility (`docs/operations/COST_CONTROLS.md`
  covers GCP bill protection; the provider key budget is separate).
- **Never disable the extension by unsetting the bucket in prod** — fail-fast
  startup catches it, roll the env var back (re-pointing a bucket is fine; an
  empty value is a hard stop by design).