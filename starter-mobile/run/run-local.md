# Run Mobile Locally

## Prerequisites

- Supported Node/npm for the pinned Expo SDK
- iOS Simulator, Android emulator, or Expo-compatible physical-device workflow
- DEV Firebase web configuration with email/password enabled
- Local or DEV backend URL

## Configure

```bash
cp .env.example .env
```

Set:

```text
APP_ENV=development
API_BASE_URL_DEV=http://localhost:8080
EXPO_PUBLIC_FIREBASE_API_KEY=...
EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN=starter-dev.firebaseapp.com
EXPO_PUBLIC_FIREBASE_PROJECT_ID=starter-dev
```

For a physical device, `localhost` refers to the device. Use the computer's reachable LAN address only on a trusted network, or prefer an HTTPS DEV/tunnel URL.

The current app uses real Firebase client auth even when the backend uses its local mock verifier. Firebase Auth emulator support is not wired into the current mobile adapter.

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
