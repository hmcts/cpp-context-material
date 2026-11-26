package uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.FAILED_MERGE_FILE_TASK;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.UPLOAD_BUNDLE_TO_ALFRESCO_TASK;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus;
import uk.gov.moj.cpp.material.client.MaterialClient;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.FailedBundleUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.MergeFileJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.UploadBundleToAlfrescoJobData;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.File;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class MergeFileTaskTest {

    @Mock
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Mock
    private MaterialClient materialClient;

    @Mock
    private Response response;

    @Mock
    private FileService fileService;
    @Mock
    private UtcClock clock;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @InjectMocks
    private MergeFileTask mergeFileTask;

    @Mock
    private Logger logger;

    private static final String BUNDLED_MATERIAL_NAME = "Barkingside Magistrates' Court 17072021.pdf";

    @BeforeEach
    public void setup() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldMergeMaterialFilesAndInitiateUploadBundleToAlfrescoTask() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID fileStoreId = UUID.randomUUID();
        final ZonedDateTime nextTaskStartTime = new UtcClock().now();

        //given
        UUID bundledMaterialId = UUID.randomUUID();
        Metadata expectedEventMetadata = metadataWithRandomUUIDAndName().build();
        MergeFileJobData mergeFileJobData = MergeFileJobData.MergeFileJobDataBuilder.mergeFileJobDataBuilder()
                .withBundledMaterialId(bundledMaterialId)
                .withBundledMaterialName(BUNDLED_MATERIAL_NAME)
                .withMaterialIds(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()))
                .withEventMetadata(expectedEventMetadata.asJsonObject())
                .build();

        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(materialClient.getMaterialAsPdf(any(UUID.class), eq(userId))).thenReturn(response);
        final ClassLoader classLoader = getClass().getClassLoader();
        final File file1 = new File(classLoader.getResource("file1.pdf").getFile());
        final File file2 = new File(classLoader.getResource("file2.pdf").getFile());
        when(response.getLocation()).thenReturn(new URI("file:" + file1.getAbsolutePath())).thenReturn(new URI("file:" + file2.getAbsolutePath()));
        when(response.getStatus()).thenReturn(200);

        when(fileService.store(any(), any())).thenReturn(fileStoreId);
        when(clock.now()).thenReturn(nextTaskStartTime);

        ExecutionInfo initialExecutionInfo = ExecutionInfo.executionInfo().withJobData(objectToJsonObjectConverter.convert(mergeFileJobData)).build();

        //when
        ExecutionInfo actualExecutionInfo = mergeFileTask.execute(initialExecutionInfo);

        //then
        assertThat(actualExecutionInfo.getExecutionStatus(), is(ExecutionStatus.INPROGRESS));
        assertThat(actualExecutionInfo.getNextTaskStartTime(), is(nextTaskStartTime));
        assertThat(actualExecutionInfo.getNextTask(), is(UPLOAD_BUNDLE_TO_ALFRESCO_TASK));
        assertFalse(actualExecutionInfo.isShouldRetry());

        UploadBundleToAlfrescoJobData expectedJobData = jsonObjectConverter.convert(actualExecutionInfo.getJobData(), UploadBundleToAlfrescoJobData.class);
        assertThat(expectedJobData.getBundledMaterialId(), is(mergeFileJobData.getBundledMaterialId()));
        assertThat(expectedJobData.getBundledMaterialName(), is(mergeFileJobData.getBundledMaterialName()));
        assertThat(expectedJobData.getFileServiceId(), is(fileStoreId));
        assertThat(expectedJobData.getFileSize(), is(141006L));
        assertThat(expectedJobData.getPageCount(), is(12));
        assertThat(expectedJobData.getEventMetadata(), is(mergeFileJobData.getEventMetadata()));
    }

    @Test
    public void shouldInitiateFailedMergeTaskWhenMaterialNotFound() throws Exception {

        UUID userId = UUID.randomUUID();
        final ZonedDateTime nextTaskStartTime = new UtcClock().now();

        //given
        UUID bundledMaterialId = UUID.randomUUID();
        Metadata expectedEventMetadata = metadataWithRandomUUIDAndName().build();
        MergeFileJobData mergeFileJobData = MergeFileJobData.MergeFileJobDataBuilder.mergeFileJobDataBuilder()
                .withBundledMaterialId(bundledMaterialId)
                .withBundledMaterialName(BUNDLED_MATERIAL_NAME)
                .withMaterialIds(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()))
                .withEventMetadata(expectedEventMetadata.asJsonObject())
                .build();

        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(materialClient.getMaterialAsPdf(any(UUID.class), eq(userId))).thenReturn(response);
        when(response.getStatus()).thenReturn(404);
        when(clock.now()).thenReturn(nextTaskStartTime);

        ExecutionInfo initialExecutionInfo = ExecutionInfo.executionInfo().withJobData(objectToJsonObjectConverter.convert(mergeFileJobData)).build();

        //when
        ExecutionInfo actualExecutionInfo = mergeFileTask.execute(initialExecutionInfo);

        //then
        assertThat(actualExecutionInfo.getExecutionStatus(), is(ExecutionStatus.INPROGRESS));
        assertThat(actualExecutionInfo.getNextTaskStartTime(), is(nextTaskStartTime));
        assertThat(actualExecutionInfo.getNextTask(), is(FAILED_MERGE_FILE_TASK));
        assertThat(jsonObjectConverter.convert(actualExecutionInfo.getJobData(), FailedBundleUploadJobData.class), is(instanceOf(FailedBundleUploadJobData.class)));
        assertFalse(actualExecutionInfo.isShouldRetry());
    }

    @Test
    public void shouldInitiateFailedMergeTaskWhenExceptionStoringMergedDocument() throws Exception {

        UUID userId = UUID.randomUUID();
        final ZonedDateTime nextTaskStartTime = new UtcClock().now();

        //given
        UUID bundledMaterialId = UUID.randomUUID();
        Metadata expectedEventMetadata = metadataWithRandomUUIDAndName().build();
        MergeFileJobData mergeFileJobData = MergeFileJobData.MergeFileJobDataBuilder.mergeFileJobDataBuilder()
                .withBundledMaterialId(bundledMaterialId)
                .withBundledMaterialName(BUNDLED_MATERIAL_NAME)
                .withMaterialIds(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()))
                .withEventMetadata(expectedEventMetadata.asJsonObject())
                .build();

        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(materialClient.getMaterialAsPdf(any(UUID.class), eq(userId))).thenReturn(response);
        final ClassLoader classLoader = getClass().getClassLoader();
        final File file1 = new File(classLoader.getResource("file1.pdf").getFile());
        final File file2 = new File(classLoader.getResource("file2.pdf").getFile());
        when(response.getLocation()).thenReturn(new URI("file:" + file1.getAbsolutePath())).thenReturn(new URI("file:" + file2.getAbsolutePath()));
        when(response.getStatus()).thenReturn(200);

        when(fileService.store(any(), any())).thenThrow(FileServiceException.class);
        when(clock.now()).thenReturn(nextTaskStartTime);

        //when
        ExecutionInfo initialExecutionInfo = ExecutionInfo.executionInfo().withJobData(objectToJsonObjectConverter.convert(mergeFileJobData)).build();

        //then
        ExecutionInfo actualExecutionInfo = mergeFileTask.execute(initialExecutionInfo);

        assertThat(actualExecutionInfo.getExecutionStatus(), is(ExecutionStatus.INPROGRESS));
        assertThat(actualExecutionInfo.getNextTaskStartTime(), is(nextTaskStartTime));
        assertThat(actualExecutionInfo.getNextTask(), is(FAILED_MERGE_FILE_TASK));
        assertThat(jsonObjectConverter.convert(actualExecutionInfo.getJobData(), FailedBundleUploadJobData.class), is(instanceOf(FailedBundleUploadJobData.class)));
        assertFalse(actualExecutionInfo.isShouldRetry());
    }


}