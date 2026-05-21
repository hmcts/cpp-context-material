package uk.gov.moj.cpp.material.filestore.azure;

import static com.azure.core.util.Context.NONE;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DownloadRetryOptions;

import org.slf4j.Logger;

/**
 * Retrieves blobs from the general Azure Blob storage container, returning both the
 * content stream and blob metadata.
 */
@ApplicationScoped
public class StorageFileRetriever {

    private static final int HTTP_NOT_FOUND = 404;
    private static final long MAX_BLOB_SIZE_BYTES = 1_000_000_000L;

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @Inject
    @StorageBlobContainer
    private BlobContainerClient blobContainerClient;

    public Optional<StoredFile> retrieve(final StoragePath storagePath, final UUID fileId) {
        final String blobName = storagePath.blobName(fileId);
        final BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        try {
            final BlobProperties properties = blobClient.getProperties();
            final PipedOutputStream pipedOutputStream = new PipedOutputStream();
            final PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);
            new Thread(() -> {
                try (final PipedOutputStream toClose = pipedOutputStream) {
                    blobClient.downloadStreamWithResponse(toClose,
                            new BlobRange(0, MAX_BLOB_SIZE_BYTES),
                            new DownloadRetryOptions(), null, false, Duration.ofSeconds(30), NONE);
                } catch (final IOException ignored) {
                    // pipe already closed or consumer closed early
                }
            }).start();
            return Optional.of(new StoredFile(pipedInputStream, properties.getMetadata()));
        } catch (final BlobStorageException e) {
            if (e.getStatusCode() == HTTP_NOT_FOUND) {
                logger.info("Blob not found blobName='{}' fileId='{}'", blobName, fileId);
                return Optional.empty();
            }
            throw e;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
