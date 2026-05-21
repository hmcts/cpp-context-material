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
import uk.gov.moj.cpp.jobstore.api.annotation.Task;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.service.FileUploadRetryConfiguration;
import uk.gov.moj.cpp.material.event.processor.jobstore.upload.AlfrescoFileUploader;
import uk.gov.moj.cpp.material.filestore.azure.StorageFileRetriever;
import uk.gov.moj.cpp.material.filestore.azure.StoragePath;
import uk.gov.moj.cpp.material.filestore.azure.StoredFile;

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
    private StorageFileRetriever storageFileRetriever;

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
            return handleStoredFileUpload(fileServiceId, jobData);
        } else {
            return handleAzureUpload(jobData);
        }
    }

    private ExecutionInfo handleStoredFileUpload(final UUID fileServiceId, final UploadMaterialToAlfrescoJobData jobData) {
        try {
            return uploadStoredFileToAlfresco(fileServiceId, jobData);
        } catch (final Exception e) {
            final String errorMessage = format("Unexpected error occurred when attempting to upload file to alfresco for fileId: '%s'. %s:%s",
                    fileServiceId, e.getClass().getSimpleName(), e.getMessage());
            return createRetryTask(jobData, errorMessage);
        }
    }

    private ExecutionInfo handleAzureUpload(final UploadMaterialToAlfrescoJobData jobData) {
        try {
            return uploadAzureFileToAlfresco(jobData);
        } catch (final Exception e) {
            final String errorMessage = format("Unexpected error occurred when attempting to upload file to Alfresco for cloudLocation: '%s'. %s: %s",
                    jobData.getCloudLocation(), e.getClass().getSimpleName(), e.getMessage());
            return createRetryTask(jobData, errorMessage);
        }
    }

    private ExecutionInfo createRetryTask(final UploadMaterialToAlfrescoJobData jobData, final String errorMessage) {
        return hardFailureTaskFactory.createRetryWithHardFailureTaskOnExhaust(jobData, errorMessage);
    }

    private ExecutionInfo uploadAzureFileToAlfresco(final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData) {
        final SuccessfulMaterialUploadJobData successfulMaterialUploadJobData = alfrescoFileUploader.uploadFileFromAzureToAlfresco(
                uploadMaterialToAlfrescoJobData);
        return executionInfo()
                .withExecutionStatus(INPROGRESS)
                .withNextTask(SUCCESSFUL_MATERIAL_UPLOAD_COMMAND_TASK)
                .withNextTaskStartTime(clock.now())
                .withJobData(objectToJsonObjectConverter.convert(successfulMaterialUploadJobData))
                .build();
    }

    private ExecutionInfo uploadStoredFileToAlfresco(final UUID fileServiceId, final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData) {
        final Optional<StoredFile> storedFileOptional = storageFileRetriever.retrieve(StoragePath.internal(), fileServiceId);

        if (storedFileOptional.isPresent()) {
            final SuccessfulMaterialUploadJobData successfulMaterialUploadJobData = alfrescoFileUploader.uploadFileToAlfresco(
                    storedFileOptional.get(),
                    uploadMaterialToAlfrescoJobData);

            return executionInfo()
                    .withExecutionStatus(INPROGRESS)
                    .withNextTask(SUCCESSFUL_MATERIAL_UPLOAD_COMMAND_TASK)
                    .withNextTaskStartTime(clock.now())
                    .withJobData(objectToJsonObjectConverter.convert(successfulMaterialUploadJobData))
                    .build();
        } else {
            final String errorMessage = format("Failed to upload file to alfresco. No file found in storage with id '%s'", fileServiceId);
            logger.error(errorMessage);
            return hardFailureTaskFactory.createHardFailureTask(uploadMaterialToAlfrescoJobData, errorMessage);
        }
    }
}
