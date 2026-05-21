package uk.gov.moj.cpp.material.filestore.azure;

import static com.azure.core.util.Context.NONE;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DownloadRetryOptions;

import java.io.OutputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class StorageFileRetrieverTest {

    @Mock
    private Logger logger;

    @Mock
    @StorageBlobContainer
    private BlobContainerClient blobContainerClient;

    @InjectMocks
    private StorageFileRetriever storageFileRetriever;

    @Test
    void shouldReturnStoredFileWhenBlobExists() throws Exception {
        final UUID fileId = randomUUID();
        final BlobClient blobClient = mock(BlobClient.class);
        final BlobProperties properties = mock(BlobProperties.class);
        final Map<String, String> metadata = Map.of("filename", "test.pdf");

        when(blobContainerClient.getBlobClient("internal/" + fileId)).thenReturn(blobClient);
        when(blobClient.getProperties()).thenReturn(properties);
        when(properties.getMetadata()).thenReturn(metadata);
        doAnswer(invocation -> {
            final OutputStream outputStream = invocation.getArgument(0);
            outputStream.write(42);
            return null;
        }).when(blobClient).downloadStreamWithResponse(
                any(OutputStream.class), any(BlobRange.class), any(DownloadRetryOptions.class),
                isNull(), eq(false), eq(Duration.ofSeconds(30)), eq(NONE));

        final Optional<StoredFile> result = storageFileRetriever.retrieve(StoragePath.internal(), fileId);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getInputStream().read(), is(42));
        assertThat(result.get().getMetadata(), is(metadata));

        final ArgumentCaptor<BlobRange> rangeCaptor = ArgumentCaptor.forClass(BlobRange.class);
        verify(blobClient).downloadStreamWithResponse(
                any(OutputStream.class), rangeCaptor.capture(), any(DownloadRetryOptions.class),
                isNull(), eq(false), eq(Duration.ofSeconds(30)), eq(NONE));
        assertThat(rangeCaptor.getValue().getOffset(), is(0L));
        assertThat(rangeCaptor.getValue().getCount(), is(1_000_000_000L));
    }

    @Test
    void shouldReturnEmptyWhenBlobNotFound() {
        final UUID fileId = randomUUID();
        final BlobClient blobClient = mock(BlobClient.class);
        final BlobStorageException notFoundException = mock(BlobStorageException.class);

        when(blobContainerClient.getBlobClient("internal/" + fileId)).thenReturn(blobClient);
        when(blobClient.getProperties()).thenThrow(notFoundException);
        when(notFoundException.getStatusCode()).thenReturn(404);

        final Optional<StoredFile> result = storageFileRetriever.retrieve(StoragePath.internal(), fileId);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    void shouldRethrowNon404BlobStorageException() {
        final UUID fileId = randomUUID();
        final BlobClient blobClient = mock(BlobClient.class);
        final BlobStorageException serverException = mock(BlobStorageException.class);

        when(blobContainerClient.getBlobClient("internal/" + fileId)).thenReturn(blobClient);
        when(blobClient.getProperties()).thenThrow(serverException);
        when(serverException.getStatusCode()).thenReturn(500);

        assertThrows(BlobStorageException.class, () -> storageFileRetriever.retrieve(StoragePath.internal(), fileId));
    }
}
