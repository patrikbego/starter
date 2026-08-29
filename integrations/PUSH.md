# PUSH.md — Push Notifications Integration (Expo/FCM/APNs)

**Status: designed, not implemented.** Neither template ships push today — push notifications are
an explicit non-goal of starter core (mobile `docs/mobile_mvp_scope_checklist.md`, backend
non-goals list). This runbook is the activation plan for a product app that needs push: what to
add, where it must live, and the rules that keep the template's security model intact. Same shape
as [`RESEND.md`](./RESEND.md) and [`STRIPE.md`](./STRIPE.md), but with **nothing pre-wired** —
every piece below is a deliberate addition.

Positioning: **the backend is authoritative for push**, exactly as it is for email and billing.
The mobile client's only job is to obtain the OS/expo push token after authentication and keep the
backend's registry current. Product logic decides *what* to notify about; the template only
decides *how* tokens are stored and how provider calls are made.

## Architecture (target)

```text
mobile (expo-notifications)                     backend (starter-push extension)
  getExpoPushTokenAsync()                            │
      │  POST /api/v1/push/token  (uid from ID token)│
      └──────────────────────────────────────────────▶│ PushTokenPort ──▶ Firestore token registry
                                                      │
   device ◀──APNs──┐                                  │ PushPort.send(PushMessage)
   device ◀──FCM ──┴── Expo push service ◀── REST ───┘ (RestClient; receipts polled)
```

- One authenticated route pair registers/unregisters the device token. Identity comes only from
  the verified Firebase ID token (backend security rule 1) — the request body never names a user.
- Sending is a backend-internal capability behind a port — **no product route ever triggers a push
  on the user's behalf**; use cases call it as a side effect, mirroring `WelcomeEmailService`.
- Expo's push service abstracts APNs (iOS) and FCM v1 (Android) behind one REST API.

## Decision point — Expo push service vs direct FCM/APNs

**Template default: Expo push service.** One endpoint for both platforms, receipts for delivery
state, no platform credential parsing in backend code. Go direct (FCM v1 HTTP + APNs) only if a
product needs FCM topics/device-group fan-out at scale or must avoid the third-party hop — that
is a per-product fork of `PushPort`, not a template change. Firebase **Auth** keeps using the same
Firebase project; push credentials (FCM service account) live in the same project.

⚠️ Remote push does **not** work in Expo Go on recent SDKs — a development build (EAS) is
required. Verify against the SDK pinned in `package.json` before planning the build flow
(mobile repo rule: check every Expo API against the exact pinned SDK version).

## Where the implementation will live (to create, per piece)

| Piece | Path to create | Mirrors |
|---|---|---|
| Port | `starter-backend/.../ports/PushPort.java` + `PushTokenPort.java` | [`EmailPort.java`](../starter-backend/src/main/java/com/starter/ports/EmailPort.java) |
| Config binding (fail-closed) | `starter-backend/.../config/PushConfig.java` | [`EmailConfig.java`](../starter-backend/src/main/java/com/starter/config/EmailConfig.java) |
| Expo adapter (HTTP lives only here) | `starter-backend/.../adapters/expo/ExpoPushAdapter.java` | [`ResendEmailAdapter.java`](../starter-backend/src/main/java/com/starter/adapters/resend/ResendEmailAdapter.java) |
| Local mock adapter | `starter-backend/.../adapters/expo/MockPushAdapter.java` | `MockEmailAdapter` (`local` only) |
| Token registry | `starter-backend/.../adapters/firestore/FirestorePushTokenAdapter.java` | [`FirestoreUserRepositoryAdapter`](../starter-backend/src/main/java/com/starter/adapters/gcp/) |
| Register/unregister routes | `starter-backend/.../api/PushTokenController.java` | `MeController` (principal-derived uid) |
| Terraform secret grant | `infra/main.tf` → `local.secrets` (`fcm-service-account`) | `resend-api-key` entries |
| Secret-creation flag | `infra/scripts/set-secrets.sh` (`--fcm-service-account`) | `--resend-api-key` case |
| Deploy steps (DEV/PROD) | `deploy-dev.yml` / `promote-prod.yml` "Configure push extension" | email/billing steps (`--update-*` only) |
| Mobile: token acquisition + registration | `starter-mobile/src/features/push/` + adapter method | `src/features/profile/` |
| Mobile: permission UX | `usePushPermissions` hook (ask in context, never at first launch) | `AuthGate` pattern |
| Design & security notes | `starter-backend/docs/PUSH_EXTENSION.md` | [`EMAIL_EXTENSION.md`](../starter-backend/docs/EMAIL_EXTENSION.md) |

## Contract surface (required — this is a contract change)

Registering a token needs two authenticated routes; nothing else enters the contract:

```yaml
PUT    /api/v1/push/token   # idempotent register: {token, platform, deviceId?}
DELETE /api/v1/push/token   # unregister: {token} (or by deviceId)
```

- `PUT` is idempotent per (uid, token); re-registers overwrite `platform`/`lastSeenAt`.
- 401/403/4xx reuse the existing envelope; no new error codes.
- **Sequence rule (mirror of the digest-pin flow):** backend `openapi/openapi.yaml` +
  `OpenApiContractTest` first, then copy to mobile (`contract/openapi.yaml`), refresh
  `contract/openapi.yaml.sha256` in the **same commit**, and regenerate/extend
  `src/api/types.ts`. `npm run validate:contract` must pass before push work ships.
- Alternative for a product that wants zero contract change: transport the token inside an
  existing authenticated payload — discouraged; it hides a real entity behind a generic one.

## What needs to be done (per environment)

### 1. Firebase console (Android — FCM v1)

- [ ] Same Firebase project as Auth (per-app; already exists from
      [`FIREBASE_AUTH.md`](./FIREBASE_AUTH.md)).
- [ ] Create a **service account** with the *Firebase Cloud Messaging API Admin* role and download
      the JSON key — this is the FCM v1 credential Expo push sends with.
- [ ] Project ID + package name must match `app.config.ts` (`android.package`).

### 2. Apple (iOS — APNs)

- [ ] Apple Developer account with a **push-capable App ID** (bundle id matches
      `app.config.ts` `ios.bundleIdentifier`).
- [ ] Create an **APNs Auth Key (.p8)**, note its `Key ID` and `teamId`.
- [ ] Upload the `.p8` via `eas credentials` (or the Expo dashboard) per build profile.

### 3. Expo / EAS (mobile repo)

- [ ] `npx expo install expo-notifications`; add its config plugin to `app.config.ts`.
- [ ] Ensure `extra.eas.projectId` is set (Expo push tokens are project-scoped).
- [ ] Development + preview + production builds via EAS — Expo Go cannot receive remote push
      (verify against the pinned SDK).
- [ ] `getExpoPushTokenAsync()` only **after** the auth gate resolves an authenticated user, then
      `PUT /api/v1/push/token`; `DELETE` on sign-out and on token refresh events. Tokens are
      device-scoped — a user may hold several; never assume one.

### 4. Backend secrets & variables (repo: `<app>-backend`)

- [ ] `PushConfig` (to create) fail-closed like `EmailConfig`:
      `starter.push.enabled`, `starter.push.expo-base-url`
      (default `https://exp.host`); FCM credential is only needed if going direct.
- [ ] Non-secret values → GitHub **variables**: `PUSH_ENABLED_DEV=true` (PROD `_PROD` suffix).
- [ ] If direct FCM is chosen: secret `fcm-service-account` via
      `set-secrets.sh dev --fcm-service-account '<json>'` after `terraform apply`.

### 5. Deploy & verify (DEV)

- [ ] `./mvnw verify` green, then deploy; the push step flips Cloud Run env vars with
      `--update-env-vars` **only** (see Rules).
- [ ] Startup proves config: enabled + missing material must fail closed (`PushConfig`).
- [ ] Smoke: register a token from a development build against DEV, verify the Firestore registry
      document (`<uid>/tokens/<hash>`), send one test push from the backend, confirm delivery via
      Expo **push receipts** (`POST /--/api/v2/push/getReceipts`).
- [ ] Uninstall/reinstall + second device cases: registry must hold multiple tokens per uid and
      drop tokens receipts mark `DeviceNotRegistered`.

### 6. Local development

- [ ] `local` profile: `MockPushAdapter` logs the message and returns a fake receipt id — no
      network, no Expo account. Emulator push (APNs/FCM) is possible but not required for template
      work.

### 7. PROD (only after DEV is proven)

- [ ] Separate credentials per environment; `_PROD` variables; same CD/rollback rules as email
      ([`starter-backend/docs/cicd_deployment_plan.md`](../starter-backend/docs/cicd_deployment_plan.md)).

## Rules that must not be broken

- **Push failures never fail the user flow.** Use cases catch `PushProviderException`, log WARN
  with correlationId, continue — same contract as email.
- **Fail-closed activation**: `starter.push.enabled=true` with missing material must fail startup
  (`PushConfig`), never run half-configured.
- **Identity only from the verified principal** on register/unregister; the body may name a
  device, never a user.
- **Never log push tokens, endpoint contents, or message bodies** — tokens are long-lived
  credentials (backend security rule 4). Log `userId`/correlationId + template key + receipt id.
- **No sensitive payloads in push bodies.** Notifications render on lock screens; send a signal,
  not the data — the app fetches the real content over the authenticated API.
- **Firestore rules**: the token registry is user-owned data — deny-all except via the backend,
  like the user collection (existing deny-all posture, roadmap Phase 3).
- **Secrets only in Secret Manager**; deploy steps use `--update-secrets`/`--update-env-vars`
  with the `^|^` delimiter — never `--set-*` (documented Stripe incident: full env wipe, every
  authenticated endpoint 401'd while health stayed green).
- **Mock adapter only under `local`; never deploy `local`.**
- **Contract changes ship with the digest-pin refresh in the same commit** (see Contract surface).

## Maintenance point

The Expo push send/receipts API shape is isolated in the (future) `ExpoPushAdapter` — if Expo
changes it, that file is the only edit. Registry shape (`<uid>/tokens/<tokenHash>`) lives behind
`PushTokenPort`; swap Firestore for anything else without touching use cases. Any new
Firebase-exempt route would be a security decision (none is needed for push).
