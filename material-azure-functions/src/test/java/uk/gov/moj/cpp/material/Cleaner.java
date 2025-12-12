package uk.gov.moj.cpp.material;

import static java.lang.String.format;

import uk.gov.moj.cpp.material.azure.exception.AzureBlobClientException;

import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobListingDetails;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cleaner.class.getName());

    private final CloudBlobContainer cloudBlobContainer;

    private int largeFilesExpiryInMinutes = 5;

    public Cleaner(final CloudBlobContainer azureBlobClientService) {
        this.cloudBlobContainer = azureBlobClientService;
    }

    public void clean() {
        try {
            final EnumSet<BlobListingDetails> enumBlobListingDetails = EnumSet.of(BlobListingDetails.METADATA);

            final List<ListBlobItem> listBlobItems = toStream(cloudBlobContainer.listBlobs(null, true, enumBlobListingDetails, null, null))
                    .collect(Collectors.toList());

            final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            Instant result = now.minusMinutes(largeFilesExpiryInMinutes).atZone(ZoneOffset.UTC).toInstant();
            final Date timeOut = Date.from(result);
            LOGGER.info(largeFilesExpiryInMinutes + " = timeOutMinutes");
            final Set<CloudBlobDirectory> deletionDirectories = new HashSet<>();
            listBlobItems.forEach(blobItem -> {

                if (blobItem instanceof CloudBlob) {
                    final BlobProperties properties = ((CloudBlob) blobItem).getProperties();
                    if (isOlderThanDeletionThreshold(((CloudBlob) blobItem).getName(), properties.getLastModified(), timeOut)) {
                        try {
                            deletionDirectories.add(blobItem.getParent());
                        } catch (URISyntaxException ex) {
                            throw new AzureBlobClientException("Connection URI parse error", ex);
                        } catch (StorageException ex) {
                            throw new AzureBlobClientException(format(
                                    "Error returned from azure service. Http code: %d and error code: %s",
                                    ex.getHttpStatusCode(), ex.getErrorCode()), ex);
                        }
                    }
                }
            });
            LOGGER.info("Deleting directories in container = " + deletionDirectories.size());
            //delete(deletionDirectories);
        } catch (Exception exp) {
            LOGGER.error("Exception= " + exp);
        }
    }

    public boolean isOlderThanDeletionThreshold( final String fileName,final Date lastModified, final Date timeOut) {
        try {
            if (lastModified.before(timeOut)) {
                LOGGER.info(fileName + " is IN deletion threshold, lastModified=" + lastModified);
                return true;
            } else {
                LOGGER.info(fileName + " is NOT in threshold, lastModified=" + lastModified);
            }
        } catch (DateTimeException e) {
            LOGGER.error("DateTimeException" + e);
        }
        return false;
    }

    private void delete(final Set<CloudBlobDirectory> directories) throws URISyntaxException, StorageException {
        for (final CloudBlobDirectory dir : directories) {
            deleteDirectoryContents(dir);
        }
    }

    private void deleteDirectoryContents(final CloudBlobDirectory dir) throws URISyntaxException, StorageException {
        final List<CloudBlockBlob> blobs;
        blobs = toStream(dir.listBlobs())
                    .filter(blobItem -> blobItem instanceof CloudBlockBlob)
                    .map(blobItem -> (CloudBlockBlob) blobItem)
                    .collect(Collectors.toList());
        for (final CloudBlockBlob blob : blobs) {
                blob.delete();
        }
    }

    public Stream<ListBlobItem> toStream(Iterable<ListBlobItem> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}