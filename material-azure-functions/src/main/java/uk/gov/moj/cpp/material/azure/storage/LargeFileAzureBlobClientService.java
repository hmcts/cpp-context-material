package uk.gov.moj.cpp.material.azure.storage;

import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.moj.cpp.material.azure.exception.AzureBlobClientException;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

public class LargeFileAzureBlobClientService {

    private static final String ERROR_MSG = "Azure %s is not specified. Please add configuration for `%s`";

    private static final String CONTAINER_NAME = ofNullable(getenv("material.alfrescoAzureStorageContainerName"))
            .orElse("largefile-blob-container");

    private CloudBlobContainer container = null;

    public void connect(final String connectionString) {

        if(isBlank(connectionString)) {
            throw new AzureBlobClientException(format(ERROR_MSG, "connection string",
                    "material.alfrescoAzureStorageConnectionString"));
        }

        try {
            final CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
            final CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference(CONTAINER_NAME);
        } catch (InvalidKeyException ex) {
            throw new AzureBlobClientException("Invalid connection string", ex);
        } catch (URISyntaxException ex) {
            throw new AzureBlobClientException("Connection URI parse error", ex);
        } catch (StorageException ex) {
            throw new AzureBlobClientException(format(
                    "Error returned from azure service. Http code: %d and error code: %s",
                    ex.getHttpStatusCode(), ex.getErrorCode()), ex);
        }
    }

    public CloudBlobContainer getContainer() {
        return container;
    }
}