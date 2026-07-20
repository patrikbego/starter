# Database

Firestore is the persistence layer for the starter backend MVP. There is no traditional ORM — repository adapters use the Firestore SDK directly behind port interfaces.

## Design principles

1. **Port interface** — `UserRepositoryPort` in `ports/`; business logic never imports Firestore types.
2. **POJO mapping** — domain models use Lombok `@Data`; adapters map via `toObject()` / `.set()`.
3. **Portable** — swap `FirestoreUserRepositoryAdapter` for a PostgreSQL adapter without changing `UserService`.

## MVP data model

### Collection: `users`

Document ID = Firebase UID.

| Field | Type | Source |
|-------|------|--------|
| `id` | string | Firebase UID |
| `email` | string | Firebase token claim |
| `displayName` | string | Firebase token or user profile |
| `createdAt` | timestamp | Set on first provision |
| `updatedAt` | timestamp | Updated on each save |

Example document:

```json
{
  "id": "abc123firebaseUid",
  "email": "user@example.com",
  "displayName": "Jane Doe",
  "createdAt": "2026-01-15T10:00:00Z",
  "updatedAt": "2026-01-15T10:00:00Z"
}
```

## UserRepositoryPort

```java
public interface UserRepositoryPort {
    Optional<User> findById(String userId);
    User save(User user);
}
```

Implemented by:

| Profile | Implementation |
|---------|----------------|
| `local` | `MockUserRepositoryAdapter` (in-memory map) |
| `dev-local`, `dev`, `prod` | `FirestoreUserRepositoryAdapter` |

## Firestore setup

### GCP (DEV / PROD)

Created during [COMMON_GCP_SETUP.md](../scripts/COMMON_GCP_SETUP.md):

```bash
gcloud firestore databases create \
  --project=starter-dev \
  --location=eur3 \
  --type=firestore-native
```

Service account needs `roles/datastore.user`.

### Local emulator

```bash
firebase emulators:start --only firestore
export FIRESTORE_EMULATOR_HOST=localhost:8080
export GOOGLE_CLOUD_PROJECT=starter-local
```

With `local` profile, `MockUserRepositoryAdapter` avoids Firestore entirely.

### Dev-local (real Firestore)

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$HOME/.gcp/starter-dev-sa.json"
export GCP_PROJECT_ID=starter-dev
```

Connects to real `starter-dev` Firestore.

## Adding domain collections

When forking for a product:

1. Add domain model in `domain/`
2. Add `XxxRepositoryPort` in `ports/`
3. Add `FirestoreXxxRepositoryAdapter` in `adapters/gcp/`
4. **Always scope queries by `userId`** — never fetch by resource ID alone

Example composite index: create via Firebase Console when Firestore returns an index-required error with a direct link.

## Security rules

Firestore security rules are **not** the primary access control for the API — the backend uses a service account with full collection access. Client apps do not connect to Firestore directly in the starter architecture.

If you add client-side Firestore access later, write restrictive rules per collection.

## Future portability

| Current | Migration path |
|---------|----------------|
| Firestore | PostgreSQL + JPA adapter implementing same port |
| Firestore vectors | pgvector, Qdrant, Pinecone behind `VectorSearchPort` |

## Related docs

- [backend_architecture_plan.md](./backend_architecture_plan.md) § Data Model
- [AUTHENTICATION.md](./AUTHENTICATION.md) — user provisioning flow
- [scripts/COMMON_GCP_SETUP.md](../scripts/COMMON_GCP_SETUP.md)
