package uk.gov.moj.cpp.material.event.processor.jobstore.upload;

import static java.util.Map.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.file.api.sender.FileData;
import uk.gov.justice.services.file.api.sender.FileSender;
import uk.gov.moj.cpp.material.event.processor.azure.service.StorageCloudClientService;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.util.AlfrescoFileNameGenerator;
import uk.gov.moj.cpp.material.filestore.azure.StoredFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class AlfrescoFileUploaderTest {

    @Mock
    private FileSender fileSender;

    @Mock
    private Logger logger;

    @Mock
    private AlfrescoFileNameGenerator alfrescoFileService;

    @InjectMocks
    private AlfrescoFileUploader alfrescoFileUploader;

    @Mock
    private JsonObject uploadedEventMetadata;

    @Mock
    private FileData fileData;

    @Mock
    private InputStream contentStream;

    @Mock
    private StorageCloudClientService storageCloudClientService;

    private final boolean unbundledDocument = true;

    @Test
    public void shouldUploadAFileFromTheFileStoreToAlfresco() throws IOException {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final UUID alfrescoFileId = randomUUID();
        final String fileName = "file name";
        final String uniqueAlfrescoFileName = "file name.pdf";
        final String mediaType = "application/pdf";

        final StoredFile storedFile = new StoredFile(contentStream, of("filename", fileName, "media_type", mediaType));
        final InputStream trackingStream = storedFile.getInputStream();
        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId, fileServiceId, unbundledDocument, uploadedEventMetadata, "");

        when(fileSender.send(uniqueAlfrescoFileName, trackingStream)).thenReturn(fileData);
        when(fileData.fileId()).thenReturn(alfrescoFileId.toString());
        when(alfrescoFileService.generateAlfrescoCompliantFileName(fileName, materialId, mediaType)).thenReturn(uniqueAlfrescoFileName);

        final SuccessfulMaterialUploadJobData result = alfrescoFileUploader.uploadFileToAlfresco(storedFile, uploadMaterialToAlfrescoJobData);

        assertThat(result.getMaterialId(), is(materialId));
        assertThat(result.getFileName(), is(fileName));
        assertThat(result.getMediaType(), is(mediaType));
        assertThat(result.getFileServiceId(), is(fileServiceId));
        assertThat(result.isUnbundledDocument(), is(unbundledDocument));
        assertThat(result.getFileUploadedEventMetadata(), is(uploadedEventMetadata));
        assertThat(result.getAlfrescoFileId(), is(alfrescoFileId));

        verify(contentStream).close();
    }

    @Test
    public void shouldUploadAFileFromTheFileStoreToAlfrescoAfterCorrectingFileName()  {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final UUID alfrescoFileId = randomUUID();
        final String mediaType = "application/pdf";
        final String fileName = "RING doorbell footage";
        final String uniqueAlfrescoFileName = "RING doorbell footage.pdf";

        final StoredFile storedFile = new StoredFile(contentStream, of("filename", fileName, "media_type", mediaType));
        final InputStream trackingStream = storedFile.getInputStream();
        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId, fileServiceId, unbundledDocument, uploadedEventMetadata, "");

        when(fileSender.send(uniqueAlfrescoFileName, trackingStream)).thenReturn(fileData);
        when(fileData.fileId()).thenReturn(alfrescoFileId.toString());
        when(alfrescoFileService.generateAlfrescoCompliantFileName(fileName, materialId, mediaType)).thenReturn(uniqueAlfrescoFileName);

        final SuccessfulMaterialUploadJobData result = alfrescoFileUploader.uploadFileToAlfresco(storedFile, uploadMaterialToAlfrescoJobData);

        assertThat(result.getMaterialId(), is(materialId));
        assertThat(result.getFileName(), is(fileName));
        assertThat(result.getMediaType(), is(mediaType));
        assertThat(result.getFileServiceId(), is(fileServiceId));
        assertThat(result.isUnbundledDocument(), is(unbundledDocument));
        assertThat(result.getFileUploadedEventMetadata(), is(uploadedEventMetadata));
        assertThat(result.getAlfrescoFileId(), is(alfrescoFileId));
    }

    @Test
    public void shouldLogAWarningIfTheCloseOfInputStreamFails() throws IOException {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = fromString("66ea82b7-8797-4d9c-ae25-b1c04ea70184");
        final UUID alfrescoFileId = randomUUID();
        final String fileName = "file name";
        final String uniqueAlfrescoFileName = "file name.pdf";
        final String mediaType = "application/pdf";
        final IOException ioException = new IOException();

        final StoredFile storedFile = new StoredFile(contentStream, of("filename", fileName, "media_type", mediaType));
        final InputStream trackingStream = storedFile.getInputStream();
        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId, fileServiceId, unbundledDocument, uploadedEventMetadata, "");

        when(fileSender.send(uniqueAlfrescoFileName, trackingStream)).thenReturn(fileData);
        when(fileData.fileId()).thenReturn(alfrescoFileId.toString());
        when(alfrescoFileService.generateAlfrescoCompliantFileName(fileName, materialId, mediaType)).thenReturn(uniqueAlfrescoFileName);
        doThrow(ioException).when(contentStream).close();

        final SuccessfulMaterialUploadJobData result = alfrescoFileUploader.uploadFileToAlfresco(storedFile, uploadMaterialToAlfrescoJobData);

        assertThat(result.getMaterialId(), is(materialId));
        assertThat(result.getFileName(), is(fileName));
        assertThat(result.getMediaType(), is(mediaType));
        assertThat(result.getFileServiceId(), is(fileServiceId));
        assertThat(result.isUnbundledDocument(), is(unbundledDocument));
        assertThat(result.getFileUploadedEventMetadata(), is(uploadedEventMetadata));
        assertThat(result.getAlfrescoFileId(), is(alfrescoFileId));

        verify(logger).warn("Failed to close InputStream to the file store for file with id '66ea82b7-8797-4d9c-ae25-b1c04ea70184'", ioException);
    }

    @Test
    void shouldUploadFileFromAzureToAlfresco() throws IOException {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final UUID alfrescoFileId = randomUUID();
        final String blobName = "container/test.pdf";

        final StoredFile storedFile = new StoredFile(contentStream, of());
        final InputStream trackingStream = storedFile.getInputStream();
        when(storageCloudClientService.downloadBlobContents(blobName)).thenReturn(storedFile);
        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId, fileServiceId, false, uploadedEventMetadata, blobName);
        when(alfrescoFileService.generateAlfrescoCompliantFileName("test.pdf", materialId, "application/pdf")).thenReturn("test.pdf");
        when(fileSender.send("test.pdf", trackingStream)).thenReturn(fileData);
        when(fileData.fileId()).thenReturn(alfrescoFileId.toString());

        final SuccessfulMaterialUploadJobData result = alfrescoFileUploader.uploadFileFromAzureToAlfresco(uploadMaterialToAlfrescoJobData);

        assertThat(result.getMediaType(), is("application/pdf"));
        verify(contentStream).close();
    }

    @Test
    void shouldUploadFileFromAzureToAlfrescoAndUpdateSuccessfulMaterialUploadJobData() throws IOException {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final UUID alfrescoFileId = randomUUID();
        final String fileName = "WitnessStatementDocument_2.pdf";
        final String cloudLocation = "CONTAINER/2025-09-09/FOLDER/d2a45f11-6c7e-459d-ad05-3fc449305523/" + fileName;

        final StoredFile storedFile = new StoredFile(contentStream, of());
        final InputStream trackingStream = storedFile.getInputStream();
        when(storageCloudClientService.downloadBlobContents(cloudLocation)).thenReturn(storedFile);
        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId, fileServiceId, false, uploadedEventMetadata, cloudLocation);
        when(alfrescoFileService.generateAlfrescoCompliantFileName(fileName, materialId, "application/pdf")).thenReturn(fileName);
        when(fileSender.send(fileName, trackingStream)).thenReturn(fileData);
        when(fileData.fileId()).thenReturn(alfrescoFileId.toString());

        final SuccessfulMaterialUploadJobData result = alfrescoFileUploader.uploadFileFromAzureToAlfresco(uploadMaterialToAlfrescoJobData);

        assertThat(result.getMaterialId(), is(materialId));
        assertThat(result.getFileUploadedEventMetadata(), is(uploadedEventMetadata));
        assertFalse(result.isUnbundledDocument());
        assertThat(result.getAlfrescoFileId(), is(alfrescoFileId));
        assertThat(result.getFileName(), is(fileName));
        assertThat(result.getFileCloudLocation(), is(cloudLocation));
        verify(contentStream).close();
    }

    @Test
    void shouldLogWarningWhenStoredFileCloseFailsOnAzureUpload() throws IOException {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final UUID alfrescoFileId = randomUUID();
        final String cloudLocation = "container/test.pdf";

        final StoredFile storedFile = new StoredFile(contentStream, of());
        final InputStream trackingStream = storedFile.getInputStream();
        when(storageCloudClientService.downloadBlobContents(cloudLocation)).thenReturn(storedFile);
        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId, fileServiceId, false, uploadedEventMetadata, cloudLocation);
        when(alfrescoFileService.generateAlfrescoCompliantFileName("test.pdf", materialId, "application/pdf")).thenReturn("test.pdf");
        when(fileSender.send("test.pdf", trackingStream)).thenReturn(fileData);
        when(fileData.fileId()).thenReturn(alfrescoFileId.toString());
        doThrow(new IOException()).when(contentStream).close();

        alfrescoFileUploader.uploadFileFromAzureToAlfresco(uploadMaterialToAlfrescoJobData);

        verify(logger).warn("Failed to close StoredFile for cloud location '{}'", cloudLocation);
    }
}
