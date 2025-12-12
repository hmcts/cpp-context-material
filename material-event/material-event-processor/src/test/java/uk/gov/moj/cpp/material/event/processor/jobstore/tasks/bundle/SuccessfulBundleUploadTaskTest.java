package uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.SuccessfulBundleUploadJobData;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class SuccessfulBundleUploadTaskTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private RecordBundleDetailCommandFactory recordBundleDetailCommandFactory;

    @Mock
    private Sender sender;

    @Mock
    private Logger logger;

    @InjectMocks
    private SuccessfulBundleUploadTask successfulBundleUploadTask;

    private UUID bundledMaterialId = UUID.randomUUID();
    private UUID fileServiceId = UUID.randomUUID();
    private UUID alfrescoFileId = UUID.randomUUID();
    private String fileName = "fileName";
    private String mediaType = "mediaType";
    private Long fileSize = 12345L;
    private int pageCount = 10;

    @BeforeEach
    public void setup() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSuccessfulRecordBundleDetailCommandEnvelopeAndAddMaterialCommandEnvelope() {
        //given
        Metadata expectedEventMetadata = metadataWithRandomUUIDAndName().build();
        SuccessfulBundleUploadJobData successfulBundleUploadJobData = new SuccessfulBundleUploadJobData(bundledMaterialId, fileServiceId, alfrescoFileId,
                false, fileName, mediaType, fileSize, pageCount, expectedEventMetadata.asJsonObject());
        ExecutionInfo initialExecutionInfo = ExecutionInfo.executionInfo().withJobData(objectToJsonObjectConverter.convert(successfulBundleUploadJobData)).build();

        //when
        ExecutionInfo resultExecutionInfo = successfulBundleUploadTask.execute(initialExecutionInfo);

        //then
        assertThat(resultExecutionInfo.getExecutionStatus(), is(ExecutionStatus.COMPLETED));
        verify(recordBundleDetailCommandFactory).recordBundleCommand(successfulBundleUploadJobData.getMaterialId(),
                successfulBundleUploadJobData.getFileName(), successfulBundleUploadJobData.getAlfrescoFileId(),
                successfulBundleUploadJobData.getMediaType(), successfulBundleUploadJobData.getFileSize(),
                successfulBundleUploadJobData.getPageCount(), successfulBundleUploadJobData.getFileUploadedEventMetadata());
        verify(sender, times(1)).send(any());
    }


}