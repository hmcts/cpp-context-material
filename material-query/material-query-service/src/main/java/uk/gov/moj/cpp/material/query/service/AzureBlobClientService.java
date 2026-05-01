package uk.gov.moj.cpp.material.query.service;

import static java.lang.String.format;

import com.microsoft.azure.storage.blob.BlobOutputStream;
import org.apache.commons.io.IOUtils;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.material.query.service.exception.AzureBlobClientException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.EnumSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AzureBlobClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureBlobClientService.class);

    @Inject
    @Value(key = "material.alfrescoAzureStorageConnectionString")
    private String storageConnectionString;

    @Inject
    @Value(key = "material.alfrescoAzureStorageContainerName")
    private String containerName;

    @Inject
    @Value(key = "material.sasUrlExpiryInMinutes", defaultValue = "30")
    private long expiryMinutes;

    private CloudBlobContainer container = null;

    private void connect() {
        try {
            final CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            final CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference(containerName);
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

    /**
     * Upload a inputStream to Azure blob storage and generate SAS URL
     *
     * @param inputStream                File to upload
     * @param destinationFileName inputStream name
     * @return SAS Download URL String
     * @throws AzureBlobClientException
     */
    public String upload(InputStream inputStream, String destinationFileName, final String documentContentType) {

        final LocalDateTime localDateTime = LocalDateTime.now();

        try {
            connect();
            // get reference to the Blob you want to generate the SAS for
            final CloudBlockBlob fileBlob = container.getBlockBlobReference(destinationFileName);
            fileBlob.getProperties().setContentType(documentContentType);
            LOGGER.info("Uploading {} inputStream to azure blob storage on {}", destinationFileName, localDateTime);
            final BlobOutputStream blobOutputStream = fileBlob.openOutputStream();
            IOUtils.copy(inputStream,blobOutputStream);
            blobOutputStream.close();
            return generateDownloadSasUrl(fileBlob);
        } catch (StorageException ex) {
            throw new AzureBlobClientException(format(
                    "Error returned from azure service. Http code: %d and error code: %s",
                    ex.getHttpStatusCode(), ex.getErrorCode()), ex);
        } catch (URISyntaxException ex) {
            throw new AzureBlobClientException("Connection URI parse error", ex);
        } catch (IOException ex) {
            throw new AzureBlobClientException("Error while uploading inputStream to azure blob storage", ex);
        } catch (InvalidKeyException ex) {
            throw new AzureBlobClientException("Invalid connection string", ex);
        } finally {
            LOGGER.info("Uploading destinationFileName={} as inputStream to azure blob storage took={} ms.", destinationFileName, Duration.between(localDateTime, LocalDateTime.now()).toMillis());
        }
    }

    /**
     * Generates a time-limited read-only SAS download URL for the given blob.
     *
     * <p>Permissions are set explicitly on the token ({@code sp=r}) rather than via a stored
     * container access policy. This avoids the Azure propagation delay (up to 30 seconds) that
     * occurs when a stored policy is overwritten, which caused intermittent 307 redirects and
     * CORS errors under concurrent uploads.
     *
     * @param fileBlob the blob to generate a download URL for
     * @return SAS URL valid from 15 minutes ago until {@code expiryMinutes} from now
     */
    String generateDownloadSasUrl(final CloudBlockBlob fileBlob) {
        try {
            final SharedAccessBlobPolicy itemPolicy = new SharedAccessBlobPolicy();
            final LocalDateTime now = LocalDateTime.now();
            final Instant startInstant = now.minusMinutes(15).atZone(ZoneOffset.UTC).toInstant();
            final Instant expiryInstant = now.plusMinutes(expiryMinutes).atZone(ZoneOffset.UTC).toInstant();
            itemPolicy.setSharedAccessStartTime(Date.from(startInstant));
            itemPolicy.setSharedAccessExpiryTime(Date.from(expiryInstant));
            itemPolicy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ));
            final String sasToken = fileBlob.generateSharedAccessSignature(itemPolicy, null);
            return String.format("%s?%s", fileBlob.getUri(), sasToken);
        } catch (StorageException ex) {
            throw new AzureBlobClientException(format(
                    "Error returned from azure service. Http code: %d and error code: %s",
                    ex.getHttpStatusCode(), ex.getErrorCode()), ex);
        } catch (InvalidKeyException ex) {
            throw new AzureBlobClientException("Invalid connection string", ex);
        }
    }

}
