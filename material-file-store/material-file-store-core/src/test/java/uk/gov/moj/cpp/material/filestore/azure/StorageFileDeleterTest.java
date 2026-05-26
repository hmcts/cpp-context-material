package uk.gov.moj.cpp.material.filestore.azure;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageFileDeleterTest {

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @InjectMocks
    private StorageFileDeleter storageFileDeleter;

    @Test
    void shouldDeleteBlobAtInternalStoragePath() {
        final UUID fileId = randomUUID();
        final String expectedBlobName = "internal/" + fileId;
        when(blobContainerClient.getBlobClient(expectedBlobName)).thenReturn(blobClient);

        storageFileDeleter.delete(StoragePath.internal(), fileId);

        verify(blobClient).deleteIfExists();
    }
}
