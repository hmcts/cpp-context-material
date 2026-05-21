package uk.gov.moj.cpp.material.filestore.azure;

import static java.lang.String.format;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * CDI producer for the Alfresco-facing {@link BlobContainerClient} and {@link BlobServiceClient}.
 *
 * <p>Resolves credentials in this order:
 * <ol>
 *   <li>If {@code material.alfresco.storage.connection-string} is non-blank, connects using the
 *       connection string directly — intended for local development against Azurite only.</li>
 *   <li>Otherwise uses {@code DefaultAzureCredential} with the endpoint — on AKS this resolves
 *       to Workload Identity automatically.</li>
 * </ol>
 *
 * <p>Both produced clients are qualified with {@link AlfrescoBlobContainer} to distinguish them
 * from the storage and archive container clients in the same WAR deployment.
 *
 * <p><strong>Scope note:</strong> {@link BlobContainerClient} is {@code final} — Weld cannot proxy
 * it, so the {@link Produces} methods are {@link Dependent} rather than {@code @ApplicationScoped}.
 * A single shared instance is constructed once in {@link #initialise()} and returned on every
 * injection point.
 */
@ApplicationScoped
public class AlfrescoAzureBlobContainerClientProducer {

    private static final int HTTP_CONFLICT = 409;

    @Inject
    private Logger logger;

    @Inject
    private AlfrescoAzureBlobConfiguration configuration;

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient blobContainerClient;

    @PostConstruct
    public void initialise() {
        blobServiceClient = buildBlobServiceClient(configuration);
        blobContainerClient = blobServiceClient.getBlobContainerClient(configuration.getContainerName());
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
    @AlfrescoBlobContainer
    public BlobContainerClient blobContainerClient() {
        return blobContainerClient;
    }

    @Produces
    @Dependent
    @AlfrescoBlobContainer
    public BlobServiceClient blobServiceClient() {
        return blobServiceClient;
    }

    protected BlobServiceClient buildBlobServiceClient(final AlfrescoAzureBlobConfiguration config) {
        final JdkHttpClientBuilder httpClientBuilder = new JdkHttpClientBuilder()
                .connectionTimeout(config.getConnectionTimeout())
                .responseTimeout(config.getResponseTimeout());
        if (config.hasConnectionString()) {
            return new BlobServiceClientBuilder()
                    .httpClient(httpClientBuilder.build())
                    .connectionString(config.getConnectionString())
                    .buildClient();
        }
        return new BlobServiceClientBuilder()
                .httpClient(httpClientBuilder.build())
                .credential(new DefaultAzureCredentialBuilder().build())
                .endpoint(config.getEndpoint())
                .buildClient();
    }
}
