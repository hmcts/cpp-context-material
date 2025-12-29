package uk.gov.moj.cpp.material.command.handler;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.material.command.handler.alfresco.AlfrescoUploadService;
import uk.gov.moj.cpp.material.command.handler.azure.service.AzureArchiveBlobClientService;
import uk.gov.moj.cpp.material.domain.FileDetails;
import uk.gov.moj.cpp.material.domain.UploadedMaterial;
import uk.gov.moj.cpp.material.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.material.domain.aggregate.Material;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleDetailsRecorded;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialCommandHandlerTest {

    private static final String EVENT_NAME = "material.add-material";
    private static final String ZIP_MATERIAL_EVENT_NAME = "material.zip-material";
    private static final UUID MATERIAL_ID = randomUUID();
    private static final UUID FILE_SERVICE__ID = randomUUID();
    private static final String ERROR_MESSAGE = "Error";
    private static final String FILENAME = "file name";
    private static final String DOCUMENT_DATA = "Document";
    private static final String EXTERNAL_LINK = "http://go.somewhere";
    private static final ZonedDateTime DATE_TIME = ZonedDateTime.of(2012, 10, 5, 11, 30, 0, 0, ZoneId.systemDefault());
    public static final String FILE_SERVICE_ID = "fileServiceId";
    public static final String FILE_REFERENCE = "fileReference";
    public static final String FILE_NAME = "fileName";
    public static final String DOCUMENT = "document";
    public static final String MATERIAL_ID_STRING = "materialId";
    public static final String BUNDLED_MATERIAL_ID_STRING = "bundledMaterialId";
    public static final String BUNDLED_MATERIAL_NAME_STRING = "bundledMaterialName";
    public static final String MATERIAL_IDS_STRING = "materialIds";
    public static final String MIME_TYPE = "mimeType";

    @Mock
    private Stream<Object> newEvents;
    @Mock
    private EventStream eventStream;
    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private Stream<Object> mappedNewEvents;
    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptorStream;

    private JsonEnvelope command;

    @Mock
    private AlfrescoUploadService alfrescoUploadService;

    @Mock
    private AzureArchiveBlobClientService azureArchiveBlobClientService;

    @Mock
    private Material material;

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private UtcClock utcClock;

    @InjectMocks
    private MaterialCommandHandler materialCommandHandler;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    private final static UploadedMaterial uploadedMaterial = new UploadedMaterial("externalId", "mimeType");
    private static final List<UUID> MATERIAL_IDS = Arrays.asList(randomUUID(), randomUUID());
    private static final List<UUID> FILE_IDS = Arrays.asList(randomUUID(), randomUUID());
    private static final UUID BUNDLED_MATERIAL_ID = randomUUID();
    private static final String FILE_SIZE = "1";
    private static final int PAGE_COUNT = 1;
    private static final String BUNDLED_MATERIAL_NAME = "Barkingside Magistrates' Court 17072021.pdf";
    private final static MaterialBundleDetailsRecorded materialBundleDetailsRecorded = new MaterialBundleDetailsRecorded(BUNDLED_MATERIAL_ID, new FileDetails(randomUUID().toString(), "application/pdf", BUNDLED_MATERIAL_NAME), FILE_SIZE, PAGE_COUNT, new UtcClock().now());
    
    @Test
    public void shouldHandleTheAddFileReferenceCommand() throws Exception {

        final String fileReference = randomUUID().toString();
        final String mimeType = "mime/type";
        final String fileName = "Boys Bumper Book of Facts";
        final FileDetails fileDetails = new FileDetails(fileReference, mimeType, fileName);

        final JsonEnvelope materialReferenceCommand = envelopeFrom(
                metadataOf(randomUUID().toString(), EVENT_NAME),
                createObjectBuilder()
                        .add(MATERIAL_ID_STRING, MATERIAL_ID.toString())
                        .add(FILE_NAME, fileName)
                        .add(DOCUMENT, createObjectBuilder()
                                .add(FILE_REFERENCE, fileReference)
                                .add(MIME_TYPE, mimeType))
                        .build());

        when(utcClock.now()).thenReturn(DATE_TIME);
        when(eventSource.getStreamById(MATERIAL_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);
        when(material.addFileReference(MATERIAL_ID, fileDetails, DATE_TIME, false)).thenReturn(newEvents);
        materialCommandHandler.addMaterialReference(materialReferenceCommand);
        checkEventsAppendedAreMappedNewEvents();
    }

    @Test
    public void shouldHandleAddMaterialCommand_ThatContainsDocumentContentToUpload() throws Exception {
        command = buildCommand(withDocumentToUpload());
        when(alfrescoUploadService.uploadFile(any())).thenReturn(uploadedMaterial);
        when(utcClock.now()).thenReturn(DATE_TIME);
        when(eventSource.getStreamById(MATERIAL_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);
        when(material.addUploadedFile(MATERIAL_ID, FILENAME, uploadedMaterial, DATE_TIME)).thenReturn(newEvents);
        materialCommandHandler.addMaterial(command);
        checkEventsAppendedAreMappedNewEvents();
    }

    @Test
    public void shouldHandleAddMaterialCommand_ThatContainsDocumentExternalLink() throws Exception {
        command = buildCommand(withDocumentWithExternalLink());
        when(utcClock.now()).thenReturn(DATE_TIME);
        when(eventSource.getStreamById(MATERIAL_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);
        when(material.addExternalMaterial(MATERIAL_ID, FILENAME, EXTERNAL_LINK, DATE_TIME)).thenReturn(newEvents);
        materialCommandHandler.addExternalMaterial(command);
        checkEventsAppendedAreMappedNewEvents();
    }

    @Test
    public void shouldHandleCreateMaterialBundleCommand() throws Exception {

        when(eventSource.getStreamById(BUNDLED_MATERIAL_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);
        when(material.createMaterialBundle(BUNDLED_MATERIAL_ID, MATERIAL_IDS, BUNDLED_MATERIAL_NAME)).thenReturn(newEvents);
        materialCommandHandler.createMaterialBundle(createMaterialBundleCommand(BUNDLED_MATERIAL_ID, MATERIAL_IDS));
        checkEventsAppendedAreMappedNewEvents();
    }

    @Test
    public void shouldHandleRecordMaterialBundleFailedCommand() throws Exception {
        List<UUID> materialIds = Arrays.asList(randomUUID(), randomUUID());
        when(eventSource.getStreamById(BUNDLED_MATERIAL_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);
        when(material.recordBundleDetailsFailure(eq(BUNDLED_MATERIAL_ID), eq(materialIds), any(), anyString(), anyString(), any(ZonedDateTime.class))).thenReturn(newEvents);

        materialCommandHandler.recordBundleDetailsFailure(getRecordMaterialBundleFailedCommand(BUNDLED_MATERIAL_ID, materialIds));
        checkEventsAppendedAreMappedNewEvents();
    }

    @Test
    public void shouldRecordAddMaterialFailed() throws Exception {
        final JsonEnvelope recordAddMaterialFailedCommand = envelopeFrom(
                metadataOf(randomUUID().toString(), "material.record-add-material-failed"),
                createObjectBuilder()
                        .add(MATERIAL_ID_STRING, MATERIAL_ID.toString())
                        .add(FILE_SERVICE_ID, FILE_SERVICE__ID.toString())
                        .add("errorMessage", ERROR_MESSAGE)
                        .add("failedTime", DATE_TIME.toString()));

        when(eventSource.getStreamById(MATERIAL_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);
        when(material.recordAddMaterialFailed(eq(MATERIAL_ID), eq(FILE_SERVICE__ID), any(ZonedDateTime.class), eq(ERROR_MESSAGE))).thenReturn(newEvents);
        materialCommandHandler.recordAddMaterialFailed(recordAddMaterialFailedCommand);
        checkEventsAppendedAreMappedNewEvents();
    }

    @Test
    public void shouldHandleZipMaterialCommand() throws Exception {
        when(eventSource.getStreamById(BUNDLED_MATERIAL_ID)).thenReturn(eventStream);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.materialZipped(any(), any(), any(), any())).thenReturn(newEvents);
        materialCommandHandler.createMaterialZip(createZipMaterialCommand(BUNDLED_MATERIAL_ID, MATERIAL_IDS, FILE_IDS));
        checkEventsAppendedAreMappedNewEvents();
    }

    private static JsonEnvelope buildCommand(JsonObject document) {
        final JsonObject payloadAsJsonObject = createObjectBuilder()
                .add(MATERIAL_ID_STRING, MATERIAL_ID.toString())
                .add(FILE_NAME, FILENAME)
                .add(DOCUMENT, document)
                .build();

        return envelopeFrom(metadataOf(randomUUID().toString(), EVENT_NAME), payloadAsJsonObject);
    }

    private JsonObject withDocumentWithExternalLink() {
        return createObjectBuilder().add("externalLink", EXTERNAL_LINK).build();
    }


    private static JsonObject withDocumentToUpload() {
        return createObjectBuilder().add("content", DOCUMENT_DATA).build();
    }

    private JsonEnvelope getRecordMaterialBundleCommand() {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithDefaults().withName(EVENT_NAME),
                JsonObjects.createObjectBuilder()
                        .add(BUNDLED_MATERIAL_ID_STRING, BUNDLED_MATERIAL_ID.toString())
                        .add(FILE_REFERENCE, materialBundleDetailsRecorded.getFileDetails().getAlfrescoAssetId())
                        .add(MIME_TYPE, materialBundleDetailsRecorded.getFileDetails().getMimeType())
                        .add(FILE_NAME, BUNDLED_MATERIAL_NAME)
                        .add(FILE_SIZE, FILE_SIZE)
                        .add("pageCount", PAGE_COUNT)
                        .build()

        );
    }

    private JsonEnvelope getRecordMaterialBundleFailedCommand(final UUID bundledMaterialId, final List<UUID> materialIds) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithDefaults().withName(EVENT_NAME),
                JsonObjects.createObjectBuilder()
                        .add(BUNDLED_MATERIAL_ID_STRING, bundledMaterialId.toString())
                        .add(MATERIAL_IDS_STRING,
                                JsonObjects.createArrayBuilder()
                                        .add(materialIds.get(0).toString())
                                        .add(materialIds.get(1).toString()).build())
                        .add("errorCode", "errorCode")
                        .add("errorMessage", "errorMessage")
                        .add("failedTime", DATE_TIME.toString())
                        .build()

        );
    }

    private JsonEnvelope createMaterialBundleCommand(final UUID bundledMaterialId, final List<UUID> materialIds) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithDefaults().withName(EVENT_NAME),
                JsonObjects.createObjectBuilder()
                        .add(BUNDLED_MATERIAL_ID_STRING, bundledMaterialId.toString())
                        .add(FILE_REFERENCE, randomUUID().toString())
                        .add(BUNDLED_MATERIAL_NAME_STRING, BUNDLED_MATERIAL_NAME)
                        .add(MATERIAL_IDS_STRING,
                                JsonObjects.createArrayBuilder()
                                        .add(materialIds.get(0).toString())
                                        .add(materialIds.get(1).toString()).build())
                        .build()

        );
    }
    private Envelope<ZipMaterial> createZipMaterialCommand(final UUID caseId, final List<UUID> materialIds, final List<UUID> fileIds) {
        return Envelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithDefaults().withName(ZIP_MATERIAL_EVENT_NAME),
                getZipMaterialCommandPayload(caseId, materialIds, fileIds)
        );
    }

    private ZipMaterial getZipMaterialCommandPayload(final UUID caseId, final List<UUID> materialIds, final List<UUID> fileIds) {
        return ZipMaterial.zipMaterial()
                .withCaseId(caseId)
                .withCaseURN("test")
                .withFileIds(fileIds)
                .withMaterialIds(materialIds)
                .build();
    }

    private void checkEventsAppendedAreMappedNewEvents() throws EventStreamException {
        verify(eventStream).append(argumentCaptorStream.capture());
        final Stream<JsonEnvelope> stream = argumentCaptorStream.getValue();
        MatcherAssert.assertThat(stream, Is.is(mappedNewEvents));
    }
}
