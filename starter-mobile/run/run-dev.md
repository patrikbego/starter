# Build Mobile for DEV QA

DEV QA uses an EAS preview/internal build configured only for DEV Firebase and the DEV backend.

## DEV Firebase project and app

DEV must have its own Firebase/GCP project such as `{app}-dev-<unique-suffix>`. Do not implement DEV by registering another Firebase app inside the PROD project: Authentication users, Firestore, IAM, billing, secrets, and quotas would still be shared.

Inside the DEV project, register a Web app such as `{app}-mobile-dev`. The current Expo client uses the Firebase JavaScript SDK and reads that Web app's configuration object. Firebase Hosting is not required. Apple/Android Firebase registrations are only needed later if a native Firebase integration requires them.

The mobile `EXPO_PUBLIC_FIREBASE_PROJECT_ID` and backend `GCP_PROJECT_ID` must name the same DEV Firebase/GCP project.

## Preconditions

- DEV backend deployed and healthy
- DEV API contract compatible with this mobile commit
- DEV Firebase sign-in provider/test user configured
- EAS project linked and `EXPO_TOKEN` configured for CI
- DEV EAS environment variables populated

Configure the preview environment with public client configuration from the DEV Web app and the deployed DEV backend:

| Variable | DEV value/source |
|---|---|
| `APP_ENV` | `development` |
| `API_BASE_URL_DEV` | HTTPS URL of the DEV Cloud Run service |
| `EXPO_PUBLIC_FIREBASE_API_KEY` | DEV Web app `firebaseConfig.apiKey` |
| `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN` | DEV Web app `firebaseConfig.authDomain` |
| `EXPO_PUBLIC_FIREBASE_PROJECT_ID` | Dedicated DEV project ID |
| `EAS_PROJECT_ID` | EAS project created/linked for this mobile repository |

The Firebase client values are public configuration embedded in the app; service-account files, Admin SDK credentials, and AI keys must never be placed in `EXPO_PUBLIC_*` variables. Store the preview values in the EAS `preview` environment rather than relying on a developer's local `.env`.

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

See Firebase's guidance on [separate projects per environment](https://firebase.google.com/docs/projects/dev-workflows/overview-environments) and [configuring multiple projects](https://firebase.google.com/docs/projects/multiprojects).
