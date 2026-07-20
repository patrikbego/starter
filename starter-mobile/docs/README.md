# Mobile Documentation

This directory is self-contained within the mobile repository.

| Document | Purpose |
|---|---|
| [mobile_architecture_plan.md](./mobile_architecture_plan.md) | Target client boundaries, state, auth, configuration, and extension rules |
| [BACKEND_INTEGRATION.md](./BACKEND_INTEGRATION.md) | Versioned API contract, token behavior, errors, and compatibility |
| [mobile_cicd_deployment_plan.md](./mobile_cicd_deployment_plan.md) | Independent CI, preview builds, store release candidates, OTA, and rollback |
| [mobile_mvp_scope_checklist.md](./mobile_mvp_scope_checklist.md) | Prototype status and template-v1 exit criteria |
| [mobile_ui_integration_plan.md](./mobile_ui_integration_plan.md) | Ordered implementation work |

## Documentation labels

- **Prototype** means code present today.
- **Target v1** means required before this repository should be used as a production template.
- **Extension** means product-specific and excluded from starter core.

The backend is an independent repository. Cross-repository integration is described through the backend's published OpenAPI contract, not filesystem links.

Expo APIs must be checked against the exact SDK version pinned by `package.json`. Upgrade the SDK deliberately and update this documentation in the same change.
