# Starter CI/CD and Deployment Plan

## 1. Goal

This document defines a simple, practical CI/CD plan for the starter backend.

The initial deployment strategy is:

```text
Developer pushes code
   -> automatic deployment to DEV
   -> test and validate in DEV
   -> manually promote the same build to PROD
```

The goal is to keep deployment simple, fast, and cheap during early development while following good production practices.

### Architecture assumptions

- Spring Boot backend
- Docker container images
- Google Cloud Run
- Google Artifact Registry
- Firestore
- Firebase Authentication
- Spring AI + OpenRouter (`openai-api-key` secret)
- GitHub Actions
- **Expo mobile app** ([starter-mobile](../starter-mobile/)) — EAS Build/Submit; tested against DEV API before PROD promote
- Terraform / OpenTofu (optional; not in repo)

### 1.1 Implementation Status

| Item | Status |
|------|--------|
| Documentation | Complete |
| GitHub Actions `deploy-dev.yml` on push to `main` | Planned |
| Manual `deploy-prod.yml` | Planned |
| Single Cloud Run service per env (`starter-api-dev` / `starter-api-prod`) | Planned |
| Workload Identity Federation for GitHub | Planned |
| Terraform/OpenTofu in repo | Not planned for MVP |

---

## 2. Environment Strategy

Two environments:

```text
DEV
PROD
```

### DEV Environment

Purpose:

- Fast testing and feature validation
- Mobile app integration testing (auth, `/api/me`, chat)
- AI integration testing

Characteristics:

- Automatically deployed from `main` branch
- Uses `starter-dev` GCP project and Firebase project
- Lower rate limits and quotas
- CORS may allow `*` for development
- Can contain test data

### PROD Environment

Purpose:

- Real users
- Stable releases only

Characteristics:

- Manually promoted after DEV validation
- Uses `starter-prod` GCP project and Firebase project
- Stricter CORS (specific origins)
- Protected secrets
- Higher Cloud Run limits (CPU, memory, max instances)

---

## 3. Branching Strategy

```text
main
feature/*
```

Flow:

```text
feature branch
   -> pull request
   -> merge to main
   -> automatic DEV deployment
   -> test in DEV (API + mobile)
   -> manually trigger PROD deployment
```

Use `main` + feature branches only for MVP simplicity.

---

## 4. Deployment Principle: Build Once, Promote Same Image

The same Docker image tag (git commit SHA) tested in DEV is deployed to PROD.

```text
git push main (commit abc123)
   -> build image tagged abc123
   -> deploy abc123 to Cloud Run DEV
   -> validate
   -> deploy abc123 to Cloud Run PROD (manual workflow)
```

Do not rebuild for PROD unless DEV failed and was fixed on a new commit.

---

## 5. Cloud Run Configuration

### DEV (`starter-api-dev`)

| Setting | Value |
|---------|-------|
| Region | `europe-west2` |
| CPU | 1 |
| Memory | 1Gi |
| Min instances | 0 |
| Max instances | 5 |
| Concurrency | 80 |
| `--allow-unauthenticated` | Yes (auth at app layer) |
| Service account | `starter-api@starter-dev.iam.gserviceaccount.com` |

### PROD (`starter-api-prod`)

| Setting | Value |
|---------|-------|
| Region | `europe-west2` |
| CPU | 2 |
| Memory | 2Gi |
| Min instances | 0 |
| Max instances | 20 |
| Concurrency | 80 |
| `--allow-unauthenticated` | Yes |
| Service account | `starter-api@starter-prod.iam.gserviceaccount.com` |

### Environment variables (Cloud Run)

| Variable | DEV | PROD |
|----------|-----|------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` |
| `GCP_PROJECT_ID` | `starter-dev` | `starter-prod` |
| `STARTER_CORS_ALLOWED_ORIGINS` | `*` | specific origins |

### Secrets (via `--set-secrets`)

| Secret | Env var |
|--------|---------|
| `openai-api-key:latest` | `OPENAI_API_KEY` |
| `actuator-password:latest` | `ACTUATOR_PASSWORD` |

---

## 6. GitHub Actions Workflows (planned)

### 1.1 Monorepo workflows

GitHub Actions workflows live at the **repository root**:

```text
.github/workflows/
  ci-backend.yml          # paths: starter-backend/**
  deploy-dev-backend.yml
  deploy-prod-backend.yml
  ci-mobile.yml           # paths: starter-mobile/**
  eas-build-dev.yml
  eas-submit-prod.yml
```

Use `paths` filters so a change in `starter-mobile/` does not redeploy the backend (and vice versa).

### 6.1 `deploy-dev.yml` (backend)

Trigger: push to `main`

Steps:

1. Checkout
2. Authenticate to GCP via Workload Identity Federation
3. `mvn clean package -DskipTests` (run tests locally until CI test step is added)
4. `docker build` + push to Artifact Registry (`:sha-${{ github.sha }}`)
5. `gcloud run deploy starter-api-dev` with new image

### 6.2 `deploy-prod.yml`

Trigger: `workflow_dispatch` with confirmation input (e.g. type `deploy`)

Steps:

1. Same build/push (or reuse DEV image tag from input)
2. `gcloud run deploy starter-api-prod`
3. GitHub `environment: production` for approval gate

### 6.3 Authentication to GCP

Use **Workload Identity Federation** — no long-lived service account keys in GitHub secrets.

Setup documented in `scripts/COMMON_GCP_SETUP.md` § Workload Identity Federation.

Required GitHub secrets:

| Secret | Description |
|--------|-------------|
| `WIF_PROVIDER` | `projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/...` |
| `WIF_SERVICE_ACCOUNT` | `github-actions@starter-dev.iam.gserviceaccount.com` |

---

## 7. Dockerfile (planned)

Multi-stage build:

```dockerfile
# Stage 1: Maven build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 8. Rollback

### Quick rollback (Cloud Run)

```bash
# List revisions
gcloud run revisions list --service=starter-api-prod --region=europe-west2

# Route 100% traffic to previous revision
gcloud run services update-traffic starter-api-prod \
  --to-revisions=starter-api-prod-00042-abc=100 \
  --region=europe-west2
```

### Redeploy previous image

```bash
gcloud run deploy starter-api-prod \
  --image=europe-west2-docker.pkg.dev/starter-prod/starter-api/starter-api:sha-PREVIOUS_SHA \
  --region=europe-west2
```

---

## 9. Mobile Coordination

Before promoting backend PROD:

1. DEV mobile build validated against DEV API
2. `GET /api/me` and `POST /api/chat` work end-to-end
3. Firebase project alignment confirmed (DEV mobile → DEV backend)

Mobile CI/CD: [starter-mobile/docs/mobile_cicd_deployment_plan.md](../../starter-mobile/docs/mobile_cicd_deployment_plan.md).

---

## 10. Observability

- **Logs:** Cloud Logging (JSON structured logs in `dev`/`prod` profiles)
- **Health:** Cloud Run uses `/actuator/health`
- **Metrics:** Cloud Run built-in metrics; optional Spring Actuator metrics behind admin auth
- **Alerts:** Configure Cloud Monitoring alerts on error rate and latency (post-MVP)

---

## Related docs

- [scripts/COMMON_GCP_SETUP.md](../scripts/COMMON_GCP_SETUP.md)
- [scripts/DEV_SETUP.md](../scripts/DEV_SETUP.md)
- [scripts/PROD_SETUP.md](../scripts/PROD_SETUP.md)
- [../../docs/NEW_APP_WORKFLOW.md](../../docs/NEW_APP_WORKFLOW.md)
