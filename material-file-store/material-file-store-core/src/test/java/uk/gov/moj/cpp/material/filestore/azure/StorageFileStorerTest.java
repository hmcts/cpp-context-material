package uk.gov.moj.cpp.material.filestore.azure;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static com.azure.core.util.Context.NONE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class StorageFileStorerTest {

    @Mock
    private Logger logger;

    @Mock
    @StorageBlobContainer
    private BlobContainerClient blobContainerClient;

    @Mock
    private StorageAzureBlobConfiguration storageAzureBlobConfiguration;

    @InjectMocks
    private StorageFileStorer storageFileStorer;

    @Test
    void shouldStoreAndReturnFileId() {
        final UUID correlationId = randomUUID();
        final InputStream content = new ByteArrayInputStream(new byte[0]);
        final BlobClient blobClient = mock(BlobClient.class);
        when(storageAzureBlobConfiguration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));
        when(blobContainerClient.getBlobClient(org.mockito.ArgumentMatchers.startsWith("internal/"))).thenReturn(blobClient);

        final UUID fileId = storageFileStorer.store(StoragePath.internal(), correlationId, "test.pdf", content);

        assertThat(fileId, notNullValue());
        final ArgumentCaptor<BlobParallelUploadOptions> optionsCaptor = ArgumentCaptor.forClass(BlobParallelUploadOptions.class);
        verify(blobClient).uploadWithResponse(optionsCaptor.capture(), eq(Duration.ofSeconds(300)), eq(NONE));
        assertThat(optionsCaptor.getValue().getMetadata().get("correlation_id"), notNullValue());
        assertThat(optionsCaptor.getValue().getMetadata().get("filename"), notNullValue());
    }

    @Test
    void shouldUseBlobNameFromStoragePath() {
        final UUID correlationId = randomUUID();
        final InputStream content = new ByteArrayInputStream(new byte[0]);
        final BlobClient blobClient = mock(BlobClient.class);
        when(storageAzureBlobConfiguration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));

        final ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);
        when(blobContainerClient.getBlobClient(blobNameCaptor.capture())).thenReturn(blobClient);

        final UUID fileId = storageFileStorer.store(StoragePath.internal(), correlationId, "doc.pdf", content);

        assertThat(blobNameCaptor.getValue(), startsWith("internal/"));
        assertThat(blobNameCaptor.getValue(), containsString(fileId.toString()));
    }
}
