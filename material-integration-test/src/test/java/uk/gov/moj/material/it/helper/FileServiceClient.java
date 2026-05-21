package uk.gov.moj.material.it.helper;

import static java.util.UUID.randomUUID;

import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import java.util.Map;
import java.util.UUID;

public class FileServiceClient {

    private static final String AZURITE_CONNECTION_STRING =
            "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;" +
            "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" + // gitleaks:allow
            "BlobEndpoint=http://localhost:10000/devstoreaccount1;";

    private static final String CONTAINER_NAME = "material-files";

    public static UUID create(final String fileName, final String mimeType, final byte[] content) {
        final UUID fileId = randomUUID();
        final String blobName = "internal/" + fileId;

        final BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .httpClient(new JdkHttpClientBuilder().build())
                .connectionString(AZURITE_CONNECTION_STRING)
                .buildClient();

        final BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
        containerClient.createIfNotExists();

        final BlobHttpHeaders blobHttpHeaders = new BlobHttpHeaders().setContentType(mimeType);
        final Map<String, String> metadata = Map.of("filename", fileName.strip(), "media_type", mimeType);

        containerClient.getBlobClient(blobName)
                .uploadWithResponse(
                        new BlobParallelUploadOptions(BinaryData.fromBytes(content))
                                .setHeaders(blobHttpHeaders)
                                .setMetadata(metadata),
                        null,
                        null
                );

        return fileId;
    }
}
