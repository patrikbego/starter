# Upstream Sync — Flowing Starter Changes into Derived Apps

Policy and runbook for how an integration landed in `starter-backend` / `starter-mobile`
reaches every product repository derived from them. Written 2026-08-29.

The model in one line: **an app is a template clone with preserved git history, so a starter
improvement reaches the app as a normal `git merge` — done once in the starter, inherited
everywhere.**

```text
starter-backend (template, tagged v1.x)
   │  one-time derivation: clone WITH history (NEW_APP_WORKFLOW.md §1)
   ▼
<app>-backend (origin) ──"upstream" remote, read-only──▶ starter-backend
   ▲
   │ git fetch upstream + git merge upstream/main   (explicit, per app, on your schedule)
   └── every integration done once in the starter flows here: CI, infra, Stripe/Resend glue,
       auth, billing extension, docs — conflicts only in files the app actually changed
```

## 1. Rules

1. **Derive apps by cloning with history** (NEW_APP_WORKFLOW.md §1) — never *Use this
   template*, which creates a fresh history and breaks merge-based sync (§6).
2. **Syncs are explicit and opt-in per app.** Never auto-merge, never scheduled PRs from the
   template. An app owns its upgrade cadence (REPOSITORY_STRATEGY.md "Template lifecycle").
3. **Template releases are tags** (`vX.Y.Z`). Apps usually sync per release, not per commit;
   what ships in the starter lands in an app only when that app merges it.
4. **Never push to `upstream`** and **never `git push --tags` in an app repo** — the clone
   brings the template's tags along; they are local references only.
5. **Breaking contract changes (API v2) never ride a routine sync.** They are a versioned
   migration project per REPOSITORY_STRATEGY.md "Contract ownership".
6. **Respect the divergence budget (§3).** Every file an app edits is a file it pays to
   re-merge at every sync.

## 2. Remote layout in every app repo

```bash
git remote -v
```

```text
origin    git@github.com:patrikbego/<app>-backend.git    # you push here
upstream  git@github.com:patrikbego/starter-backend.git  # read-only template reference
```

## 3. Divergence budget — what an app may edit

The cheaper the divergence, the cheaper every future sync. Fix things upstream, inherit
everywhere; do not fork logic app-side.

| Area | Owner | App may edit? |
|---|---|---|
| `.github/workflows/` | template | No — app identity lives in repo `vars.*` / `secrets.*`. Backend deploys read `PROJECT_ID_DEV` / `PROJECT_ID_PROD`, `ARTIFACT_REPOSITORY`, `IMAGE_NAME`, optional `REGION`, `CORS_ALLOWED_ORIGINS_PROD` — set once via `gh variable set`, never by editing these files |
| `infra/*.tf`, `infra/scripts/` | template | No — app-specific values go through tfvars / env (`TFSTATE_PREFIX`, `TFSTATE_BUCKET`, …) |
| Platform code (auth, AI, billing, storage, error envelope, config) | template | No — improvements go into the starter first, then flow down via sync |
| `openapi/openapi.yaml` | app | Yes — this IS the app's contract; template contract additions merge in cleanly while changes stay additive within `/api/v1` |
| `infra/dev.tfvars`, `infra/prod.tfvars` | app | Yes — app identity (project ids, artifact repo, service names) |
| `app.config.ts`, `eas.json`, `package.json` (mobile) | app | Yes — app identity. Exception: template-level `eas.json` env changes that gate platform behavior (e.g. the `EXPO_PUBLIC_APP_CHECK_ENABLED` baking for `preview`/`production`, 2026) must be **ported deliberately** to existing apps — stripping them silently disables App Check and breaks native auth at enforcement flips |
| Product domain/application code, feature modules | app | Yes — keep it in clearly separated packages/dirs |
| Docs and README | both | App sections yes; template-owned sections minimized |

Rule of thumb: **if you are about to edit a template-owned file inside an app, stop** —
either the value can be parameterized (add the parameter in the starter, then sync down), or
the change genuinely belongs only to that app (then isolate it in app-owned files).

Migration note (2026-08-29): the backend deploy workflows now read their identity from repo
variables, so **`starter-backend` itself needs its variables set once** before its next
deploy: `PROJECT_ID_DEV=starter-demo-dev`, `PROJECT_ID_PROD=starter-demo-prod`,
`ARTIFACT_REPOSITORY=starter`, `IMAGE_NAME=starter-api`, and `CORS_ALLOWED_ORIGINS_PROD`
if the template's PROD serves browser clients. Missing variables fail at the "Fail fast"
step naming the variable — no silent half-renames.

## 4. Sync runbook (per app repo)

Prerequisite: clean working tree — finish or stash local work first.

```bash
cd ~/develop/myapp-backend
git fetch upstream --tags
git merge upstream/main
```

Then, in order:

1. Resolve conflicts by §3 ownership. Template-owned files where the app made no legitimate
   change: `git checkout --theirs <file>`. App-owned files: `git checkout --ours <file>`.
   Workflows and tfvars: never resolve blind — read the file.
2. Re-apply app identity check: re-run the identifier grep from STEP6 §3a
   (`git grep -n "starter" -- .github infra scripts` for backend; the §4a grep for mobile).
   A merge can re-introduce template defaults.
3. Gate locally before pushing: backend `./scripts/local-gate.sh <app>-backend`
   (or `./mvnw verify`); mobile `npm ci && npm run lint && npx tsc --noEmit && npm test`.
4. Commit the merge, push, and let the repo's CI go green before the next app.

```bash
git push origin main
```

Fan-out across all app repos — prepare the merges first, gate and push each one individually:

```bash
for repo in myvault-backend docsera-backend myvault-mobile docsera-mobile; do
  (cd ~/develop/$repo && git fetch upstream -q && git merge upstream/main --no-edit) \
    || echo "CONFLICT or unclean tree: $repo"
done
```

Repos that printed `CONFLICT or unclean tree: …` are left un-merged (git aborts cleanly);
handle them one by one via the runbook above. Record the synced template version in the app
README changelog.

## 5. One-time derivation summary

Full detail: NEW_APP_WORKFLOW.md §1 and guides/STEP6_NEW_APP_FROM_STARTER.md §3a/§4a.

```bash
gh repo create patrikbego/myapp-backend --private
git clone git@github.com:patrikbego/starter-backend.git myapp-backend
cd myapp-backend
git remote rename origin upstream
git remote add origin git@github.com:patrikbego/myapp-backend.git
git checkout -B main vX.Y.Z
git push -u origin main
```

`git checkout -B main vX.Y.Z` starts the app at the recorded template release; if no tag
exists yet, keep `upstream/main`'s tip and record the commit SHA instead. Record in the app
README: template repo + tag + commit, and `upstream = patrikbego/starter-backend`.

## 6. Migrating an app created via *Use this template*

Fresh history means no merge base — the first re-attach is the noisy one:

```bash
git remote add upstream git@github.com:patrikbego/starter-backend.git
git fetch upstream --tags
git merge upstream/main --allow-unrelated-histories
```

- With no common ancestor, any file changed on both sides conflicts even for non-overlapping
  edits. Do this immediately after creation, before app divergence grows.
- After this one merge, §4 works normally.
- If the app is already too diverged to merge: port template changes by hand per release
  (`git diff upstream/vPrev upstream/vNext`) and treat the repo as legacy — do not scale that
  to ten apps.

## 7. What never rides a sync

- API contract v2 or breaking field changes — separate migration, coordinated with the mobile
  contract pin (REPOSITORY_STRATEGY.md "Contract ownership").
- GCP project ids, Firebase projects, EAS project ids, store identifiers — app-owned by design
  (STEP6 §2), never present in template defaults.
- Secrets and credentials — never in either repository.

## 8. Common failures

| Symptom | Cause | Fix |
|---|---|---|
| `git merge` refuses: "unrelated histories" | App was created via *Use this template* | §6 one-time re-attach, then continue on §4 |
| `--ours` / `--theirs` feels backwards | Merging upstream INTO the app inverts the labels | Verify with `git diff upstream/main -- <file>` before committing |
| App identity reverted after a merge | Template file re-introduced `starter` defaults | Re-run the STEP6 identifier greps (§3a / §4a) and re-apply |
| Template tags appeared in the app repo | `git push --tags` | Delete the pushed tags from origin; push branches explicitly |
| Push to upstream rejected | Attempted `git push upstream` | Upstream is read-only; template changes go through starter PRs |
| Synced app fails CI while others pass | App-specific divergence in a template-owned file | §3 ownership review, then parameterize the divergence in the starter |
| Deploy fails at "Fail fast if app identity variables are missing" | Repo variables not set on the derived repo | `gh variable set` the named variable (STEP6 §3a) |

## Related documents

- [Repository strategy](./REPOSITORY_STRATEGY.md) — why products stay independent
- [New app workflow](./NEW_APP_WORKFLOW.md) — full product bring-up
- [Step 6 guide](../guides/STEP6_NEW_APP_FROM_STARTER.md) — step-by-step derivation
- [Environment matrix](./ENVIRONMENT_MATRIX.md) — what is per-app vs shared
