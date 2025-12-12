package uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.FailedBundleUploadJobData;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FailedBundleUploadToAlfrescoTaskTest {

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private RecordBundleDetailCommandFactory recordBundleDetailCommandFactory;

    @Mock
    private Sender sender;

    @InjectMocks
    private FailedBundleUploadToAlfrescoTask failedBundleUploadToAlfrescoTask;

    private UUID bundledMaterialId = UUID.randomUUID();
    private UUID fileServiceId = UUID.randomUUID();
    private List<UUID> materialIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());

    @Captor
    private ArgumentCaptor<JsonEnvelope> commandEnvelopeCaptor;

    @BeforeEach
    public void setup() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSendRecordBundleDetailFailedCommandAndAddMaterialCommandFailedCommand() {

        Metadata expectedEventMetadata = metadataWithRandomUUIDAndName().build();
        String errorMessage = "ERROR!";
        final ZonedDateTime failedTime = new UtcClock().now();

        ExecutionInfo executionInfo = ExecutionInfo.executionInfo()
                .withJobData(objectToJsonObjectConverter.convert(getFailedBundleUploadJobData(expectedEventMetadata, errorMessage, failedTime)))
                .build();

        //when
        ExecutionInfo resultExecutionInfo = failedBundleUploadToAlfrescoTask.execute(executionInfo);

        //then
        assertThat(resultExecutionInfo.getExecutionStatus(), is(ExecutionStatus.COMPLETED));
        verify(recordBundleDetailCommandFactory).recordBundleFailedCommand(eq(bundledMaterialId), eq(materialIds), eq(Optional.of(fileServiceId)),
                eq(expectedEventMetadata.asJsonObject()), eq(BundleErrorType.UPLOAD_FILE_ERROR), eq(errorMessage), any(ZonedDateTime.class));
        verify(sender, times(1)).send(any());
    }

    private FailedBundleUploadJobData getFailedBundleUploadJobData(Metadata expectedEventMetadata, String errorMessage, ZonedDateTime failedTime) {
        return new FailedBundleUploadJobData(bundledMaterialId, materialIds, Optional.of(fileServiceId),
                expectedEventMetadata.asJsonObject(), BundleErrorType.UPLOAD_FILE_ERROR, errorMessage, failedTime);
    }
}