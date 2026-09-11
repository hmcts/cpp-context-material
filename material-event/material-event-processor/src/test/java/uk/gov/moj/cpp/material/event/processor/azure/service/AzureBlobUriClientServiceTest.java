package uk.gov.moj.cpp.material.event.processor.azure.service;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.moj.cpp.material.event.processor.azure.service.exception.CloudException;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

class AzureBlobUriClientServiceTest {

    private final AzureBlobUriClientService azureBlobUriClientService = new AzureBlobUriClientService();

    @Test
    void shouldWrapAFailureToAddressTheBlobUriInACloudException() {
        final String malformedFileUri = "not a valid blob uri";

        final CloudException cloudException = assertThrows(CloudException.class,
                () -> azureBlobUriClientService.downloadBlobContents(malformedFileUri));

        assertThat(cloudException.getMessage(), is("Error while reading blob at fileUri: " + malformedFileUri));
        assertThat(cloudException.getCause(), is(notNullValue()));
    }

    @Test
    void shouldReuseTheSameCredentialAcrossCalls() throws Exception {
        final String malformedFileUri = "not a valid blob uri";

        assertThrows(CloudException.class, () -> azureBlobUriClientService.downloadBlobContents(malformedFileUri));
        final Object firstCredential = tokenCredentialFieldOf(azureBlobUriClientService);
        assertThat(firstCredential, is(notNullValue()));

        assertThrows(CloudException.class, () -> azureBlobUriClientService.downloadBlobContents(malformedFileUri));
        final Object secondCredential = tokenCredentialFieldOf(azureBlobUriClientService);

        assertThat(secondCredential, is(sameInstance(firstCredential)));
        assertThat(secondCredential, is(instanceOf(com.azure.core.credential.TokenCredential.class)));
    }

    private Object tokenCredentialFieldOf(final AzureBlobUriClientService service) throws Exception {
        final Field field = AzureBlobUriClientService.class.getDeclaredField("tokenCredential");
        field.setAccessible(true);
        return field.get(service);
    }
}
