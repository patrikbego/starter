# Run Backend in DEV

DEV uses real Firebase, Firestore, and AI provider configuration. Use a dedicated `{app}-dev` project and low-cost provider limits.

## Option A — local JVM against DEV

Configure developer Application Default Credentials and variables outside the repository:

```bash
export SPRING_PROFILES_ACTIVE=dev-local
export GCP_PROJECT_ID=starter-dev
export GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/dev-service-account.json
export OPENAI_API_KEY=your-dev-provider-key
./mvnw spring-boot:run
```

Prefer `gcloud auth application-default login` over a downloaded key when organization policy permits. Never use PROD credentials.

## Option B — Cloud Run DEV

Target workflow after repository extraction:

```text
verify -> build one image -> capture digest -> deploy digest to DEV -> smoke test
```

Follow [the CI/CD design](../docs/cicd_deployment_plan.md) and setup guides in `scripts/`. The current parent-workspace workflow is a prototype: deploy and CI are not yet one gated chain.

## Verify

```bash
curl -i https://DEV_API_URL/actuator/health
```

For an authenticated request, obtain a DEV Firebase ID token through a controlled test client/account and call:

```bash
curl -i https://DEV_API_URL/api/me \
  -H 'Authorization: Bearer DEV_FIREBASE_ID_TOKEN'
```

Do not paste tokens into committed scripts or issue trackers.

## DEV checklist

- [ ] Active profile is `dev-local` or `dev`, never `local`
- [ ] GCP project and Firebase issuer are DEV
- [ ] Runtime identity cannot access PROD data/secrets
- [ ] AI key has DEV limits/budget alerts
- [ ] Deployment records image digest and Cloud Run revision
- [ ] Unauthenticated protected request returns `401`
