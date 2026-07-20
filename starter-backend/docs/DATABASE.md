# Database

Firestore is the starter persistence default. Application/domain code depends on repository ports and does not import Firestore types.

## Starter data model

```text
users/{firebaseUid}
```

| Field | Type | Policy |
|---|---|---|
| `id` | string | Equals verified Firebase UID/document ID |
| `email` | string/null | Initial verified claim; synchronization policy explicit |
| `displayName` | string/null | Initial verified claim or user-managed by product policy |
| `createdAt` | timestamp | Server-set once |
| `updatedAt` | timestamp | Server-set when a real update occurs |

The current prototype implements load-or-create. Before template v1, prove concurrent first requests cannot produce inconsistent data and define claim synchronization.

## Repository boundary

```java
public interface UserRepositoryPort {
    Optional<User> findById(String id);
    User save(User user);
}
```

The port expresses application needs, not Firestore vocabulary. Product repositories should add narrower intent-based methods when authorization/query behavior matters rather than growing one generic CRUD repository.

## Ownership rule for extensions

Every user-owned record includes an ownership/tenant key and every lookup is scoped:

```text
findByIdAndOwnerId(resourceId, authenticatedUserId)
```

Looking up by ID and checking ownership later can leak existence through timing/errors and encourages missed checks.

## Environment isolation

| Profile | Persistence |
|---|---|
| `local` | In-memory fake or explicit emulator |
| `dev-local` | DEV Firestore only |
| `dev` | DEV Firestore |
| `prod` | PROD Firestore |

Use distinct runtime identities. Add a startup guard comparing intended environment/project identifiers so a DEV process cannot point at PROD by typo.

## New collection checklist

- [ ] Access patterns and required indexes documented
- [ ] Owner/tenant source is verified principal
- [ ] Maximum document/request sizes defined
- [ ] Concurrency/idempotency behavior defined
- [ ] Retention, deletion, export/restore, and privacy policy defined
- [ ] Sensitive fields excluded from logs
- [ ] Emulator/adapter tests plus cross-user denial tests added
- [ ] Backfill/migration is backward-compatible and resumable

Firestore rules do not protect Admin SDK server access; backend IAM and application authorization do.
