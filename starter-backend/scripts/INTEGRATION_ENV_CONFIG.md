# Backend Environment Configuration

## Profiles

| Profile | Auth | Data | AI | Intended location |
|---|---|---|---|---|
| `local` | Mock | In-memory mock | Deterministic mock | Developer machine only |
| `dev-local` | DEV real/emulator | DEV Firestore | DEV provider | Developer machine |
| `dev` | DEV real | DEV Firestore | DEV provider | DEV Cloud Run |
| `prod` | PROD real | PROD Firestore | PROD provider | PROD Cloud Run |

Target v1 requires an explicit profile. The current prototype has `local` as a default; remove that default before deployment use.

## Current prototype variables

| Variable | Profiles | Purpose |
|---|---|---|
| `GCP_PROJECT_ID` | `dev-local`, `dev`, `prod` | Firestore/Firebase project |
| `GOOGLE_APPLICATION_CREDENTIALS` | `dev-local` when not using other ADC | Local credentials outside repo |
| `FIREBASE_AUTH_EMULATOR_HOST` | local/dev-local testing | Firebase Auth emulator |
| `OPENAI_API_KEY` | non-local | OpenAI-compatible provider key |
| `STARTER_CORS_ALLOWED_ORIGINS` | cloud | Browser origins |
| `ACTUATOR_PASSWORD` | cloud | Prototype admin Basic auth |

## Target additions/changes

- Remove production/local defaults for required values.
- Rename the configuration prefix/environment variable when creating a product.
- Add validated `AI_MODEL`, request timeout, rate, concurrency, and budget configuration.
- Reject wildcard production CORS.
- Validate the GCP/Firebase project matches the profile/environment.
- Keep provider keys and operation credentials in Secret Manager.
- Prefer dedicated health endpoints and operator identity over exposing general actuator metadata.

## Local

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

No cloud/provider credentials are required. Emulator variables do not automatically replace the current in-memory repository adapter.

## DEV local

```bash
export SPRING_PROFILES_ACTIVE=dev-local
export GCP_PROJECT_ID=starter-dev
export OPENAI_API_KEY=your-dev-key
./mvnw spring-boot:run
```

Use developer ADC or an external credentials file. A real or emulator-issued Firebase ID token is required; an arbitrary `test-token` works only in the explicit `local` mock profile.

## Cloud

Cloud Run sets `SPRING_PROFILES_ACTIVE=dev|prod`, project/region configuration, and Secret Manager references. Missing values stop startup in target v1.
