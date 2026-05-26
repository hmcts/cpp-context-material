package uk.gov.moj.cpp.material.filestore.azure;

import static java.lang.Long.parseLong;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.util.LazyValue;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * JNDI-backed configuration for the Alfresco-facing Azure Blob container.
 *
 * <p>In local development the connection string points to Azurite and the endpoint is ignored.
 * In production (AKS) the connection string defaults to {@code "DefaultAzureCredential"} so that
 * {@link AlfrescoAzureBlobContainerClientProducer} authenticates via Workload Identity.
 */
@SuppressWarnings("java:S6813")
@ApplicationScoped
public class AlfrescoAzureBlobConfiguration {

    @Inject
    @Value(key = "material.alfresco.storage.connection-string", defaultValue = "DefaultAzureCredential")
    private String connectionString;

    @Inject
    @Value(key = "material.alfresco.storage.endpoint")
    private String endpoint;

    @Inject
    @Value(key = "material.alfresco.storage.container-name")
    private String containerName;

    @Inject
    @Value(key = "material.alfresco.storage.connection-timeout-seconds", defaultValue = "10")
    private String connectionTimeoutSeconds;

    @Inject
    @Value(key = "material.alfresco.storage.response-timeout-seconds", defaultValue = "30")
    private String responseTimeoutSeconds;

    @Inject
    @Value(key = "material.alfresco.storage.transfer-timeout-seconds", defaultValue = "300")
    private String transferTimeoutSeconds;

    @Inject
    @Value(key = "material.alfresco.sas-expiry-minutes", defaultValue = "30")
    private String sasExpiryMinutes;

    @Inject
    @Value(key = "material.alfresco.sas-start-skew-minutes", defaultValue = "15")
    private String sasStartSkewMinutes;

    private final LazyValue connectionTimeoutLazyValue = new LazyValue();
    private final LazyValue responseTimeoutLazyValue = new LazyValue();
    private final LazyValue transferTimeoutLazyValue = new LazyValue();
    private final LazyValue sasExpiryLazyValue = new LazyValue();
    private final LazyValue sasStartSkewLazyValue = new LazyValue();

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

    public Duration getSasExpiry() {
        return sasExpiryLazyValue.createIfAbsent(() -> ofMinutes(parseLong(sasExpiryMinutes)));
    }

    public Duration getSasStartSkew() {
        return sasStartSkewLazyValue.createIfAbsent(() -> ofMinutes(parseLong(sasStartSkewMinutes)));
    }
}
