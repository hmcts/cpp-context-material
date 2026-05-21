package uk.gov.moj.cpp.material.command.handler.azure.service;

import static com.azure.core.util.Context.NONE;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.material.command.handler.ZipMaterial;
import uk.gov.moj.cpp.material.command.handler.azure.service.exception.AzureBlobClientException;
import uk.gov.moj.cpp.material.filestore.azure.ArchiveAzureBlobConfiguration;
import uk.gov.moj.cpp.material.filestore.azure.StorageFileRetriever;
import uk.gov.moj.cpp.material.filestore.azure.StoragePath;
import uk.gov.moj.cpp.material.filestore.azure.StoredFile;
import uk.gov.moj.cpp.material.query.service.AlfrescoReadService;
import uk.gov.moj.cpp.material.query.view.MaterialView;
import uk.gov.moj.cpp.platform.data.utils.service.ZipCreator;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class AzureArchiveBlobClientServiceTest {

    @Mock
    private Logger logger;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private ArchiveAzureBlobConfiguration archiveAzureBlobConfiguration;

    @Mock
    private ZipCreator zipCreator;

    @Mock
    private AlfrescoReadService alfrescoReadService;

    @Mock
    private StorageFileRetriever storageFileRetriever;

    @Mock
    private UtcClock clock;

    @Mock
    private BlobClient blobClient;

    @InjectMocks
    private AzureArchiveBlobClientService azureArchiveBlobClientService;

    @Test
    void shouldUploadFileToAzureBlobStorage() {
        final String destinationFileName = randomUUID() + ".zip";

        when(blobContainerClient.getBlobClient(destinationFileName)).thenReturn(blobClient);
        when(archiveAzureBlobConfiguration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));
        when(clock.now()).thenReturn(new UtcClock().now());

        azureArchiveBlobClientService.upload(new ByteArrayInputStream(new byte[0]), destinationFileName, AzureArchiveBlobClientService.CONTENT_TYPE_ZIP);

        final ArgumentCaptor<BlobParallelUploadOptions> uploadCaptor = ArgumentCaptor.forClass(BlobParallelUploadOptions.class);
        verify(blobClient).uploadWithResponse(uploadCaptor.capture(), eq(Duration.ofSeconds(300)), eq(NONE));
        assertThat(uploadCaptor.getValue().getHeaders().getContentType(), containsString(AzureArchiveBlobClientService.CONTENT_TYPE_ZIP));
    }

    @Test
    void shouldThrowAzureBlobClientExceptionWhenUploadFails() {
        final String destinationFileName = randomUUID() + ".zip";

        when(blobContainerClient.getBlobClient(destinationFileName)).thenReturn(blobClient);
        when(archiveAzureBlobConfiguration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));
        when(clock.now()).thenReturn(new UtcClock().now());
        when(blobClient.uploadWithResponse(any(BlobParallelUploadOptions.class), eq(Duration.ofSeconds(300)), eq(NONE))).thenThrow(new RuntimeException("upload failed"));

        assertThrows(AzureBlobClientException.class,
                () -> azureArchiveBlobClientService.upload(new ByteArrayInputStream(new byte[0]), destinationFileName, AzureArchiveBlobClientService.CONTENT_TYPE_ZIP));
    }

    @Test
    void shouldCreateZipAndUploadWhenFileIdsHaveStoredFiles() throws Exception {
        final UUID fileId = randomUUID();
        final UUID caseId = randomUUID();
        final StoredFile storedFile = new StoredFile(new ByteArrayInputStream(new byte[0]), Map.of("filename", "document.pdf", "media_type", "application/pdf"));
        final ZipMaterial zipMaterial = new ZipMaterial(caseId, "TEST-CASE-001", List.of(fileId), List.of());
        final ZonedDateTime now = new UtcClock().now();

        when(storageFileRetriever.retrieve(StoragePath.internal(), fileId)).thenReturn(of(storedFile));
        when(clock.now()).thenReturn(now);
        when(blobContainerClient.getBlobClient(any(String.class))).thenReturn(blobClient);
        when(archiveAzureBlobConfiguration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));

        azureArchiveBlobClientService.createAndUploadZip(zipMaterial);

        final ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(blobContainerClient).getBlobClient(blobNameCaptor.capture());
        assertThat(blobNameCaptor.getValue(), containsString("CaseArchive-TEST-CASE-001-"));

        final ArgumentCaptor<File> zipFileCaptor = ArgumentCaptor.forClass(File.class);
        verify(zipCreator).zipFiles(any(), zipFileCaptor.capture());
        assertThat(zipFileCaptor.getValue().getName(), containsString("CaseArchive-TEST-CASE-001-"));
    }

    @Test
    void shouldCreateZipAndUploadWhenFileIdsHaveNoStoredFiles() throws Exception {
        final UUID fileId = randomUUID();
        final UUID caseId = randomUUID();
        final ZipMaterial zipMaterial = new ZipMaterial(caseId, "TEST-CASE-002", List.of(fileId), List.of());
        final ZonedDateTime now = new UtcClock().now();

        when(storageFileRetriever.retrieve(StoragePath.internal(), fileId)).thenReturn(empty());
        when(clock.now()).thenReturn(now);
        when(blobContainerClient.getBlobClient(any(String.class))).thenReturn(blobClient);
        when(archiveAzureBlobConfiguration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));

        azureArchiveBlobClientService.createAndUploadZip(zipMaterial);

        verify(zipCreator).zipFiles(any(), any(File.class));
    }

    @Test
    void shouldCreateZipWithMaterialViews() throws Exception {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final ZipMaterial zipMaterial = new ZipMaterial(caseId, "TEST-CASE-003", List.of(), List.of(materialId));
        final ZonedDateTime now = new UtcClock().now();
        final MaterialView materialView = mock(MaterialView.class);

        when(alfrescoReadService.getDataById(materialId)).thenReturn(of(materialView));
        when(materialView.getFileName()).thenReturn("evidence.pdf");
        when(materialView.getDocumentInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(clock.now()).thenReturn(now);
        when(blobContainerClient.getBlobClient(any(String.class))).thenReturn(blobClient);
        when(archiveAzureBlobConfiguration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));

        azureArchiveBlobClientService.createAndUploadZip(zipMaterial);

        verify(zipCreator).zipFiles(any(), any(File.class));
    }
}
