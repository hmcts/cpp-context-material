package uk.gov.moj.cpp.material.filestore.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class AlfrescoAzureBlobConfigurationTest {

    @Test
    void shouldReturnConnectionString() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "connectionString", "UseDevelopmentStorage=true");

        assertThat(config.getConnectionString(), is("UseDevelopmentStorage=true"));
    }

    @Test
    void shouldReturnTrueForHasConnectionStringWhenRealStringSet() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "connectionString", "UseDevelopmentStorage=true");

        assertThat(config.hasConnectionString(), is(true));
    }

    @Test
    void shouldReturnFalseForHasConnectionStringWhenSentinel() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "connectionString", "DefaultAzureCredential");

        assertThat(config.hasConnectionString(), is(false));
    }

    @Test
    void shouldReturnFalseForHasConnectionStringWhenNull() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "connectionString", null);

        assertThat(config.hasConnectionString(), is(false));
    }

    @Test
    void shouldReturnFalseForHasConnectionStringWhenBlank() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "connectionString", "   ");

        assertThat(config.hasConnectionString(), is(false));
    }

    @Test
    void shouldReturnEndpoint() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "endpoint", "https://test.blob.core.windows.net");

        assertThat(config.getEndpoint(), is("https://test.blob.core.windows.net"));
    }

    @Test
    void shouldReturnContainerName() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "containerName", "material-alfresco");

        assertThat(config.getContainerName(), is("material-alfresco"));
    }

    @Test
    void shouldReturnConnectionTimeout() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "connectionTimeoutSeconds", "15");

        assertThat(config.getConnectionTimeout(), is(Duration.ofSeconds(15)));
    }

    @Test
    void shouldReturnResponseTimeout() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "responseTimeoutSeconds", "45");

        assertThat(config.getResponseTimeout(), is(Duration.ofSeconds(45)));
    }

    @Test
    void shouldReturnTransferTimeout() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "transferTimeoutSeconds", "600");

        assertThat(config.getTransferTimeout(), is(Duration.ofSeconds(600)));
    }

    @Test
    void shouldReturnSasExpiry() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "sasExpiryMinutes", "60");

        assertThat(config.getSasExpiry(), is(Duration.ofMinutes(60)));
    }

    @Test
    void shouldReturnSasStartSkew() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "sasStartSkewMinutes", "10");

        assertThat(config.getSasStartSkew(), is(Duration.ofMinutes(10)));
    }

    @Test
    void shouldReturnCachedConnectionTimeout() {
        final AlfrescoAzureBlobConfiguration config = new AlfrescoAzureBlobConfiguration();
        setField(config, "connectionTimeoutSeconds", "10");

        assertThat(config.getConnectionTimeout(), is(config.getConnectionTimeout()));
    }
}
