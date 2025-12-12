package uk.gov.moj.cpp.material.azure.storage;

import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;

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

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobListingDetails;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

public class CleanLargeFileStore {

    private final CloudBlobContainer cloudBlobContainer;

    private static final Integer PURGE_TIME_MINUTES = Integer.parseInt(ofNullable(getenv("material.largefileExpiryInMinutes"))
            .orElse("30"));

    public CleanLargeFileStore(final CloudBlobContainer azureBlobClientService) {
        this.cloudBlobContainer = azureBlobClientService;
    }

    public void clean(final ExecutionContext context) {
        try {
            final EnumSet<BlobListingDetails> enumBlobListingDetails = EnumSet.of(BlobListingDetails.METADATA);

            final List<ListBlobItem> listBlobItems = toStream(cloudBlobContainer.listBlobs(null, true, enumBlobListingDetails, null, null))
                    .collect(Collectors.toList());

            final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            final Instant result = now.minusMinutes(PURGE_TIME_MINUTES).atZone(ZoneOffset.UTC).toInstant();
            final Date timeOut = Date.from(result);
            context.getLogger().info(() ->format( "timeOutMinutes = '%s'", PURGE_TIME_MINUTES));
            final Set<CloudBlobDirectory> deletionDirectories = new HashSet<>();
            listBlobItems.stream().filter(CloudBlob.class::isInstance).forEach(blobItem -> {
                final BlobProperties properties = ((CloudBlob) blobItem).getProperties();
                if (isOlderThanDeletionThreshold(((CloudBlob) blobItem).getName(), properties.getLastModified(), timeOut, context)) {
                    try {
                        deletionDirectories.add(blobItem.getParent());
                    } catch (URISyntaxException ex) {
                        throw new AzureBlobClientException("Connection URI parse error", ex);
                    } catch (StorageException ex) {
                        throw new AzureBlobClientException(format(
                                "Error returned from azure service. Http code: %d and error code: %s", ex.getHttpStatusCode(), ex.getErrorCode()), ex);
                    }
                }
            });

            context.getLogger().info(() -> format("Deleting %s directories in container", deletionDirectories.size()));
            delete(deletionDirectories);
        } catch (URISyntaxException ex) {
            throw new AzureBlobClientException("Connection URI parse error", ex);
        } catch (StorageException ex) {
            throw new AzureBlobClientException(format("Error returned from azure service. Http code: %d and error code: %s", ex.getHttpStatusCode(), ex.getErrorCode()), ex);
        }
    }

    public boolean isOlderThanDeletionThreshold( final String fileName,final Date lastModified, final Date timeOut, final  ExecutionContext executionContext) {
        try {
            if (lastModified.before(timeOut)) {
                executionContext.getLogger().info(() -> format("%s is IN deletion threshold, %s = ", fileName, lastModified));
                return true;
            } else {
                executionContext.getLogger().info(() -> format("%s is NOT in deletion threshold, lastModified = %s", fileName, lastModified));
            }
        } catch (DateTimeException e) {
            executionContext.getLogger().severe("DateTimeException" + e);
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
                    .filter(CloudBlockBlob.class::isInstance)
                    .map(CloudBlockBlob.class::cast)
                    .collect(Collectors.toList());
        for (final CloudBlockBlob blob : blobs) {
                blob.delete();
        }
    }

    public Stream<ListBlobItem> toStream(Iterable<ListBlobItem> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
