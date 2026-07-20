# Starter Mobile CI/CD and Deployment Plan

## 1. Goal

CI/CD plan for the starter mobile app (Expo / React Native).

```text
Developer pushes code
   -> CI runs lint and typecheck
   -> automatic EAS build for DEV (internal distribution)
   -> test against DEV backend
   -> manually promote same build to PROD store tracks
```

Matches backend **DEV auto / PROD manual** discipline.

### Assumptions

- Expo SDK 54
- EAS Build and EAS Submit
- EAS Update for JS-only fixes (where compatible)
- Firebase Authentication (separate DEV / PROD projects)
- Spring Boot API on Cloud Run (DEV / PROD)
- GitHub Actions for CI and EAS triggers (workflows at monorepo root, `paths: starter-mobile/**`)

**Related:** [mobile_architecture_plan.md](./mobile_architecture_plan.md), [starter-backend CI/CD](../starter-backend/docs/cicd_deployment_plan.md).

### 1.1 Implementation Status

| Item | Status |
|------|--------|
| Documentation | Complete |
| `eas.json` | Planned |
| `app.config.ts` | Planned |
| `.github/workflows/ci.yml` | Planned |
| `.github/workflows/eas-build-dev.yml` | Planned |
| `.github/workflows/eas-submit-prod.yml` | Planned |

---

## 2. Environment Strategy

### DEV

- Fast iteration and QA
- Integration with DEV Cloud Run API
- Firebase `starter-dev` project
- Internal testers (EAS internal, TestFlight internal, Play internal)

### PROD

- App Store / Google Play
- Firebase `starter-prod` project
- Production API URL
- Manual submit after DEV validation

Staging is optional and not required for MVP.

---

## 3. Branching Strategy

```text
main
feature/*
```

```text
feature branch → PR → merge to main → CI + DEV EAS build → test → manual PROD submit
```

---

## 4. Build Once, Promote Same Build

The EAS build tested in DEV is submitted to PROD — do not create a separate production build with unverified changes.

Record per release:

```text
commit SHA
EAS build ID
platform (ios / android)
submitter
timestamp
```

---

## 5. EAS Configuration (planned)

### 5.1 `eas.json`

```json
{
  "cli": {
    "version": ">= 16.0.0",
    "appVersionSource": "remote"
  },
  "build": {
    "development": {
      "developmentClient": true,
      "distribution": "internal",
      "env": { "APP_ENV": "development" }
    },
    "preview": {
      "distribution": "internal",
      "env": { "APP_ENV": "development" }
    },
    "production": {
      "autoIncrement": true,
      "env": { "APP_ENV": "production" }
    }
  },
  "submit": {
    "production": {
      "ios": { "ascAppId": "REPLACE_ME" },
      "android": { "track": "internal" }
    }
  }
}
```

### 5.2 `app.config.ts`

See [mobile_architecture_plan.md](./mobile_architecture_plan.md) § Environment Configuration.

Runtime access:

```typescript
import Constants from 'expo-constants';
const { apiBaseUrl, appEnv, firebase } = Constants.expoConfig?.extra ?? {};
```

---

## 6. GitHub Actions Workflows (planned)

### 6.1 GitHub Actions workflows (monorepo root)

Workflows live at `.github/workflows/` in the repository root. Use path filters:

```yaml
on:
  push:
    paths:
      - 'starter-mobile/**'
```

### 6.2 `ci.yml`

Trigger: pull request and push to `main`

```yaml
# working-directory: starter-mobile
steps:
  - npm ci
  - npx expo lint
  - npx tsc --noEmit
```

### 6.3 `eas-build-dev.yml`

Trigger: push to `main`

```yaml
steps:
  - uses: expo/expo-github-action@v8
    with:
      eas-version: latest
      token: ${{ secrets.EXPO_TOKEN }}
  - run: eas build --profile preview --platform all --non-interactive
```

### 6.4 `eas-submit-prod.yml`

Trigger: `workflow_dispatch` with `build_id` input

```yaml
environment: production
steps:
  - run: eas submit --platform all --id ${{ inputs.build_id }} --non-interactive
```

---

## 7. Secrets

### GitHub repository secrets

| Secret | Description |
|--------|-------------|
| `EXPO_TOKEN` | Expo access token |

### EAS environment variables (per profile)

| Variable | DEV | PROD |
|----------|-----|------|
| `APP_ENV` | `development` | `production` |
| `API_BASE_URL_DEV` | DEV Cloud Run URL | — |
| `API_BASE_URL_PROD` | — | PROD Cloud Run URL |
| `EXPO_PUBLIC_FIREBASE_API_KEY` | DEV key | PROD key |
| `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN` | DEV domain | PROD domain |
| `EXPO_PUBLIC_FIREBASE_PROJECT_ID` | `starter-dev` | `starter-prod` |

Set via EAS dashboard or `eas env:create`.

---

## 8. OTA Updates (EAS Update)

Use for JS-only fixes without store review.

**Requires new native build when:**

- Firebase SDK version changes
- New native modules added
- Expo SDK upgrade

---

## 9. Backend Coordination

Before PROD mobile submit:

1. DEV build tested against DEV API (`/api/me`, `/api/chat`)
2. Backend PROD deployed (manual workflow)
3. Update EAS production env with PROD API URL
4. Submit **same build ID** tested in DEV (if API contract unchanged)

If backend PROD has breaking changes, create new build after backend PROD deploy.

---

## 10. Rollback

### Store rollback

- iOS: remove version from sale or expedite fix build
- Android: halt rollout in Play Console

### OTA rollback

```bash
eas update:rollback
```

---

## Related docs

- [BACKEND_INTEGRATION.md](./BACKEND_INTEGRATION.md)
- [../../docs/ENVIRONMENT_MATRIX.md](../../docs/ENVIRONMENT_MATRIX.md)
- [../../docs/NEW_APP_WORKFLOW.md](../../docs/NEW_APP_WORKFLOW.md)
