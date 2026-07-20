# MVP Scope Checklist

Minimal backend APIs for the starter kit. Use this to track implementation progress.

**Status:** Docs phase — all items planned.

---

## 1. API Endpoints

### 1.1 Health

| Endpoint | Method | Auth | Status |
|----------|--------|------|--------|
| `/actuator/health` | GET | Public | Planned |
| `/actuator/info` | GET | Public | Planned |

**Acceptance criteria:**

- [ ] Returns `200` with `{"status":"UP"}` when healthy
- [ ] Cloud Run uses this for liveness probe
- [ ] Mobile home screen can call this without auth

### 1.2 User

| Endpoint | Method | Auth | Status |
|----------|--------|------|--------|
| `/api/me` | GET | Bearer | Planned |

**Acceptance criteria:**

- [ ] Returns `401` without valid Firebase token
- [ ] Returns user profile with `id`, `email`, `displayName`, `createdAt`
- [ ] Creates Firestore user on first call (auto-provision)
- [ ] Subsequent calls return same user without duplicate records

### 1.3 AI Chat

| Endpoint | Method | Auth | Status |
|----------|--------|------|--------|
| `/api/chat` | POST | Bearer | Planned |

**Request body:**

```json
{ "message": "string", "sessionId": "optional" }
```

**Acceptance criteria:**

- [ ] Returns `401` without valid token
- [ ] Returns `400` for empty message
- [ ] Returns `{ "reply": "...", "sessionId": "..." }`
- [ ] `local` profile uses mock adapter (no OpenRouter call)
- [ ] `dev`/`prod` profiles call OpenRouter via Spring AI
- [ ] Returns `502` when AI provider fails (with safe error message)

---

## 2. Infrastructure

| Item | Status |
|------|--------|
| Spring Boot project scaffold | Planned |
| Profiles: `local`, `dev-local`, `dev`, `prod` | Planned |
| Firebase auth filter | Planned |
| Firestore user repository | Planned |
| Mock adapters for `local` profile | Planned |
| Dockerfile | Planned |
| `deploy-dev.yml` GitHub Action | Planned |
| `deploy-prod.yml` GitHub Action | Planned |
| Correlation ID logging | Planned |
| JSON logs in cloud profiles | Planned |

---

## 3. Explicitly out of scope (starter MVP)

| Feature | Extension doc |
|---------|---------------|
| File upload / GCS | `STORAGE_EXTENSION.md` |
| RevenueCat / subscriptions | Architecture § Future extensions |
| OCR / Document AI | Architecture § Future extensions |
| Vector search / RAG | `AI_INTEGRATION.md` § Extension |
| Cloud Tasks workers | Architecture § Future extensions |
| Webhooks | Architecture § Future extensions |
| Admin dashboard | Post-MVP |

---

## 4. Test scenarios

### Local profile

```bash
# Health (no auth)
curl http://localhost:8080/actuator/health

# Me (mock accepts any token)
curl -H "Authorization: Bearer test-token" http://localhost:8080/api/me

# Chat
curl -X POST -H "Authorization: Bearer test-token" \
  -H "Content-Type: application/json" \
  -d '{"message":"hello"}' \
  http://localhost:8080/api/chat
```

### DEV Cloud Run

```bash
# Get Firebase token from mobile or emulator, then:
curl -H "Authorization: Bearer $TOKEN" https://starter-api-dev-XXX.run.app/api/me
```

---

## 5. Mobile integration checklist

Coordinate with [starter-mobile/docs/mobile_mvp_scope_checklist.md](../../starter-mobile/docs/mobile_mvp_scope_checklist.md):

- [ ] Mobile login obtains Firebase ID token
- [ ] Mobile calls `GET /api/me` with Bearer header
- [ ] Mobile calls `POST /api/chat` and displays reply
- [ ] DEV mobile build uses DEV API URL and DEV Firebase project

---

## Related docs

- [backend_architecture_plan.md](./backend_architecture_plan.md)
- [AI_INTEGRATION.md](./AI_INTEGRATION.md)
- [AUTHENTICATION.md](./AUTHENTICATION.md)
