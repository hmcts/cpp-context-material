package uk.gov.moj.cpp.material.event.processor.azure.service;

import static java.util.Objects.isNull;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.material.event.processor.azure.service.exception.CloudException;

import javax.inject.Inject;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.specialized.BlobInputStream;

public class StorageCloudClientService {

    @Inject
    @Value(key = "azure.storage.connection-string")
    private String storageConnectionString;

    @Inject
    @Value(key = "azure.storage.container-name")
    private String azureStorageContainerName;

    private BlobContainerClient blobContainerClient;

    private void setBlobContainerClient() {
        if (isNull(blobContainerClient)) {
            try{
                blobContainerClient = new BlobContainerClientBuilder()
                        .connectionString(storageConnectionString)
                        .containerName(azureStorageContainerName)
                        .buildClient();
            } catch (Exception e) {
                throw new CloudException("Error while creating BlobContainerClient", e);
            }
        }
    }

    /**
     * Downloads the content of a blob
     *
     * @param blobName - String
     * @return The download content of the blob as a String,or an empty string if there's an error.
     */
    public BlobInputStream downloadBlobContents(String blobName) {
        setBlobContainerClient();
        return blobContainerClient.getBlobClient(blobName).openInputStream(); // to work on
    }
    
    /**
     *
     *  uncomment when yoo want to test but will eventually go when stable
     public static void main(String[] args) {
     final BlobInputStream theStream = storageCloudClientService.downloadBlobContents("28DI1855718/test1.pdf");
     }
     *
     */
}
