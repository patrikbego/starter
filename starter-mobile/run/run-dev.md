# Build Mobile for DEV QA

DEV QA uses an EAS preview/internal build configured only for DEV Firebase and the DEV backend.

## Preconditions

- DEV backend deployed and healthy
- DEV API contract compatible with this mobile commit
- DEV Firebase sign-in provider/test user configured
- EAS project linked and `EXPO_TOKEN` configured for CI
- DEV EAS environment variables populated

## Build

```bash
npm ci
npm run lint
npx tsc --noEmit
eas build --profile preview --platform all --non-interactive
```

Install via EAS internal distribution and record both platform build IDs.

## Device smoke test

- app displays expected DEV identity/diagnostic marker;
- sign in uses DEV Firebase;
- `/me` uses DEV API and returns the same identity;
- AI request succeeds or maps provider/rate-limit errors correctly;
- invalid session refreshes once and then signs out;
- no production data/account is used.

The preview artifact is for QA only. It is not uploaded as the production store binary.
