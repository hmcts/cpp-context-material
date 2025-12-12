package uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle;

import static java.lang.String.format;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.INPROGRESS;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.FAILED_BUNDLE_UPLOAD_COMMAND_TASK;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.SUCCESSFUL_BUNDLE_UPLOAD_COMMAND_TASK;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.UPLOAD_BUNDLE_TO_ALFRESCO_TASK;

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
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.FailedBundleUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.SuccessfulBundleUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.UploadBundleToAlfrescoJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.service.FileUploadRetryConfiguration;
import uk.gov.moj.cpp.material.event.processor.jobstore.upload.AlfrescoFileUploader;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

@Task(UPLOAD_BUNDLE_TO_ALFRESCO_TASK)
public class AlfrescoUploadBundleTask implements ExecutableTask {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private FileService fileService;

    @Inject
    private AlfrescoFileUploader alfrescoFileUploader;

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

        final UploadBundleToAlfrescoJobData uploadBundleToAlfrescoJobData = jsonObjectConverter.convert(
                executionInfo.getJobData(),
                UploadBundleToAlfrescoJobData.class);

        final UUID fileServiceId = uploadBundleToAlfrescoJobData.getFileServiceId();

        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = toUploadMaterialToAlfrescoJobData(uploadBundleToAlfrescoJobData);
        try {
            final Optional<FileReference> fileReferenceOptional = fileService.retrieve(fileServiceId);

            if (fileReferenceOptional.isPresent()) {
                try(final FileReference fileReference = fileReferenceOptional.get()) {
                    final SuccessfulMaterialUploadJobData successfulMaterialUploadJobData = alfrescoFileUploader.uploadFileToAlfresco(fileReference, uploadMaterialToAlfrescoJobData);

                    final SuccessfulBundleUploadJobData successfulBundleUploadJobData = new SuccessfulBundleUploadJobData(successfulMaterialUploadJobData.getMaterialId(), successfulMaterialUploadJobData.getFileServiceId(), successfulMaterialUploadJobData.getAlfrescoFileId(),
                            false, successfulMaterialUploadJobData.getFileName(), successfulMaterialUploadJobData.getMediaType(), uploadBundleToAlfrescoJobData.getFileSize(), uploadBundleToAlfrescoJobData.getPageCount(),
                            uploadBundleToAlfrescoJobData.getEventMetadata());

                    return executionInfo()
                            .withExecutionStatus(INPROGRESS)
                            .withNextTask(SUCCESSFUL_BUNDLE_UPLOAD_COMMAND_TASK)
                            .withNextTaskStartTime(clock.now())
                            .withJobData(objectToJsonObjectConverter.convert(successfulBundleUploadJobData))
                            .build();
                }
            } else {
                final String errorMessage = format("Failed to upload file to alfresco. No file found in file service with id '%s'", fileServiceId);
                logger.error(errorMessage);
                return createRetryWithHardFailureTaskOnExhaust(uploadBundleToAlfrescoJobData, errorMessage);
            }
        } catch (final FileServiceException e) {
            final String errorMessage = format("Failed to retrieve file from file service with id '%s'", fileServiceId);
            return createRetryWithHardFailureTaskOnExhaust(uploadBundleToAlfrescoJobData, format("%s. %s:%s", errorMessage,
                    e.getClass().getSimpleName(), e.getMessage()));
        } catch (final Exception e) {
            final String errorMessage = format("Unexpected error occurred when attempting to upload file to alfresco for fileId: '%s'", fileServiceId);
            return createRetryWithHardFailureTaskOnExhaust(uploadBundleToAlfrescoJobData, format("%s. %s:%s", errorMessage,
                    e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    private ExecutionInfo createRetryWithHardFailureTaskOnExhaust(UploadBundleToAlfrescoJobData uploadBundleToAlfrescoJobData, String errorMessage) {
        final ZonedDateTime failedTime = clock.now();

        final FailedBundleUploadJobData failedBundleUploadJobData = new FailedBundleUploadJobData(
                uploadBundleToAlfrescoJobData.getBundledMaterialId(),
                uploadBundleToAlfrescoJobData.getMaterialIds(),
                Optional.of(uploadBundleToAlfrescoJobData.getFileServiceId()),
                uploadBundleToAlfrescoJobData.getEventMetadata(),
                BundleErrorType.UPLOAD_FILE_ERROR,
                errorMessage,
                failedTime
        );

        return executionInfo()
                .withShouldRetry(true)
                .withNextTask(FAILED_BUNDLE_UPLOAD_COMMAND_TASK)
                .withJobData(objectToJsonObjectConverter.convert(failedBundleUploadJobData))
                .withExecutionStatus(INPROGRESS)
                .withNextTaskStartTime(failedTime)
                .build();
    }

    private UploadMaterialToAlfrescoJobData toUploadMaterialToAlfrescoJobData(UploadBundleToAlfrescoJobData uploadBundleToAlfrescoJobData) {
        return new UploadMaterialToAlfrescoJobData(
                uploadBundleToAlfrescoJobData.getBundledMaterialId(),
                uploadBundleToAlfrescoJobData.getFileServiceId(),
                false,
                uploadBundleToAlfrescoJobData.getEventMetadata(),""
        );
    }
}