package uk.gov.moj.cpp.material.event.processor.azure.service;

import static java.util.Objects.isNull;

import uk.gov.moj.cpp.material.event.processor.azure.service.exception.CloudException;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.specialized.BlobInputStream;

/**
 * FR-8 material stream-through: reads a document directly from a producer's own container,
 * addressed by the absolute blob URI carried on the {@code fileUri} upload-file branch -
 * authenticated via managed identity (container-scoped read grant), never a connection string
 * or shared account key. Unlike {@link StorageCloudClientService} (pinned to the fixed
 * {@code dlrmcontainer} via connection-string auth), this client addresses whichever
 * account/container the caller's URI points at.
 */
    public class AzureBlobUriClientService {

    private TokenCredential tokenCredential;

    private TokenCredential getTokenCredential() {
        if (isNull(tokenCredential)) {
            tokenCredential = new DefaultAzureCredentialBuilder().build();
        }
        return tokenCredential;
    }

    /**
     * Downloads the content of a blob addressed by its absolute URI.
     *
     * @param fileUri - absolute blob URI, e.g. https://<account>.blob.core.windows.net/<container>/<path>
     * @return an open stream over the blob's contents
     */
    public BlobInputStream downloadBlobContents(final String fileUri) {
        try {
            final TokenCredential credential = getTokenCredential();
            final BlobClient blobClient = new BlobClientBuilder()
                    .endpoint(fileUri)
                    .credential(credential)
                    .buildClient();
            return blobClient.openInputStream();
        } catch (final Exception e) {
            throw new CloudException("Error while reading blob at fileUri: " + fileUri, e);
        }
    }
}
