# Backend Integration

How the starter mobile app connects to the starter-backend API.

## Overview

```text
Mobile (Expo)                    Backend (Cloud Run)
─────────────                    ───────────────────
Firebase Auth ID token    →      Token verification
Bearer on every request   →      Authorization
REST JSON                 ↔      /api/me, /api/chat
```

## Base URL configuration

Set at build time via `app.config.ts`:

| `APP_ENV` | Variable | Example |
|-----------|----------|---------|
| `development` | `API_BASE_URL_DEV` | `https://starter-api-dev-XXX.europe-west2.run.app` |
| `production` | `API_BASE_URL_PROD` | `https://starter-api-prod-XXX.europe-west2.run.app` |

Runtime access:

```typescript
import Constants from 'expo-constants';

export const config = {
  apiBaseUrl: Constants.expoConfig?.extra?.apiBaseUrl as string,
  appEnv: Constants.expoConfig?.extra?.appEnv as string,
};
```

**Never hardcode** API URLs in source files.

## Authentication header

Every protected request:

```http
Authorization: Bearer <firebase_id_token>
```

### Token acquisition

```typescript
// After Firebase sign-in
const token = await firebaseUser.getIdToken();
```

### Token refresh on 401

```text
Request fails with 401
  → call getIdToken(true) to force refresh
  → retry request once
  → if still 401, sign out and redirect to login
```

## API client pattern (planned)

```typescript
// src/adapters/HttpApiClient.ts
async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const token = await authPort.getIdToken();
  const response = await fetch(`${config.apiBaseUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...options?.headers,
    },
  });
  if (response.status === 401) {
    // refresh + retry logic
  }
  if (!response.ok) throw new ApiError(response.status, await response.text());
  return response.json();
}
```

## Endpoints

### Health check (public)

```http
GET /actuator/health
```

No auth required. Used for backend reachability badge on home screen.

```typescript
// Can use fetch without Bearer for health only
const res = await fetch(`${config.apiBaseUrl}/actuator/health`);
const { status } = await res.json(); // "UP"
```

### Current user

```http
GET /api/me
Authorization: Bearer <token>
```

Response:

```json
{
  "id": "firebase-uid",
  "email": "user@example.com",
  "displayName": "Jane Doe",
  "createdAt": "2026-01-15T10:00:00Z"
}
```

TanStack Query hook:

```typescript
export function useMe() {
  return useQuery({
    queryKey: ['me'],
    queryFn: () => apiClient.get<MeResponse>('/api/me'),
  });
}
```

### AI chat

```http
POST /api/chat
Authorization: Bearer <token>
Content-Type: application/json

{
  "message": "Hello!",
  "sessionId": "optional-uuid"
}
```

Response:

```json
{
  "reply": "Hello! I'm the starter kit assistant.",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

Mutation hook:

```typescript
export function useSendChat() {
  return useMutation({
    mutationFn: (body: ChatRequest) =>
      apiClient.post<ChatResponse>('/api/chat', body),
  });
}
```

## Error handling

| Status | User message | Action |
|--------|--------------|--------|
| 401 | Session expired | Refresh token → retry → sign out |
| 400 | Invalid message | Show validation error |
| 429 | Too many requests | Show retry later |
| 502 | Service unavailable | Show try again |
| Network error | No connection | Show offline message |

## Environment pairing

| Mobile build | Firebase project | API URL |
|--------------|------------------|---------|
| DEV (`development`) | `starter-dev` | DEV Cloud Run |
| PROD (`production`) | `starter-prod` | PROD Cloud Run |

Cross-wiring DEV mobile to PROD API (or vice versa) causes auth failures.

## Firebase alignment

Mobile Firebase `projectId` must match the backend's Firebase project for the same environment.

Backend verifies tokens issued by that Firebase project via Firebase Admin SDK.

## Local development

Against local backend:

```typescript
// Expo dev with localhost (use machine IP for physical device)
apiBaseUrl: 'http://localhost:8080'
```

Against DEV Cloud Run from device: use DEV Cloud Run HTTPS URL.

## Testing without mobile

Use curl with Firebase emulator token — see [starter-backend/scripts/DEV_LOCAL_SETUP.md](../starter-backend/scripts/DEV_LOCAL_SETUP.md).

## Related docs

- [starter-backend/docs/AUTHENTICATION.md](../starter-backend/docs/AUTHENTICATION.md)
- [starter-backend/docs/AI_INTEGRATION.md](../starter-backend/docs/AI_INTEGRATION.md)
- [../../docs/ENVIRONMENT_MATRIX.md](../../docs/ENVIRONMENT_MATRIX.md)
- [mobile_architecture_plan.md](./mobile_architecture_plan.md)
