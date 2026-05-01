# Azure SAS URL Fix — Eliminate Stored-Policy Race Condition

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix intermittent CORS/307 errors caused by `AzureBlobClientService` generating SAS tokens that reference a stored access policy which is overwritten on every upload, creating a propagation-delay race condition.

**Architecture:** Extract SAS generation from `upload()` into a standalone `generateDownloadSasUrl(CloudBlockBlob)` method that sets explicit `READ` permissions on the token itself (no stored policy reference). Remove the `container.uploadPermissions()` call from the upload path entirely. The SAS token becomes self-contained — valid the moment it is issued, no container state dependency.

**Tech Stack:** Java, `com.microsoft.azure:azure-storage` SDK, JUnit 5, Hamcrest, Maven (`mvn test`)

---

## Root Cause Summary

Every call to `upload()` does two things that interact badly:

1. `container.uploadPermissions(permissions)` — overwrites the `"DownloadPolicy"` stored access policy on the container. Azure propagates policy changes in up to 30 seconds. During this window, any existing SAS token referencing `si=DownloadPolicy` is invalid.
2. `fileBlob.generateSharedAccessSignature(itemPolicy, "DownloadPolicy")` — generates a SAS that references the stored policy. If the policy hasn't propagated yet, the SAS fails with a 307 redirect. The browser follows the redirect but drops the CORS headers, producing the CORS error.

The fix: stop using stored policies. Set `sp=r` explicitly on each ad-hoc SAS token. The token carries its own permissions and needs no container state to be valid.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `material-query/material-query-service/src/main/java/uk/gov/moj/cpp/material/query/service/AzureBlobClientService.java` | Modify | Add `generateDownloadSasUrl`, refactor `upload()` |
| `material-query/material-query-service/src/test/java/uk/gov/moj/cpp/material/query/service/AzureBlobClientServiceTest.java` | Already written — make it compile and pass | Unit test for `generateDownloadSasUrl` |

---

## Implementation Notes (post-execution)

**`generateDownloadSasUrl` is package-private, not `public`:** The plan specified `public`, but during execution the code quality review confirmed that no external caller exists — the method is used only by `upload()` (same class) and `AzureBlobClientServiceTest` (same package). Making it package-private prevents external code from bypassing the upload state management. This is a deliberate architectural improvement over the plan.

**Maven tests could not run:** The artifact repository (`libraries.mdv.cpp.nonlive`) was unreachable due to a network/certificate issue. `material-viewstore-persistence:17.0.80-SNAPSHOT` could not be resolved. This is a pre-existing environment constraint; correctness was verified by code inspection and static analysis.

**Pre-existing technical debt (out of scope, raise separately):**
- `AzureBlobClientService` uses CDI field injection (`@Inject` on fields) — should be constructor injection per project coding rules
- Mutable `container` field on an `@ApplicationScoped` bean is not thread-safe under concurrent `upload()` calls — separate race condition from the one this fix addresses
- Reflection-based field injection in `AzureBlobClientServiceTest` is a symptom of the field injection anti-pattern above

---

### Task 1: Confirm the test currently fails to compile

The test file already exists at:
```
material-query/material-query-service/src/test/java/uk/gov/moj/cpp/material/query/service/AzureBlobClientServiceTest.java
```

It calls `azureBlobClientService.generateDownloadSasUrl(fileBlob)` which doesn't exist yet, so the module fails to compile.

**Files:**
- Test (exists): `material-query/material-query-service/src/test/java/uk/gov/moj/cpp/material/query/service/AzureBlobClientServiceTest.java`

- [ ] **Step 1: Run the tests to confirm compile failure**

```bash
cd material-query/material-query-service && mvn test -pl . 2>&1 | grep -E "ERROR|cannot find symbol|generateDownloadSasUrl" | head -20
```

Expected output (something like):
```
[ERROR] cannot find symbol
[ERROR]   symbol:   method generateDownloadSasUrl(CloudBlockBlob)
```

- [ ] **Step 2: Read the existing test to understand the contract**

Open `material-query/material-query-service/src/test/java/uk/gov/moj/cpp/material/query/service/AzureBlobClientServiceTest.java`.

The test asserts:
- `sasUrl` contains `sp=r` — explicit read permission on the token itself (not via stored policy)
- `sasUrl` contains `sr=b` — blob-level scope (automatic for blob SAS)
- `sasUrl` does NOT contain `si=` — no stored access policy reference

---

### Task 2: Add `generateDownloadSasUrl` to `AzureBlobClientService`

**Files:**
- Modify: `material-query/material-query-service/src/main/java/uk/gov/moj/cpp/material/query/service/AzureBlobClientService.java`

- [ ] **Step 1: Add `generateDownloadSasUrl` method**

Open `AzureBlobClientService.java`. Add the following method after the `upload` method (before the closing `}`):

```java
public String generateDownloadSasUrl(final CloudBlockBlob fileBlob) {
    try {
        final SharedAccessBlobPolicy itemPolicy = new SharedAccessBlobPolicy();
        final LocalDateTime now = LocalDateTime.now();
        final Instant startInstant = now.minusMinutes(15).atZone(ZoneOffset.UTC).toInstant();
        final Instant expiryInstant = now.plusMinutes(expiryMinutes).atZone(ZoneOffset.UTC).toInstant();
        itemPolicy.setSharedAccessStartTime(Date.from(startInstant));
        itemPolicy.setSharedAccessExpiryTime(Date.from(expiryInstant));
        itemPolicy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ));
        final String sasToken = fileBlob.generateSharedAccessSignature(itemPolicy, null);
        return String.format("%s?%s", fileBlob.getUri(), sasToken);
    } catch (StorageException ex) {
        throw new AzureBlobClientException(format(
                "Error returned from azure service. Http code: %d and error code: %s",
                ex.getHttpStatusCode(), ex.getErrorCode()), ex);
    } catch (InvalidKeyException ex) {
        throw new AzureBlobClientException("Invalid connection string", ex);
    }
}
```

Key differences from the old inline logic:
- `itemPolicy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ))` — permission baked into the token (`sp=r`)
- Second argument to `generateSharedAccessSignature` is `null` — no stored policy reference, no `si=` in the URL

- [ ] **Step 2: Run the test to verify it passes**

```bash
cd material-query/material-query-service && mvn test -Dtest=AzureBlobClientServiceTest -pl . 2>&1 | tail -20
```

Expected:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 3: Commit**

```bash
git add material-query/material-query-service/src/main/java/uk/gov/moj/cpp/material/query/service/AzureBlobClientService.java
git add material-query/material-query-service/src/test/java/uk/gov/moj/cpp/material/query/service/AzureBlobClientServiceTest.java
git commit -m "fix: add generateDownloadSasUrl with explicit READ permission, no stored policy"
```

---

### Task 3: Refactor `upload()` to use `generateDownloadSasUrl` and remove stored policy management

The `upload()` method currently:
1. Calls `container.uploadPermissions(permissions)` — overwrites the stored policy on every upload
2. Builds an `itemPolicy` with start/expiry but **no permissions** (relies on stored policy for permissions)
3. Calls `generateSharedAccessSignature(itemPolicy, "DownloadPolicy")` inline

After this task it will:
1. ~~`container.uploadPermissions(permissions)`~~ — deleted entirely
2. Call `generateDownloadSasUrl(fileBlob)` — uses self-contained SAS with explicit permissions

**Files:**
- Modify: `material-query/material-query-service/src/main/java/uk/gov/moj/cpp/material/query/service/AzureBlobClientService.java`

- [ ] **Step 1: Replace the SAS generation block in `upload()`**

Find this block in `upload()` (lines ~92–114):

```java
            final BlobContainerPermissions permissions = new BlobContainerPermissions();
            // define a read-only base policy for downloads
            final SharedAccessBlobPolicy readPolicy = new SharedAccessBlobPolicy();
            readPolicy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ));
            permissions.getSharedAccessPolicies().put("DownloadPolicy", readPolicy);
            container.uploadPermissions(permissions);
            // define rights you want to add into the SAS
            final SharedAccessBlobPolicy itemPolicy = new SharedAccessBlobPolicy();
            // calculate Start Time
            final LocalDateTime now = LocalDateTime.now();
            // SAS applicable as of 15 minutes ago
            Instant result = now.minusMinutes(15).atZone(ZoneOffset.UTC).toInstant();
            final Date startTime = Date.from(result);
            // calculate Expiration Time
            result = now.plusMinutes(expiryMinutes).atZone(ZoneOffset.UTC).toInstant();
            final Date expirationTime = Date.from(result);
            itemPolicy.setSharedAccessStartTime(startTime);
            itemPolicy.setSharedAccessExpiryTime(expirationTime);
            // generate Download SAS token
            final String sasToken = fileBlob.generateSharedAccessSignature(itemPolicy,
                    "DownloadPolicy");
            // the SAS URL is concatenation of the blob URI and the generated SAS token
            return String.format("%s?%s", fileBlob.getUri(), sasToken);
```

Replace it with:

```java
            return generateDownloadSasUrl(fileBlob);
```

- [ ] **Step 2: Remove now-unused imports**

With the stored policy code removed, the following imports are no longer used in `upload()` (they are still used in `generateDownloadSasUrl`, so check before removing):
- `BlobContainerPermissions` — used only in the deleted block; remove it
- `SharedAccessBlobPolicy` — still used in `generateDownloadSasUrl`; keep it
- `SharedAccessBlobPermissions` — still used in `generateDownloadSasUrl`; keep it

Remove this import line:
```java
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
```

- [ ] **Step 3: Run the full test suite for the module**

```bash
cd material-query/material-query-service && mvn test -pl . 2>&1 | tail -30
```

Expected:
```
[INFO] Tests run: N, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 4: Commit**

```bash
git add material-query/material-query-service/src/main/java/uk/gov/moj/cpp/material/query/service/AzureBlobClientService.java
git commit -m "fix: remove uploadPermissions race condition from upload(), delegate to generateDownloadSasUrl"
```

---

### Task 4: Run the full project build to confirm no regressions

**Files:** None (verification only)

- [ ] **Step 1: Build from the project root**

```bash
mvn clean test -pl material-query 2>&1 | tail -40
```

Expected:
```
[INFO] BUILD SUCCESS
```

If any test fails unrelated to this change, note the test name and failure — do not fix it in this task.

- [ ] **Step 2: Commit the plan doc**

```bash
git add docs/superpowers/plans/2026-05-01-azure-sas-url-fix.md
git commit -m "docs: add implementation plan for Azure SAS URL stored-policy fix"
```
