package uk.gov.moj.cpp.material.filestore.azure;

import static java.lang.Long.parseLong;
import static java.time.Duration.ofSeconds;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.util.LazyValue;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * JNDI-backed configuration for the general Azure Blob storage container.
 *
 * <p>Used for internal file storage (temp blobs between job tasks) and cloud-location
 * blob reads for the Alfresco upload pipeline.
 */
@SuppressWarnings("java:S6813")
@ApplicationScoped
public class StorageAzureBlobConfiguration {

    @Inject
    @Value(key = "material.storage.connection-string", defaultValue = "DefaultAzureCredential")
    private String connectionString;

    @Inject
    @Value(key = "material.storage.endpoint")
    private String endpoint;

    @Inject
    @Value(key = "material.storage.container-name")
    private String containerName;

    @Inject
    @Value(key = "material.storage.connection-timeout-seconds", defaultValue = "10")
    private String connectionTimeoutSeconds;

    @Inject
    @Value(key = "material.storage.response-timeout-seconds", defaultValue = "30")
    private String responseTimeoutSeconds;

    @Inject
    @Value(key = "material.storage.transfer-timeout-seconds", defaultValue = "300")
    private String transferTimeoutSeconds;

    private final LazyValue connectionTimeoutLazyValue = new LazyValue();
    private final LazyValue responseTimeoutLazyValue = new LazyValue();
    private final LazyValue transferTimeoutLazyValue = new LazyValue();

    public String getConnectionString() {
        return connectionString;
    }

    public boolean hasConnectionString() {
        return connectionString != null && !connectionString.isBlank() && !"DefaultAzureCredential".equals(connectionString);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getContainerName() {
        return containerName;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeoutLazyValue.createIfAbsent(() -> ofSeconds(parseLong(connectionTimeoutSeconds)));
    }

    public Duration getResponseTimeout() {
        return responseTimeoutLazyValue.createIfAbsent(() -> ofSeconds(parseLong(responseTimeoutSeconds)));
    }

    public Duration getTransferTimeout() {
        return transferTimeoutLazyValue.createIfAbsent(() -> ofSeconds(parseLong(transferTimeoutSeconds)));
    }
}
