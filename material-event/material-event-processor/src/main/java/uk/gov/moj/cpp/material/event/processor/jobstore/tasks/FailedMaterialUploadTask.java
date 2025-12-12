package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.COMPLETED;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.FAILED_MATERIAL_UPLOAD_COMMAND_TASK;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.jobstore.api.annotation.Task;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.FailedMaterialUploadJobData;

import javax.inject.Inject;

@Task(FAILED_MATERIAL_UPLOAD_COMMAND_TASK)
public class FailedMaterialUploadTask implements ExecutableTask {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private AddMaterialCommandFailedFactory addMaterialCommandFailedFactory;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Override
    public ExecutionInfo execute(final ExecutionInfo executionInfo) {

        final FailedMaterialUploadJobData failedMaterialUploadJobData = jsonObjectToObjectConverter.convert(
                executionInfo.getJobData(),
                FailedMaterialUploadJobData.class);

        final JsonEnvelope failedToAddMaterialCommandEnvelope = addMaterialCommandFailedFactory
                .createCommandEnvelope(failedMaterialUploadJobData);

        sender.send(failedToAddMaterialCommandEnvelope);

        return executionInfo()
                .withExecutionStatus(COMPLETED)
                .build();
    }
}
