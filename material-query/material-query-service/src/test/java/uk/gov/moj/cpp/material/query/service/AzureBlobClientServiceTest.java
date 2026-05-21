package uk.gov.moj.cpp.material.query.service;

import static com.azure.core.util.Context.NONE;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.material.filestore.azure.AlfrescoAzureBlobConfiguration;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class AzureBlobClientServiceTest {

    @Mock
    private Logger logger;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private AlfrescoAzureBlobConfiguration alfrescoAzureBlobConfiguration;

    @Mock
    private UtcClock utcClock;

    @Mock
    private BlobClient blobClient;

    @InjectMocks
    private AzureBlobClientService azureBlobClientService;

    @Test
    void shouldUploadAndReturnAccountKeySasWhenConnectionStringPresent() {
        final String destinationFileName = randomUUID() + ".pdf";
        final ZonedDateTime now = new UtcClock().now();

        when(blobContainerClient.getBlobClient(destinationFileName)).thenReturn(blobClient);
        when(alfrescoAzureBlobConfiguration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));
        when(alfrescoAzureBlobConfiguration.getSasStartSkew()).thenReturn(Duration.ofMinutes(15));
        when(alfrescoAzureBlobConfiguration.getSasExpiry()).thenReturn(Duration.ofMinutes(30));
        when(alfrescoAzureBlobConfiguration.hasConnectionString()).thenReturn(true);
        when(utcClock.now()).thenReturn(now);
        when(blobClient.getBlobUrl()).thenReturn("https://account.blob.core.windows.net/container/" + destinationFileName);
        when(blobClient.generateSas(any(BlobServiceSasSignatureValues.class))).thenReturn("account-key-sas");

        final String result = azureBlobClientService.upload(new ByteArrayInputStream(new byte[0]), destinationFileName, "application/pdf");

        assertThat(result, containsString("account-key-sas"));

        final ArgumentCaptor<BlobParallelUploadOptions> uploadCaptor = ArgumentCaptor.forClass(BlobParallelUploadOptions.class);
        verify(blobClient).uploadWithResponse(uploadCaptor.capture(), eq(Duration.ofSeconds(300)), eq(NONE));
        assertThat(uploadCaptor.getValue().getHeaders().getContentType(), containsString("application/pdf"));
        verify(logger).info("Uploaded blob destinationFileName='{}'", destinationFileName);

        final ArgumentCaptor<BlobServiceSasSignatureValues> sasCaptor = ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
        verify(blobClient).generateSas(sasCaptor.capture());
        assertThat(sasCaptor.getValue().getExpiryTime(), is(now.toOffsetDateTime().plus(Duration.ofMinutes(30))));
    }

    @Test
    void shouldUploadAndReturnUserDelegationSasWhenNoConnectionString() {
        final String destinationFileName = randomUUID() + ".pdf";
        final ZonedDateTime now = new UtcClock().now();
        final OffsetDateTime nowOffset = now.toOffsetDateTime();
        final OffsetDateTime expectedKeyExpiry = nowOffset.plus(Duration.ofMinutes(30)).plusHours(1);
        final UserDelegationKey delegationKey = mock(UserDelegationKey.class);

        when(blobContainerClient.getBlobClient(destinationFileName)).thenReturn(blobClient);
        when(alfrescoAzureBlobConfiguration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));
        when(alfrescoAzureBlobConfiguration.getSasStartSkew()).thenReturn(Duration.ofMinutes(15));
        when(alfrescoAzureBlobConfiguration.getSasExpiry()).thenReturn(Duration.ofMinutes(30));
        when(alfrescoAzureBlobConfiguration.hasConnectionString()).thenReturn(false);
        when(utcClock.now()).thenReturn(now);
        when(blobClient.getBlobUrl()).thenReturn("https://account.blob.core.windows.net/container/" + destinationFileName);
        when(blobServiceClient.getUserDelegationKey(
                eq(nowOffset.minus(Duration.ofMinutes(15))),
                eq(expectedKeyExpiry))).thenReturn(delegationKey);
        when(blobClient.generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), eq(delegationKey))).thenReturn("delegation-sas");

        final String result = azureBlobClientService.upload(new ByteArrayInputStream(new byte[0]), destinationFileName, "application/pdf");

        assertThat(result, containsString("delegation-sas"));
        verify(blobServiceClient).getUserDelegationKey(
                eq(nowOffset.minus(Duration.ofMinutes(15))),
                eq(expectedKeyExpiry));
    }

    @Test
    void shouldReuseUserDelegationKeyOnSecondUploadWhenKeyNotExpired() {
        final String firstFile = randomUUID() + ".pdf";
        final String secondFile = randomUUID() + ".pdf";
        final ZonedDateTime now = new UtcClock().now();
        final OffsetDateTime nowOffset = now.toOffsetDateTime();
        final OffsetDateTime expectedKeyExpiry = nowOffset.plus(Duration.ofMinutes(30)).plusHours(1);
        final UserDelegationKey delegationKey = mock(UserDelegationKey.class);

        when(blobContainerClient.getBlobClient(firstFile)).thenReturn(blobClient);
        when(blobContainerClient.getBlobClient(secondFile)).thenReturn(blobClient);
        when(alfrescoAzureBlobConfiguration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));
        when(alfrescoAzureBlobConfiguration.getSasStartSkew()).thenReturn(Duration.ofMinutes(15));
        when(alfrescoAzureBlobConfiguration.getSasExpiry()).thenReturn(Duration.ofMinutes(30));
        when(alfrescoAzureBlobConfiguration.hasConnectionString()).thenReturn(false);
        when(utcClock.now()).thenReturn(now);
        when(blobClient.getBlobUrl()).thenReturn("https://account.blob.core.windows.net/container/file.pdf");
        when(blobServiceClient.getUserDelegationKey(
                eq(nowOffset.minus(Duration.ofMinutes(15))),
                eq(expectedKeyExpiry))).thenReturn(delegationKey);
        when(blobClient.generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), eq(delegationKey))).thenReturn("delegation-sas");

        azureBlobClientService.upload(new ByteArrayInputStream(new byte[0]), firstFile, "application/pdf");
        azureBlobClientService.upload(new ByteArrayInputStream(new byte[0]), secondFile, "application/pdf");

        verify(blobServiceClient, times(1)).getUserDelegationKey(
                eq(nowOffset.minus(Duration.ofMinutes(15))),
                eq(expectedKeyExpiry));
    }
}
