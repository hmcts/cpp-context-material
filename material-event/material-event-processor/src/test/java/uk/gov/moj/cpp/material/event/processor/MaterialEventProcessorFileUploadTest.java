package uk.gov.moj.cpp.material.event.processor;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.STARTED;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.UPLOAD_FILE_TO_ALFRESCO_TASK;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.file.api.requester.FileRequester;
import uk.gov.justice.services.file.api.sender.FileData;
import uk.gov.justice.services.file.api.sender.FileSender;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.jobstore.api.ExecutionService;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.lang3.RandomStringUtils;
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
public class MaterialEventProcessorFileUploadTest {

    public static final String MATERIAL_ID = "materialId";
    public static final String FILE_SERVICE_ID = "fileServiceId";
    public static final String FILE_REFERENCE = "fileReference";
    public static final String FILE_NAME = "fileName";
    public static final String DOCUMENT = "document";
    public static final String MIME_TYPE = "mimeType";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private FileService fileService;

    @Mock
    private FileSender fileSender;

    @Mock
    private FileData fileData;

    @Mock
    private InputStream inputStream;

    @Mock
    private Logger logger;

    @Mock
    private ExecutionService executionService;

    @Mock
    private UtcClock clock;

    @Mock
    private FileRequester fileRequester;


    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @BeforeEach
    public void createObjectToJsonObjectConverter() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @InjectMocks
    private MaterialEventProcessor materialEventProcessor;

    @Captor
    private ArgumentCaptor<ExecutionInfo> executionInfoCaptor;

    @Test
    public void shouldHandleFileUploadedEvent() {

        final UUID materialId = fromString("d1319ff0-bf27-4814-ba55-831f65894190");
        final UUID fileServiceId = fromString("fc5d37a7-20bc-4745-8bbd-08b6f2a1a684");

        final String eventName = "material.event.file-uploaded";
        final UUID causation = fromString("f5d3c9e0-82f6-4611-8ea4-d243c0059861");
        final String userId = "69f8bd3b-cfe7-4c73-aa05-f8773bdaf16c";
        final String clientId = "c9146fdb-edd8-4b96-95aa-5be9cd62608f";
        final String eventId = "ce15d242-0d4e-4ad8-bce7-d7a5f3ab77ef";

        final ZonedDateTime now = new UtcClock().now();
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> argCaptor = ArgumentCaptor.forClass(Object.class);

        when(clock.now()).thenReturn(now);

        final JsonEnvelope event = envelope()
                .with(metadataOf(eventId, eventName)
                        .withUserId(userId)
                        .withClientCorrelationId(clientId)
                        .withCausation(causation))
                .withPayloadOf(materialId.toString(), MATERIAL_ID)
                .withPayloadOf(fileServiceId.toString(), FILE_SERVICE_ID)
                .withPayloadOf(true, "isUnbundledDocument")
                .withPayloadOf(2, "retryAttemptsRemaining")
                .build();

        materialEventProcessor.handleFileUploaded(event);

        verify(executionService).executeWith(executionInfoCaptor.capture());

        final ExecutionInfo executionInfo = executionInfoCaptor.getValue();

        assertThat(executionInfo.getNextTaskStartTime(), is(now));
        assertThat(executionInfo.getExecutionStatus(), is(STARTED));
        assertThat(executionInfo.getNextTask(), is(UPLOAD_FILE_TO_ALFRESCO_TASK));

        final String jobDataJson = executionInfo.getJobData().toString();

        with(jobDataJson)
                .assertThat("$.materialId", is(materialId.toString()))
                .assertThat("$.fileServiceId", is(fileServiceId.toString()))
                .assertThat("$.unbundledDocument", is(true))
                .assertThat("$.fileUploadedEventMetadata.name", is(eventName))
                .assertThat("$.fileUploadedEventMetadata.correlation.client", is(clientId))
                .assertThat("$.fileUploadedEventMetadata.causation[0]", is(causation.toString()))
                .assertThat("$.fileUploadedEventMetadata.context.user", is(userId))
                .assertThat("$.fileUploadedEventMetadata.id", is(eventId))
        ;

        verify(logger).info(messageCaptor.capture(), argCaptor.capture(), argCaptor.capture());
        String expectedMessage = "Added Alfresco file upload to the jobstore: task '{}', materialId '{}'";
        assertEquals(expectedMessage, messageCaptor.getValue());
        assertEquals("material.upload-file-to-alfresco", argCaptor.getAllValues().get(0));
        assertEquals("d1319ff0-bf27-4814-ba55-831f65894190", argCaptor.getAllValues().get(1).toString());
    }

    @Test
    public void shouldHandleFileUploadedAsPdfEvent() throws Exception {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final String htmlFileName = "fileName.html";
        final String pdfFileName = "fileName.pdf";
        final String htmlMediaType = "html/text";
        final String pdfMediaType = "application/pdf";

        final String alfrescoId = RandomStringUtils.randomAlphanumeric(20);

        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUID("material.event.file-uploaded")
                        .withUserId(randomUUID().toString())
                        .withClientCorrelationId(randomUUID().toString())
                        .withCausation(randomUUID()))
                .withPayloadOf(materialId.toString(), MATERIAL_ID)
                .withPayloadOf(fileServiceId.toString(), FILE_SERVICE_ID)
                .build();

        final JsonObject fileMetadata = Json.createObjectBuilder()
                .add(FILE_NAME, htmlFileName)
                .add("mediaType", htmlMediaType)
                .build();

        final FileReference fileReference = new FileReference(fileServiceId, fileMetadata, inputStream);

        when(fileService.retrieve(fileServiceId)).thenReturn(of(fileReference));
        when(fileData.fileId()).thenReturn(alfrescoId);
        when(fileSender.send(htmlFileName, inputStream)).thenReturn(fileData);
        when(fileSender.send(pdfFileName, inputStream)).thenReturn(fileData);
        when(fileRequester.requestPdf(fileData.fileId(), htmlFileName)).thenReturn(of(inputStream));

        materialEventProcessor.handleFileUploadedAsPdf(event);

        verify(fileSender).send(htmlFileName, inputStream);
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata(),
                is(metadata()
                        .withName("material.add-material")));

        JsonObject jsonObject = (JsonObject) envelopeCaptor.getValue().payload();
        assertThat(jsonObject.getString(MATERIAL_ID), is(materialId.toString()));
        assertThat(jsonObject.getString(FILE_NAME), is(pdfFileName));
        assertThat(jsonObject.getJsonObject(DOCUMENT).getString(FILE_REFERENCE), is(alfrescoId));
        assertThat(jsonObject.getJsonObject(DOCUMENT).getString(MIME_TYPE), is(pdfMediaType));
    }

}