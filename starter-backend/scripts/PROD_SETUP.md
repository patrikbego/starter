# PROD Environment Bootstrap

Provision PROD independently through reviewed infrastructure code. Never clone DEV credentials/data or grant the DEV workflow production access.

## PROD resources

- `{app}-prod` GCP/Firebase project and production billing alerts
- Firestore in the chosen residency with deletion/backup policy
- PROD Cloud Run service and runtime identity
- PROD Secret Manager values and separate AI budget/key
- read access to the immutable release artifact repository
- PROD GitHub OIDC/WIF deploy identity
- protected GitHub `production` environment with approval
- alerts, incident ownership, and rollback access

## Promotion policy

Production workflow receives an eligible image digest that already passed DEV. It does not build source. The approver checks commit, digest, contract version, DEV result, data compatibility, and mobile compatibility.

## Mobile coordination

A DEV preview binary is not a production artifact. The mobile repository creates a store-signed production candidate using PROD configuration, tests it through TestFlight/Play internal testing, then releases that same store binary.

## Checklist

- [ ] PROD cannot load local mocks
- [ ] Firebase/API pairing is PROD-only
- [ ] CORS contains exact required web origins
- [ ] Public health exposes no build/config detail
- [ ] Runtime and deploy identities have least privilege
- [ ] Provider/data/log retention policies are approved
- [ ] Backend promotion uses exact tested digest
- [ ] Store client and backend API versions are compatible
- [ ] Backend and mobile rollback drills completed

See [DEV_SETUP.md](./DEV_SETUP.md), [COMMON_GCP_SETUP.md](./COMMON_GCP_SETUP.md), and [CI/CD](../docs/cicd_deployment_plan.md).
