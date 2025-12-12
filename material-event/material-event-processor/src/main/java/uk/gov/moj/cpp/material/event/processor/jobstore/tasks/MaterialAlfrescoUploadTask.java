package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static java.lang.String.format;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.INPROGRESS;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.SUCCESSFUL_MATERIAL_UPLOAD_COMMAND_TASK;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.UPLOAD_FILE_TO_ALFRESCO_TASK;

import java.util.List;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.moj.cpp.jobstore.api.annotation.Task;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.service.FileUploadRetryConfiguration;
import uk.gov.moj.cpp.material.event.processor.jobstore.upload.AlfrescoFileUploader;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

@Task(UPLOAD_FILE_TO_ALFRESCO_TASK)
public class MaterialAlfrescoUploadTask implements ExecutableTask {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private FileService fileService;

    @Inject
    private AlfrescoFileUploader alfrescoFileUploader;

    @Inject
    private HardFailureTaskFactory hardFailureTaskFactory;


    @Inject
    private UtcClock clock;

    @SuppressWarnings({"squid:S1312"})
    @Inject
    private Logger logger;

    @Inject
    FileUploadRetryConfiguration fileUploadRetryConfiguration;

    @Override
    public Optional<List<Long>> getRetryDurationsInSecs() {
        return Optional.of(fileUploadRetryConfiguration.getAlfrescoFileUploadTaskRetryDurationsSeconds());
    }

    @Override
    @SuppressWarnings("squid:S2221")
    public ExecutionInfo execute(final ExecutionInfo executionInfo) {
        final UploadMaterialToAlfrescoJobData jobData = jsonObjectConverter.convert(
                executionInfo.getJobData(),
                UploadMaterialToAlfrescoJobData.class);

        final UUID fileServiceId = jobData.getFileServiceId();

        if (fileServiceId != null) {
            return handleFileServiceUpload(fileServiceId, jobData);
        } else {
            return handleAzureUpload(jobData);
        }
    }

    private ExecutionInfo handleFileServiceUpload(UUID fileServiceId, UploadMaterialToAlfrescoJobData jobData) {
        try {
            return uploadFileServiceFileToAlfresco(fileServiceId, jobData);
        } catch (final FileServiceException e) {
            String errorMessage = String.format("Failed to retrieve file from file service with id '%s'. %s: %s",
                    fileServiceId, e.getClass().getSimpleName(), e.getMessage());
            return createRetryTask(jobData, errorMessage);
        } catch (final Exception e) {
            String errorMessage = String.format("Unexpected error occurred when attempting to upload file to alfresco for fileId: '%s'. %s:%s", fileServiceId, e.getClass().getSimpleName(), e.getMessage());
            return createRetryTask(jobData, errorMessage);
        }
    }

    private ExecutionInfo handleAzureUpload(UploadMaterialToAlfrescoJobData jobData) {
        try {
            return uploadAzureFileToAlfresco(jobData);
        } catch (final Exception e) {
            String errorMessage = String.format("Unexpected error occurred when attempting to upload file to Alfresco for cloudLocation: '%s'. %s: %s",
                    jobData.getCloudLocation(), e.getClass().getSimpleName(), e.getMessage());
            return createRetryTask(jobData, errorMessage);
        }
    }

    private ExecutionInfo createRetryTask(UploadMaterialToAlfrescoJobData jobData, String errorMessage) {
        return hardFailureTaskFactory.createRetryWithHardFailureTaskOnExhaust(jobData, errorMessage);
    }


    private ExecutionInfo uploadAzureFileToAlfresco(UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData) throws FileServiceException {

        final SuccessfulMaterialUploadJobData successfulMaterialUploadJobData = alfrescoFileUploader.uploadFileFromAzureToAlfresco(
                uploadMaterialToAlfrescoJobData);
        return executionInfo()
                .withExecutionStatus(INPROGRESS)
                .withNextTask(SUCCESSFUL_MATERIAL_UPLOAD_COMMAND_TASK)
                .withNextTaskStartTime(clock.now())
                .withJobData(objectToJsonObjectConverter.convert(successfulMaterialUploadJobData))
                .build();

    }

    private ExecutionInfo uploadFileServiceFileToAlfresco(UUID fileServiceId, UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData) throws FileServiceException {
        final Optional<FileReference> fileReferenceOptional = fileService.retrieve(fileServiceId);

        if (fileReferenceOptional.isPresent()) {
            try (final FileReference fileReference = fileReferenceOptional.get()) {
                final SuccessfulMaterialUploadJobData successfulMaterialUploadJobData = alfrescoFileUploader.uploadFileToAlfresco(
                        fileReference,
                        uploadMaterialToAlfrescoJobData);

                return executionInfo()
                        .withExecutionStatus(INPROGRESS)
                        .withNextTask(SUCCESSFUL_MATERIAL_UPLOAD_COMMAND_TASK)
                        .withNextTaskStartTime(clock.now())
                        .withJobData(objectToJsonObjectConverter.convert(successfulMaterialUploadJobData))
                        .build();
            }
        } else {
            final String errorMessage = format("Failed to upload file to alfresco. No file found in file service with id '%s'", fileServiceId);
            logger.error(errorMessage);
            return hardFailureTaskFactory.createHardFailureTask(
                    uploadMaterialToAlfrescoJobData,
                    errorMessage
            );
        }
    }
}