# Authentication and Authorization

## Flow

1. The client signs in with Firebase Authentication.
2. The client obtains a short-lived Firebase ID token.
3. It sends `Authorization: Bearer <token>` to a protected endpoint.
4. The backend verifies signature, issuer/audience, expiry, and the configured revocation/disabled-user policy through Firebase Admin.
5. The backend constructs its own principal from verified claims.
6. Application services use that principal for authorization and ownership.

The backend never receives a Firebase password and never trusts a UID supplied in a request body/query parameter.

## Access policy

Target v1:

| Path | Access |
|---|---|
| `/health/live` | Public, minimal |
| `/health/ready` | Deployment/operations policy |
| `/api/v1/**` | Valid Firebase Bearer token unless explicitly documented |
| diagnostics | Operations role/identity only |

The prototype currently exposes `/actuator/health` and `/actuator/info` publicly and protects other routes.

## Local behavior

The explicit `local` profile may accept a deterministic test token through a mock verifier. It must be impossible for `dev`, `prod`, or an unset profile to create that verifier. Add profile-wiring tests before template v1.

When using Firebase emulators, keep emulator configuration explicit and verify the process cannot accidentally use production credentials at the same time.

## Token refresh contract

On `401`, the mobile client may force-refresh the ID token and retry once. A second `401` ends the session. The backend never asks the client to retry indefinitely.

| Status | Meaning |
|---|---|
| `401` | Missing, malformed, expired, invalid, revoked/disabled per policy |
| `403` | Identity valid but action/resource not permitted |

Both use the standard JSON error envelope and correlation ID.

## Claims and roles

Custom claims can map to server roles, but token presence is not sufficient for resource ownership. Keep role names allow-listed; do not accept arbitrary claim strings as authorities. Document claim refresh delay and admin assignment/revocation flow before relying on roles.

## User provisioning

`GET /api/v1/me` creates the starter user record from verified identity when absent. Creation must be idempotent under concurrent requests. Decide explicitly whether later token changes update email/display name; do not silently overwrite user-managed fields.

## Required tests

- Missing/malformed/invalid token -> standard `401`
- Valid token -> principal contains expected UID
- `local` mock cannot load in `dev`/`prod` or no-profile startup
- Role/claim mapping rejects unexpected values
- User A cannot access user B's resource in an example extension test
- Client refreshes/retries once and then signs out
