package uk.gov.moj.cpp.material.query.service;

import static com.azure.core.util.Context.NONE;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.material.filestore.azure.AlfrescoBlobContainer;
import uk.gov.moj.cpp.material.filestore.azure.AlfrescoAzureBlobConfiguration;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

import java.io.InputStream;
import java.time.OffsetDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class AzureBlobClientService {

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @Inject
    @AlfrescoBlobContainer
    private BlobContainerClient blobContainerClient;

    @Inject
    @AlfrescoBlobContainer
    private BlobServiceClient blobServiceClient;

    @Inject
    private AlfrescoAzureBlobConfiguration alfrescoAzureBlobConfiguration;

    @Inject
    private UtcClock utcClock;

    private volatile UserDelegationKey cachedDelegationKey;
    private volatile OffsetDateTime cachedKeyExpiry;

    public String upload(final InputStream inputStream, final String destinationFileName, final String documentContentType) {
        final BlobClient blobClient = blobContainerClient.getBlobClient(destinationFileName);
        blobClient.uploadWithResponse(
                new BlobParallelUploadOptions(inputStream)
                        .setHeaders(new BlobHttpHeaders().setContentType(documentContentType)),
                alfrescoAzureBlobConfiguration.getTransferTimeout(), NONE);
        logger.info("Uploaded blob destinationFileName='{}'", destinationFileName);
        return generateSasUri(blobClient);
    }

    private String generateSasUri(final BlobClient blobClient) {
        final OffsetDateTime now = utcClock.now().toOffsetDateTime();
        final OffsetDateTime startTime = now.minus(alfrescoAzureBlobConfiguration.getSasStartSkew());
        final OffsetDateTime expiryTime = now.plus(alfrescoAzureBlobConfiguration.getSasExpiry());
        final BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);
        final BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, permissions)
                .setStartTime(startTime);
        if (alfrescoAzureBlobConfiguration.hasConnectionString()) {
            return blobClient.getBlobUrl() + "?" + blobClient.generateSas(sasValues);
        }
        return blobClient.getBlobUrl() + "?" + blobClient.generateUserDelegationSas(
                sasValues,
                getDelegationKey(startTime, expiryTime.plusHours(1)));
    }

    private UserDelegationKey getDelegationKey(final OffsetDateTime startTime, final OffsetDateTime keyExpiry) {
        if (cachedDelegationKey == null || utcClock.now().toOffsetDateTime().isAfter(cachedKeyExpiry.minus(alfrescoAzureBlobConfiguration.getSasStartSkew()))) {
            synchronized (this) {
                if (cachedDelegationKey == null || utcClock.now().toOffsetDateTime().isAfter(cachedKeyExpiry.minus(alfrescoAzureBlobConfiguration.getSasStartSkew()))) {
                    cachedDelegationKey = blobServiceClient.getUserDelegationKey(startTime, keyExpiry);
                    cachedKeyExpiry = keyExpiry;
                }
            }
        }
        return cachedDelegationKey;
    }
}
