package uk.gov.moj.cpp.material.filestore.azure;

import com.azure.storage.blob.BlobContainerClient;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class StorageFileDeleter {

    @Inject
    @StorageBlobContainer
    private BlobContainerClient blobContainerClient;

    public void delete(final StoragePath storagePath, final UUID fileId) {
        blobContainerClient.getBlobClient(storagePath.blobName(fileId)).deleteIfExists();
    }
}
