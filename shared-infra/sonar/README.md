# Shared SonarQube for the starter's apps

One SonarQube server used by **every app** created from this starter. Each app is its own
[project key](#project-key-convention) on the same server — one server to run/host, no per-app
server to deploy.

- Version pinned: `sonarqube:26.8.0.126808-community` (SonarQube Community Build 26.8).
- Analysis supported: **Java** (backend) **and JavaScript/TypeScript/CSS** (mobile) — JS/TS
  analysis is available in the Community Build ([docs](https://docs.sonarsource.com/sonarqube-server/2025.1/analyzing-source-code/languages/javascript-typescript-css/)).
- Stack: SonarQube container + PostgreSQL 17 (docker-compose, based on the
  [official sample](https://github.com/SonarSource/docker-sonarqube/tree/master/example-compose-files/sq-with-postgres)).

## Quick start (local)

```bash
cp .env.example .env     # change POSTGRES_PASSWORD before exposing the server
docker compose up -d
```

First boot downloads the images and initializes the DB; SonarQube becomes healthy in 1–3 minutes.

```bash
docker compose ps        # wait for sonarqube to show "healthy"
curl http://localhost:9000/api/system/status
```

- **GUI (local):** http://localhost:9000
- **First login:** `admin` / `admin` — SonarQube forces a new password on first login.

## Bootstrap admin (one time)

1. Open the GUI and log in as `admin`/`admin`; set a strong password.
2. **My Account → Security → Generate** a *Global analysis token*. Give it a name like
   `sonar-token`. This token is what CI and devs pass to the scanner; store a copy as a
   per-repo secret in each app's GitHub repository (e.g. `SONAR_TOKEN`).
3. Optionally create a user for each team member rather than sharing the admin account.

## Project key convention

Every app gets **two** project keys on the shared server, so backend and mobile analysis never
collide:

| Repo / app | Project key |
|---|---|
| `<app>` backend | `<app>-backend` |
| `<app>` mobile | `<app>-mobile` |

Examples: `shop-backend`, `shop-mobile`; `fitness-backend`, `fitness-mobile`.

Create a SonarQube project implicitly on first analysis (passing `-Dsonar.projectKey` creates it
the first time the scanner runs).

## Analyzing code

Variables (set where the scanner runs; local copies of a hosted server are fine):

- `SONAR_HOST_URL` — `http://localhost:9000` locally; the hosted URL in CI.
- `SONAR_TOKEN` — the global analysis token (never commit it; use a secret in CI).

### Backend (Java / Maven)

The backend already uses Maven; run the `sonar` goal on top of the normal build:

```bash
./mvnw -B verify sonar:sonar \
  -Dsonar.projectKey=<app>-backend \
  -Dsonar.host.url=$SONAR_HOST_URL \
  -Dsonar.token=$SONAR_TOKEN
```

(`sonar.projectName` is optional — it defaults to `sonar.projectKey`.)

**Coverage note:** as written, this reports **0% coverage** unless JaCoCo configures the build. The
starter backend does not bundle the `jacoco-maven-plugin` yet, and SonarQube's default report path
(`target/site/jacoco/jacoco.xml`) reads nothing without it. To get real coverage: add the
`jacoco-maven-plugin` prepare-agent + report executions, then run `verify sonar:sonar`.

### Mobile (TypeScript / Expo)

Use the SonarQube scanner for JavaScript/TypeScript (installed ad hoc via `npx`):

```bash
npx --yes sonarqube-scanner \
  -Dsonar.projectKey=<app>-mobile \
  -Dsonar.host.url=$SONAR_HOST_URL \
  -Dsonar.token=$SONAR_TOKEN
```

(JavaScript/TypeScript/CSS is supported in the Community Build; see the
[language docs](https://docs.sonarsource.com/sonarqube-server/2025.1/analyzing-source-code/languages/javascript-typescript-css/).)

## CI = local (decision: scans run locally for now)

**Current decision: Sonar scans run locally** (this machine, `SONAR_TOKEN`/`SONAR_HOST_URL` exported
in the shell profile). No GitHub Sonar secrets or hosted URL exist, and none are needed while the
gate is local.

When (and only when) an app enables Sonar in GitHub Actions CI, add per app repo: a `SONAR_TOKEN`
repository secret, a `SONAR_HOST_URL` repository secret pointing at a **hosted** server, and run the
same commands as the local gate so local and CI agree.

## Server location for CI (decided: local-only during Step 5)

- **Local:** `http://localhost:9000` — running, used for local scans.
- **Hosted for CI:** not needed yet. CI runners cannot reach a laptop `localhost`; revisit when the
  first app enables Sonar in CI, then record the hosted URL here.

Until then the local server validates the workflow; CI's own Sonar gate (and its secrets/hosted URL)
is a later, per-app decision.

## Operations

- **Stop / start:** `docker compose stop` · `docker compose start` (data persists in named volumes).
- **Upgrade:** bump `SONARQUBE_IMAGE` (and Postgres) in `.env`, then `docker compose up -d`.
- **Backup:** the authoritative data lives in PostgreSQL (`postgresql_data` volume) plus the
  `sonarqube_*` volumes. Back up the volumes before risky upgrades; see the
  [Backup & restore guide](https://docs.sonarsource.com/sonarqube-server/2025.1/setup-and-upgrade/maintain-operate/backup-and-restore/).
- **Reset to scratch:** `docker compose down -v` destroys the volumes — irreversible.

Keep named volumes local; do not commit `.env`. See `docker-compose.yml` for the full service list.
