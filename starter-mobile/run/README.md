# How to Run the Mobile App

This folder explains how to run the **starter mobile app** — an Expo (React Native) client that connects to the backend API.

If you have never worked on this project before, read this page first, then open the guide for the environment you need.

---

## What is the mobile app?

The mobile app is what users install on their phone. It:

- Shows a **login screen** (Firebase email/password)
- Shows a **home screen** with the user's profile and backend health status
- Shows a **chat screen** to talk to the AI

The app does **not** talk to the database directly. It sends HTTP requests to the backend API, which handles auth, data, and AI.

The code lives in `starter-mobile/` inside the monorepo.

---

## Three environments (read this first)

Every project uses the same three stages. **Always test in order: Local → DEV → PROD.**

```text
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   LOCAL     │ ──► │     DEV     │ ──► │    PROD     │
│ Expo on Mac │     │ EAS internal│     │ App Store   │
│ + simulator │     │ test build  │     │ / Play Store│
└─────────────┘     └─────────────┘     └─────────────┘
```

| Environment | Guide | Who uses it | How to get the app |
|-------------|-------|-------------|-------------------|
| **Local** | [run-local.md](./run-local.md) | Developers on macOS | `npx expo start` → simulator or Expo Go |
| **DEV** | [run-dev.md](./run-dev.md) | Team QA, testers | EAS internal build (install link) |
| **PROD** | [run-prod.md](./run-prod.md) | Real users | App Store / Google Play |

**Golden rule:** Test on DEV cloud before submitting to app stores.

---

## Which guide should I open?

| I want to… | Open |
|------------|------|
| Run the app on my Mac for the first time | [run-local.md](./run-local.md) |
| Install a test build on my phone (cloud backend) | [run-dev.md](./run-dev.md) |
| Submit to App Store / Play Store | [run-prod.md](./run-prod.md) |
| Run the backend API too | [../starter-backend/run/README.md](../starter-backend/run/README.md) |

---

## Backend must be running first

The mobile app cannot work alone. Before running the app, make sure a backend is available:

| Mobile guide | Backend you need | Backend guide |
|--------------|------------------|---------------|
| [run-local.md](./run-local.md) | Backend on your Mac **or** DEV cloud | [../starter-backend/run/run-local.md](../starter-backend/run/run-local.md) or [run-dev.md](../starter-backend/run/run-dev.md) |
| [run-dev.md](./run-dev.md) | DEV cloud backend deployed | [../starter-backend/run/run-dev.md](../starter-backend/run/run-dev.md) |
| [run-prod.md](./run-prod.md) | PROD cloud backend deployed | [../starter-backend/run/run-prod.md](../starter-backend/run/run-prod.md) |

---

## Repository layout (where things live)

```text
starter/                          ← monorepo root (clone this repo)
├── starter-mobile/               ← you are here
│   ├── run/                      ← these guides
│   ├── app/                      ← screens (login, home, chat)
│   ├── src/                      ← API client, auth, hooks
│   ├── .env.example              ← copy to .env for local dev
│   └── app.config.ts             ← build-time configuration
└── starter-backend/              ← API server (separate run/ guides)
```

---

## Accounts you may need

| Account | Needed for | Sign up |
|---------|------------|---------|
| None (beyond npm) | Local with Expo Go | — |
| Firebase (`starter-dev`) | Login on local and DEV | Ask team lead or [Firebase Console](https://console.firebase.google.com) |
| Expo | DEV and PROD builds | [expo.dev](https://expo.dev) |
| Apple Developer | iOS PROD submit | [developer.apple.com](https://developer.apple.com) |
| Google Play Console | Android PROD submit | [play.google.com/console](https://play.google.com/console) |

---

## Typical first-day path for a new developer

1. **Clone the repo** and install Node.js 22+
2. **Start the backend** locally — follow [../starter-backend/run/run-local.md](../starter-backend/run/run-local.md) Option A
3. **Configure mobile** — copy `.env.example` to `.env`, fill in Firebase keys (ask team lead)
4. **Run the app** — `npx expo start`, press `i` for iOS simulator
5. **Sign up / sign in** on the login screen
6. Confirm home and chat screens work
7. Read [run-dev.md](./run-dev.md) when you need an installable test build

---

## Critical pairing rule

The mobile app and backend must use the **same Firebase project** and the **matching API URL**:

| Mobile `APP_ENV` | Firebase project | API URL variable |
|------------------|------------------|------------------|
| `development` | `starter-dev` | `API_BASE_URL_DEV` |
| `production` | `starter-prod` | `API_BASE_URL_PROD` |

Mixing DEV Firebase with PROD API (or the reverse) causes **401 errors** on every request.

---

## Glossary

| Term | Meaning |
|------|---------|
| **Expo** | Framework for building React Native apps |
| **Expo Go** | Phone app for running your project during local dev |
| **EAS** | Expo Application Services — builds installable apps in the cloud |
| **EAS profile** | Build configuration: `preview` (DEV), `production` (PROD) |
| **`.env`** | Local config file — never commit secrets to git |
| **Simulator** | Virtual iPhone/Android on your Mac |

---

## Related docs

- [../README.md](../README.md) — mobile overview
- [../docs/BACKEND_INTEGRATION.md](../docs/BACKEND_INTEGRATION.md) — how mobile talks to the API
- [../../docs/ENVIRONMENT_MATRIX.md](../../docs/ENVIRONMENT_MATRIX.md) — all environment variables
