package uk.gov.moj.cpp.material.event.processor.azure.service;

import static com.azure.core.util.Context.NONE;
import static java.util.Map.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.material.filestore.azure.StoredFile;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRange;

import java.io.OutputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageCloudClientServiceTest {

    @Mock
    private BlobContainerClient blobContainerClient;

    @InjectMocks
    private StorageCloudClientService storageCloudClientService;

    @Test
    void shouldDownloadBlobContentsForGivenBlobName() throws Exception {
        final String blobName = randomUUID() + "/document.pdf";
        final Map<String, String> metadata = of("name", "document.pdf");
        final BlobClient blobClient = mock(BlobClient.class);
        final BlobProperties blobProperties = mock(BlobProperties.class);

        when(blobContainerClient.getBlobClient(blobName)).thenReturn(blobClient);
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(metadata);
        doAnswer(invocation -> {
            final OutputStream outputStream = invocation.getArgument(0);
            outputStream.write(42);
            return null;
        }).when(blobClient).downloadStreamWithResponse(
                any(OutputStream.class), any(BlobRange.class),
                isNull(), isNull(), eq(false), isNull(), eq(NONE));

        final StoredFile result = storageCloudClientService.downloadBlobContents(blobName);

        assertThat(result.getMetadata(), is(metadata));
        assertThat(result.getInputStream().read(), is(42));
    }
}
