package uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.INPROGRESS;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.FAILED_BUNDLE_UPLOAD_COMMAND_TASK;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.SUCCESSFUL_BUNDLE_UPLOAD_COMMAND_TASK;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.FailedBundleUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.SuccessfulBundleUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.UploadBundleToAlfrescoJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.service.FileUploadRetryConfiguration;
import uk.gov.moj.cpp.material.event.processor.jobstore.upload.AlfrescoFileUploader;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;


@ExtendWith(MockitoExtension.class)
public class AlfrescoUploadBundleTaskTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private FileService fileService;

    @Mock
    private AlfrescoFileUploader alfrescoFileUploader;

    @Mock
    private UtcClock clock;

    @SuppressWarnings({"squid:S1312"})
    @Mock
    private Logger logger;

    @Mock
    private FileReference fileReference;

    @Mock
    private FileUploadRetryConfiguration fileUploadRetryConfiguration;

    @InjectMocks
    private AlfrescoUploadBundleTask alfrescoUploadBundleTask;
    private static final String BUNDLED_MATERIAL_NAME = "Barkingside Magistrates' Court 17072021.pdf";

    private UUID bundledMaterialId = UUID.randomUUID();
    private Metadata expectedEventMetadata = metadataWithRandomUUIDAndName().build();
    private long expectedFileSize = 12345L;
    private int expectedPageCount = 12;

    @BeforeEach
    public void setup() {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        setField(objectToJsonObjectConverter, "mapper", objectMapper);
        setField(jsonObjectConverter, "objectMapper", objectMapper);
    }

    @Test
    public void shouldUploadBundleFileToAlfrescoThenScheduleNextTask() throws Exception {
        UUID fileStoreId = UUID.randomUUID();
        UUID alfrescoFileId = UUID.randomUUID();
        final ZonedDateTime nextTaskStartTime = new UtcClock().now();
        List<UUID> materialIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());

        //given
        UploadBundleToAlfrescoJobData uploadBundleToAlfrescoJobData = new UploadBundleToAlfrescoJobData(bundledMaterialId, BUNDLED_MATERIAL_NAME, materialIds,
                fileStoreId, expectedFileSize, expectedPageCount,
                expectedEventMetadata.asJsonObject());

        ExecutionInfo executionInfo = ExecutionInfo.executionInfo().withJobData(objectToJsonObjectConverter.convert(uploadBundleToAlfrescoJobData)).build();
        when(fileService.retrieve(fileStoreId)).thenReturn(of(fileReference));
        SuccessfulMaterialUploadJobData successfulMaterialUploadJobData = getSuccessfulMaterialUploadJobData(bundledMaterialId, fileStoreId, alfrescoFileId, BUNDLED_MATERIAL_NAME, "mediaType", expectedEventMetadata.asJsonObject());
        when(alfrescoFileUploader.uploadFileToAlfresco(eq(fileReference), any(UploadMaterialToAlfrescoJobData.class))).thenReturn(successfulMaterialUploadJobData);
        when(clock.now()).thenReturn(nextTaskStartTime);

        //when
        ExecutionInfo actualExecutionInfo = alfrescoUploadBundleTask.execute(executionInfo);

        //then
        assertThat(actualExecutionInfo.getExecutionStatus(), is(ExecutionStatus.INPROGRESS));
        assertThat(actualExecutionInfo.getNextTaskStartTime(), is(nextTaskStartTime));
        assertThat(actualExecutionInfo.getNextTask(), is(SUCCESSFUL_BUNDLE_UPLOAD_COMMAND_TASK));
        SuccessfulBundleUploadJobData actualSuccessfulBundleUploadJobData = jsonObjectConverter.convert(actualExecutionInfo.getJobData(), SuccessfulBundleUploadJobData.class);
        assertThat(actualSuccessfulBundleUploadJobData.getMaterialId(), is(successfulMaterialUploadJobData.getMaterialId()));
        assertThat(actualSuccessfulBundleUploadJobData.getAlfrescoFileId(), is(successfulMaterialUploadJobData.getAlfrescoFileId()));
        assertThat(actualSuccessfulBundleUploadJobData.getFileServiceId(), is(successfulMaterialUploadJobData.getFileServiceId()));
        assertThat(actualSuccessfulBundleUploadJobData.getMediaType(), is(successfulMaterialUploadJobData.getMediaType()));
        assertThat(actualSuccessfulBundleUploadJobData.getFileUploadedEventMetadata(), is(successfulMaterialUploadJobData.getFileUploadedEventMetadata()));
        assertThat(actualSuccessfulBundleUploadJobData.getFileName(), is(successfulMaterialUploadJobData.getFileName()));
        assertThat(actualSuccessfulBundleUploadJobData.getFileSize(), is(expectedFileSize));
        assertThat(actualSuccessfulBundleUploadJobData.getPageCount(), is(expectedPageCount));
    }

    @Test
    public void shouldReturnFailureTaskWhenBundleMaterialNotFoundInFileStore() throws Exception {
        UUID fileStoreId = UUID.randomUUID();
        final ZonedDateTime nextTaskStartTime = new UtcClock().now();
        List<UUID> materialIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());

        //given
        UploadBundleToAlfrescoJobData uploadBundleToAlfrescoJobData = new UploadBundleToAlfrescoJobData(bundledMaterialId, BUNDLED_MATERIAL_NAME, materialIds,
                fileStoreId, expectedFileSize, expectedPageCount,
                expectedEventMetadata.asJsonObject());

        ExecutionInfo executionInfo = ExecutionInfo.executionInfo().withJobData(objectToJsonObjectConverter.convert(uploadBundleToAlfrescoJobData)).build();
        when(fileService.retrieve(fileStoreId)).thenReturn(Optional.empty());
        when(clock.now()).thenReturn(nextTaskStartTime);

        //when
        ExecutionInfo actualExecutionInfo = alfrescoUploadBundleTask.execute(executionInfo);

        //then
        assertThat(actualExecutionInfo.getExecutionStatus(), is(ExecutionStatus.INPROGRESS));
        assertThat(actualExecutionInfo.getNextTaskStartTime(), is(nextTaskStartTime));
        assertThat(actualExecutionInfo.getNextTask(), is(FAILED_BUNDLE_UPLOAD_COMMAND_TASK));
        FailedBundleUploadJobData actualFailedBundleUploadJobData = jsonObjectConverter.convert(actualExecutionInfo.getJobData(), FailedBundleUploadJobData.class);
        assertThat(actualFailedBundleUploadJobData.getBundledMaterialId(), is(bundledMaterialId));
        assertThat(actualFailedBundleUploadJobData.getMaterialIds(), is(materialIds));
        assertThat(actualFailedBundleUploadJobData.getErrorMessage(), is("Failed to upload file to alfresco. No file found in file service with id '" + fileStoreId + "'"));
        assertThat(actualFailedBundleUploadJobData.getFailedTime(), notNullValue());
        assertThat(actualFailedBundleUploadJobData.getEventMetadata(), is(expectedEventMetadata.asJsonObject()));
    }

    static Stream<Arguments> failureScenarios() {
        return Stream.of(
                Arguments.of(new FileServiceException(""), "Failed to retrieve file from file service with id '%s'"),
                Arguments.of(new RuntimeException(), "Unexpected error occurred when attempting to upload file to alfresco for fileId: '%s'")
        );
    }

    @ParameterizedTest
    @MethodSource("failureScenarios")
    public void shouldReturnRetryTaskWhenUploadBundleToAlfrescoFailWithFileServiceException(Exception exceptionToThrow, String expectedErrorMessage) throws Exception {
        final UUID fileStoreId = UUID.randomUUID();
        final ZonedDateTime exhaustTaskStartTime = new UtcClock().now().plusMinutes(5);
        final List<UUID> materialIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        final UploadBundleToAlfrescoJobData uploadBundleToAlfrescoJobData = new UploadBundleToAlfrescoJobData(bundledMaterialId, BUNDLED_MATERIAL_NAME, materialIds,
                fileStoreId, expectedFileSize, expectedPageCount,
                expectedEventMetadata.asJsonObject());
        final ExecutionInfo executionInfo = ExecutionInfo.executionInfo().withJobData(objectToJsonObjectConverter.convert(uploadBundleToAlfrescoJobData)).build();
        doThrow(exceptionToThrow).when(fileService).retrieve(eq(fileStoreId));
        when(clock.now()).thenReturn(exhaustTaskStartTime);

        ExecutionInfo actualExecutionInfo = alfrescoUploadBundleTask.execute(executionInfo);

        assertRetryWithExhaustTaskExecutionInfo(exhaustTaskStartTime, uploadBundleToAlfrescoJobData, actualExecutionInfo, expectedErrorMessage);
    }

    @Test
    void getRetryDurationsInSecs_shouldReturnDurations() {
        List<Long> retryDurations = List.of(2L);
        when(fileUploadRetryConfiguration.getAlfrescoFileUploadTaskRetryDurationsSeconds()).thenReturn(retryDurations);

        Optional<List<Long>> actual = alfrescoUploadBundleTask.getRetryDurationsInSecs();

        assertThat(actual.get(), is(retryDurations));
    }

    private void assertRetryWithExhaustTaskExecutionInfo(ZonedDateTime exhaustTaskStartTime, UploadBundleToAlfrescoJobData uploadBundleToAlfrescoJobData,
                                                         ExecutionInfo actualExecutionInfo, String expectedErrorMessage) {
        assertThat(actualExecutionInfo.getNextTask(), is(FAILED_BUNDLE_UPLOAD_COMMAND_TASK));
        assertThat(actualExecutionInfo.getExecutionStatus(), is(INPROGRESS));
        assertThat(actualExecutionInfo.getNextTaskStartTime(), is(exhaustTaskStartTime));
        assertThat(actualExecutionInfo.isShouldRetry(), is(true));

        final FailedBundleUploadJobData failedMaterialUploadJobData = jsonObjectConverter.convert(actualExecutionInfo.getJobData(), FailedBundleUploadJobData.class);
        assertThat(failedMaterialUploadJobData.getBundledMaterialId(), is(uploadBundleToAlfrescoJobData.getBundledMaterialId()));
        assertThat(failedMaterialUploadJobData.getMaterialIds(), is(uploadBundleToAlfrescoJobData.getMaterialIds()));
        assertThat(failedMaterialUploadJobData.getFileServiceId().get(), is(uploadBundleToAlfrescoJobData.getFileServiceId()));
        assertThat(failedMaterialUploadJobData.getErrorType(), is(BundleErrorType.UPLOAD_FILE_ERROR));
        assertThat(failedMaterialUploadJobData.getEventMetadata(), is(uploadBundleToAlfrescoJobData.getEventMetadata()));
        assertThat(failedMaterialUploadJobData.getErrorMessage(), startsWith(String.format(expectedErrorMessage, uploadBundleToAlfrescoJobData.getFileServiceId())));
        assertThat(failedMaterialUploadJobData.getFailedTime(), is(exhaustTaskStartTime));
    }

    private SuccessfulMaterialUploadJobData getSuccessfulMaterialUploadJobData(UUID materialId, UUID fileServiceId,
                                                                               UUID alfrescoFileId, String fileName,
                                                                               String mediaType, JsonObject fileUploadedEventMetadata) {
        return new SuccessfulMaterialUploadJobData(materialId, fileServiceId,"" , alfrescoFileId,
                false, fileName, mediaType, fileUploadedEventMetadata);
    }

}