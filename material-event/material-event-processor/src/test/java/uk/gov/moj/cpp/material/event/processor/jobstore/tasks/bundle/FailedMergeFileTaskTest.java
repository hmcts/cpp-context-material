package uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.FailedBundleUploadJobData;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FailedMergeFileTaskTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private RecordBundleDetailCommandFactory recordBundleDetailCommandFactory;

    @Mock
    private Sender sender;

    @InjectMocks
    private FailedMergeFileTask failedMergeFileTask;

    @BeforeEach
    public void setup() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSendRecordBundleDetailFailedCommand() {

        //given
        UUID bundledMaterialId = UUID.randomUUID();
        final ZonedDateTime failedTime = new UtcClock().now();
        Metadata expectedEventMetadata = metadataWithRandomUUIDAndName().build();
        FailedBundleUploadJobData failedBundleUploadJobData = new FailedBundleUploadJobData(bundledMaterialId,
                Arrays.asList(UUID.randomUUID(), UUID.randomUUID()), Optional.empty(), expectedEventMetadata.asJsonObject(),
                BundleErrorType.MERGE_FILE_ERROR, "errorMessage", failedTime);
        ExecutionInfo initialExecutionInfo = ExecutionInfo.executionInfo().withJobData(objectToJsonObjectConverter.convert(failedBundleUploadJobData)).build();

        //when
        ExecutionInfo resultExecutionInfo = failedMergeFileTask.execute(initialExecutionInfo);

        //then
        assertThat(resultExecutionInfo.getExecutionStatus(), is(ExecutionStatus.COMPLETED));
        verify(recordBundleDetailCommandFactory).recordBundleFailedCommand(eq(failedBundleUploadJobData.getBundledMaterialId()),
                eq(failedBundleUploadJobData.getMaterialIds()), any(), eq(failedBundleUploadJobData.getEventMetadata()),
                eq(failedBundleUploadJobData.getErrorType()), eq(failedBundleUploadJobData.getErrorMessage()), any(ZonedDateTime.class));
        verify(sender).send(any());
    }
}