# Step 6 — Starting a New App from the Starter Templates

**Status: written 2026-08-23.** Step-by-step guide for deriving a **new product** from the two
template repos (`patrikbego/starter-backend`, `patrikbego/starter-mobile`) on this machine,
through first deploy to store submission. Style mirrors
[`STEP5_FIRST_PUSH_CHECKLIST.md`](./STEP5_FIRST_PUSH_CHECKLIST.md) — work top-to-bottom; each
phase's Done unlocks the next.

Throughout, the example product is **`myapp`** — replace with your slug. Brand namespace in this
setup is `com.starter.*`; keep it unless you deliberately re-brand.

---

## 0. How derivation works

```text
patrikbego/starter-backend ──"Use this template"──▶ patrikbego/<app>-backend   (new repo, fresh history)
patrikbego/starter-mobile  ──"Use this template"──▶ patrikbego/<app>-mobile    (new repo, fresh history)

<app>-backend ── owns openapi/openapi.yaml (contract v1) ──▶ pinned by <app>-mobile via
              validate:contract      ◀── the ONLY integration boundary between the repos
```

- Both templates are already flagged **Template repository** (Step 5 §1/§5) — *Use this template*
  copies the code **without commit history**, which is what you want for a product.
- The repos stay independent; they meet only at the versioned API contract.
- CI/workflows come along with the copy — most renames below are about making them target the
  NEW app's cloud resources instead of the template's.

## 1. Decide every identifier ONCE (fill this in before touching anything)

| Identifier | Rule | Example for `myapp` |
|---|---|---|
| Product slug | lowercase, used in repos/EAS/schemes | `myapp` |
| GitHub repos | `<slug>-backend`, `<slug>-mobile` | `myapp-backend`, `myapp-mobile` |
| GCP project IDs | **globally unique** — `starter-prod` was already taken by someone else (hit live 2026-08-22). Prefix with brand | `starter-demo-myapp-dev`, `starter-demo-myapp-prod` |
| TF state bucket | one per app | `gs://starter-tfstate-myapp` |
| iOS bundle IDs | keep the variant suffixes — side-by-side installs depend on them | `com.starter.myapp` / `.dev` / `.preview` |
| Android package | same | `com.starter.myapp(.dev/.preview)` |
| URL schemes | template pattern = repo slug de-hyphenated (`starter-mobile` → `startermobile`) | `myappmobile-dev`, `-preview`, bare for prod |
| Display names | `"MyApp"` / `"MyApp (dev)"` / `"MyApp (preview)"` | |
| Sonar project keys | `<repo-name>` | `myapp-backend`, `myapp-mobile` |
| EAS project name & app slug | match the template convention: slug = `<slug>-mobile` (immutable once linked!) | `myapp-mobile` |

⚠️ **EAS slugs are immutable once set** and locked to the project id (Step 5 pain). Pick once.

## 2. What is SHARED vs what must be NEW per app

The core question of this guide. Same computer, same accounts:

| Credential / resource | Reuse? | Per-app action |
|---|---|---|
| GitHub account (`patrikbego`) | ✅ reuse | just create the 2 new repos via *Use this template* |
| Expo account (`@p4trik`) | ✅ reuse | **new EAS project per app** (`eas init`) |
| `EXPO_TOKEN` value | ✅ same value works for all apps | set as secret in EACH new mobile repo |
| Apple Developer Program ($99/yr) | ✅ membership reused | distribution cert is team-level → reusable; **App IDs/profiles are per bundle id** → minted fresh per app; **ascAppId exists only after you register THIS app in App Store Connect** |
| Google Cloud account + billing | ✅ reuse billing account | **new GCP projects per app, per env** (dev+prod); never share projects between products |
| Terraform state bucket | ⚠️ **NOT as-is** | `scripts/plan.sh` hard-codes `prefix=starter-${ENV}` — reusing `gs://starter-tfstate` would collide with the original project's state. Either create `gs://<brand>-tfstate-<app>` (zero code change) or parameterize the prefix first |
| OpenRouter account | ✅ reuse | **create a new API key per app** — per-app spend tracking; feed it to that app's Secret Manager only |
| Firebase | ❌ new | new project per app (DEV + PROD), new Web-app registration per project, new test users; never point an app's prod binary at another app's Firebase |
| Play Console ($25 once) | ✅ account fee is one-time | new app entry per Android app + its own signing/upload key flow |
| Sonar (localhost:9000) | ✅ reuse server | new project keys; run `local-gate.sh <new-key>` |
| `FIREBASE_WEB_API_KEY` + `FIREBASE_TEST_USER_EMAIL/_PASSWORD` (optional auth smoke, future E2E) | ❌ new values | from THIS app's DEV Firebase project; same secret names as the template |
| `SLACK_WEBHOOK` (optional deploy-failure alerts) | ❌ new if used | per-repo secret; a shared webhook would misroute alerts between apps |
| `gh` CLI auth | ✅ reuse | — |
| `gcloud` / ADC | ✅ reuse | `gcloud config set project <new-dev-project>` before terraform runs |
| eas-cli pin (`>=16 <17`) | ✅ same pin | revisit only on an SDK bump (see Step 5 §5 trap) |
| GCP `_DEV/_PROD` GitHub secrets | ❌ new values | come from THIS app's `terraform output` — WIF provider strings embed the project number |
| Free-plan limits | — | branch protection, required reviewers, Code Scanning SARIF remain unavailable (same as Step 5) |

## 3. Backend bring-up (`<app>-backend`)

### 3a. Create + rename

- [ ] *Use this template* → `patrikbego/myapp-backend` (private).
      ```
      github.com/patrikbego/starter-backend → green "Use this template" button
      → Repository name myapp-backend → Private ✓ → Create
      ```
- [ ] Clone locally next to the other repos: `~/develop/starter/<app>-backend`.
- [ ] Rename identifiers — full inventory (grep confirms nothing else):
      ```bash
      cd myapp-backend && grep -rn "starter" --include="*.yml" --include="*.tfvars" --include="*.sh" --include="*.md" \
        .github infra scripts | grep -v "\.md:"
      ```
      | File | Change |
      |---|---|
      | `infra/dev.tfvars` + `infra/prod.tfvars` | `app` → `myapp`, `project_id` → your new unique IDs, `artifact_repository_id` → e.g. `myapp-api`, `github_organization` stays `patrikbego`, `github_repository` → `myapp-backend`. ⚠️ `REPLACE_ME_ORG` breaks WIF silently — exact-repo match (Step 5 §2) |
      | `.github/workflows/deploy-dev.yml` | env block (~line 31): `PROJECT_ID` → new DEV id, `IMAGE_NAME` → `myapp-api`, `SERVICE_NAME` → `myapp-api-dev`, and **`ARTIFACT_REPOSITORY` → must equal `artifact_repository_id` in tfvars** (push targets `<region>-docker.pkg.dev/<project>/<ARTIFACT_REPOSITORY>/<IMAGE_NAME>`); CORS origins in `--set-env-vars` stay as-is (localhost ports unchanged); line 160's `--service-account starter-api@…` → `myapp-api@…` (the WIF-authed steps use secrets — nothing else is name-bound) |
      | `.github/workflows/promote-prod.yml` | same five env values for PROD + its own `--service-account`; the example digest at ~line 18 still shows the old `starter-demo-dev/starter/starter-api` path — cosmetic, update to avoid confusion |
      | `scripts/local-gate.sh` usage | pass the new key at run time: `./scripts/local-gate.sh myapp-backend` (no file edit needed) |
      | docs + `openapi.yaml` info block | cosmetic — mentions of `starter` in README/docs and the API title/version string; update at will, nothing name-bound |
      | optional template improvement | make `plan.sh`'s `prefix=starter-${ENV}` read `TFSTATE_PREFIX` (then use `myapp-${ENV}`) — otherwise rely on the per-app bucket from §2 |
- [ ] Local gate green: `./scripts/local-gate.sh myapp-backend` (Sonar shows up under the new
      key automatically once scanned).

### 3b. Push + cloud

- [ ] Push `main`; CI green.
- [ ] Create BOTH new GCP projects + link billing (reuse the billing account from Step 5):
      ```bash
      gcloud projects create starter-demo-myapp-dev
      gcloud billing projects link starter-demo-myapp-dev \
        --billing-account=$(gcloud billing accounts list --format='value(name)' | head -1)
      # repeat for starter-demo-myapp-prod
      ```
- [ ] State bucket: `gcloud storage buckets create gs://starter-tfstate-myapp --location=europe-west2`
- [ ] Auth: `gcloud auth application-default login` (skip if ADC still valid) +
      `gcloud config set project starter-demo-myapp-dev`.
- [ ] `cd infra && TFSTATE_BUCKET=starter-tfstate-myapp ./scripts/plan.sh dev` → review →
      `./scripts/apply.sh dev`.
      ⚠️ brand-new project: AR repo + WIF pool creates can 403 on API-enable propagation —
      re-run plan+apply; second pass adds only stragglers (hit live 2026-08-22).
- [ ] Paste-safe secrets from output (never hand-copy, never quote placeholders — bit us 2026-08-22):
      ```bash
      gh secret set GCP_WORKLOAD_IDENTITY_PROVIDER_DEV -R patrikbego/myapp-backend \
        --body "$(terraform output -raw workload_identity_provider)"
      gh secret set GCP_SERVICE_ACCOUNT_DEV -R patrikbego/myapp-backend \
        --body "$(terraform output -raw deployer_service_account)"
      ```
- [ ] Runtime secrets (NEW OpenRouter key for this app):
      ```bash
      ./scripts/set-secrets.sh dev --openai-api-key 'sk-or-v1-…' --actuator-password '<strong>'
      ```
- [ ] Trigger **Deploy to DEV** → chain green (`verify → build-and-push → deploy-dev → smoke-dev`),
      `/health/*` 200, unauth `/api/v1/me` 401. Copy the DEV URL — the mobile repo needs it.
- [ ] PROD mirror: `prod.tfvars` → plan/apply prod → `_PROD` secrets → **two** cross-project
      grants on the DEV registry (deployer SA **and** `service-<PROJ_NUM>@serverless-robot-prod.iam…`)
      → `./scripts/set-secrets.sh prod --openai-api-key 'sk-or-v1-…' --actuator-password '<strong>'`
      → dispatch **Promote to PROD** with the digest from DEV's `release-metadata.json`.

## 4. Mobile bring-up (`<app>-mobile`)

### 4a. Create + rename

- [ ] *Use this template* → `patrikbego/myapp-mobile`; clone alongside.
- [ ] `npm ci`; verify gates: `npm run lint && npx tsc --noEmit && npm test`.
- [ ] Rename identifiers — inventory:
      ```bash
      cd myapp-mobile && grep -rn "com.starter.mobile\|startermobile\|Starter\b\|420f02dd" \
        app.config.ts eas.json package.json src docs
      ```
      | File | Change |
      |---|---|
      | `app.config.ts` | all six bundle/package ids in `VARIANT_CONFIGS` (`com.starter.myapp{,.dev,.preview}`), three schemes (`myappmobile{,-dev,-preview}`), displayNames, and `extra.eas.projectId` fallback (**placeholder until 4c**) |
      | `eas.json` | leave `cli.version` pin; `submit.production.ios.ascAppId` stays `REPLACE_ME` until store step |
      | `package.json` | `name` field → `myapp-mobile` |
      ⚠️ **`app.config.ts` must stay 100% plain JavaScript** — the EAS cloud parses it without TS
      transformation; rules + test snippet live in Step 5 §5's plain-JS dropdown. Don't reintroduce types while renaming.

### 4b. Accounts & wiring

- [ ] `EXPO_TOKEN`: same value as always → secret on the new repo.
- [ ] New EAS project:
      ```bash
      npx eas-cli@16 init     # New project → name myapp → owner p4trik
      ```
      ⚠️ dynamic-config wrinkle again: capture the printed project id, put it in
      `extra.eas.projectId ?? '<id>'` fallback, and in `.env` (`EAS_PROJECT_ID=…`).
- [ ] Firebase: do **NOT** create a separate `*-local-*` project for DEV. Instead **add Firebase
      to the same GCP project the backend deploys to** (e.g. `myapp-dev`): console → Add project
      → import existing GCP project → register **Web app** → Email/Password enabled. One project
      per environment keeps the mobile token `aud` aligned with what the backend's Admin SDK
      verifies.
      ⚠️ Incident 2026-08-23: mobile DEV pointed at a `starter-local-b9525` Firebase project while
      the backend lived in `starter-demo-dev` → every `/api/v1/me` returned 401 (audience
      mismatch) even though sign-in itself worked. Caught only by the browser-E2E P0 spike.
      Also: a fresh project's Auth config does not exist until someone opens **Authentication →
      Get started** once in the console — there is no API to create it.
      Put the new `EXPO_PUBLIC_FIREBASE_*` trio as repo **variables**
      AND EAS `preview` environment vars (both places — runner env *and* cloud build env; Step 5 §5).
      ⚠️ name projects to respect the env guards (`src/config/envValidation.ts:31`): a PROD
      build fails if `EXPO_PUBLIC_FIREBASE_PROJECT_ID` matches `-dev([.-]|$)`; DEV/preview builds
      fail on `-prod` ids. Keep dev/prod Firebase ids unambiguous.
- [ ] Repo variables: `API_BASE_URL_DEV=https://myapp-api-dev….run.app` (your new deploy!),
      plus `API_BASE_URL_PROD` when PROD exists. Workflows read `${{ vars.* }}`. ⚠️ same
      dual-location rule as the Firebase trio: `API_BASE_URL_DEV` must ALSO exist in the EAS
      `preview` environment — repo variables reach only the runner, EAS env only the worker.
- [ ] Mint native credentials **for this app's identifiers** (per-package/per-bundle-id —
      nothing is inherited from the template):
      ```bash
      set -a; . ./.env; set +a
      EAS_BUILD_PROFILE=preview API_BASE_URL_DEV=https://…run.app \
        npx eas-cli@16 build --profile preview --platform android   # Generate keystore? yes → Ctrl-C
      EAS_BUILD_PROFILE=preview API_BASE_URL_DEV=https://…run.app \
        npx eas-cli@16 build --profile preview --platform ios       # register com.starter.myapp.preview
      ```
- [ ] Push `main` → **EAS Build DEV** goes green end-to-end (the workflow already carries the
      submission-vs-worker fix — `EAS_BUILD_PROFILE` + `API_BASE_URL_*` job env — so a fresh
      clone inherits a working pipeline; that whole saga is documented in Step 5 §5's dropdowns).

## 5. Release process for the new app (recap)

Identical mechanics to the template's — proven once here, replay per app:

```text
tag v0.1.0 ──▶ build-release (store-signed candidates, PROD config guards)
             ──▶ Submit release (workflow_dispatch) ──▶ TestFlight + Play internal
             ──▶ §6 drills (phased rollout halt/fix)  ──▶ close roadmap ticks
```

Prerequisites in order (full detail in Step 5 §3–§6 + the deferral/resume-order block there):

① PROD Firebase project + web app for this app → ② `API_BASE_URL_PROD` variable + PROD values
in EAS `production` env → ③ App Store Connect: accept agreement, register
`com.starter.myapp` → `ascAppId` into `eas.json` → ④ Play Console app + internal track →
⑤ tag → submit.

## 6. Per-app cost picture

| Item | One-time | Recurring |
|---|---|---|
| GCP projects ×2 (Cloud Run min instances 0, AR storage) | — | ~$0 idle; budget alerts available via tfvars (`budget_amount_usd`) |
| Apple Developer Program | — | $99/yr **total** (covers all apps) |
| Play Console | $25 once (account) | — |
| OpenRouter | — | per-use, fractions of a cent per AI call |
| Expo EAS builds | — | free tier queue; build minutes metered on paid plans |
| Sonar/Terraform/GitHub Actions (this scale) | — | $0 |

## 7. Definition of done (first new app)

- [ ] Both repos created via template, renamed, gates green (§3a/§4a tables complete)
- [ ] Backend: DEV deployed + smoke green; PROD promoted (digest-only)
- [ ] Mobile: EAS project linked; preview build green from CI; credentials minted for the new ids
- [ ] Contract check passes against the NEW backend (`npm run validate:contract`)
- [ ] Device install of the new preview build; `login → me → sign-out` works
- [ ] Store path done when needed (§5 order ①→⑤)
