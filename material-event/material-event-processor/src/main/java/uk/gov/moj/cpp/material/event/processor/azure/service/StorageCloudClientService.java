package uk.gov.moj.cpp.material.event.processor.azure.service;

import static com.azure.core.util.Context.NONE;

import uk.gov.moj.cpp.material.filestore.azure.StorageBlobContainer;
import uk.gov.moj.cpp.material.filestore.azure.StoredFile;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobRange;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class StorageCloudClientService {

    private static final long MAX_BLOB_SIZE_BYTES = 1_073_741_824L;

    @Inject
    @StorageBlobContainer
    private BlobContainerClient blobContainerClient;

    public StoredFile downloadBlobContents(final String blobName) {
        final BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        try {
            final PipedOutputStream pipedOutputStream = new PipedOutputStream();
            final PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);
            new Thread(() -> {
                try (final PipedOutputStream toClose = pipedOutputStream) {
                    blobClient.downloadStreamWithResponse(toClose, new BlobRange(0, MAX_BLOB_SIZE_BYTES),
                            null, null, false, null, NONE);
                } catch (final IOException ignored) {
                    // pipe already closed or consumer closed early
                }
            }).start();
            return new StoredFile(pipedInputStream, blobClient.getProperties().getMetadata());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
