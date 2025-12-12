package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.INPROGRESS;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.SUCCESSFUL_MATERIAL_UPLOAD_COMMAND_TASK;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.service.FileUploadRetryConfiguration;
import uk.gov.moj.cpp.material.event.processor.jobstore.upload.AlfrescoFileUploader;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
 class MaterialAlfrescoUploadTaskTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private FileService fileService;

    @Mock
    private AlfrescoFileUploader alfrescoFileUploader;

    @Mock
    private HardFailureTaskFactory hardFailureTaskFactory;

    @Mock
    private UtcClock clock;

    @Mock
    private Logger logger;

    @Mock
    private FileUploadRetryConfiguration fileUploadRetryConfiguration;

    @InjectMocks
    private MaterialAlfrescoUploadTask materialAlfrescoUploadTask;

    @Test
  void shouldUploadFileToAlfrescoThenScheduleNextTask() throws Exception {

        final UUID fileServiceId = randomUUID();
        final ZonedDateTime now = new UtcClock().now();

        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = mock(UploadMaterialToAlfrescoJobData.class);
        final ExecutionInfo inputExecutionInfo = mock(ExecutionInfo.class);
        final JsonObject inputJobData = mock(JsonObject.class);
        final JsonObject outputJobData = mock(JsonObject.class);
        final FileReference fileReference = mock(FileReference.class);
        final SuccessfulMaterialUploadJobData successfulMaterialUploadJobData = mock(SuccessfulMaterialUploadJobData.class);

        when(inputExecutionInfo.getJobData()).thenReturn(inputJobData);
        when(uploadMaterialToAlfrescoJobData.getFileServiceId()).thenReturn(fileServiceId);

        when(jsonObjectConverter.convert(
                inputJobData,
                UploadMaterialToAlfrescoJobData.class)).thenReturn(uploadMaterialToAlfrescoJobData);
        when(fileService.retrieve(fileServiceId)).thenReturn(of(fileReference));
        when(alfrescoFileUploader.uploadFileToAlfresco(
                fileReference,
                uploadMaterialToAlfrescoJobData)).thenReturn(successfulMaterialUploadJobData);
        when(objectToJsonObjectConverter.convert(successfulMaterialUploadJobData)).thenReturn(outputJobData);
        when(clock.now()).thenReturn(now);

        final ExecutionInfo outputExecutionInfo = materialAlfrescoUploadTask.execute(inputExecutionInfo);

        assertThat(outputExecutionInfo.getNextTask(), is(SUCCESSFUL_MATERIAL_UPLOAD_COMMAND_TASK));
        assertThat(outputExecutionInfo.getExecutionStatus(), is(INPROGRESS));
        assertThat(outputExecutionInfo.getJobData(), is(outputJobData));
        assertThat(outputExecutionInfo.getNextTaskStartTime(), is(now));
    }

    @Test
    void shouldUploadAzureFileToAlfrescoThenScheduleNextTask() throws Exception {

        final ZonedDateTime now = new UtcClock().now();

        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = mock(UploadMaterialToAlfrescoJobData.class);
        final ExecutionInfo inputExecutionInfo = mock(ExecutionInfo.class);
        final JsonObject inputJobData = mock(JsonObject.class);
        final JsonObject outputJobData = mock(JsonObject.class);
        final SuccessfulMaterialUploadJobData successfulMaterialUploadJobData = mock(SuccessfulMaterialUploadJobData.class);

        when(inputExecutionInfo.getJobData()).thenReturn(inputJobData);
        when(uploadMaterialToAlfrescoJobData.getFileServiceId()).thenReturn(null);

        when(jsonObjectConverter.convert(
                inputJobData,
                UploadMaterialToAlfrescoJobData.class)).thenReturn(uploadMaterialToAlfrescoJobData);
        when(alfrescoFileUploader.uploadFileFromAzureToAlfresco(
                uploadMaterialToAlfrescoJobData)).thenReturn(successfulMaterialUploadJobData);
        when(objectToJsonObjectConverter.convert(successfulMaterialUploadJobData)).thenReturn(outputJobData);
        when(clock.now()).thenReturn(now);

        final ExecutionInfo outputExecutionInfo = materialAlfrescoUploadTask.execute(inputExecutionInfo);

        assertThat(outputExecutionInfo.getNextTask(), is(SUCCESSFUL_MATERIAL_UPLOAD_COMMAND_TASK));
        assertThat(outputExecutionInfo.getExecutionStatus(), is(INPROGRESS));
        assertThat(outputExecutionInfo.getJobData(), is(outputJobData));
        assertThat(outputExecutionInfo.getNextTaskStartTime(), is(now));
    }

    @Test
    void shouldCreateHardFailureTaskIfNoFileFoundInFileService() throws Exception {

        final UUID fileServiceId = fromString("dae2f001-96cc-49ac-938b-b569f4adfb3a");
        final String errorMessage = "Failed to upload file to alfresco. No file found in file service with id 'dae2f001-96cc-49ac-938b-b569f4adfb3a'";

        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = mock(UploadMaterialToAlfrescoJobData.class);
        final ExecutionInfo inputExecutionInfo = mock(ExecutionInfo.class);
        final JsonObject inputJobData = mock(JsonObject.class);
        final ExecutionInfo outputExecutionInfo = mock(ExecutionInfo.class);

        when(inputExecutionInfo.getJobData()).thenReturn(inputJobData);
        when(uploadMaterialToAlfrescoJobData.getFileServiceId()).thenReturn(fileServiceId);
        when(jsonObjectConverter.convert(inputJobData, UploadMaterialToAlfrescoJobData.class)).thenReturn(uploadMaterialToAlfrescoJobData);
        when(fileService.retrieve(fileServiceId)).thenReturn(empty());
        when(hardFailureTaskFactory.createHardFailureTask(uploadMaterialToAlfrescoJobData, errorMessage)).thenReturn(outputExecutionInfo);

        ExecutionInfo result = materialAlfrescoUploadTask.execute(inputExecutionInfo);
        assertThat(result, is(outputExecutionInfo));

        verify(logger).error(errorMessage);
    }

    static Stream<Arguments> failureScenarios() {
        return Stream.of(
                Arguments.of(new FileServiceException(""), "Failed to retrieve file from file service with id '%s'"),
                Arguments.of(new RuntimeException(), "Unexpected error occurred when attempting to upload file to alfresco for fileId: '%s'")
        );
    }

    @ParameterizedTest
    @MethodSource("failureScenarios")
    void shouldCreateRetryTaskWithExhaustTaskConfiguredIfGettingException(Exception exceptionToThrow, String expectedErrorMessage) throws Exception {

        final UUID fileServiceId = fromString("dd3eb062-de73-4ba1-af99-b01c52b6d104");

        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = mock(UploadMaterialToAlfrescoJobData.class);
        final ExecutionInfo inputExecutionInfo = mock(ExecutionInfo.class);
        final JsonObject inputJobData = mock(JsonObject.class);

        when(inputExecutionInfo.getJobData()).thenReturn(inputJobData);
        when(uploadMaterialToAlfrescoJobData.getFileServiceId()).thenReturn(fileServiceId);
        when(jsonObjectConverter.convert(inputJobData, UploadMaterialToAlfrescoJobData.class)).thenReturn(uploadMaterialToAlfrescoJobData);
        when(fileService.retrieve(fileServiceId)).thenThrow(exceptionToThrow);

        materialAlfrescoUploadTask.execute(inputExecutionInfo);

        verify(hardFailureTaskFactory).createRetryWithHardFailureTaskOnExhaust(
                eq(uploadMaterialToAlfrescoJobData),
                startsWith(String.format(expectedErrorMessage, fileServiceId)));
    }

    @Test
    void shouldCreateRetryTaskWithExhaustTaskConfigurationAzure( ) throws Exception {

        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = mock(UploadMaterialToAlfrescoJobData.class);
        final ExecutionInfo inputExecutionInfo = mock(ExecutionInfo.class);
        final JsonObject inputJobData = mock(JsonObject.class);

        when(inputExecutionInfo.getJobData()).thenReturn(inputJobData);
        when(uploadMaterialToAlfrescoJobData.getFileServiceId()).thenReturn(null);
        when(jsonObjectConverter.convert(inputJobData, UploadMaterialToAlfrescoJobData.class)).thenReturn(uploadMaterialToAlfrescoJobData);
        when(alfrescoFileUploader.uploadFileFromAzureToAlfresco(
                uploadMaterialToAlfrescoJobData)).thenThrow(new RuntimeException());

        materialAlfrescoUploadTask.execute(inputExecutionInfo);

        verify(hardFailureTaskFactory).createRetryWithHardFailureTaskOnExhaust(
                eq(uploadMaterialToAlfrescoJobData),
                startsWith(String.format("Unexpected error occurred when attempting to upload file to Alfresco for cloudLocation: ")));
    }

    @Test
    void getRetryDurationsInSecs_shouldReturnDurations() {
        List<Long> retryDurations = List.of(2L);
        when(fileUploadRetryConfiguration.getAlfrescoFileUploadTaskRetryDurationsSeconds()).thenReturn(retryDurations);

        Optional<List<Long>> actual = materialAlfrescoUploadTask.getRetryDurationsInSecs();

        assertThat(actual.get(), Matchers.is(retryDurations));
    }
}