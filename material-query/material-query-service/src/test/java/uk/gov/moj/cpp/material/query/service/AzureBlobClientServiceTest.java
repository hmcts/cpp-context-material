package uk.gov.moj.cpp.material.query.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import org.junit.jupiter.api.Test;

class AzureBlobClientServiceTest {

    private static final String CONNECTION_STRING =
            "DefaultEndpointsProtocol=https;"
                    + "AccountName=testaccount;"
                    + "AccountKey=Eby8vdM02xNOcqFeqCnf2g==;"
                    + "EndpointSuffix=core.windows.net";

    @Test
    void shouldGenerateReadOnlyBlobSasWithExplicitPermissions()
            throws StorageException, InvalidKeyException, URISyntaxException, NoSuchFieldException, IllegalAccessException {

        final AzureBlobClientService azureBlobClientService = new AzureBlobClientService();
        setExpiryMinutes(azureBlobClientService, 30L);

        final CloudStorageAccount storageAccount = CloudStorageAccount.parse(CONNECTION_STRING);
        final CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        final CloudBlobContainer container = blobClient.getContainerReference("test-container");
        final CloudBlockBlob fileBlob = container.getBlockBlobReference("test.pdf");

        final String sasUrl = azureBlobClientService.generateDownloadSasUrl(fileBlob);

        assertThat(sasUrl, containsString("sp=r"));
        assertThat(sasUrl, containsString("sr=b"));
        assertThat(sasUrl.contains("si="), is(false));
    }

    private void setExpiryMinutes(final AzureBlobClientService service, final long expiryMinutes)
            throws NoSuchFieldException, IllegalAccessException {
        final Field expiryMinutesField = AzureBlobClientService.class.getDeclaredField("expiryMinutes");
        expiryMinutesField.setAccessible(true);
        expiryMinutesField.set(service, expiryMinutes);
    }
}
