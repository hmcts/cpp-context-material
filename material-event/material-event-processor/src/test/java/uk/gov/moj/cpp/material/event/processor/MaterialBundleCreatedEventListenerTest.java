package uk.gov.moj.cpp.material.event.processor;

import static com.google.common.collect.ImmutableList.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.MERGE_FILE_TASK;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.jobstore.api.ExecutionService;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleRequested;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.MergeFileJobData;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.time.ZonedDateTime;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class MaterialBundleCreatedEventListenerTest {

    @Mock
    Envelope<MaterialBundleRequested> envelope;
    @InjectMocks
    private MaterialEventProcessor materialEventProcessor;
    @Mock
    private Enveloper enveloper;
    @Mock
    private Sender sender;
    @Mock
    private JsonObject payload;

    @Mock
    private MaterialBundleRequested materialBundleCreated;

    @Mock
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private UtcClock clock;
    @Mock
    private ExecutionService executionService;

    @Mock
    private Logger logger;

    private static final String BUNDLED_MATERIAL_NAME = "Barkingside Magistrates' Court 17072021.pdf";

    @Captor
    private ArgumentCaptor<ExecutionInfo> executionInfoCaptor;

    @BeforeEach
    public void setup() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleMaterialAddedEventMessage() {
        //given
        final ZonedDateTime nextTaskStartTime = new UtcClock().now();
        Metadata expectedEventMetadata = metadataWithRandomUUIDAndName().build();
        MaterialBundleRequested materialBundleRequested = new MaterialBundleRequested(randomUUID(), of(randomUUID(), randomUUID()), BUNDLED_MATERIAL_NAME);

        when(envelope.payload()).thenReturn(materialBundleRequested);
        when(envelope.metadata()).thenReturn(expectedEventMetadata);
        when(clock.now()).thenReturn(nextTaskStartTime);

        //when
        materialEventProcessor.materialBundleCreated(envelope);

        //then
        verify(executionService).executeWith(executionInfoCaptor.capture());

        assertThat(executionInfoCaptor.getValue(), notNullValue());
        assertThat(executionInfoCaptor.getValue().getExecutionStatus(), is(ExecutionStatus.STARTED));
        assertThat(executionInfoCaptor.getValue().getNextTask(), is(MERGE_FILE_TASK));
        assertThat(executionInfoCaptor.getValue().getNextTaskStartTime(), is(nextTaskStartTime));

        MergeFileJobData actualMergeFileJobData = jsonObjectConverter.convert(executionInfoCaptor.getValue().getJobData(), MergeFileJobData.class);
        assertThat(actualMergeFileJobData, notNullValue());
        assertThat(actualMergeFileJobData.getBundledMaterialId(), is(materialBundleRequested.getBundledMaterialId()));
        assertThat(actualMergeFileJobData.getBundledMaterialName(), is(materialBundleRequested.getBundledMaterialName()));
        assertThat(actualMergeFileJobData.getMaterialIds(), is(materialBundleRequested.getMaterialIds()));
        assertThat(actualMergeFileJobData.getEventMetadata(), is(expectedEventMetadata.asJsonObject()));
    }


}

