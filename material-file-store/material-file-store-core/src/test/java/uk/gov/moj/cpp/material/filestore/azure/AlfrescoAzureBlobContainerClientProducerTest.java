package uk.gov.moj.cpp.material.filestore.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpResponse;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class AlfrescoAzureBlobContainerClientProducerTest {

    @Mock
    private Logger logger;

    @Mock
    private AlfrescoAzureBlobConfiguration configuration;

    @InjectMocks
    private AlfrescoAzureBlobContainerClientProducer producer;

    @Test
    void shouldProduceBlobContainerClientAfterInitialise() {
        final BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        final BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(configuration.getContainerName()).thenReturn("material-alfresco");
        when(blobServiceClient.getBlobContainerClient("material-alfresco")).thenReturn(blobContainerClient);

        final AlfrescoAzureBlobContainerClientProducer spyProducer = new AlfrescoAzureBlobContainerClientProducer() {
            @Override
            protected BlobServiceClient buildBlobServiceClient(final AlfrescoAzureBlobConfiguration config) {
                return blobServiceClient;
            }
        };
        setField(spyProducer, "logger", logger);
        setField(spyProducer, "configuration", configuration);

        spyProducer.initialise();

        assertThat(spyProducer.blobContainerClient(), sameInstance(blobContainerClient));
        assertThat(spyProducer.blobServiceClient(), sameInstance(blobServiceClient));
    }

    @Test
    void shouldLogWarningOn409ConflictDuringCreateIfNotExists() {
        final BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        final BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(configuration.getContainerName()).thenReturn("material-alfresco");
        when(blobServiceClient.getBlobContainerClient("material-alfresco")).thenReturn(blobContainerClient);
        when(httpResponse.getStatusCode()).thenReturn(409);

        final HttpResponseException conflictException = new HttpResponseException("conflict", httpResponse, null);
        when(blobContainerClient.createIfNotExists()).thenThrow(conflictException);

        final AlfrescoAzureBlobContainerClientProducer spyProducer = new AlfrescoAzureBlobContainerClientProducer() {
            @Override
            protected BlobServiceClient buildBlobServiceClient(final AlfrescoAzureBlobConfiguration config) {
                return blobServiceClient;
            }
        };
        setField(spyProducer, "logger", logger);
        setField(spyProducer, "configuration", configuration);

        spyProducer.initialise();

        verify(logger).warn("BlobContainerClient.createIfNotExists returned 409 Conflict for container '{}' — container already exists",
                "material-alfresco");
    }

    @Test
    void shouldThrowCreationExceptionOnNon409ErrorDuringCreateIfNotExists() {
        final BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        final BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(configuration.getContainerName()).thenReturn("material-alfresco");
        when(blobServiceClient.getBlobContainerClient("material-alfresco")).thenReturn(blobContainerClient);
        when(httpResponse.getStatusCode()).thenReturn(500);

        final HttpResponseException serverException = new HttpResponseException("server error", httpResponse, null);
        when(blobContainerClient.createIfNotExists()).thenThrow(serverException);

        final AlfrescoAzureBlobContainerClientProducer spyProducer = new AlfrescoAzureBlobContainerClientProducer() {
            @Override
            protected BlobServiceClient buildBlobServiceClient(final AlfrescoAzureBlobConfiguration config) {
                return blobServiceClient;
            }
        };
        setField(spyProducer, "logger", logger);
        setField(spyProducer, "configuration", configuration);

        assertThrows(AzureBlobContainerClientCreationException.class, spyProducer::initialise);
    }

    @Test
    void shouldThrowCreationExceptionWhenHttpResponseIsNull() {
        final BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        final BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(configuration.getContainerName()).thenReturn("material-alfresco");
        when(blobServiceClient.getBlobContainerClient("material-alfresco")).thenReturn(blobContainerClient);

        final HttpResponseException nullResponseException = new HttpResponseException("error", null, null);
        when(blobContainerClient.createIfNotExists()).thenThrow(nullResponseException);

        final AlfrescoAzureBlobContainerClientProducer spyProducer = new AlfrescoAzureBlobContainerClientProducer() {
            @Override
            protected BlobServiceClient buildBlobServiceClient(final AlfrescoAzureBlobConfiguration config) {
                return blobServiceClient;
            }
        };
        setField(spyProducer, "logger", logger);
        setField(spyProducer, "configuration", configuration);

        assertThrows(AzureBlobContainerClientCreationException.class, spyProducer::initialise);
    }

    @Test
    void shouldReturnTrueForHasConnectionString() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "connectionString", "UseDevelopmentStorage=true");

        assertThat(config.hasConnectionString(), is(true));
    }
}
