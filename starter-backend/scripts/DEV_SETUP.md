# DEV Environment Bootstrap

Target v1 provisions DEV through versioned infrastructure code in this backend repository. The commands/resources below are the required outcome, not a substitute for idempotent automation.

## DEV resources

- `{app}-dev` GCP project with billing/budgets
- linked Firebase project and required sign-in providers
- Firestore in the chosen immutable data location
- Cloud Run service and least-privilege runtime service account
- private release Artifact Registry access
- Secret Manager entries for DEV provider/admin secrets
- GitHub OIDC/WIF deploy identity restricted to this backend repository
- logs, error/latency alerts, and explicit cost caps

## Repository configuration

Configure a GitHub `development` environment with non-secret variables for project, region, service, runtime identity, and artifact image. Configure WIF identifiers as protected environment/repository values.

The target deployment chain is:

```text
verify -> build -> capture digest -> deploy DEV digest -> smoke test
```

Do not run deploy in parallel with CI or use long-lived service-account JSON in GitHub.

## Verify

- [ ] Active backend profile is `dev`
- [ ] Cloud Run revision uses the recorded digest
- [ ] Runtime identity has DEV-only data/secret access
- [ ] Liveness/readiness succeed
- [ ] Protected endpoint rejects missing token
- [ ] DEV Firebase token reaches `/me`
- [ ] AI key has a low environment budget and no prompt logging
- [ ] DEV mobile preview uses this URL and the same Firebase project

See [COMMON_GCP_SETUP.md](./COMMON_GCP_SETUP.md) for the resource/IAM blueprint and [CI/CD](../docs/cicd_deployment_plan.md) for workflow requirements.
