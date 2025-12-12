package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.COMPLETED;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.SUCCESSFUL_MATERIAL_UPLOAD_COMMAND_TASK;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.jobstore.api.annotation.Task;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

@Task(SUCCESSFUL_MATERIAL_UPLOAD_COMMAND_TASK)
public class SuccessfulMaterialUploadTask implements ExecutableTask {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private AddMaterialCommandFactory addMaterialCommandFactory;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @Override
    public ExecutionInfo execute(final ExecutionInfo executionInfo) {

        final JsonObject jobData = executionInfo.getJobData();
        final SuccessfulMaterialUploadJobData sendMaterialToAlfrescoJobState = jsonObjectConverter.convert(jobData, SuccessfulMaterialUploadJobData.class);

        final JsonEnvelope addMaterialCommandEnvelope = addMaterialCommandFactory
                .createCommandEnvelope(sendMaterialToAlfrescoJobState);

        logger.debug("addMaterialCommandEnvelope is {} ", addMaterialCommandEnvelope);
        sender.send(addMaterialCommandEnvelope);

        return executionInfo()
                .withExecutionStatus(COMPLETED)
                .build();
    }
}
