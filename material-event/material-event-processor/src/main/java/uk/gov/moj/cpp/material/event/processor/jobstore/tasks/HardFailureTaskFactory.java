package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.INPROGRESS;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.FAILED_MATERIAL_UPLOAD_COMMAND_TASK;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.FailedMaterialUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData;

import java.time.ZonedDateTime;

import javax.inject.Inject;

public class HardFailureTaskFactory {

    @Inject
    private UtcClock clock;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    
    public ExecutionInfo createHardFailureTask(
            final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData,
            final String errorMessage) {

        final ZonedDateTime failedTime = clock.now();

        final FailedMaterialUploadJobData failedMaterialUploadJobData = new FailedMaterialUploadJobData(
                uploadMaterialToAlfrescoJobData.getMaterialId(),
                uploadMaterialToAlfrescoJobData.getFileServiceId(),
                "",
                uploadMaterialToAlfrescoJobData.getFileUploadedEventMetadata(),
                errorMessage,
                failedTime
        );

        return executionInfo()
                .withNextTask(FAILED_MATERIAL_UPLOAD_COMMAND_TASK)
                .withJobData(objectToJsonObjectConverter.convert(failedMaterialUploadJobData))
                .withExecutionStatus(INPROGRESS)
                .withNextTaskStartTime(failedTime)
                .build();
    }

    public ExecutionInfo createRetryWithHardFailureTaskOnExhaust(
            final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData,
            final String errorMessage) {

        final ZonedDateTime failedTime = clock.now();

        final FailedMaterialUploadJobData failedMaterialUploadJobData = new FailedMaterialUploadJobData(
                uploadMaterialToAlfrescoJobData.getMaterialId(),
                uploadMaterialToAlfrescoJobData.getFileServiceId(),
                "",
                uploadMaterialToAlfrescoJobData.getFileUploadedEventMetadata(),
                errorMessage,
                failedTime
        );

        return executionInfo()
                .withShouldRetry(true)
                .withNextTask(FAILED_MATERIAL_UPLOAD_COMMAND_TASK)
                .withJobData(objectToJsonObjectConverter.convert(failedMaterialUploadJobData))
                .withExecutionStatus(INPROGRESS)
                .withNextTaskStartTime(failedTime)
                .build();
    }
}
