package uk.gov.moj.cpp.material.event.processor.jobstore.upload;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData.SuccessfulMaterialUploadJobDataBuilder.successfulMaterialUploadJobData;

import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlobInputStream;
import uk.gov.justice.services.file.api.sender.FileData;
import uk.gov.justice.services.file.api.sender.FileSender;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.moj.cpp.material.event.processor.azure.service.StorageCloudClientService;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData.SuccessfulMaterialUploadJobDataBuilder;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.util.AlfrescoFileNameGenerator;
import uk.gov.moj.cpp.material.event.processor.jobstore.util.FileExtensionResolver;

import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

public class AlfrescoFileUploader {

    public static final String ALFRESCO_FILE_NAME_CHECKER_REGEX = "(.*[\\\"\\*\\\\\\>\\<\\?\\/\\:\\|]+.*)|(.*[\\.]?.*[\\.]+$)|(.*[ ]+$)";
    @Inject
    private FileSender fileSender;

    @Inject
    private AlfrescoFileNameGenerator alfrescoFileNameGenerator;

    @Inject
    private StorageCloudClientService storageCloudClientService;

    @SuppressWarnings({"squid:S1312"})
    @Inject
    private Logger logger;

    @SuppressWarnings("squid:S2221")
    public SuccessfulMaterialUploadJobData uploadFileToAlfresco(
            final FileReference fileReference,
            final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData) {

            final UUID fileServiceId = uploadMaterialToAlfrescoJobData.getFileServiceId();

            final SuccessfulMaterialUploadJobDataBuilder successfulMaterialUploadJobData = successfulMaterialUploadJobData()
                    .withFileServiceId(fileServiceId)
                    .withMaterialId(uploadMaterialToAlfrescoJobData.getMaterialId())
                    .withUnbundledDocument(uploadMaterialToAlfrescoJobData.isUnbundledDocument())
                    .withFileUploadedEventMetadata(uploadMaterialToAlfrescoJobData.getFileUploadedEventMetadata());

                try {
                    final JsonObject fileReferenceMetadata = fileReference.getMetadata();
                    final String originalFileName = fileReferenceMetadata.getString("fileName");
                    final String mediaType = fileReferenceMetadata.getString("mediaType");
                    final String uniqueAlfrescoFileName = alfrescoFileNameGenerator.generateAlfrescoCompliantFileName(originalFileName, uploadMaterialToAlfrescoJobData.getMaterialId(), mediaType);

                    final FileData fileData = fileSender.send(uniqueAlfrescoFileName, fileReference.getContentStream());

                    final UUID alfrescoFileId = fromString(fileData.fileId());

                    successfulMaterialUploadJobData
                            .withAlfrescoFileId(alfrescoFileId)
                            .withFileName(originalFileName)
                            .withMediaType(mediaType);

                } finally {
                    try {
                        fileReference.close();
                    } catch (final Exception e) {
                        logger.warn(format("Failed to close InputStream to the file store for file with id '%s'", fileServiceId), e);
                    }
                }

                return successfulMaterialUploadJobData.build();
    }

    public SuccessfulMaterialUploadJobData uploadFileFromAzureToAlfresco(final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData) {
        final String cloudLocation = uploadMaterialToAlfrescoJobData.getCloudLocation();
        final BlobInputStream blobInputStream = storageCloudClientService.downloadBlobContents(cloudLocation);
        final BlobProperties blobProperties = blobInputStream.getProperties();
        final String originalFileName = Objects.requireNonNullElse(blobProperties.getMetadata().get("name"),cloudLocation.substring(cloudLocation.lastIndexOf("/")+1));
        String fileExtension = cloudLocation.substring(cloudLocation.lastIndexOf('.') + 1);
        final String mediaType = FileExtensionResolver.getMimeType(fileExtension);
        final String uniqueAlfrescoFileName = alfrescoFileNameGenerator.generateAlfrescoCompliantFileName(originalFileName, uploadMaterialToAlfrescoJobData.getMaterialId(), mediaType);
        final FileData fileData = fileSender.send(uniqueAlfrescoFileName, blobInputStream);
        final UUID alfrescoFileId = fromString(fileData.fileId());

        final SuccessfulMaterialUploadJobDataBuilder successfulMaterialUploadJobData = successfulMaterialUploadJobData()
                .withMaterialId(uploadMaterialToAlfrescoJobData.getMaterialId())
                .withUnbundledDocument(uploadMaterialToAlfrescoJobData.isUnbundledDocument())
                .withFileUploadedEventMetadata(uploadMaterialToAlfrescoJobData.getFileUploadedEventMetadata())
                .withAlfrescoFileId(alfrescoFileId)
                .withFileName(originalFileName)
                .withMediaType(mediaType)
                .withFileCloudLocation(cloudLocation);

        return successfulMaterialUploadJobData.build();
    }

}
