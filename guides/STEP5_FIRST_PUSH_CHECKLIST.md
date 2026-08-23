# Step 5 — Completion & First-Push Checklist

Everything below is **live-setup work** (real GitHub/EAS/cloud accounts). All Step 5 *code and
runbooks* are done and `actionlint`-clean. Work top-to-bottom; each phase's Done unlocks the next.
Status is updated as we go (last update 2026-08-23, after EAS project linked + CI pins).

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
- [x] **GitHub repo settings** on both repos — branch protection + Dependabot/secret 
      scanning + template flags. Done/skipped status and the full click-paths are in the
      dropdown below.
  <details><summary><strong>GitHub repo settings</strong> (click-path)</summary>

- **Branch protection** on `main` (require PR + status check `verify`/`ci` + owners review) —
  **Skipped: requires a paid plan** (free personal plan; staying on direct push, decided
  2026-08-20). Revisit on Team/public visibility.
  ```
  <repo> -> Settings -> Branches (left, under "Code and automation") -> Add branch protection
  rule -> branch "main"
   -> require a pull request before merging (+ approvals, dismiss stale)
   -> require status checks to pass -> select `verify` (backend) / `ci` (mobile)
   -> require review from Code Owners (needs `CODEOWNERS` in the repo)
  -> Create rule. DO BOTH REPOS.
  ```
- **Dependabot alerts/updates + secret scanning / push protection** — **done 2026-08-20**
  (both repos):
  ```
  <repo> -> Settings -> Security analysis (left, "Security") -> toggle on:
  Dependabot alerts, Dependabot security updates, Secret scanning, push protection.
  Version updates: same page -> Dependabot block -> "Create config" (writes `.github/dependabot.yml`).
  ```
- **Mark both as template repositories** — **done 2026-08-20**, so copying the app starts the
  whole tree:
  ```
  <repo> -> Settings -> General -> scroll to the bottom -> ☑ "Template repository"
  ```
  A green **"Use this template"** button appears for anyone with access (public if you flip
  visibility, private = access-restricted).

  </details>
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
  <details><summary><strong>First run: no Google Cloud account yet?</strong> (click-path)</summary>

  The gcloud commands above assume an existing Google account with a billing-linked GCP project.
  Cloud Run and Artifact Registry compute on an active billing account (you hit a billing/403 error
  otherwise), so a from-scratch setup needs:
  ```
  1. A Google account — [accounts.google.com](https://accounts.google.com) (sign in / create).
  2. A Google Cloud account + enable a free trial at [cloud.google.com](https://cloud.google.com).
  3. Create the project you deploy to (e.g. `starter-demo-dev`) at
     [console.cloud.google.com](https://console.cloud.google.com) -> New Project.
  4. Attach a billing account at [console.cloud.google.com/billing](https://console.cloud.google.com/billing)
     (Cloud Run uses a live account; there is no free tier without billing enabled).
  ```
  Only then run the Auth command above. PROD reuses the same account and just creates a second project.
  </details>

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
  <details><summary><strong>Where to get the OpenAI-compatible API key (OpenRouter)</strong> (click-path)</summary>

  The backend calls an OpenAI-compatible `/v1/chat/completions` endpoint. The template is wired
  for an OpenRouter key (prefix `sk-or-v1-`), which needs its own (free) account:
  ```
  1. Sign up at [openrouter.ai](https://openrouter.ai).
  2. Keys → Create API key -> copy the `sk-or-v1-…` string (shown once).
  3. Add a small credit balance if needed (Chat Completions cost fractions of a cent per call).
  ```
  Pass it with `--openai-api-key` in `set-secrets.sh dev` and again for PROD. 
  *⚠️ The 2026-08-22 run captured a truncated key (`sk-or-v1-cae7…`); if `/api/v1/ai/chat` returns
  401, re-run `set-secrets.sh dev` with the full key.*
  </details>

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
  <details><summary><strong>Create a Slack Incoming Webhook</strong> (click-path)</summary>

  Needs a Slack workspace you can admin. Full click-path:
  ```
  1. Go to [api.slack.com/apps](https://api.slack.com/apps).
  2. Create New App -> From scratch -> name it + pick a workspace -> Create App.
  3. Incoming Webhooks (left) -> toggle Activate.
  4. Add New Webhook to Workspace (below) -> pick a channel -> Allow.
  5. Copy the `https://hooks.slack.com/services/T…/B…/…` URL into
     `<backend repo> -> Settings -> Secrets and variables -> Actions -> New repository secret`,
     named `SLACK_WEBHOOK`.
  ```
  *Without it, a deploy failure shows up only as a failing Actions run (the `notify` job still
  succeeds silently).*
  </details>
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

- [x] **Create your own PROD GCP project** — the template default `starter-prod` is **not usable**
      as-is: project IDs are global, and that ID already exists under some other account, so every
      Terraform call 403s (`iam.serviceAccounts.create`, WIF pool create, even
      `projects.describe` — hit live 2026-08-22). Pick a unique ID and link billing (Cloud Run /
      Artifact Registry need an active billing account, like DEV had):
  ```bash
  gcloud projects create starter-demo-prod --name="starter prod"
  gcloud billing projects link starter-demo-prod \
    --billing-account=$(gcloud billing accounts list --format='value(name)' | head -1)
  ```
  ✅ **Done 2026-08-22**: `starter-demo-prod` created, billing linked
  (`billingAccounts/008945-747CA7-523650`).
  *Chose a different ID? Update it in `infra/prod.tfvars` (`project_id`) — `prod.tfvars` below
  ships with `starter-demo-prod`. Verify ownership any time with `gcloud projects list`
  (you must see the project there before `plan.sh` can work). State isolation vs DEV is safe:
  the backend uses prefix `starter-${ENV}` per environment.*
- [x] Reviewer/protection rules on the `production` environment (GitHub Settings → Environments):
      add reviewers, disallow self-approval. The env exists; this is the approval gate.
      **Skipped — free-plan private repos don't expose "Required reviewers"** (2026-08-22; same
      family as branch protection). Interim controls instead: environment's
      **Deployment branches and tags → main only** (next box), prod workflows are
      workflow_dispatch-only, promotion is digest-only. Revisit on Team/public plan.
- [x] Set production environment's **Deployment branches and tags** to `main` only (2026-08-22,
      done via API on **both** repos; visible on free plan; stops stray-branch prod deploys).
- [x] **PROD infra apply + secrets** (your PROD project from the first box):
      `GCP_WORKLOAD_IDENTITY_PROVIDER_PROD`, `GCP_SERVICE_ACCOUNT_PROD` as GitHub secrets
      (values come from the `terraform output` at the end of `apply.sh prod`).
      ✅ **Done 2026-08-22**: apply complete (27 resources; needed one re-run — on a brand-new
      project the AR repo + WIF pool creates 403 on API-enable propagation, second pass adds
      only the stragglers); both GH `_PROD` secrets set from `terraform output`; PROD
      Secret Manager `openai-api-key` v1 + `actuator-password` v1 enabled.
  ```bash
  cd starter-backend/infra
  TFSTATE_BUCKET=starter-tfstate ./scripts/plan.sh prod   # review — should be ~27 to add
  ./scripts/apply.sh prod
  ```
  *Hit 2026-08-22: on a brand-new project, AR repo + WIF pool creates can 403 (API-enable
  propagation race) while everything else succeeds — just re-run `plan.sh` + `apply.sh`;
  the second pass adds only the stragglers.*
  ```bash
  # paste-safe: reads values straight from tfstate, never hand-copy
  gh secret set GCP_WORKLOAD_IDENTITY_PROVIDER_PROD -R patrikbego/starter-backend \
    --body "$(terraform output -raw workload_identity_provider)"
  gh secret set GCP_SERVICE_ACCOUNT_PROD            -R patrikbego/starter-backend \
    --body "$(terraform output -raw deployer_service_account)"
  ./scripts/set-secrets.sh prod --openai-api-key 'sk-or-v1-…' --actuator-password 'change-me'
  ```
  *⚠️ never run the `gh secret set` lines with literal `'<...output>'` placeholders — the
  string itself becomes the secret value (bit us 2026-08-22; re-set both afterwards).*
  ⚠️ also before re-running: `prod.tfvars` must have your real `github_organization`
  (fixed on `main` 2026-08-22 — it was `REPLACE_ME_ORG`, which would have created a WIF
  provider trusting a repo that can never match) — and the real `project_id`.
- [x] **Grant PROD read on the DEV registry** (cross-project; template gap hit live
      2026-08-22 — `Promote to PROD` verifies/pulls the image from DEV's Artifact Registry).
      **Two** grants are needed, not one: the deployer SA (describe/verify step) *and* the
      Cloud Run service agent (the actual image pull at revision creation):
  ```bash
  gcloud projects add-iam-policy-binding starter-demo-dev \
    --member=serviceAccount:starter-api@starter-demo-prod.iam.gserviceaccount.com \
    --role=roles/artifactregistry.reader
  gcloud projects add-iam-policy-binding starter-demo-dev \
    --member=serviceAccount:service-<PROD_PROJECT_NUMBER>@serverless-robot-prod.iam.gserviceaccount.com \
    --role=roles/artifactregistry.reader
  # <PROD_PROJECT_NUMBER>: gcloud projects describe starter-demo-prod --format='value(projectNumber)'
  #   here: service-599289271429@serverless-robot-prod.iam.gserviceaccount.com (both granted 2026-08-22)
  ```
- [x] Run **`Promote to PROD`** (workflow_dispatch) with the digest from the DEV `release-metadata`;
      confirm PROD smoke green. *On the free plan there is no approval prompt — dispatching the
      workflow from `main` is itself the deliberate act.*
  ✅ **Done 2026-08-22, run [32598761852](https://github.com/patrikbego/starter-backend/actions/runs/32598761852)**:
  digest `…starter-api@sha256:841bd5efe8323a28de26a6819de5b621d80f3dc9bb49e42eaa53d4b2a7f49de2`
  promoted; revision **`starter-api-prod-00002-czt`** at
  `https://starter-api-prod-pi7t6ivt6q-nw.a.run.app`; live `/health/live` + `/health/ready` 200,
  unauth `/api/v1/me` 401 (fail closed). Two failures on the way (both documented above):
  stale `PROJECT_ID` in `promote-prod.yml` (fixed `03039f1`), then the service-agent pull grant.
- [x] Reserve PROD-only firebase/other values strictly apart from DEV.
      PROD Secret Manager has its own `openai-api-key` / `actuator-password`; DEV and PROD never
      share values. *Still pending: registering a Firebase web app in the PROD project for
      mobile PROD auth (needed by §5–§6 store builds, not by the backend smoke)* — recipe below.
  <details><summary><strong>Register a Firebase web app in PROD</strong> (click-path)</summary>

  Same flow as the DEV one in §2, but in the **PROD** project (`starter-demo-prod`) and with
  strictly separated values:
  ```
  1. console.firebase.google.com → open project `starter-demo-prod` → ⚙️ Project settings
     → General → Your apps.
  2. No Web app yet? Click the Web icon, register one (hosting off).
  3. Copy `firebaseConfig.apiKey`, `authDomain`, `projectId` for the PROD mobile config.
  4. Enable Email/Password at Build → Authentication → Sign-in method (if not already on).
  5. Keep PROD values out of the DEV secrets set; the mobile PROD build gets its own bucket.
  ```
  </details>

## 4. Backend rollback drill

> **Deferred (2026-08-22)** — skipped for now, to be run later. The mechanics it exercises are
> already in place: Cloud Run retains the previous healthy revision as the rollback target, and
> both rollbacks are one command (`gcloud run services update-traffic` / redeploy prior digest).

- [ ] Follow [`starter-backend/docs/rollback_runbook.md`](../starter-backend/docs/rollback_runbook.md):
      deliberately break DEV, confirm smoke/notify fail (exercises A4), then PRIMARY (route traffic)
      and SECONDARY (redeploy good digest) rollback; record ≤5 min.
- [ ] Repeat the drill in PROD during a maintenance window.

## 5. Mobile

- [x] Set `EXPO_TOKEN` repo secret (mobile); create the EAS project (`eas init` → writes `eas.json`
      `projectId`) and install iOS/Android credentials (`eas credentials`).
      ✅ **Done 2026-08-23**: `EXPO_TOKEN` set; EAS project `@p4trik/starter-mobile`
      (id `420f02dd-d3b8-4c17-aa17-ad3ae37baaff`) linked via `eas init`. Needed two fixes first:
      - **eas-cli version**: the pin was `latest`, which now resolves to eas-cli 22.x — and 22 breaks
        against this template's Expo SDK 54 (`eas init` dies calling `expo config --type private`,
        which SDK 54 rejects). Downgraded to `>=16 <17` in all three workflows + used `eas-cli@16`
        locally. Re-pin if a later SDK changes the `--type` contract.
      - **dynamic config**: `eas init` can't write `extra.eas.projectId` into a function-based
        `app.config.ts` automatically → set it as the fallback in
        `app.config.ts` (`process.env.EAS_PROJECT_ID ?? '<id>'`) and added `owner: 'p4trik'`.
      `ios.ascAppId` + store credentials still pending — see the next box.
  <details><summary><strong>EXPO_TOKEN / EAS project / credentials</strong> (click-path)</summary>

  Two owned accounts gate this box: the **Expo account** (free) and, later, paid **store**
  accounts. The project id is not a secret, so it can live in the repo; the token is.

  **Expo account + `EXPO_TOKEN`:**
  1. Create the free [Expo](https://expo.dev) account (owns the EAS project + all builds).
  2. Token: expo.dev → top-right avatar → **Account Settings → Access → Secure Tokens →
     Create new token**; name `github-actions-starter-mobile`; copy it — shown **once**.
  3. GitHub secret: *`<mobile repo>` → Settings → Secrets and variables → Actions → New
     repository secret* → name `EXPO_TOKEN`, value = token.
     (Repo-level is fine even for `submit-release`; it runs under the `production` environment,
     but repo secrets stay available there — no env-scoped copy needed.)

  **Create the EAS project** — name it **`starter-mobile`**, matching the app `slug`. The slug is
  immutable once set and is locked to the project id, so pick it once. Tie it to the repo via CLI:
  ```bash
  npm i -g eas-cli
  cd starter-mobile
  eas init
  ```
  First prompt → keep **New project** → name `starter-mobile` → pick your account/organization.
  *⚠️ wrinkle:* `app.config.ts` here is a *dynamic* config, so `eas init` prints
  `Cannot automatically write to dynamic config at: app.config.ts` and tells you to set
  `extra.eas.projectId` yourself. Expected — capture the **project id** it creates and commit it as
  the fallback in `app.config.ts` (`process.env.EAS_PROJECT_ID ?? '<project-id>'`). EAS injects
  `EAS_PROJECT_ID` on every cloud build, so CI needs no other secret. (`eas.json` also sets
  `cli.appVersionSource: remote`, so release versions are auto-incremented by EAS, not by code.)

  **`eas credentials` / store builds (paid, not needed for the first box):**
  1. **Apple** — [developer.apple.com](https://developer.apple.com), program membership $99/yr, for
     iOS credentials + TestFlight + the `ascAppId`. **App Store Connect API key** (App Store Connect
     → Users and Access → Integrations → App Store Connect API → Team Keys → generate, Admin/App
     Manager role) registered with EAS so CI reaches TestFlight without your Apple password.
  2. **Google** — Play Console 25 one-time, create an app + upload Android keystore, enable the
     `internal` track (needed by §5's submit target).
  </details>
- [ ] Set `ios.ascAppId` in `eas.json` `submit.production.ios` (currently `REPLACE_ME`); confirm
      branch protection + Dependabot + secret scanning (never commit `.p8`/provisioning files).
      (Same Settings paths as [§1](#1-push-backend).)
  <details><summary><strong>Actually: skip this until the store-submit step</strong> (context)</summary>

  You only need `ascAppId` at the very last §5 box (`Submit release`). If App Store Connect shows
  no apps under **Apps**, nothing is wrong — the id doesn't exist yet. It's created *after* you:
  join the paid **Apple Developer Program** ($99/yr), accept the App Store Connect agreement, and
  register the app's `ios.bundleIdentifier` (= `com.starter.mobile`). Then it appears under **Apps
  → App Information → General Information → Apple ID**. So this box stays open until we reach the
  store submit — build the DEV preview first (box below).
  </details>
- [x] Reviewer/protection rules on the mobile **`production` environment** too
      (Settings → Environments → production): add reviewers, disallow self-approval. Same as
      [§3](#3-backend-production) — it gates `submit-release.yml`, i.e. the store submission.
      **Skipped — free-plan private repos don't expose "Required reviewers"** (2026-08-22);
      branch policy `main`-only is set. Add reviewers when on a Team/public plan.
- [x] Mark mobile as a **template repository** — `<mobile repo> → Settings → General → ☑ "Template repository"`.

- [ ] Build DEV preview (`build-preview` workflow); install on a device, confirm DEV identifiers.
  <details><summary><strong>Steps + settings paths</strong> (click-path)</summary>

  1. **Commit and push the mobile changes** (the eas-cli 16 pin + the EAS-project linkage already
     in `a9345f7`). This push **triggers the first preview build** (that's the workflow's only gate —
     it also fires on any later push to `main`).
  2. **Set the Firebase env vars on the EAS project** so CI can evaluate `app.config.ts` (it fails
     closed without `EXPO_PUBLIC_FIREBASE_API_KEY`, `EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN`,
     `EXPO_PUBLIC_FIREBASE_PROJECT_ID`). Path:
     [expo.dev](https://expo.dev) → **Dashboard → your EAS project → Environment variables** →
     set the DEV/preview profile values **matching your [`.env`](starter-mobile/.env)**.
  3. **Watch the run** go `eas build` → success → the workflow records the EAS build IDs.
  4. **Install on a device** — Expo emails / shows a QR install link for internal-distribution
     builds — then confirm the DEV identifiers (e.g. `com.starter.mobile.dev`, `startermobile-dev`
     scheme).
  5. **Manual re-run** (if not pushed): [github.com/patrikbego/starter-mobile](https://github.com/patrikbego/starter-mobile)
     → **Actions → EAS Build DEV → Run workflow** → branch `main` → **Run**.
  </details>
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

Current position (2026-08-22, late): **§2 + §3 complete — DEV and PROD both green.**
- DEV: run [32593078612](https://github.com/patrikbego/starter-backend/actions/runs/32593078612),
  revision `starter-api-dev-00003-n9l`, promotable.
- PROD: run [32598761852](https://github.com/patrikbego/starter-backend/actions/runs/32598761852)
  promoted that exact digest → `starter-api-prod-00002-czt` at
  `https://starter-api-prod-pi7t6ivt6q-nw.a.run.app` (health 200s, unauth `/me` 401).
- Sonar gates green (backend 93.8 / mobile 87.1 new coverage). Plan-gated items consciously
  skipped: branch protection, environment required-reviewers (both repos), Code Scanning SARIF.
Still open: §4 rollback drill (DEV, then PROD window); optional Firebase test-user secrets +
SLACK_WEBHOOK; verify the OpenRouter key actually works on `/api/v1/ai/chat`; PROD Firebase web
app for mobile auth; then §5–§7 mobile.
