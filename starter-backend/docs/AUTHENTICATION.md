# Authentication Flow and API Protection

The Starter Backend API uses Firebase Authentication for user identity and API protection.

## Overview

All protected endpoints require a valid Firebase ID Token (JWT) sent in the `Authorization` header as a Bearer token.

## Authentication Mechanism

1. **Client-side Authentication**: The mobile app authenticates with Firebase.
2. **ID Token Acquisition**: After successful login, the client retrieves a Firebase ID Token.
3. **API Request**: The client includes the ID Token in the request header:

   ```http
   Authorization: Bearer <FIREBASE_ID_TOKEN>
   ```

4. **Backend Verification**:
   - The `FirebaseAuthenticationFilter` intercepts the request.
   - It extracts the token and uses `FirebaseAuthService` (Firebase Admin SDK) to verify it.
   - If valid, a `FirebaseAuthenticationToken` is created and stored in the Spring `SecurityContext`.
   - The principal is a `FirebaseUser` object containing UID, email, and claims.
   - If invalid or missing (for protected endpoints), `401 Unauthorized` is returned.

5. **User Provisioning**: On `GET /api/me`, `UserService.getOrCreateUser()` creates a Firestore user record if one does not exist for the Firebase UID.

## Protected Endpoints

By default, all endpoints require authentication, except:

- `GET /actuator/health`
- `GET /actuator/info`

Admin endpoints (all other `/actuator/**` paths) require the `ADMIN` role with HTTP Basic authentication.

## MVP Endpoints

| Endpoint | Auth required |
|----------|---------------|
| `GET /actuator/health` | No |
| `GET /actuator/info` | No |
| `GET /api/me` | Yes (Bearer) |
| `POST /api/chat` | Yes (Bearer) |

## Configuration

Firebase initialization is handled in `FirebaseConfig`. It uses Google Application Default Credentials (ADC).

For local development, set:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$HOME/.gcp/starter-dev-sa.json"
```

### Local profile (mocks)

With `spring.profiles.active=local`, `MockFirebaseAuthService` accepts any Bearer token and returns a fixed mock user. No Firebase connection required.

### Auth emulator

For `dev-local` with Firebase Auth emulator:

```bash
export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

### Disabling Firebase in tests

```yaml
firebase:
  enabled: false
```

Mock `FirebaseAuthService` in integration tests.

## Error Handling

| Status | Cause |
|--------|-------|
| `401 Unauthorized` | Token missing, expired, or invalid |
| `403 Forbidden` | Authenticated but not authorized (rare in MVP) |

Response body contains a brief error message. Mobile client should refresh token on 401 and retry once before signing out.

## Mobile integration

See [starter-mobile/docs/BACKEND_INTEGRATION.md](../../starter-mobile/docs/BACKEND_INTEGRATION.md).

## Related docs

- [SECURITY.md](./SECURITY.md)
- [DATABASE.md](./DATABASE.md) — user record created on first `/api/me`
