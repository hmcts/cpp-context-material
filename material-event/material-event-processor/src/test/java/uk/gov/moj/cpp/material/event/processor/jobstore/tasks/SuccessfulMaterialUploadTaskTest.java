package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.COMPLETED;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class SuccessfulMaterialUploadTaskTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private AddMaterialCommandFactory addMaterialCommandFactory;

    @Mock
    private Logger logger;

    @Mock
    private Sender sender;

    @InjectMocks
    private SuccessfulMaterialUploadTask successfulMaterialUploadTask;

    @Test
    public void shouldSendAddMaterialCommandInTheTask() {

        final ExecutionInfo executionInfo = mock(ExecutionInfo.class);

        final JsonObject jobData = mock(JsonObject.class);
        final SuccessfulMaterialUploadJobData sendMaterialToAlfrescoJobState = mock(SuccessfulMaterialUploadJobData.class);
        final JsonEnvelope addMaterialCommandEnvelope = mock(JsonEnvelope.class);

        when(executionInfo.getJobData()).thenReturn(jobData);
        when(jsonObjectConverter.convert(jobData, SuccessfulMaterialUploadJobData.class)).thenReturn(sendMaterialToAlfrescoJobState);
        when(addMaterialCommandFactory.createCommandEnvelope(sendMaterialToAlfrescoJobState)).thenReturn(addMaterialCommandEnvelope);

        final ExecutionInfo successfulExecutionInfo = successfulMaterialUploadTask.execute(executionInfo);

        assertThat(successfulExecutionInfo.getExecutionStatus(), is(COMPLETED));

        verify(sender).send(addMaterialCommandEnvelope);
    }
}