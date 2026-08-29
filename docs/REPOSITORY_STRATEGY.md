# Repository Strategy

## Decision

Maintain two independent template repositories:

- `starter-backend`: API, OpenAPI contract, cloud infrastructure, and backend delivery
- `starter-mobile`: Expo client, client compatibility declaration, EAS configuration, and store delivery

The current parent repository is a temporary workspace used to design and extract those repositories. It is not the target distribution model.

## Why two repositories

- Backend and mobile releases are independent.
- Access controls and secrets differ: GCP deploy credentials do not belong in the mobile repository, and store credentials do not belong in the backend repository.
- A backend can serve multiple clients, and a mobile app can move between compatible backend releases.
- CI remains smaller and failures are easier to attribute.
- Each product can adopt template updates at its own pace.

The cost is cross-repository contract coordination. A versioned OpenAPI artifact and compatibility checks address that cost more reliably than sharing source folders.

## Required contents

### Backend repository root

```text
.github/workflows/
docs/
infra/                 # OpenTofu/Terraform or an equally repeatable bootstrap
openapi/openapi.yaml
scripts/
src/
Dockerfile
pom.xml
README.md
```

### Mobile repository root

```text
.github/workflows/
app/
docs/
src/
app.config.ts
eas.json
package.json
README.md
```

Neither repository contains relative links that require a sibling checkout. Cross-repository links use a configurable canonical HTTPS URL or describe the contract generically.

## Template lifecycle

1. Protect `main`; require CI and review.
2. Tag stable template releases using semantic versions.
3. Publish release notes with breaking changes and a short migration guide.
4. Maintain a compatibility table in the mobile repository, for example `mobile-template v1.x -> backend contract v1`.
5. Create products from a known backend tag and a known mobile tag, recording both in the product docs. Derive by cloning the template with history (see [Upstream sync](./UPSTREAM_SYNC.md)) — never *Use this template*, which strips history and breaks merge-based propagation.
6. Treat created products as independent. Pull template improvements explicitly; never auto-merge upstream changes into product repositories. The explicit mechanism is `git fetch upstream && git merge upstream/main` per product repo — runbook in [Upstream sync](./UPSTREAM_SYNC.md).

## Contract ownership

The backend is the source of truth for `openapi/openapi.yaml`. Contract changes follow these rules:

- Additive changes within `/api/v1` are preferred.
- Removing or changing existing fields requires a new API version or a documented deprecation window.
- Backend CI validates the spec and implementation.
- Mobile CI checks the pinned spec or generated client and runs contract fixtures.
- A backend release must remain compatible with the currently supported mobile store version.

## Extraction plan from this workspace

After the documentation is approved:

1. Move the relevant root workflows into each child repository and remove monorepo path filters/working directories.
2. Copy only repository-owned cross-cutting docs into the appropriate child docs.
3. Add the OpenAPI contract to the backend and a contract-version pin to mobile.
4. Initialize independent Git histories or use `git filter-repo` if preserving the current history matters.
5. Configure separate remotes, branch protection, environments, WIF identities, and EAS credentials.
6. Verify both repositories from clean clones.
7. Archive the parent coordination repository or keep only a short index; do not continue tracking both codebases in two places.

The history-preservation choice should be made before initializing nested `.git` directories. This docs phase does not make that irreversible choice.
