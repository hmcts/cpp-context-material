package uk.gov.moj.cpp.material.filestore.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpResponse;
import com.azure.storage.blob.BlobContainerClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class ArchiveAzureBlobContainerClientProducerTest {

    @Mock
    private Logger logger;

    @Mock
    private ArchiveAzureBlobConfiguration configuration;

    @InjectMocks
    private ArchiveAzureBlobContainerClientProducer producer;

    @Test
    void shouldProduceBlobContainerClientAfterInitialise() {
        final BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);

        final ArchiveAzureBlobContainerClientProducer spyProducer = new ArchiveAzureBlobContainerClientProducer() {
            @Override
            protected BlobContainerClient buildBlobContainerClient(final ArchiveAzureBlobConfiguration config) {
                return blobContainerClient;
            }
        };
        setField(spyProducer, "logger", logger);
        setField(spyProducer, "configuration", configuration);

        spyProducer.initialise();

        assertThat(spyProducer.blobContainerClient(), sameInstance(blobContainerClient));
    }

    @Test
    void shouldLogWarningOn409ConflictDuringCreateIfNotExists() {
        final BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(configuration.getContainerName()).thenReturn("material-archive");
        when(httpResponse.getStatusCode()).thenReturn(409);

        final HttpResponseException conflictException = new HttpResponseException("conflict", httpResponse, null);
        when(blobContainerClient.createIfNotExists()).thenThrow(conflictException);

        final ArchiveAzureBlobContainerClientProducer spyProducer = new ArchiveAzureBlobContainerClientProducer() {
            @Override
            protected BlobContainerClient buildBlobContainerClient(final ArchiveAzureBlobConfiguration config) {
                return blobContainerClient;
            }
        };
        setField(spyProducer, "logger", logger);
        setField(spyProducer, "configuration", configuration);

        spyProducer.initialise();

        verify(logger).warn("BlobContainerClient.createIfNotExists returned 409 Conflict for container '{}' — container already exists",
                "material-archive");
    }

    @Test
    void shouldThrowCreationExceptionOnNon409ErrorDuringCreateIfNotExists() {
        final BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(configuration.getContainerName()).thenReturn("material-archive");
        when(httpResponse.getStatusCode()).thenReturn(500);

        final HttpResponseException serverException = new HttpResponseException("server error", httpResponse, null);
        when(blobContainerClient.createIfNotExists()).thenThrow(serverException);

        final ArchiveAzureBlobContainerClientProducer spyProducer = new ArchiveAzureBlobContainerClientProducer() {
            @Override
            protected BlobContainerClient buildBlobContainerClient(final ArchiveAzureBlobConfiguration config) {
                return blobContainerClient;
            }
        };
        setField(spyProducer, "logger", logger);
        setField(spyProducer, "configuration", configuration);

        assertThrows(AzureBlobContainerClientCreationException.class, spyProducer::initialise);
    }

    @Test
    void shouldThrowCreationExceptionWhenHttpResponseIsNull() {
        final BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(configuration.getContainerName()).thenReturn("material-archive");

        final HttpResponseException nullResponseException = new HttpResponseException("error", null, null);
        when(blobContainerClient.createIfNotExists()).thenThrow(nullResponseException);

        final ArchiveAzureBlobContainerClientProducer spyProducer = new ArchiveAzureBlobContainerClientProducer() {
            @Override
            protected BlobContainerClient buildBlobContainerClient(final ArchiveAzureBlobConfiguration config) {
                return blobContainerClient;
            }
        };
        setField(spyProducer, "logger", logger);
        setField(spyProducer, "configuration", configuration);

        assertThrows(AzureBlobContainerClientCreationException.class, spyProducer::initialise);
    }
}
