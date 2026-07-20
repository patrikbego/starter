# Build and Release Mobile PROD

Production uses a store-signed build with PROD Firebase and API configuration. Build once per platform, test that store artifact through TestFlight/Play internal testing, then release the same artifact.

## Preconditions

- Compatible backend contract is deployed to PROD without breaking the released app.
- Production build-time validation rejects DEV/localhost identifiers.
- Store metadata, privacy disclosures, signing, and review requirements are ready.
- Protected GitHub/EAS production access is configured.

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
