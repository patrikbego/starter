# Deploy Backend to PROD

Production is an approved promotion of a tested immutable image digest. Do not run the current prototype `deploy-prod-backend.yml` for a real production release: it rebuilds from source and does not meet this policy yet.

## Production environment boundary

PROD must use its own Firebase/GCP project such as `{app}-prod-<unique-suffix>`. A Firebase app registration inside the DEV project is not a separate environment and must not be used for production.

The PROD project owns its own:

- Firebase Authentication users/providers and mobile Web app registration, such as `{app}-mobile-prod`;
- Firestore database and immutable location choice;
- Cloud Run service and least-privilege runtime identity;
- Artifact Registry access, Secret Manager secrets, quotas, billing, and audit logs;
- AI provider key, budgets, and rate limits.

The backend uses Admin SDK credentials for the PROD project; it does not use the mobile Web app's public Firebase API key. Mark the Firebase project as a production environment in the console and never grant its runtime identity access to DEV as a shortcut.

## Preconditions

- Backend repository extraction is complete.
- CI passed for the commit.
- The image digest was deployed and smoke-tested in DEV.
- The API change is compatible with released mobile clients.
- Data changes are backward-compatible and resumable.
- Production GitHub environment approval and PROD WIF identity are configured.
- The PROD Firebase/GCP project is separate from DEV and billing/budget monitoring is configured.
- The backend project ID and mobile PROD Firebase `projectId` identify that same PROD project.
- Firestore was deliberately provisioned in the recorded PROD location; its location cannot be changed later.
- Authentication providers, authorized domains, test accounts, secrets, and AI keys are production-specific.

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
