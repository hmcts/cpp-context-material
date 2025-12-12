package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.INPROGRESS;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.FAILED_MATERIAL_UPLOAD_COMMAND_TASK;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.FailedMaterialUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HardFailureTaskFactoryTest {

    @Mock
    private UtcClock clock;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private HardFailureTaskFactory hardFailureTaskFactory;

    @Captor
    private ArgumentCaptor<FailedMaterialUploadJobData> failedMaterialUploadJobDataCaptor;

    @Test
    public void shouldCreateTheHardFailureNextTask() throws Exception {

        final String errorMessage = "The Norwegians are leaving!";
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final ZonedDateTime failedTime = new UtcClock().now();
        final JsonObject fileUploadedEventMetadata = mock(JsonObject.class);

        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = mock(UploadMaterialToAlfrescoJobData.class);
        final JsonObject jobData = mock(JsonObject.class);

        when(uploadMaterialToAlfrescoJobData.getMaterialId()).thenReturn(materialId);
        when(uploadMaterialToAlfrescoJobData.getFileServiceId()).thenReturn(fileServiceId);
        when(uploadMaterialToAlfrescoJobData.getFileUploadedEventMetadata()).thenReturn(fileUploadedEventMetadata);
        when(clock.now()).thenReturn(failedTime);
        when(objectToJsonObjectConverter.convert(any(FailedMaterialUploadJobData.class))).thenReturn(jobData);

        final ExecutionInfo nextTaskExecutionInfo = hardFailureTaskFactory.createHardFailureTask(
                uploadMaterialToAlfrescoJobData,
                errorMessage);

        assertThat(nextTaskExecutionInfo.getNextTask(), is(FAILED_MATERIAL_UPLOAD_COMMAND_TASK));
        assertThat(nextTaskExecutionInfo.getExecutionStatus(), is(INPROGRESS));
        assertThat(nextTaskExecutionInfo.getJobData(), is(jobData));
        assertThat(nextTaskExecutionInfo.getNextTaskStartTime(), is(failedTime));

        verify(objectToJsonObjectConverter).convert(failedMaterialUploadJobDataCaptor.capture());

        final FailedMaterialUploadJobData failedMaterialUploadJobData = failedMaterialUploadJobDataCaptor.getValue();

        assertThat(failedMaterialUploadJobData.getMaterialId(), is(materialId));
        assertThat(failedMaterialUploadJobData.getFileServiceId(), is(fileServiceId));
        assertThat(failedMaterialUploadJobData.getFileUploadedEventMetadata(), is(fileUploadedEventMetadata));
        assertThat(failedMaterialUploadJobData.getErrorMessage(), is(errorMessage));
        assertThat(failedMaterialUploadJobData.getFailedTime(), is(failedTime));
    }

    @Test
    public void shouldCreateRetryWithHardFailureExhaustTask() throws Exception {

        final String errorMessage = "The Norwegians are leaving!";
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final ZonedDateTime failedTime = new UtcClock().now();
        final JsonObject uploadMaterialJobDataJson = metadataWithRandomUUIDAndName().build().asJsonObject();
        final JsonObject failedMaterialUploadJobDataJson = metadataWithRandomUUIDAndName().build().asJsonObject();
        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(materialId, fileServiceId, false, uploadMaterialJobDataJson, "");
        when(objectToJsonObjectConverter.convert(any(FailedMaterialUploadJobData.class))).thenReturn(failedMaterialUploadJobDataJson);
        when(clock.now()).thenReturn(failedTime);

        final ExecutionInfo nextTaskExecutionInfo = hardFailureTaskFactory.createRetryWithHardFailureTaskOnExhaust(
                uploadMaterialToAlfrescoJobData,
                errorMessage);

        assertThat(nextTaskExecutionInfo.getNextTask(), is(FAILED_MATERIAL_UPLOAD_COMMAND_TASK));
        assertThat(nextTaskExecutionInfo.getExecutionStatus(), is(INPROGRESS));
        assertThat(nextTaskExecutionInfo.getJobData(), is(failedMaterialUploadJobDataJson));
        assertThat(nextTaskExecutionInfo.getNextTaskStartTime(), is(failedTime));
        assertThat(nextTaskExecutionInfo.isShouldRetry(), is(true));

        verify(objectToJsonObjectConverter).convert(failedMaterialUploadJobDataCaptor.capture());
        final FailedMaterialUploadJobData failedMaterialUploadJobData = failedMaterialUploadJobDataCaptor.getValue();
        assertThat(failedMaterialUploadJobData.getMaterialId(), is(materialId));
        assertThat(failedMaterialUploadJobData.getFileServiceId(), is(fileServiceId));
        assertThat(failedMaterialUploadJobData.getFileUploadedEventMetadata(), is(uploadMaterialJobDataJson));
        assertThat(failedMaterialUploadJobData.getErrorMessage(), is(errorMessage));
        assertThat(failedMaterialUploadJobData.getFailedTime(), is(failedTime));
    }
}