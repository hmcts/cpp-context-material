package uk.gov.moj.cpp.material.event.processor.jobstore.upload;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData.SuccessfulMaterialUploadJobDataBuilder.successfulMaterialUploadJobData;

import uk.gov.justice.services.file.api.sender.FileData;
import uk.gov.justice.services.file.api.sender.FileSender;
import uk.gov.moj.cpp.material.event.processor.azure.service.StorageCloudClientService;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData.SuccessfulMaterialUploadJobDataBuilder;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.util.AlfrescoFileNameGenerator;
import uk.gov.moj.cpp.material.event.processor.jobstore.util.FileExtensionResolver;
import uk.gov.moj.cpp.material.filestore.azure.StoredFile;

import java.io.IOException;
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
            final StoredFile storedFile,
            final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData) {

        final UUID fileServiceId = uploadMaterialToAlfrescoJobData.getFileServiceId();

        final SuccessfulMaterialUploadJobDataBuilder successfulMaterialUploadJobDataBuilder = successfulMaterialUploadJobData()
                .withFileServiceId(fileServiceId)
                .withMaterialId(uploadMaterialToAlfrescoJobData.getMaterialId())
                .withUnbundledDocument(uploadMaterialToAlfrescoJobData.isUnbundledDocument())
                .withFileUploadedEventMetadata(uploadMaterialToAlfrescoJobData.getFileUploadedEventMetadata());

        try {
            final String originalFileName = storedFile.getMetadata().get("filename");
            final String mediaType = storedFile.getMetadata().getOrDefault("media_type", "application/octet-stream");
            final String uniqueAlfrescoFileName = alfrescoFileNameGenerator.generateAlfrescoCompliantFileName(
                    originalFileName, uploadMaterialToAlfrescoJobData.getMaterialId(), mediaType);

            final FileData fileData = fileSender.send(uniqueAlfrescoFileName, storedFile.getInputStream());
            final UUID alfrescoFileId = fromString(fileData.fileId());

            successfulMaterialUploadJobDataBuilder
                    .withAlfrescoFileId(alfrescoFileId)
                    .withFileName(originalFileName)
                    .withMediaType(mediaType);

        } finally {
            try {
                storedFile.close();
            } catch (final IOException e) {
                logger.warn(format("Failed to close InputStream to the file store for file with id '%s'", fileServiceId), e);
            }
        }

        return successfulMaterialUploadJobDataBuilder.build();
    }

    public SuccessfulMaterialUploadJobData uploadFileFromAzureToAlfresco(final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData) {
        final String cloudLocation = uploadMaterialToAlfrescoJobData.getCloudLocation();
        final StoredFile storedFile = storageCloudClientService.downloadBlobContents(cloudLocation);
        try {
            final String originalFileName = Objects.requireNonNullElse(storedFile.getMetadata().get("name"), cloudLocation.substring(cloudLocation.lastIndexOf("/") + 1));
            final String fileExtension = cloudLocation.substring(cloudLocation.lastIndexOf('.') + 1);
            final String mediaType = FileExtensionResolver.getMimeType(fileExtension);
            final String uniqueAlfrescoFileName = alfrescoFileNameGenerator.generateAlfrescoCompliantFileName(
                    originalFileName, uploadMaterialToAlfrescoJobData.getMaterialId(), mediaType);
            final FileData fileData = fileSender.send(uniqueAlfrescoFileName, storedFile.getInputStream());
            final UUID alfrescoFileId = fromString(fileData.fileId());

            return successfulMaterialUploadJobData()
                    .withMaterialId(uploadMaterialToAlfrescoJobData.getMaterialId())
                    .withUnbundledDocument(uploadMaterialToAlfrescoJobData.isUnbundledDocument())
                    .withFileUploadedEventMetadata(uploadMaterialToAlfrescoJobData.getFileUploadedEventMetadata())
                    .withAlfrescoFileId(alfrescoFileId)
                    .withFileName(originalFileName)
                    .withMediaType(mediaType)
                    .withFileCloudLocation(cloudLocation)
                    .build();
        } finally {
            try {
                storedFile.close();
            } catch (final IOException ignored) {
                logger.warn("Failed to close StoredFile for cloud location '{}'", cloudLocation);
            }
        }
    }
}
