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
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.FailedMaterialUploadJobData;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FailedMaterialUploadTaskTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private AddMaterialCommandFailedFactory addMaterialCommandFailedFactory;

    @Mock
    private Sender sender;

    @InjectMocks
    private FailedMaterialUploadTask failedMaterialUploadTask;

    @Test
    public void shouldSendAddMaterialCommandInTheTask() {

        final ExecutionInfo executionInfo = mock(ExecutionInfo.class);

        final JsonObject jobData = mock(JsonObject.class);
        final FailedMaterialUploadJobData failedMaterialUploadJobData = mock(FailedMaterialUploadJobData.class);
        final JsonEnvelope recordAddMaterialFailedCommandEnvelope = mock(JsonEnvelope.class);

        when(executionInfo.getJobData()).thenReturn(jobData);
        when(jsonObjectConverter.convert(jobData, FailedMaterialUploadJobData.class)).thenReturn(failedMaterialUploadJobData);
        when(addMaterialCommandFailedFactory.createCommandEnvelope(failedMaterialUploadJobData)).thenReturn(recordAddMaterialFailedCommandEnvelope);

        final ExecutionInfo failedExecutionInfo = failedMaterialUploadTask.execute(executionInfo);

        assertThat(failedExecutionInfo.getExecutionStatus(), is(COMPLETED));

        verify(sender).send(recordAddMaterialFailedCommandEnvelope);
    }
}