# DEV-Local Setup

`dev-local` runs the JVM on a developer machine with real DEV Firestore/Firebase/AI. It can spend money and modify shared DEV data.

## Prerequisites

- DEV cloud project provisioned
- Developer granted only required DEV access
- Application Default Credentials configured outside the repo
- DEV AI provider key with limits

Prefer:

```bash
gcloud auth application-default login
gcloud config set project starter-dev
```

If organization policy requires a credentials file, store it outside the repository and rotate it. Do not create/download broad runtime keys casually.

## Start

```bash
export SPRING_PROFILES_ACTIVE=dev-local
export GCP_PROJECT_ID=starter-dev
export OPENAI_API_KEY=your-dev-provider-key
./mvnw spring-boot:run
```

Optional Firebase Auth emulator:

```bash
export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
firebase emulators:start --project starter-dev --only auth
```

Use a token actually issued by the selected real Firebase project or emulator. The mock arbitrary-token behavior belongs only to `local`.

## Safety checklist

- [ ] Project ID contains the expected DEV slug
- [ ] No PROD credentials/keys are present in the shell/IDE run config
- [ ] Provider key has DEV budget/limits
- [ ] Logs do not contain tokens/prompts/replies
- [ ] Test data can be safely deleted

See [environment configuration](./INTEGRATION_ENV_CONFIG.md) and [DEV bootstrap](./DEV_SETUP.md).
