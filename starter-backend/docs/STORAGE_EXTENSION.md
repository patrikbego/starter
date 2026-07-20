# Storage Extension (Optional)

> **Not required for the starter MVP.** Add this when your product needs user file uploads.

This document describes the signed-URL upload pattern used in [docsera](https://github.com/patrikbego/docsera). Adapt when forking the starter for apps with file storage needs.

## Pattern overview

```text
Client                    Backend                    GCS
  |                          |                        |
  |-- POST /upload-session ->|                        |
  |<- { uploadUrl, id } -----|                        |
  |-- PUT file -------------------------------------->|
  |-- POST /confirm-upload ->|                        |
  |                          |-- verify + metadata --->|
```

The API never proxies file bytes — clients upload directly to Google Cloud Storage via short-lived signed URLs.

## Port interface

```java
public interface ObjectStoragePort {
    String generateUploadUrl(String objectPath, String contentType, long maxSizeBytes);
    String generateDownloadUrl(String objectPath);
    void delete(String objectPath);
}
```

## Implementation steps

1. **GCP:** Create bucket `{project-id}-documents` — see docsera `scripts/COMMON_GCP_SETUP.md` § Cloud Storage
2. **Adapter:** `GcsObjectStorageAdapter` with `@Profile("!local")`; `FakeGcsStorageAdapter` for `local`
3. **Service account:** `roles/storage.objectAdmin` + `roles/iam.serviceAccountTokenCreator` for signed URLs
4. **API endpoints:**
   - `POST /api/resources/upload-session`
   - `POST /api/resources/{id}/confirm-upload`
   - `GET /api/resources/{id}/download-url`
5. **Security:** Always scope objects by `userId` in path prefix: `users/{userId}/...`

## Bucket security

- Uniform bucket-level access (IAM only)
- Public access prevention enforced
- No public URLs — signed URLs only
- Short TTL on upload/download URLs (e.g. 15 minutes)

## CORS

Configure bucket CORS for client `PUT` from mobile/web origins.

## Local development

Use [fake-gcs-server](https://github.com/fsouza/fake-gcs-server):

```bash
docker run -p 4443:4443 fsouza/fake-gcs-server
export STORAGE_EMULATOR_HOST=localhost:4443
```

## Related reference

- Docsera: `docs/STORAGE_CONFIGURATION.md` in the docsera repository
- [backend_architecture_plan.md](./backend_architecture_plan.md) § Future Extensions
