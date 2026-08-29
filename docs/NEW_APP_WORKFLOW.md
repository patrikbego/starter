# New App Workflow

Use this process after the backend and mobile templates have independent repositories and tagged releases. Creating a product from arbitrary `main` branches is intentionally unsupported.

## Inputs to decide first

| Input | Example | Rule |
|---|---|---|
| App slug | `myvault` | Lowercase; stable across cloud resources |
| Display name | `MyVault` | User-facing; can change later |
| Java base package | `com.example.myvault` | Reverse-domain namespace |
| iOS bundle ID | `com.example.myvault` | Unique and immutable after store release |
| Android application ID | `com.example.myvault` | Unique and immutable after store release |
| Backend template tag | `v1.0.0` | Record in product docs |
| Mobile template tag | `v1.0.0` | Must support the backend contract version |
| GCP region/data residency | `europe-west2` / chosen Firestore location | Choose before data exists |

## Prerequisites

- GitHub organization with permission to create and protect repositories
- Google Cloud billing/project permissions
- Firebase access
- Expo/EAS account
- Apple Developer and Google Play accounts before store delivery
- AI provider account with separate DEV and PROD limits/keys

## 1. Create two product repositories

Derive each product by cloning the template **with git history** so starter improvements can
later flow in as a normal `git merge upstream/main` (see [Upstream sync](./UPSTREAM_SYNC.md)).
Do not use *Use this template* — it strips history and breaks merge-based propagation.

```text
myvault-backend   <- starter-backend release vX.Y.Z (clone, history preserved)
myvault-mobile    <- starter-mobile release vA.B.C  (clone, history preserved)
```

```bash
gh repo create patrikbego/myvault-backend --private
git clone git@github.com:patrikbego/starter-backend.git myvault-backend
cd myvault-backend
git remote rename origin upstream
git remote add origin git@github.com:patrikbego/myvault-backend.git
git checkout -B main vX.Y.Z
git push -u origin main
```

Repeat for `myvault-mobile` from `starter-mobile` at tag `vA.B.C`. `origin` is the product
repository; `upstream` is the template repository and stays read-only.

In each product README, record:

```text
Created from starter-backend vX.Y.Z (commit <sha>) — upstream = patrikbego/starter-backend
Created from starter-mobile vA.B.C (commit <sha>) — upstream = patrikbego/starter-mobile
Supported API contract: v1
```

Do not copy the parent `starter` workspace and do not preserve sibling-relative links.

## 2. Rename and validate

Backend checklist:

- [ ] Maven coordinates, application name, Java package, and source directory
- [ ] Configuration prefix such as `myvault.*`
- [ ] Cloud Run service and runtime service account names
- [ ] OpenAPI title/server examples; retain `/api/v1` compatibility
- [ ] Logging namespace and test packages

Mobile checklist:

- [ ] npm package, Expo name/slug, URL scheme
- [ ] iOS bundle ID and Android application ID
- [ ] EAS project ID
- [ ] Icons, splash screen, app name, colors, and legal URLs
- [ ] Backend contract pin and generated/validated types

Run a repository-wide search for `starter`, `com.starter`, and placeholder values. Review every match; do not blindly replace prose or dependency names.

## 3. Provision DEV and PROD

Use the backend repository's versioned infrastructure code. Provision each environment separately:

- GCP/Firebase project
- Required APIs
- Firestore and indexes
- Cloud Run service identity
- Artifact Registry access
- Secret Manager secrets and IAM bindings
- GitHub Workload Identity Federation
- Budgets, logs, alerts, and retention policy

Never point a local or DEV identity at production resources during bootstrap.

## 4. Configure Firebase

For both `{app}-dev` and `{app}-prod`:

1. Link Firebase to the corresponding GCP project.
2. Enable only required sign-in providers.
3. Register iOS, Android, and web configurations as needed.
4. Configure authorized domains, email templates, and password policy.
5. Place public client configuration in the matching EAS environment.
6. Verify backend Admin SDK credentials resolve to the same project.

## 5. Configure backend delivery

In the backend GitHub repository:

1. Add DEV and protected PROD environments.
2. Configure OIDC/WIF identities; do not upload service-account JSON keys.
3. Set the non-secret app identity as **repo variables** (`PROJECT_ID_DEV`, `PROJECT_ID_PROD`, `ARTIFACT_REPOSITORY`, `IMAGE_NAME`; optional `REGION`, `CORS_ALLOWED_ORIGINS_PROD`) — the template deploy workflows read `vars.*`; never edit the workflow files.
4. Store runtime provider keys and admin credentials in Secret Manager.
5. Require CI before merge and before deployment.
6. Configure production approval and prevent self-approval where the GitHub plan supports it.

First deployment sequence:

```text
merge -> tests -> build image -> capture digest -> deploy DEV digest -> smoke test
manual approval -> deploy the same digest to PROD -> smoke test
```

## 6. Configure mobile delivery

In the mobile GitHub repository:

1. Create/link the EAS project.
2. Configure `development`, `preview`, and `production` build profiles.
3. Store DEV and PROD public configuration in matching EAS environments.
4. Add `EXPO_TOKEN` and protect store-submission workflows.
5. Configure separate app variants if DEV and PROD builds must coexist on devices.
6. Configure runtime-version/update channels if using EAS Update.

Artifact rule:

- Preview builds use DEV config and support rapid internal QA.
- Production store builds use PROD config and store signing.
- Test the store build through TestFlight/Play internal testing, then release that same store binary.
- Never submit a DEV/internal preview artifact as production.

## 7. Verify the starter loop

From clean checkouts and a real device:

1. Backend CI passes.
2. DEV backend readiness is healthy.
3. DEV mobile build signs in to DEV Firebase.
4. `GET /api/v1/me` creates/returns only that authenticated user's record.
5. `POST /api/v1/ai/chat` returns a bounded, stateless response.
6. Invalid/expired tokens return the standard `401` error and the client retries once.
7. AI quotas and error paths are visible without logging prompt content.
8. Production deployment and store release remain approval-gated.

## 8. Add product logic

Backend additions go through domain/application/port boundaries. Mobile additions go under feature modules and consume the API contract. Avoid editing platform code until the product requirement genuinely changes it.

For every new domain resource:

- scope reads/writes by authenticated user or tenant;
- document lifecycle, retention, and deletion;
- add contract and authorization tests;
- add metrics without sensitive payloads;
- decide idempotency and retry behavior;
- update the OpenAPI contract before mobile integration.

## 9. Release and rollback record

Record at minimum:

| Backend | Mobile |
|---|---|
| Git commit and image digest | Git commit and EAS build ID |
| Cloud Run revision | iOS/Android version and build number |
| Database/contract version | Supported backend contract version |
| Approver and timestamp | Store track, approver, and timestamp |

## Common failures

| Symptom | Likely cause |
|---|---|
| `401` from `/me` | Mobile and backend use different Firebase projects |
| App build starts with missing config | `app.config.ts` validation is incomplete |
| AI call returns `502` | Provider secret, timeout, quota, or provider outage |
| DEV deploy occurs despite failed tests | Deployment workflow is not dependent on CI |
| PROD differs from DEV | Workflow rebuilt a tag instead of promoting a digest |
| Store submission rejects build | Internal preview artifact used instead of store build |

## Related documents

- [Repository strategy](./REPOSITORY_STRATEGY.md)
- [Upstream sync](./UPSTREAM_SYNC.md) — how starter improvements flow into this app later
- [Environment matrix](./ENVIRONMENT_MATRIX.md)
- [Implementation roadmap](./IMPLEMENTATION_ROADMAP.md)
- [Project review](./REVIEW_FINDINGS.md)
