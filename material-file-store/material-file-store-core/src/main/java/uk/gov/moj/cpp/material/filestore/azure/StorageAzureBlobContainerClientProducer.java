package uk.gov.moj.cpp.material.filestore.azure;

import static java.lang.String.format;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * CDI producer for the general storage {@link BlobContainerClient}.
 *
 * <p>The produced client is qualified with {@link StorageBlobContainer} to distinguish it from
 * the Alfresco and archive container clients in the same WAR deployment.
 */
@ApplicationScoped
public class StorageAzureBlobContainerClientProducer {

    private static final int HTTP_CONFLICT = 409;

    @Inject
    private Logger logger;

    @Inject
    private StorageAzureBlobConfiguration configuration;

    private BlobContainerClient blobContainerClient;

    @PostConstruct
    public void initialise() {
        blobContainerClient = buildBlobContainerClient(configuration);
        try {
            blobContainerClient.createIfNotExists();
        } catch (final HttpResponseException e) {
            if (e.getResponse() != null && e.getResponse().getStatusCode() == HTTP_CONFLICT) {
                logger.warn("BlobContainerClient.createIfNotExists returned 409 Conflict for container '{}' — container already exists",
                        configuration.getContainerName());
            } else {
                throw new AzureBlobContainerClientCreationException(
                        format("Failed to create BlobContainerClient for container '%s'", configuration.getContainerName()), e);
            }
        }
    }

    @Produces
    @Dependent
    @StorageBlobContainer
    public BlobContainerClient blobContainerClient() {
        return blobContainerClient;
    }

    protected BlobContainerClient buildBlobContainerClient(final StorageAzureBlobConfiguration config) {
        final JdkHttpClientBuilder httpClientBuilder = new JdkHttpClientBuilder()
                .connectionTimeout(config.getConnectionTimeout())
                .responseTimeout(config.getResponseTimeout());
        if (config.hasConnectionString()) {
            return new BlobServiceClientBuilder()
                    .httpClient(httpClientBuilder.build())
                    .connectionString(config.getConnectionString())
                    .buildClient()
                    .getBlobContainerClient(config.getContainerName());
        }
        return new BlobServiceClientBuilder()
                .httpClient(httpClientBuilder.build())
                .credential(new DefaultAzureCredentialBuilder().build())
                .endpoint(config.getEndpoint())
                .buildClient()
                .getBlobContainerClient(config.getContainerName());
    }
}
