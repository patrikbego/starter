# Build and Release Mobile PROD

Production uses a store-signed build with PROD Firebase and API configuration. Build once per platform, test that store artifact through TestFlight/Play internal testing, then release the same artifact.

## PROD Firebase project and app

Create a dedicated Firebase/GCP project such as `{app}-prod-<unique-suffix>` and mark it as production in the Firebase Console. Register a Web app such as `{app}-mobile-prod` inside that project for the current Firebase JavaScript SDK.

Do not register the PROD app inside the DEV Firebase project. App registrations in one project still share Authentication users, Firestore data, IAM, billing, quotas, and other backend resources.

The production build must use:

| Variable | PROD value/source |
|---|---|
| `APP_ENV` | `production` |
| `API_BASE_URL_PROD` | HTTPS URL of the PROD backend |
| `EXPO_PUBLIC_FIREBASE_API_KEY` | PROD Web app `firebaseConfig.apiKey` |
| `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN` | PROD Web app `firebaseConfig.authDomain` |
| `EXPO_PUBLIC_FIREBASE_PROJECT_ID` | Dedicated PROD project ID |

The mobile Firebase project ID must match the backend's PROD Firebase/GCP project. Public Firebase client configuration may be embedded in the app; Admin SDK credentials, service accounts, AI keys, and other server secrets must not be included.

## Preconditions

- Compatible backend contract is deployed to PROD without breaking the released app.
- Production build-time validation rejects DEV/localhost identifiers.
- Store metadata, privacy disclosures, signing, and review requirements are ready.
- Protected GitHub/EAS production access is configured.
- PROD Firebase/GCP project, Authentication providers, authorized domains, and controlled test account are configured independently from DEV.
- EAS production environment contains only the PROD API/Firebase pairing.

## Build release candidate

```bash
npm ci
npm run lint
npx tsc --noEmit
eas build --profile production --platform all --non-interactive
```

Record commit, contract version, iOS/Android EAS build IDs, app versions/build numbers, and runtime version.

## Submit to testing

Upload those existing production build IDs with the protected submit workflow/EAS Submit. Use TestFlight and Play internal testing for real-device validation.

Verify:

- production API/Firebase pairing;
- login restoration and token refresh;
- `/me` and stateless AI flow;
- offline/provider/rate-limit errors;
- analytics/privacy/permissions as applicable;
- supported OS/device behavior.

## Release

After approval, promote/release the same tested store binary through App Store Connect and Play Console. Do not rebuild between testing and release.

## Rollback

- Halt a phased rollout when possible.
- Keep the backend compatible with the previous app version.
- For compatible JavaScript-only issues, use the tested EAS Update rollback/republish process if enabled.
- Native/config/plugin changes require a new build and review.

See Firebase's guidance on [separate projects per environment](https://firebase.google.com/docs/projects/dev-workflows/overview-environments) and [configuring multiple projects](https://firebase.google.com/docs/projects/multiprojects).
