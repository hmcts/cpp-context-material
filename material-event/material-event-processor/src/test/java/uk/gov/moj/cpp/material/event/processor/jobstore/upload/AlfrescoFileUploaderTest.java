package uk.gov.moj.cpp.material.event.processor.jobstore.upload;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.file.api.sender.FileData;
import uk.gov.justice.services.file.api.sender.FileSender;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.moj.cpp.material.event.processor.azure.service.StorageCloudClientService;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.util.AlfrescoFileNameGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.json.JsonObject;

import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlobInputStream;
import org.junit.jupiter.api.BeforeEach;
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
    private FileReference fileReference;
    @Mock
    private FileData fileData;
    @Mock
    private InputStream contentStream;
    @Mock
    private StorageCloudClientService storageCloudClientService;

    @Mock
    private BlobInputStream blobInputStream;

    @Mock
    private BlobProperties blobProperties;

    private UUID materialId;
    private UUID fileServiceId;
    private UUID alfrescoFileId;

    private final boolean unbundledDocument = true;

    @BeforeEach
    public void setup() {
        materialId = randomUUID();
        fileServiceId = randomUUID();
        alfrescoFileId = randomUUID();
    }

    @Test
    public void shouldUploadAFileFromTheFileStoreToAlfresco() throws Exception {
        final String fileName = "file name";
        final String uniqueAlfrescoFileName = "file name.pdf";
        final String mediaType = "application/pdf";
        final JsonObject fileReferenceMetadata = createObjectBuilder()
                .add("fileName", fileName)
                .add("mediaType", mediaType)
                .build();

        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId,
                fileServiceId,
                unbundledDocument,
                uploadedEventMetadata, "");

        when(fileReference.getMetadata()).thenReturn(fileReferenceMetadata);
        when(fileReference.getContentStream()).thenReturn(contentStream);
        when(fileSender.send(uniqueAlfrescoFileName, contentStream)).thenReturn(fileData);
        when(fileData.fileId()).thenReturn(alfrescoFileId.toString());
        when(alfrescoFileService.generateAlfrescoCompliantFileName(fileName, materialId, mediaType)).thenReturn(uniqueAlfrescoFileName);

        final SuccessfulMaterialUploadJobData successfulMaterialUploadJobData = alfrescoFileUploader.uploadFileToAlfresco(
                fileReference,
                uploadMaterialToAlfrescoJobData);

        assertThat(successfulMaterialUploadJobData.getMaterialId(), is(materialId));
        assertThat(successfulMaterialUploadJobData.getFileName(), is(fileName));
        assertThat(successfulMaterialUploadJobData.getMediaType(), is(mediaType));
        assertThat(successfulMaterialUploadJobData.getMaterialId(), is(materialId));
        assertThat(successfulMaterialUploadJobData.getFileServiceId(), is(fileServiceId));
        assertThat(successfulMaterialUploadJobData.isUnbundledDocument(), is(unbundledDocument));
        assertThat(successfulMaterialUploadJobData.getFileUploadedEventMetadata(), is(uploadedEventMetadata));
        assertThat(successfulMaterialUploadJobData.getAlfrescoFileId(), is(alfrescoFileId));

        verify(fileReference).close();
    }

    @Test
    public void shouldUploadAFileFromTheFileStoreToAlfrescoAfterCorrectingFileName() throws Exception {
        final String mediaType = "application/pdf";
        final String fileName = "RING doorbell footage";
        final String uniqueAlfrescoFileName = "RING doorbell footage.pdf";

        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId,
                fileServiceId,
                unbundledDocument,
                uploadedEventMetadata, "");

        when(fileReference.getContentStream()).thenReturn(contentStream);
        when(fileSender.send(anyString(), eq(contentStream))).thenReturn(fileData);
        when(fileData.fileId()).thenReturn(alfrescoFileId.toString());
        when(alfrescoFileService.generateAlfrescoCompliantFileName(fileName, materialId, mediaType)).thenReturn(uniqueAlfrescoFileName);

        final JsonObject fileReferenceMetadata = createObjectBuilder()
                .add("fileName", fileName)
                .add("mediaType", mediaType)
                .build();
        when(fileReference.getMetadata()).thenReturn(fileReferenceMetadata);

        final SuccessfulMaterialUploadJobData successfulMaterialUploadJobData = alfrescoFileUploader.uploadFileToAlfresco(
                fileReference,
                uploadMaterialToAlfrescoJobData);

        assertThat(successfulMaterialUploadJobData.getMaterialId(), is(materialId));
        assertThat(successfulMaterialUploadJobData.getFileName(), is(fileName));
        assertThat(successfulMaterialUploadJobData.getMediaType(), is(mediaType));
        assertThat(successfulMaterialUploadJobData.getMaterialId(), is(materialId));
        assertThat(successfulMaterialUploadJobData.getFileServiceId(), is(fileServiceId));
        assertThat(successfulMaterialUploadJobData.isUnbundledDocument(), is(unbundledDocument));
        assertThat(successfulMaterialUploadJobData.getFileUploadedEventMetadata(), is(uploadedEventMetadata));
        assertThat(successfulMaterialUploadJobData.getAlfrescoFileId(), is(alfrescoFileId));
    }

    @Test
    public void shouldLogAWaringIfTheCloseOfFileReferenceFails() throws Exception {
        final UUID fileServiceId = fromString("66ea82b7-8797-4d9c-ae25-b1c04ea70184");
        final String fileName = "file name";
        final String uniqueAlfrescoFileName = "file name.pdf";
        final String mediaType = "application/pdf";
        final JsonObject fileReferenceMetadata = createObjectBuilder()
                .add("fileName", fileName)
                .add("mediaType", mediaType)
                .build();
        final IOException ioException = new IOException();

        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId,
                fileServiceId,
                unbundledDocument,
                uploadedEventMetadata, "");

        when(fileReference.getMetadata()).thenReturn(fileReferenceMetadata);
        when(fileReference.getContentStream()).thenReturn(contentStream);
        when(fileSender.send(uniqueAlfrescoFileName, contentStream)).thenReturn(fileData);
        when(fileData.fileId()).thenReturn(alfrescoFileId.toString());
        when(alfrescoFileService.generateAlfrescoCompliantFileName(fileName, materialId, mediaType)).thenReturn(uniqueAlfrescoFileName);

        doThrow(ioException).when(fileReference).close();

        final SuccessfulMaterialUploadJobData successfulMaterialUploadJobData = alfrescoFileUploader.uploadFileToAlfresco(
                fileReference,
                uploadMaterialToAlfrescoJobData);

        assertThat(successfulMaterialUploadJobData.getMaterialId(), is(materialId));
        assertThat(successfulMaterialUploadJobData.getFileName(), is(fileName));
        assertThat(successfulMaterialUploadJobData.getMediaType(), is(mediaType));
        assertThat(successfulMaterialUploadJobData.getMaterialId(), is(materialId));
        assertThat(successfulMaterialUploadJobData.getFileServiceId(), is(fileServiceId));
        assertThat(successfulMaterialUploadJobData.isUnbundledDocument(), is(unbundledDocument));
        assertThat(successfulMaterialUploadJobData.getFileUploadedEventMetadata(), is(uploadedEventMetadata));
        assertThat(successfulMaterialUploadJobData.getAlfrescoFileId(), is(alfrescoFileId));

        verify(logger).warn("Failed to close InputStream to the file store for file with id '66ea82b7-8797-4d9c-ae25-b1c04ea70184'", ioException);
    }

    @Test
    void shouldUploadFileFromAzureToAlfresco() {
        String blobName = "28DI3233185/test.pdf";
        when(storageCloudClientService.downloadBlobContents(blobName)).thenReturn(blobInputStream);
        when(blobInputStream.getProperties()).thenReturn(blobProperties);
        UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(materialId, fileServiceId,
                false, uploadedEventMetadata, blobName);
        when(alfrescoFileService.generateAlfrescoCompliantFileName("test.pdf", materialId, "application/pdf")).thenReturn("test.pdf");
        when(fileSender.send("test.pdf", blobInputStream)).thenReturn(fileData);
        when(fileData.fileId()).thenReturn(alfrescoFileId.toString());

        final SuccessfulMaterialUploadJobData result = alfrescoFileUploader.uploadFileFromAzureToAlfresco(uploadMaterialToAlfrescoJobData);
        assertThat(result.getMediaType(), is("application/pdf"));
    }

    @Test
    void shouldUploadFileFromAzureToAlfrescoAndUpdateSuccessfulMaterialUploadJobData() {
        String fileName = "WitnessStatementDocument_2.pdf";
        String cloudLocation = "XHIBIT/2025-09-09/DL848492465/d2a45f11-6c7e-459d-ad05-3fc449305523/" + fileName;
        when(storageCloudClientService.downloadBlobContents(cloudLocation)).thenReturn(blobInputStream);
        when(blobInputStream.getProperties()).thenReturn(blobProperties);
        UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId,
                fileServiceId,
                false,
                uploadedEventMetadata,
                cloudLocation);
        when(alfrescoFileService.generateAlfrescoCompliantFileName(fileName, materialId, "application/pdf"))
                .thenReturn(fileName);
        when(fileSender.send(fileName, blobInputStream)).thenReturn(fileData);
        when(fileData.fileId()).thenReturn(alfrescoFileId.toString());

        final SuccessfulMaterialUploadJobData result = alfrescoFileUploader.uploadFileFromAzureToAlfresco(uploadMaterialToAlfrescoJobData);
        assertThat(result.getMaterialId(), is(materialId));
        assertThat(result.getFileUploadedEventMetadata(), is(uploadedEventMetadata));
        assertFalse(result.isUnbundledDocument());
        assertThat(result.getAlfrescoFileId(), is(alfrescoFileId));
        assertThat(result.getFileName(), is(fileName));
        assertThat(result.getFileCloudLocation(), is(cloudLocation));
    }
}