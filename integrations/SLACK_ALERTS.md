# SLACK_ALERTS.md — Deploy-Failure Alerts per Product App

**Status: implemented, optional (backend only).** The backend deploy workflows (`deploy-dev.yml`,
`promote-prod.yml`) end in a `notify` job that posts a Slack message when a deploy or promote
smoke fails. The mobile repo has no Slack wiring today.

**Where it lives:** the `notify` jobs in
[`starter-backend/.github/workflows/deploy-dev.yml`](../starter-backend/.github/workflows/deploy-dev.yml)
and [`promote-prod.yml`](../starter-backend/.github/workflows/promote-prod.yml).

## Per-repo secret rule

`SLACK_WEBHOOK` is a **per-repo GitHub secret** — never share one webhook across repos. A
shared webhook would misroute app A's failure alerts into app B's channel. Create one webhook
per app (one per repo is enough; DEV and PROD share the repo's webhook and the message carries
the run URL).

## Create a Slack Incoming Webhook (click-path)

Needs a Slack workspace you can admin:

1. Go to [api.slack.com/apps](https://api.slack.com/apps).
2. **Create New App → From scratch** → name it + pick a workspace → **Create App**.
3. **Incoming Webhooks** (left) → toggle **Activate**.
4. **Add New Webhook to Workspace** → pick a channel → **Allow**.
5. Copy the `https://hooks.slack.com/services/T…/B…/…` URL into
   `<backend repo> → Settings → Secrets and variables → Actions → New repository secret`,
   named `SLACK_WEBHOOK`.

## Behavior

- With the secret set: on failure the `notify` job posts
  `DEV deploy FAILED (<result>): <run-url>` (PROD likewise, plus the digest).
- Without it: a deploy failure shows up only as a failing Actions run — the `notify` job still
  succeeds silently. No alert, but no block either.
- Success posts only to the Actions log (no Slack spam); Slack is failure-only.
