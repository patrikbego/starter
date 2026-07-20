# Google Cloud Resource Blueprint

This is the target resource and IAM blueprint for each product. Implement it with versioned OpenTofu/Terraform (or an equivalently idempotent tool) before template v1. Manual console/CLI setup is useful for exploration but does not scale reliably across many apps.

## Required APIs/resources

| Resource | Purpose |
|---|---|
| Cloud Run | Backend runtime |
| Firestore | Starter user record and product data |
| Secret Manager | Runtime provider/admin secrets |
| Artifact Registry | Immutable container artifacts |
| IAM Credentials / WIF | Short-lived GitHub deployment identity |
| Cloud Logging/Monitoring | Logs, metrics, alerts |

Firebase Authentication is linked to the environment's GCP project. Cloud Storage/Tasks/Document AI are extensions and remain disabled until a product needs them.

## Resource ownership

Use separate `{app}-dev` and `{app}-prod` runtime/data projects. Select Firestore/data region before provisioning. For immutable backend promotion, use a release Artifact Registry that both environments can read at repository scope, or copy the tested digest without rebuilding and verify digest equality.

## Identities

| Identity | Permissions |
|---|---|
| Runtime API | Firestore data access required by app; access to named runtime secrets; extension permissions only when used |
| Image publisher | Push image/attestations to release repository |
| DEV deployer | Deploy DEV service; act as DEV runtime identity; read release image |
| PROD deployer | Deploy PROD service after approval; act as PROD runtime identity; read release image |
| Cloud Run service agent | Read the chosen image repository, including cross-project grant when needed |

Do not grant Artifact Registry writer to the runtime identity. Do not grant project-wide Owner/Editor. Avoid broad `run.admin` or project-wide `iam.serviceAccountUser` when narrower service/account permissions meet the need.

## GitHub OIDC/WIF

- Trust only `https://token.actions.githubusercontent.com`.
- Map repository identity and apply an attribute condition for the exact owner/repository.
- Restrict DEV/PROD by protected branch/environment claims as supported.
- Grant `roles/iam.workloadIdentityUser` only to the intended principal set.
- Use separate deploy service accounts for DEV and PROD.
- Store provider/service-account identifiers as configuration; store no JSON key.

GitHub's current guidance is linked from [the CI/CD document](../docs/cicd_deployment_plan.md).

## Secrets

Create separate environment values for:

- AI provider key;
- actuator/operations credential only if retained;
- future integration secrets.

Grant runtime access to individual secrets where practical. Define rotation owner and verify a new version before disabling the old one.

## Firestore

- Native mode and deliberately selected location
- environment-specific database/project
- indexes declared/versioned with the product
- deletion protection/backup/export policy appropriate to PROD
- runtime access only; mobile does not directly access starter server-owned collections unless a product explicitly designs Firebase rules for it

## Required outputs

Infrastructure automation should output:

```text
project ID
region
Cloud Run service name/URL
runtime service account
Artifact Registry image path
WIF provider and deploy service account identifiers
Firebase project ID
```

Do not output secret values.

## Validation

- [ ] Second apply is idempotent
- [ ] DEV/PROD plans show no cross-environment data access
- [ ] CI can authenticate without a key
- [ ] Runtime can read only required data/secrets
- [ ] PROD can deploy the tested digest from the release repository
- [ ] Budget/alert ownership is configured
- [ ] Destroy behavior protects production state/data
