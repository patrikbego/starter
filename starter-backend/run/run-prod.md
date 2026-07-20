# Deploy Backend to PROD

Production is an approved promotion of a tested immutable image digest. Do not run the current prototype `deploy-prod-backend.yml` for a real production release: it rebuilds from source and does not meet this policy yet.

## Preconditions

- Backend repository extraction is complete.
- CI passed for the commit.
- The image digest was deployed and smoke-tested in DEV.
- The API change is compatible with released mobile clients.
- Data changes are backward-compatible and resumable.
- Production GitHub environment approval and PROD WIF identity are configured.

## Promotion

1. Select the eligible DEV-tested digest/release record.
2. Start the protected production promotion workflow.
3. Approver verifies commit, digest, contract version, and DEV result.
4. Workflow deploys `IMAGE_URI@sha256:DIGEST` without checking out/building source.
5. Record Cloud Run revision and run minimal smoke tests.
6. Monitor error rate, latency, auth failures, and AI cost/availability.

## Smoke tests

- minimal liveness/readiness;
- missing token -> standard `401`;
- controlled production test account -> `/api/v1/me` after route migration;
- AI request only when the release plan permits a real provider call;
- deployed revision reports/records the expected digest.

## Rollback

Route traffic to the previous known-good Cloud Run revision or redeploy its recorded digest. Rollback does not undo data changes; use forward/backward-compatible migrations.

After rollback, preserve logs/release metadata and open an incident or defect with the correlation IDs and timing—not tokens or user content.
