package uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.COMPLETED;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.SUCCESSFUL_BUNDLE_UPLOAD_COMMAND_TASK;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.jobstore.api.annotation.Task;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.SuccessfulBundleUploadJobData;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

@Task(SUCCESSFUL_BUNDLE_UPLOAD_COMMAND_TASK)
public class SuccessfulBundleUploadTask implements ExecutableTask {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private RecordBundleDetailCommandFactory recordBundleDetailCommandFactory;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @SuppressWarnings({"squid:S1312"})
    @Inject
    private Logger logger;

    @Override
    public ExecutionInfo execute(final ExecutionInfo executionInfo) {

        final JsonObject jobData = executionInfo.getJobData();

        final SuccessfulBundleUploadJobData successfulBundleUploadJobData = jsonObjectConverter.convert(jobData, SuccessfulBundleUploadJobData.class);

        final JsonEnvelope recordBundleDetailCommandEnvelope = recordBundleDetailCommandFactory
                .recordBundleCommand(successfulBundleUploadJobData.getMaterialId(), successfulBundleUploadJobData.getFileName(),
                        successfulBundleUploadJobData.getAlfrescoFileId(), successfulBundleUploadJobData.getMediaType(),
                        successfulBundleUploadJobData.getFileSize(), successfulBundleUploadJobData.getPageCount(),
                        successfulBundleUploadJobData.getFileUploadedEventMetadata());
        sender.send(recordBundleDetailCommandEnvelope);
        logger.info("Sent recordBundleDetailCommand for bundledMaterialId={}", successfulBundleUploadJobData.getMaterialId());

        return executionInfo()
                .withExecutionStatus(COMPLETED)
                .build();
    }
}
