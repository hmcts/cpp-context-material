package uk.gov.moj.cpp.material.filestore.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class ArchiveAzureBlobConfigurationTest {

    @Test
    void shouldReturnConnectionString() {
        final ArchiveAzureBlobConfiguration config = new ArchiveAzureBlobConfiguration();
        setField(config, "connectionString", "UseDevelopmentStorage=true");

        assertThat(config.getConnectionString(), is("UseDevelopmentStorage=true"));
    }

    @Test
    void shouldReturnTrueForHasConnectionStringWhenRealStringSet() {
        final ArchiveAzureBlobConfiguration config = new ArchiveAzureBlobConfiguration();
        setField(config, "connectionString", "UseDevelopmentStorage=true");

        assertThat(config.hasConnectionString(), is(true));
    }

    @Test
    void shouldReturnFalseForHasConnectionStringWhenSentinel() {
        final ArchiveAzureBlobConfiguration config = new ArchiveAzureBlobConfiguration();
        setField(config, "connectionString", "DefaultAzureCredential");

        assertThat(config.hasConnectionString(), is(false));
    }

    @Test
    void shouldReturnFalseForHasConnectionStringWhenNull() {
        final ArchiveAzureBlobConfiguration config = new ArchiveAzureBlobConfiguration();
        setField(config, "connectionString", null);

        assertThat(config.hasConnectionString(), is(false));
    }

    @Test
    void shouldReturnFalseForHasConnectionStringWhenBlank() {
        final ArchiveAzureBlobConfiguration config = new ArchiveAzureBlobConfiguration();
        setField(config, "connectionString", " ");

        assertThat(config.hasConnectionString(), is(false));
    }

    @Test
    void shouldReturnEndpoint() {
        final ArchiveAzureBlobConfiguration config = new ArchiveAzureBlobConfiguration();
        setField(config, "endpoint", "https://archive.blob.core.windows.net");

        assertThat(config.getEndpoint(), is("https://archive.blob.core.windows.net"));
    }

    @Test
    void shouldReturnContainerName() {
        final ArchiveAzureBlobConfiguration config = new ArchiveAzureBlobConfiguration();
        setField(config, "containerName", "material-archive");

        assertThat(config.getContainerName(), is("material-archive"));
    }

    @Test
    void shouldReturnConnectionTimeout() {
        final ArchiveAzureBlobConfiguration config = new ArchiveAzureBlobConfiguration();
        setField(config, "connectionTimeoutSeconds", "10");

        assertThat(config.getConnectionTimeout(), is(Duration.ofSeconds(10)));
    }

    @Test
    void shouldReturnResponseTimeout() {
        final ArchiveAzureBlobConfiguration config = new ArchiveAzureBlobConfiguration();
        setField(config, "responseTimeoutSeconds", "30");

        assertThat(config.getResponseTimeout(), is(Duration.ofSeconds(30)));
    }

    @Test
    void shouldReturnTransferTimeout() {
        final ArchiveAzureBlobConfiguration config = new ArchiveAzureBlobConfiguration();
        setField(config, "transferTimeoutSeconds", "300");

        assertThat(config.getTransferTimeout(), is(Duration.ofSeconds(300)));
    }
}
