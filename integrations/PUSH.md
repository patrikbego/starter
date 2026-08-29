# PUSH.md — Push Notifications Integration (Expo/FCM/APNs)

**Status: implemented in both templates, opt-in.** The backend ships the token
registry and send capability behind `starter.push.enabled=false` (see
[`starter-backend/docs/PUSH_EXTENSION.md`](../starter-backend/docs/PUSH_EXTENSION.md));
the mobile client ships best-effort token registration in
`starter-mobile/src/features/push/`. Product notification UX (what to notify
about, screens, badges) remains a non-goal — that is the part a product app
still builds on top.

Positioning: **the backend is authoritative for push**, exactly as it is for
email and billing. The mobile client's only job is to obtain the Expo push
token after authentication and keep the backend's registry current. Product
logic decides *what* to notify about; the template decides *how* tokens are
stored and how provider calls are made.

## Architecture (as implemented)

```text
mobile (expo-notifications)                     backend (starter-push extension)
  getExpoPushTokenAsync()                            │
      │  PUT /api/v1/push/token  (uid from ID token) │
      └──────────────────────────────────────────────▶│ PushTokenPort ──▶ Firestore token registry
                                                      │      (pushTokens/{uid}/tokens/{derivedDocId})
   device ◀──APNs──┐                                  │
   device ◀──FCM ──┴── Expo push service ◀─ REST ─────┘ PushPort.send(PushMessage)
                        (receipt evaluated on the send response;        │
                         DeviceNotRegistered → prune the token)         │
                                                            use cases ──┘ PushService.sendToUser()
```

- `PUT`/`DELETE /api/v1/push/token` register/unregister the device token.
  Identity comes only from the verified Firebase ID token (backend security
  rule 1) — the request body never names a user.
- Sending is a backend-internal capability behind a port — **no product route
  ever triggers a push on the user's behalf**; use cases call it as a side
  effect, mirroring `WelcomeEmailService`. `sendToUser` never fails the user
  flow: disabled → no-op, dead token → pruned and skipped, other provider
  errors → logged (sanitized) and skipped.
- Expo's push service abstracts APNs (iOS) and FCM v1 (Android) behind one
  REST API (`POST {base}/--/api/v2/push/send`). Receipts are evaluated on the
  synchronous send response — there is no receipts-polling job.

## Where it lives (implemented)

| Piece | Path | Mirrors |
|---|---|---|
| Ports | `starter-backend/.../ports/PushPort.java`, `PushTokenPort.java` | `EmailPort.java` |
| Config (fail-closed) | `starter-backend/.../config/PushConfig.java` | `EmailConfig.java` |
| Expo adapter (HTTP lives only here) | `starter-backend/.../adapters/expo/ExpoPushAdapter.java` | `ResendEmailAdapter.java` |
| Local mocks | `.../adapters/expo/MockPushAdapter.java`, `.../expo/InMemoryPushTokenAdapter.java` (`local` only) | `MockEmailAdapter` |
| Token registry | `starter-backend/.../adapters/gcp/FirestorePushTokenAdapter.java` | `FirestoreUserRepositoryAdapter` |
| Use cases | `starter-backend/.../application/PushService.java` | `WelcomeEmailService` |
| Routes | `starter-backend/.../api/PushTokenController.java` | `MeController` (principal-derived uid) |
| Mobile registration | `starter-mobile/src/features/push/` (`registerPushToken`, `usePushRegistration`, `PushRegistrationEffect` mounted in `app/_layout.tsx`) | `src/features/profile/` |
| Mobile plugin | `expo-notifications` in `app.config.ts` plugins | — |
| Design & security notes | `starter-backend/docs/PUSH_EXTENSION.md` | `EMAIL_EXTENSION.md` |

## Decision point — Expo push service vs direct FCM/APNs

**Template default: Expo push service.** One endpoint for both platforms,
receipts for delivery state, no platform credential parsing in backend code.
Go direct (FCM v1 HTTP + APNs) only if a product needs FCM topics/device-group
fan-out at scale or must avoid the third-party hop — that is a per-product
fork of `PushPort`, not a template change. Firebase **Auth** keeps using the
same Firebase project.

⚠️ Remote push does **not** work in Expo Go on recent SDKs — a development
build (EAS) is required. Verify against the SDK pinned in `package.json`.

## Contract surface (done)

`PUT`/`DELETE /api/v1/push/token` are in the backend contract
(`openapi/openapi.yaml`) with schemas `PushTokenRegistration` /
`PushTokenDelete`; the mobile pin is refreshed
(`contract/openapi.yaml` + `contract/openapi.yaml.sha256` — digest
`c5804f36e37b…`). Both routes answer `503 PUSH_DISABLED` while the extension
is disabled.

## Activation steps remaining per product app

### 1. Firebase console (Android — FCM v1)

- [ ] Same Firebase project as Auth (per-app; see
      [`FIREBASE_AUTH.md`](./FIREBASE_AUTH.md)).
- [ ] Create a **service account** with the *Firebase Cloud Messaging API
      Admin* role and upload the JSON key to the **Expo dashboard** (or via
      `eas credentials`) — Expo push uses it to deliver to FCM.
- [ ] Project ID + package name must match `app.config.ts` (`android.package`).

### 2. Apple (iOS — APNs)

- [ ] Apple Developer account with a **push-capable App ID** (bundle id
      matches `app.config.ts` `ios.bundleIdentifier`).
- [ ] Create an **APNs Auth Key (.p8)**, note its `Key ID` and `teamId`.
- [ ] Upload the `.p8` via `eas credentials` (or the Expo dashboard) per
      build profile.

### 3. Expo / EAS (mobile repo)

- [x] `expo-notifications` installed (SDK-pinned) and its config plugin added
      to `app.config.ts`; `extra.eas.projectId` is read from the config.
- [ ] Development + preview + production builds via EAS — Expo Go cannot
      receive remote push (verify against the pinned SDK).
- [x] Registration runs after authentication (`usePushRegistration`),
      best-effort: silent skips for simulator/web, missing EAS project id,
      declined permission, or a disabled backend; never blocks sign-in.

### 4. Backend variables (repo: `<app>-backend`)

- [x] `starter.push.*` wired: `enabled` (`PUSH_ENABLED`, default `false`),
      `access-token` (`EXPO_ACCESS_TOKEN`, optional — Expo accepts
      unauthenticated sends; a token only lifts rate limits), `base-url`
      (`EXPO_PUSH_BASE_URL`, default `https://exp.host`).
- [ ] Non-secret values → GitHub **variables**: `PUSH_ENABLED_DEV=true`
      (PROD `_PROD` suffix). No new Secret Manager entry is required for the
      Expo push service path; `EXPO_ACCESS_TOKEN` is optional and belongs in
      Secret Manager if a product chooses to set it.

### 5. Deploy & verify (DEV)

- [ ] `./mvnw verify` green, then deploy; the push step flips Cloud Run env
      vars with `--update-env-vars` **only** (see Rules).
- [ ] Startup proves config: enabled + blank base URL must fail closed
      (`PushConfig`).
- [ ] Smoke: register a token from a development build against DEV, verify
      the Firestore registry document (`pushTokens/{uid}/tokens/{docId}`),
      send one test push from the backend, confirm delivery via Expo **push
      receipts** (`POST /--/api/v2/push/getReceipts`).
- [ ] Uninstall/reinstall + second device cases: the registry holds multiple
      tokens per uid, and sends to dead tokens self-prune on
      `DeviceNotRegistered`.

### 6. Local development

- [x] `local` profile: `MockPushAdapter` logs the message and returns a fake
      receipt id; `InMemoryPushTokenAdapter` keeps tokens in memory — no
      network, no Expo account. Enable with `PUSH_ENABLED=true`.

### 7. PROD (only after DEV is proven)

- [ ] Separate credentials per environment; `_PROD` variables; same
      CD/rollback rules as email
      ([`starter-backend/docs/cicd_deployment_plan.md`](../starter-backend/docs/cicd_deployment_plan.md)).

## Rules that must not be broken

- **Push failures never fail the user flow.** Use cases catch
  `PushProviderException` (or call `sendToUser`, which never throws), log WARN
  with correlationId, continue — same contract as email.
- **Fail-closed activation**: `starter.push.enabled=true` with a blank
  `base-url` fails startup (`PushConfig`), never runs half-configured.
- **Identity only from the verified principal** on register/unregister; the
  body may name a device, never a user.
- **Never log push tokens, endpoint contents, or message bodies** — tokens
  are long-lived credentials (backend security rule 4). Provider failures
  carry no response body (it can echo message content).
- **No sensitive payloads in push bodies.** Notifications render on lock
  screens; send a signal, not the data — the app fetches the real content
  over the authenticated API.
- **Client does not DELETE on sign-out.** Tokens are pruned server-side when
  a send receipt reports `DeviceNotRegistered` (app uninstalled, token
  rotated); `DELETE` exists for explicit product-level cases only. This
  avoids the 401 race on sign-out.
- **Firestore rules**: the token registry is user-owned data — deny-all
  except via the backend (existing deny-all posture covers the new
  collection; access is server-only via IAM).
- **Secrets only in Secret Manager**; deploy steps use
  `--update-secrets`/`--update-env-vars` with the `^|^` delimiter — never
  `--set-*` (documented Stripe incident: full env wipe, every authenticated
  endpoint 401'd while health stayed green).
- **Mock adapters only under `local`; never deploy `local`.**

## Maintenance point

The Expo push send API shape is isolated in `ExpoPushAdapter` — if Expo
changes it, that file is the only edit. Registry shape
(`pushTokens/{uid}/tokens/{docId}`) lives behind `PushTokenPort`; swap
Firestore for anything else without touching use cases. Any new
Firebase-exempt route would be a security decision (none is needed for push).
