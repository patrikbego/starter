# Run Mobile Locally

## Prerequisites

- Supported Node/npm for the pinned Expo SDK
- iOS Simulator, Android emulator, or Expo-compatible physical-device workflow
- DEV Firebase web configuration with email/password enabled
- Local or DEV backend URL

## Environment boundary

The current mobile client has no mock-auth adapter, so local Expo builds use the real **DEV Firebase project** for sign-up and sign-in. Do not create a second Firebase app inside PROD for local use, and never point a local build at PROD.

A Firebase project is the environment boundary. A Firebase app is only a platform registration inside that project. The recommended structure is:

| Environment | Firebase/GCP project | Current client registration |
|---|---|---|
| Local mobile + DEV | `{app}-dev-<unique-suffix>` | Web app `{app}-mobile-dev` |
| PROD | `{app}-prod-<unique-suffix>` | Web app `{app}-mobile-prod` |

Because this prototype uses the Firebase JavaScript SDK, it currently consumes a Web app configuration even when Expo targets mobile. Add Apple/Android Firebase registrations later only when a native Firebase product or SDK requires them.

## Configure

```bash
cp .env.example .env
```

Set:

```text
APP_ENV=development
API_BASE_URL_DEV=http://localhost:8080
EXPO_PUBLIC_FIREBASE_API_KEY=...
EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN={app}-dev-<unique-suffix>.firebaseapp.com
EXPO_PUBLIC_FIREBASE_PROJECT_ID={app}-dev-<unique-suffix>
```

### Get the Firebase configuration

Use the dedicated DEV Firebase project; do not connect local builds to the production project.

1. Open the [Firebase Console](https://console.firebase.google.com/) and create or select the dedicated DEV project. Creating only another app registration inside a shared project is not environment isolation.
2. From **Project overview**, select **Add app**, then select the **Web** (`</>`) platform and use a nickname such as `{app}-mobile-dev`. This Expo app uses the Firebase JavaScript SDK, so it needs the web configuration object. Firebase Hosting is not required.
3. Register the app. To retrieve the configuration later, open **Project settings** (the gear icon) -> **General** -> **Your apps**, select the web app, then choose **SDK setup and configuration** -> **Config**.
4. Copy these fields from the displayed `firebaseConfig` object into `.env`:

   | Firebase field | `.env` variable |
   |---|---|
   | `apiKey` | `EXPO_PUBLIC_FIREBASE_API_KEY` |
   | `authDomain` | `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN` |
   | `projectId` | `EXPO_PUBLIC_FIREBASE_PROJECT_ID` |

5. In the Firebase Console, open **Authentication** -> **Sign-in method**, enable **Email/Password**, and save it.

Firebase's official references cover [registering a web app](https://firebase.google.com/docs/web/setup), [finding an existing app's configuration object](https://support.google.com/firebase/answer/7015592), and [enabling email/password authentication](https://firebase.google.com/docs/auth/web/password-auth).

Firebase recommends a [separate project for each environment](https://firebase.google.com/docs/projects/dev-workflows/overview-environments). All apps registered in one project share that project's backend resources and security boundary.

These `EXPO_PUBLIC_*` values are client configuration and will be visible in the built app. The Firebase API key identifies the Firebase project; it is not an Admin SDK credential. Never put a service-account JSON file, private key, or server secret in `.env` variables prefixed with `EXPO_PUBLIC_`. See Firebase's [API key guidance](https://firebase.google.com/docs/projects/api-keys).

For a physical device, `localhost` refers to the device. Use the computer's reachable LAN address only on a trusted network, or prefer an HTTPS DEV/tunnel URL.

The current app uses real DEV Firebase client auth even when the backend uses its local mock verifier. The backend will therefore display a generated `Local Developer` identity rather than the Firebase profile. Firebase Auth emulator support is not wired into the current mobile adapter.

## Start

```bash
npm ci
npx expo start
```

## Verify manually

1. Sign up/sign in with the configured DEV Firebase project.
2. Home loads `/api/me` and health.
3. Chat returns the backend's stateless response.
4. Sign out returns to login and clears protected query data.

Cold-start auth persistence still needs explicit implementation/verification before template v1.
