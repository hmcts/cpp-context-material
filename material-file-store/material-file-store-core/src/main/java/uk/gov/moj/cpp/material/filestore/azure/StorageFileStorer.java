package uk.gov.moj.cpp.material.filestore.azure;

import static com.azure.core.util.Context.NONE;
import static java.util.Map.of;
import static java.util.UUID.randomUUID;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import java.io.InputStream;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Stores blobs into the general Azure Blob storage container, recording
 * {@code correlation_id} and {@code filename} as blob metadata.
 */
@ApplicationScoped
public class StorageFileStorer {

    private static final String METADATA_CORRELATION_ID = "correlation_id";
    private static final String METADATA_FILENAME = "filename";

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @Inject
    @StorageBlobContainer
    private BlobContainerClient blobContainerClient;

    @Inject
    private StorageAzureBlobConfiguration storageAzureBlobConfiguration;

    public UUID store(final StoragePath storagePath,
                      final UUID correlationId,
                      final String filename,
                      final InputStream content) {
        final UUID fileId = randomUUID();
        final String blobName = storagePath.blobName(fileId);
        blobContainerClient.getBlobClient(blobName)
                .uploadWithResponse(
                        new BlobParallelUploadOptions(content)
                                .setMetadata(of(METADATA_CORRELATION_ID, correlationId.toString(),
                                        METADATA_FILENAME, filename)),
                        storageAzureBlobConfiguration.getTransferTimeout(), NONE);
        logger.info("Stored blob '{}' correlationId='{}' filename='{}'", blobName, correlationId, filename);
        return fileId;
    }
}
