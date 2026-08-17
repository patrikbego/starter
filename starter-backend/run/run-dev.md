# Run Backend in DEV

DEV uses real Firebase token verification, Firestore, and AI provider configuration. Use a dedicated Firebase/GCP project such as `{app}-dev-<unique-suffix>` and low-cost provider limits.

## Firebase/GCP environment model

A Firebase project is also its underlying Google Cloud project. Create a separate **project** for DEV; adding another Firebase app registration to a local or PROD project does not isolate Authentication users, Firestore data, IAM, billing, quotas, secrets, or Cloud Run.

| Resource | DEV example | Purpose |
|---|---|---|
| Firebase/GCP project | `{app}-dev-<unique-suffix>` | DEV security and billing boundary |
| Firebase Web app registration | `{app}-mobile-dev` | Client configuration for the current Firebase JS SDK |
| Cloud Run service | `{app}-api-dev` | Shared DEV backend |
| Firestore database | `(default)` in the selected DEV region | DEV user/application data only |
| AI provider key | Dedicated DEV key | Separate quotas, rotation, and spend limits |

Before running DEV:

1. Create the DEV project in the [Firebase Console](https://console.firebase.google.com/). Choose the project ID carefully; it is globally unique and cannot be changed after provisioning.
2. Link a billing account if Cloud Run will be used, then configure budget alerts. Billing applies to the whole Firebase/GCP project.
3. Register a **Web app** such as `{app}-mobile-dev` for the current Expo/Firebase JS client and enable its required Authentication providers. The backend does not use this Web app's API key.
4. Create the default Firestore database in a deliberate region close to the backend, for example `europe-west2` when Cloud Run is in London. A Firestore database location cannot be changed after creation.
5. Keep PROD in a completely different Firebase/GCP project.

See Firebase's guidance on [separate projects per environment](https://firebase.google.com/docs/projects/dev-workflows/overview-environments), [project versus app hierarchy](https://firebase.google.com/docs/projects/learn-more), and [Firestore locations](https://firebase.google.com/docs/firestore/locations).

## Option A — local JVM against DEV

Stop any backend already using port `8080`. Configure developer Application Default Credentials and variables outside the repository:

```bash
export DEV_PROJECT_ID={app}-dev-<unique-suffix>
gcloud auth application-default login
gcloud auth application-default set-quota-project "$DEV_PROJECT_ID"

export SPRING_PROFILES_ACTIVE=dev-local
export GCP_PROJECT_ID="$DEV_PROJECT_ID"
export OPENAI_API_KEY=your-dev-provider-key
./mvnw spring-boot:run
```

Despite its current environment-variable name, `OPENAI_API_KEY` contains the DEV key for the configured OpenAI-compatible provider (currently OpenRouter). Set it through a secure shell/password-manager workflow; do not commit it or paste it into documentation.

Prefer Application Default Credentials over a downloaded service-account key when organization policy permits. Use `GOOGLE_APPLICATION_CREDENTIALS` only when a deliberately provisioned DEV credential file is required. Never use PROD credentials.

## Option B — Cloud Run DEV

Target workflow after repository extraction:

```text
verify -> build one image -> capture digest -> deploy digest to DEV -> smoke test
```

Follow [the CI/CD design](../docs/cicd_deployment_plan.md) and setup guides in `scripts/`. The current parent-workspace workflow is a prototype: deploy and CI are not yet one gated chain.

Before using the current workflow, replace its hard-coded `starter-dev` project, service account, and resource names with the real DEV project values or repository-level configuration. Configure Workload Identity Federation and Secret Manager; do not store a service-account JSON key in GitHub.

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
- [ ] Firebase/GCP project is dedicated to DEV, not merely another app registration in a shared project
- [ ] Backend GCP project and mobile Firebase `projectId` identify the same DEV project
- [ ] Runtime identity cannot access PROD data/secrets
- [ ] Firestore location is intentional and recorded before database creation
- [ ] AI key has DEV limits/budget alerts
- [ ] Deployment records image digest and Cloud Run revision
- [ ] Unauthenticated protected request returns `401`
