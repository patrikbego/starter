# Run the Mobile App

Choose one workflow:

| Guide | Build/config | Purpose |
|---|---|---|
| [run-local.md](./run-local.md) | Local Expo + DEV Firebase + local/DEV backend | Development |
| [run-dev.md](./run-dev.md) | EAS preview + DEV services | Shared QA |
| [run-prod.md](./run-prod.md) | Store production build + PROD services | Release |

This guide assumes `starter-mobile` is the repository root. The backend is an independently deployed service; obtain its URL and supported contract version from the backend release record.

DEV and PROD use separate Firebase/GCP projects. The current Firebase Web app registrations live inside those projects; creating multiple app registrations in one project does not isolate environments.

## Fastest path

```bash
cp .env.example .env
npm ci
npx expo start
```

The current prototype expects `API_BASE_URL_DEV`/`API_BASE_URL_PROD`. Target v1 consolidates this through validated EAS environment values as described in [the architecture](../docs/mobile_architecture_plan.md).

## Verify

```bash
npm run lint
npx tsc --noEmit
```

The current dual-architecture local setup can make `expo lint` fail to load `unrs-resolver` even after a clean install. Direct `eslint . --no-cache` passes, but template v1 must pin one supported Node toolchain and make the repository's standard `npm run lint` command deterministic. Do not edit valid application imports to mask the resolver/toolchain failure.
