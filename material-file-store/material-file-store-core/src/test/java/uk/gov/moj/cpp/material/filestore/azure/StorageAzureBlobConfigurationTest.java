package uk.gov.moj.cpp.material.filestore.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class StorageAzureBlobConfigurationTest {

    @Test
    void shouldReturnConnectionString() {
        final StorageAzureBlobConfiguration config = new StorageAzureBlobConfiguration();
        setField(config, "connectionString", "UseDevelopmentStorage=true");

        assertThat(config.getConnectionString(), is("UseDevelopmentStorage=true"));
    }

    @Test
    void shouldReturnTrueForHasConnectionStringWhenRealStringSet() {
        final StorageAzureBlobConfiguration config = new StorageAzureBlobConfiguration();
        setField(config, "connectionString", "UseDevelopmentStorage=true");

        assertThat(config.hasConnectionString(), is(true));
    }

    @Test
    void shouldReturnFalseForHasConnectionStringWhenSentinel() {
        final StorageAzureBlobConfiguration config = new StorageAzureBlobConfiguration();
        setField(config, "connectionString", "DefaultAzureCredential");

        assertThat(config.hasConnectionString(), is(false));
    }

    @Test
    void shouldReturnFalseForHasConnectionStringWhenNull() {
        final StorageAzureBlobConfiguration config = new StorageAzureBlobConfiguration();
        setField(config, "connectionString", null);

        assertThat(config.hasConnectionString(), is(false));
    }

    @Test
    void shouldReturnFalseForHasConnectionStringWhenBlank() {
        final StorageAzureBlobConfiguration config = new StorageAzureBlobConfiguration();
        setField(config, "connectionString", "  ");

        assertThat(config.hasConnectionString(), is(false));
    }

    @Test
    void shouldReturnEndpoint() {
        final StorageAzureBlobConfiguration config = new StorageAzureBlobConfiguration();
        setField(config, "endpoint", "https://storage.blob.core.windows.net");

        assertThat(config.getEndpoint(), is("https://storage.blob.core.windows.net"));
    }

    @Test
    void shouldReturnContainerName() {
        final StorageAzureBlobConfiguration config = new StorageAzureBlobConfiguration();
        setField(config, "containerName", "material-storage");

        assertThat(config.getContainerName(), is("material-storage"));
    }

    @Test
    void shouldReturnConnectionTimeout() {
        final StorageAzureBlobConfiguration config = new StorageAzureBlobConfiguration();
        setField(config, "connectionTimeoutSeconds", "20");

        assertThat(config.getConnectionTimeout(), is(Duration.ofSeconds(20)));
    }

    @Test
    void shouldReturnResponseTimeout() {
        final StorageAzureBlobConfiguration config = new StorageAzureBlobConfiguration();
        setField(config, "responseTimeoutSeconds", "60");

        assertThat(config.getResponseTimeout(), is(Duration.ofSeconds(60)));
    }

    @Test
    void shouldReturnTransferTimeout() {
        final StorageAzureBlobConfiguration config = new StorageAzureBlobConfiguration();
        setField(config, "transferTimeoutSeconds", "120");

        assertThat(config.getTransferTimeout(), is(Duration.ofSeconds(120)));
    }
}
