# Step 5 — Completion & First-Push Checklist

Everything below is **live-setup work** (real GitHub/EAS/cloud accounts). All Step 5 *code and
runbooks* are done and `actionlint`-clean. Work top-to-bottom; each phase's Done unlocks the next.
Status is updated as we go (last update 2026-08-22, after DEV infra applied).

Two independent repos: `starter-backend` (Spring Boot, Cloud Run) and `starter-mobile` (Expo/EAS).
Backend and mobile promote independently; they meet only at the versioned API contract.

---

## 0a. First-run fixes already applied (2026-08-19)

- Backend CI was red on the first push. **Firestore emulator integration test** was flaky on
  GitHub-hosted runners (write not visible to an immediate read even with a 15s retry; passes
  locally). De-scoped from the gate (`verify.yml` → `RUN_FIRESTORE_EMULATOR_TEST=false`); test +
  bounded retry remain and run locally. **TODO:** re-enable once a runner-stable emulator fix lands.
- **Trivy** action version was `@0.36.0` (no `v`) and GitHub couldn't resolve it — corrected to
  `@v0.36.0` in `deploy-dev.yml` and `promote-prod.yml`.
- `production` GitHub environments created in **both** repos (reviewer rules skipped — free-plan
  private repos don't expose them; see [3](#3-backend-production)).

---

## 0. Preflight (both repos)

- [x] Repos exist on GitHub (`patrikbego/starter-backend`, `patrikbego/starter-mobile`, private) and
      `main` is pushed. Backend **CI (`verify`) ✅ green**, mobile **CI ✅ green** (2026-08-19).
- [x] Backend commit history includes all Step 5 (gated deploy, digest+metadata, PROD promote,
      smoke/alerts); mobile incl. build-release/submit-release workflows.
- [x] On **your machine**, run both local gates green (shared Sonar at `http://localhost:9000`):
  ```bash
  # backend
  cd starter-backend && ./scripts/local-gate.sh starter-backend
  # mobile
  cd starter-mobile && ./scripts/local-gate.sh starter-mobile
  ```
  (done 2026-08-20: backend `status: OK` — new_coverage 93.8, dups 0, violations 0; mobile `status: OK` — new_coverage 87.1, dups 0, violations 0.)
- [x] Confirm Sonar dashboards: `/dashboard?id=starter-backend` (Java, coverage ~93%) and
      `/dashboard?id=starter-mobile` (TS/JS, coverage ~87%).

## 1. Push backend

- [x] `starter-backend` pushed to `origin/main` (2026-08-19); CI green.
- [x] `starter-mobile` pushed to `origin/main`; CI green.
- [x] **Branch protection** on `main` for both repos: require PR + required check `verify` (backend) /
      CI checks (mobile) + owners review. **Skipped — requires a paid plan** (free personal plan;
      staying on direct push, decided 2026-08-20). Revisit on Team/public visibility.
  *Later:* `<repo> -> Settings -> Branches (left, under "Code and automation") -> Add branch
  protection rule -> "main" -> ☑ Require a pull request before merging (+ approvals, dismiss stale
  approvals) -> ☑ Require status checks to pass -> select `verify` (backend) / `ci` (mobile)
  -> ☑ Require review from Code Owners (needs `CODEOWNERS` in the repo) -> Create rule. Do both repos.*
- [x] Enable **Dependabot alerts/updates**, **secret scanning / push protection** (both repos)
      (done 2026-08-20).
  *Reference path: `<repo> -> Settings -> Security analysis` (left, "Security") -> toggle on:
  Dependabot alerts, Dependabot security updates, Secret scanning, push protection. Version updates:
  same page -> Dependabot block -> "Create config" (writes `.github/dependabot.yml`).*
- [x] **Mark both as template repositories** (Settings → Template repository) so apps are copied.
  *`patrikbego/starter-backend` and `patrikbego/starter-mobile` -> Settings -> General ->
  scroll to the bottom of General -> ☑ "Template repository". Then a green **"Use this template"**
  button appears for anyone with access (public if you flip visibility, private = access-restricted).*

## 2. Backend cloud + secrets (run on YOUR machine — needs gcloud auth + terraform)

> gcloud auth happens on your machine; planning/applying are plain `terraform` CLI commands that
> can also be driven from the agent sandbox once ADC is in place (that's how 2026-08-22's run went).

- [x] **Install Terraform** (2026-08-22, v1.11.2 — direct binary, not brew):
  ```bash
  # brew install terraform  # ✗ formula removed from homebrew/core (HashiCorp BSL move);
  #                         #   'brew tap hashicorp/tap' also fails if git < 2.24 (Xcode CLT)
  curl -LO https://releases.hashicorp.com/terraform/1.11.2/terraform_1.11.2_darwin_amd64.zip
  unzip -o terraform_1.11.2_darwin_amd64.zip -d /usr/local/bin
  terraform version   # require >= 1.6.0 (main.tf)
  ```
- [x] **Auth to GCP** (owner, first run — creates Application Default Credentials for the provider):
  ```bash
  gcloud auth application-default login
  gcloud auth application-default set-quota-project starter-demo-dev   # fixes ADC quota warnings
  gcloud config set project starter-demo-dev
  ```
- [x] **Create the Terraform state bucket** (one-time, 2026-08-22):
  ```bash
  gcloud storage buckets create gs://starter-tfstate --location=europe-west2
  # or a name you own; plan.sh expects TFSTATE_BUCKET
  ```
- [x] **Backend DEV plan + apply** (2026-08-22 — plan 24 to add; apply complete, 24 created):
  ```bash
  cd starter-backend/infra
  TFSTATE_BUCKET=starter-tfstate ./scripts/plan.sh dev
  # <- review; then:
  ./scripts/apply.sh dev     # prints `terraform output` at the end
  ```
  *⚠️ template fixes needed before it ran (both committed on `main`):*
  - `infra/main.tf` — two parenthesized multiline ternaries (`billing_account`, `monitoring_notification_channels`); HCL can't continue `? :` across a newline after a completed expression.
  - `infra/dev.tfvars` — must hold your **real** project + org, e.g. `project_id = "starter-demo-dev"`, `github_organization = "patrikbego"`. The template's `REPLACE_ME_ORG` breaks WIF (exact-repo match); default `starter-dev` may not be owned by you.
- [x] From the apply output, **copy these two values** and set them as GitHub **DEV secrets** (done 2026-08-22):
  - `workload_identity_provider` → `GCP_WORKLOAD_IDENTITY_PROVIDER_DEV`
    = `projects/906316354955/locations/global/workloadIdentityPools/github-dev/providers/github-repo`
  - `deployer_service_account` → `GCP_SERVICE_ACCOUNT_DEV`
    = `starter-api@starter-demo-dev.iam.gserviceaccount.com`
  ```bash
  gh secret set GCP_WORKLOAD_IDENTITY_PROVIDER_DEV -R patrikbego/starter-backend --body 'projects/906316354955/locations/global/workloadIdentityPools/github-dev/providers/github-repo'
  gh secret set GCP_SERVICE_ACCOUNT_DEV   -R patrikbego/starter-backend --body 'starter-api@starter-demo-dev.iam.gserviceaccount.com'
  ```
- [x] **Populate GCP *Secret Manager* runtime secrets** (NOT GitHub — workflows read them via
      `--set-secrets`): keep DEV/PROD values strictly separate.
      (Done 2026-08-22: `openai-api-key` v1 + `actuator-password` v1, both `enabled` in
      `starter-demo-dev`; the green deploy mounted them successfully.
      ⚠️ Re-verify the OpenRouter key value — it may have been captured truncated
      (`sk-or-v1-cae7…`); test `/api/v1/ai/chat` and re-run `set-secrets.sh dev` if 401.)
  ```bash
  cd starter-backend/infra
  ./scripts/set-secrets.sh dev --openai-api-key 'sk-or-v1-…' --actuator-password 'change-me'
  ```
  *⚠️ template fix needed (committed): `set-secrets.sh` originally hard-coded
  `PROJECT_ID="starter-${ENV}"`; it now reads `project_id` from `<env>.tfvars`. Without the fix it
  targets the wrong project when your project id isn't literally `starter-dev`*
- [ ] **Optional** DEV secrets for the authenticated smoke: `FIREBASE_WEB_API_KEY`,
      `FIREBASE_TEST_USER_EMAIL`, `FIREBASE_TEST_USER_PASSWORD` (GitHub).
  <details><summary><strong>Where to get each</strong> (click-path)</summary>

  All three are GitHub repo secrets — *Settings → Secrets and variables → Actions → New
  repository secret*. The smoke signs in via the Identity Toolkit REST API
  (`accounts:signInWithPassword?key=<WEB_API_KEY>`), so it needs a **real Firebase user**, not a
  service account.

  1. `FIREBASE_WEB_API_KEY` — [console.firebase.google.com](https://console.firebase.google.com)
     → select the DEV project (`starter-demo-dev`) → ⚙️ **Project settings → General → Your apps**.
     No web app yet? Click the Web `</>` icon, register one (hosting off), then copy
     `firebaseConfig.apiKey` (the long `AIzaSy…` string). Same key class the mobile app ships.
  2. Email/password provider must be on first: **Build → Authentication → Sign-in method →
     Email/Password → Enable**.
  3. `FIREBASE_TEST_USER_EMAIL` / `FIREBASE_TEST_USER_PASSWORD` — **Authentication → Users →
     Add user**, e.g. `smoke-test@starter-demo-dev.com` + a strong password. DEV-project-only;
     this account exists purely so CI can prove an authorized `/api/v1/me` returns 200.
  </details>
- [ ] **Optional** `SLACK_WEBHOOK` (GitHub secret) for the failure alert.
  *Get it at [api.slack.com/apps](https://api.slack.com/apps) → **Create New App → From scratch**
  → pick your workspace → **Incoming Webhooks** → toggle Activate → **Add New Webhook to
  Workspace** → choose a channel → copy the `https://hooks.slack.com/services/T…/B…/…` URL into
  the GitHub secret. Without it, deploy failures only show in the Actions run (Notify job still
  succeeds silently).*
- [x] **Trigger `Deploy to DEV`** (push to main or workflow_dispatch), confirm the chain
      `verify → build-and-push → deploy-dev → smoke-dev` green and `release-metadata.json` uploaded
      (`backend-artifacts` / `release-metadata` artifacts). If smoke fails: check the notify job /
      rollback runbook (§4).
  ✅ **First green run: 2026-08-22, run [32593078612](https://github.com/patrikbego/starter-backend/actions/runs/32593078612)**
  — all five jobs success; revision `starter-api-dev-00003-n9l`, `smokeResult: OK`,
  `promotable: true`; live check `/health/live` + `/health/ready` = 200, unauth
  `/api/v1/me` = 401 (fail closed). Authenticated smoke skipped until Firebase test-user
  secrets are set (optional, below). Fixes that were needed to get there (all on `main`):
  - `deploy-dev.yml`: `env.PROJECT_ID` was hard-coded `starter-dev`; set to your real project
    and use `${{ env.PROJECT_ID }}` in `--service-account`. Same stale project in
    `promote-prod.yml` example digest.
  - IAM (`main.tf`): runtime SA needed `roles/artifactregistry.writer` (image push), then
    `roles/run.admin` + self `roles/iam.serviceAccountUser` (the deploy job impersonates it for
    `gcloud run deploy` and passes it as `--service-account`). Re-run `plan.sh`/`apply.sh`.
  - Trivy gate: Boot 4.1.0's BOM shipped vulnerable netty/httpcore5/httpclient5/jackson/log4j2;
    upgraded parent to **Spring Boot 4.1.1** (+ legacy jackson-databind pinned 2.22.2,
    Spring AI 2.0.1) so the CRITICAL+HIGH gate passes.
  - SARIF upload: needs `security-events: write` + `actions: read`, but GitHub **Code Scanning is
    not available on free-plan private repos** — step is `continue-on-error` now; findings are in
    the `trivy-results` artifact either way.
  - Runtime startup (two latent template bugs surfaced only on first real deploy):
    Spring AI 2.x has no default `ChatClient` bean — adapter now injects `ChatClient.Builder`;
    and `STARTER_CORS_ALLOWED_ORIGINS='*'` trips the fail-closed CORS check — DEV passes explicit
    origins via a custom gcloud delimiter (`^@^…@`) because commas/colons break `--set-env-vars`.
  - `set-secrets.sh`: bash-3.2 rewrite (`declare -A` unsupported on macOS `/bin/bash`).

## 3. Backend production

- [x] Reviewer/protection rules on the `production` environment (GitHub Settings → Environments):
      add reviewers, disallow self-approval. The env exists; this is the approval gate.
      **Skipped — free-plan private repos don't expose "Required reviewers"** (2026-08-22; same
      family as branch protection). Interim controls instead: environment's
      **Deployment branches and tags → main only** (next box), prod workflows are
      workflow_dispatch-only, promotion is digest-only. Revisit on Team/public plan.
- [x] Set production environment's **Deployment branches and tags** to `main` only (2026-08-22,
      done via API on **both** repos; visible on free plan; stops stray-branch prod deploys).
- [ ] `starter-prod` PROD secrets: `GCP_WORKLOAD_IDENTITY_PROVIDER_PROD`, `GCP_SERVICE_ACCOUNT_PROD`
      (from `terraform apply.sh prod` output).
  ```bash
  cd starter-backend/infra
  TFSTATE_BUCKET=starter-tfstate ./scripts/plan.sh prod   # review
  ./scripts/apply.sh prod
  ./scripts/set-secrets.sh prod --openai-api-key 'sk-or-v1-…' --actuator-password 'change-me'
  ```
- [ ] Run **`Promote to PROD`** (workflow_dispatch) with the digest from the DEV `release-metadata`;
      confirm PROD smoke green. *On the free plan there is no approval prompt — dispatching the
      workflow from `main` is itself the deliberate act.*
- [ ] Reserve PROD-only firebase/other values strictly apart from DEV.

## 4. Backend rollback drill

- [ ] Follow [`starter-backend/docs/rollback_runbook.md`](../starter-backend/docs/rollback_runbook.md):
      deliberately break DEV, confirm smoke/notify fail (exercises A4), then PRIMARY (route traffic)
      and SECONDARY (redeploy good digest) rollback; record ≤5 min.
- [ ] Repeat the drill in PROD during a maintenance window.

## 5. Mobile

- [ ] Set `EXPO_TOKEN` repo secret (mobile); create the EAS project (`eas init` → writes `eas.json`
      `projectId`) and install iOS/Android credentials (`eas credentials`).
- [ ] Set `ios.ascAppId` in `eas.json` `submit.production.ios` (currently `REPLACE_ME`); confirm
      branch protection + Dependabot + secret scanning (never commit `.p8`/provisioning files).
      (Same Settings paths as [§1](#1-push-backend).)
- [x] Reviewer/protection rules on the mobile **`production` environment** too
      (Settings → Environments → production): add reviewers, disallow self-approval. Same as
      [§3](#3-backend-production) — it gates `submit-release.yml`, i.e. the store submission.
      **Skipped — free-plan private repos don't expose "Required reviewers"** (2026-08-22);
      branch policy `main`-only is set. Add reviewers when on a Team/public plan.
- [x] Mark mobile as a **template repository** — `<mobile repo> → Settings → General → ☑ "Template repository"`.

- [ ] Build DEV preview (`build-preview` workflow); install on a device, confirm DEV identifiers.
- [ ] Tag a release (`git tag v0.1.0 && git push --tags`) → `build-release` builds store-signed
      candidate, uploads `mobile-release-metadata.json`.
- [ ] Run `Submit release` with the recorded iOS/Android build IDs (needs the mobile `production`
      env — created in §0a) → TestFlight + Play internal.

## 6. Mobile device + release drill

- [ ] Test the store case on real devices: **login → `me` → AI → refresh → sign-out**.
- [ ] Follow [`starter-mobile/docs/release_rollback_runbook.md`](../starter-mobile/docs/release_rollback_runbook.md): start then halt a phased rollout; confirm installed builds keep working
      (backend compatible); queue a fixed build; record timings.

## 7. Close Step 5

- [ ] Tick remaining Step 5 boxes in [`docs/IMPLEMENTATION_ROADMAP.md`](../docs/IMPLEMENTATION_ROADMAP.md)
      (A5 drills, B1 device loop) once proven.
- [ ] First app via **“Use this template”** from both repos: rename per-app identifiers, run the
      local gate, push — this is Step 6 (v1.0.0 trial).

---

## Secrets/environment summary (quick reference)

| Secret / setting | Repo | Where |
|---|---|---|
| `GCP_WORKLOAD_IDENTITY_PROVIDER_DEV` / `_PROD` | backend | GitHub secret (from `terraform output`) |
| `GCP_SERVICE_ACCOUNT_DEV` / `_PROD` | backend | GitHub secret |
| `OPENAI_API_KEY`, `ACTUATOR_PASSWORD` | backend | **GCP Secret Manager** (`set-secrets.sh`) |
| `FIREBASE_WEB_API_KEY`, `FIREBASE_TEST_USER_*` | backend | GitHub secret (optional auth smoke) |
| `SLACK_WEBHOOK` | backend | GitHub secret (optional alerts) |
| `EXPO_TOKEN` | mobile | GitHub secret |
| iOS/Android EAS credentials | mobile | EAS (not GitHub) |
| `ios.ascAppId` in `eas.json` | mobile | file (set to real value) |
| `production` environment | both | GitHub Settings → Environments (reviewers = paid plan, skipped; `main`-only branch policy set) |

Current position (2026-08-22, evening): **§2 complete — Deploy to DEV is green end-to-end**
(run [32593078612](https://github.com/patrikbego/starter-backend/actions/runs/32593078612):
verify → build → deploy → smoke → notify all success; revision `starter-api-dev-00003-n9l`,
`smokeResult: OK`, `promotable: true`; live `/health/*` 200, unauth `/api/v1/me` 401).
Sonar gates green (backend 93.8 / mobile 87.1 new coverage). Plan-gated items consciously
skipped: branch protection, environment required-reviewers (both repos), Code Scanning SARIF
upload — interim controls documented in-place. Still open (optional): real OpenRouter key
verification in Secret Manager, Firebase test-user secrets for the authed smoke, SLACK_WEBHOOK.
Next phase: §3 backend production (PROD tfvars/secrets + Promote to PROD with the DEV digest),
then §4 rollback drill.