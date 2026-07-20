# Mobile Implementation Plan

## Phase 1 — independent repository

- [ ] Extract mobile history or initialize the approved clean history
- [ ] Move mobile workflows to the repository root
- [ ] Remove monorepo path filters and working-directory assumptions
- [ ] Make docs/links valid from a standalone clone
- [ ] Add branch protection and dependency/secret update policy

Exit: clean clone runs mobile CI without the backend source tree.

## Phase 2 — contract and configuration

- [ ] Pin the backend OpenAPI contract
- [ ] Generate/validate API types and fixtures
- [ ] Move client paths to `/api/v1`
- [ ] Implement the standard error envelope
- [ ] Replace split DEV/PROD URL names with EAS-environment values
- [ ] Add build-time DEV/PROD pairing checks

Exit: incorrect environment or stale contract fails CI/build.

## Phase 3 — authentication hardening

- [ ] Configure durable React Native Firebase Auth persistence
- [ ] Test cold-start restoration and route gating
- [ ] Coalesce concurrent token refreshes
- [ ] Clear protected query cache on user change/sign-out
- [ ] Test `401` retry-once and `403` behavior

Exit: session behavior is deterministic across cold starts and expiry.

## Phase 4 — HTTP and feature UX

- [ ] Add request timeout/abort and typed error decoding
- [ ] Preserve chat input across network/provider failures
- [ ] Respect rate-limit retry guidance
- [ ] Make chat language explicitly stateless
- [ ] Complete loading/empty/offline/error/retry states
- [ ] Complete accessibility and keyboard/safe-area review

Exit: the minimal loop behaves predictably under success and failure.

## Phase 5 — deterministic verification

- [ ] Pin one supported Node/npm/architecture path and fix the `expo lint`/`unrs-resolver` native binding failure
- [ ] Add unit/component tests with fake auth/API ports
- [ ] Add Expo Doctor and contract/config checks to CI
- [ ] Test on supported iOS/Android targets and a physical device

Exit: CI and device smoke checks are repeatable from a clean clone.

## Phase 6 — delivery

- [ ] Configure DEV preview build with internal distribution
- [ ] Configure store production build with PROD values/signing
- [ ] Upload production builds to TestFlight/Play internal testing
- [ ] Record release metadata and compatibility version
- [ ] Release tested store binaries without rebuilding
- [ ] Configure/test EAS Update channels and rollback only if OTA is enabled

Exit: preview, release candidate, release, and rollback are distinct and practiced.
