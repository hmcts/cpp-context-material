package uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.COMPLETED;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.FAILED_BUNDLE_UPLOAD_COMMAND_TASK;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.jobstore.api.annotation.Task;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.FailedBundleUploadJobData;

import javax.inject.Inject;

@Task(FAILED_BUNDLE_UPLOAD_COMMAND_TASK)
public class FailedBundleUploadToAlfrescoTask implements ExecutableTask {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private RecordBundleDetailCommandFactory recordBundleDetailCommandFactory;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Override
    public ExecutionInfo execute(final ExecutionInfo executionInfo) {

        final FailedBundleUploadJobData failedBundleUploadJobData = jsonObjectToObjectConverter
                .convert(executionInfo.getJobData(), FailedBundleUploadJobData.class);

        final JsonEnvelope recordBundleDetailFailedCommandEnvelope = recordBundleDetailCommandFactory
                .recordBundleFailedCommand(failedBundleUploadJobData.getBundledMaterialId(),
                        failedBundleUploadJobData.getMaterialIds(),
                        failedBundleUploadJobData.getFileServiceId(),
                        failedBundleUploadJobData.getEventMetadata(),
                        failedBundleUploadJobData.getErrorType(),
                        failedBundleUploadJobData.getErrorMessage(),
                        failedBundleUploadJobData.getFailedTime());

        sender.send(recordBundleDetailFailedCommandEnvelope);

        return executionInfo()
                .withExecutionStatus(COMPLETED)
                .build();
    }
}
