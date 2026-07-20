# Security Baseline

## Trust boundaries

- The mobile app and every HTTP request are untrusted.
- Firebase proves identity; it does not automatically prove resource authorization.
- AI input/output is untrusted content.
- Firestore and provider SDKs are infrastructure boundaries.
- CI workflows and deployment identities are part of the production attack surface.

## Authentication and authorization

- Verify a Firebase ID token on every protected request.
- Build the application principal only from verified claims.
- Make an explicit policy decision for revoked tokens and disabled/suspended users.
- Derive resource owner/tenant from the principal, never from a trusted-looking request field.
- Query protected resources by both resource ID and owner/tenant ID.
- Enforce business authorization in application services and test cross-user denial.

Target public endpoints are limited to minimal liveness. Readiness and detailed diagnostics should be restricted to deployment/operations access. Public build metadata is unnecessary.

## Fail-closed configuration

The prototype currently defaults to the `local` profile. Target v1 removes that default.

Required tests:

- no active profile does not load mock authentication;
- `dev` and `prod` cannot instantiate mock ports;
- missing secrets/configuration stops startup;
- production CORS cannot be wildcard;
- production does not enable verbose security logging.

## HTTP baseline

- Stateless sessions; CSRF disabled only for the Bearer-token API.
- One JSON error envelope for auth, validation, authorization, rate limits, and server failures.
- Bounded request bodies and field lengths.
- Explicit content types and supported methods.
- Secure headers appropriate to API/web use.
- Correlation ID accepted only after validation/length limits, or generated server-side.
- CORS restricted to actual browser origins. Native clients are not made safer by `*`.

## Secrets and identity

| Item | Storage |
|---|---|
| AI/provider key | Secret Manager, separate per environment |
| Runtime identity | Cloud Run service account |
| GitHub cloud access | OIDC/WIF short-lived token |
| Local developer access | ADC outside repository |
| Firebase public client config | Mobile build environment; not a server secret |

Never commit service-account JSON, API keys, tokens, actuator passwords, `.env` values, or copied production configuration.

## Logging and privacy

Allowed by default:

- method and route template;
- response status and duration;
- generated correlation/request ID;
- pseudonymous internal user identifier only when operationally required;
- provider outcome and token/cost counts without content.

Disallowed by default:

- Firebase tokens or authorization headers;
- passwords/secrets;
- AI prompts/responses;
- full request/response bodies;
- sensitive domain text;
- raw provider error payloads that may echo input.

Define retention and access controls for logs per product/data jurisdiction.

## AI abuse and cost security

Authenticate, validate, rate-limit, and check user/environment budget before the provider call. Bound timeout and concurrency. Model output never authorizes an action; tools and data access have deterministic server-side checks.

## Cloud and CI

- Separate DEV and PROD projects/data/secrets.
- Least-privilege runtime identities per environment.
- Protected `main` and protected production environment.
- Deployment uses immutable digests and verified provenance.
- No production build from unreviewed fork code with secrets available.
- Dependency, container, secret, and infrastructure scans have an explicit policy/owner.

## Deployment checklist

- [ ] Local mocks require explicit `local`
- [ ] Production Firebase project matches the production client
- [ ] Production CORS lists exact web origins
- [ ] Public endpoints reveal no build/environment detail
- [ ] Standard errors contain correlation ID and no provider internals
- [ ] AI limits, timeout, budget, and safe telemetry are active
- [ ] Service accounts have only required permissions
- [ ] Secrets are versioned/rotatable and absent from source/logs
- [ ] Production deploy promotes a verified digest with approval
- [ ] Cross-user authorization tests pass
- [ ] Rollback and incident owner are documented
