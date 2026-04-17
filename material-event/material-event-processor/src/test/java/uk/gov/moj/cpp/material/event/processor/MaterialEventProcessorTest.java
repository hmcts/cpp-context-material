package uk.gov.moj.cpp.material.event.processor;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.material.event.processor.MaterialEventProcessor.MIME_TYPE;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
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
import uk.gov.moj.cpp.material.domain.FileDetails;
import uk.gov.moj.cpp.material.domain.event.MaterialAdded;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleDetailsRecorded;
import uk.gov.moj.cpp.material.domain.event.MaterialBundlingFailed;
import uk.gov.moj.cpp.material.domain.event.MaterialZipFailed;
import uk.gov.moj.cpp.material.domain.event.MaterialZipped;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class MaterialEventProcessorTest {

    @InjectMocks
    private MaterialEventProcessor materialEventProcessor;
    @Mock
    private Enveloper enveloper;
    @Mock
    private Sender sender;

    @Mock
    private FileService fileService;

    @Mock
    private JsonEnvelope envelope;
    @Mock
    private JsonObject payload;

    @Mock
    private FileData fileData;

    @Mock
    private FileSender fileSender;

    @Mock
    private FileRequester fileRequester;

    @Mock
    private InputStream inputStream;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonEnvelope finalEnvelope;
    @Mock
    private MaterialAdded materialAddedEvent;
    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Mock
    private Envelope<MaterialBundleDetailsRecorded> materialBundleDetailsRecordedEnvelope;

    @Mock
    private Envelope<MaterialBundlingFailed> materialBundleCreationFailedEnvelope;

    @Mock
    private Envelope<MaterialZipped> materialZipRequestedEnvelope;

    @Mock
    private Envelope<MaterialZipFailed> materialZipFailedEnvelope;

    @Mock
    private Logger logger;


    @Test
    public void shouldHandleMaterialAddedEventMessage() throws Exception {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, MaterialAdded.class)).thenReturn(materialAddedEvent);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());

        materialEventProcessor.handleMaterialAdded(envelope);
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata(),
                is(metadata()
                        .withName("material.material-added")));
    }

    @Test
    public void shouldHandleDuplicateMaterialNotCreated() throws Exception {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());

        materialEventProcessor.handleDuplicateMaterialNotCreated(envelope);
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata(),
                is(metadata()
                        .withName("material.duplicate-material-not-created")));
    }

    @Test
    public void shouldHandleMaterialBundleDetailsRecordedEventMessage() {
        final MaterialBundleDetailsRecorded materialBundleDetailsRecorded = new MaterialBundleDetailsRecorded(randomUUID(), new FileDetails(randomUUID().toString(), MIME_TYPE, "fileName"), "1", 1, new UtcClock().now());

        when(materialBundleDetailsRecordedEnvelope.payload()).thenReturn(materialBundleDetailsRecorded);
        when(materialBundleDetailsRecordedEnvelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());

        materialEventProcessor.handleMaterialBundleDetailsRecorded(materialBundleDetailsRecordedEnvelope);
        verify(sender, times(2)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getAllValues().get(0).metadata(), is(metadata().withName("material.add-material")));
        assertThat(envelopeCaptor.getAllValues().get(1).metadata(), is(metadata().withName("public.material.material-bundle-created")));
    }

    @Test
    public void shouldHandleMaterialBundleCreationFailedEventMessage() {

        final MaterialBundlingFailed materialBundlingFailed = new MaterialBundlingFailed(randomUUID(), Arrays.asList(UUID.randomUUID(), UUID.randomUUID()),
                Optional.of(UUID.randomUUID()), "UPLOAD_ERROR", "Alfresco down", new UtcClock().now());

        when(materialBundleCreationFailedEnvelope.payload()).thenReturn(materialBundlingFailed);
        when(materialBundleCreationFailedEnvelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());

        materialEventProcessor.handleMaterialBundleCreationFailed(materialBundleCreationFailedEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata(), is(metadata().withName("public.material.material-bundle-creation-failed")));
    }

    @Test
    public void shouldHandleMaterialZipRequestedEventMessage() throws IOException {
        final MaterialZipped materialZipped = new MaterialZipped(UUID.randomUUID(), "UB134343", Arrays.asList(randomUUID(), randomUUID()), emptyList());
        when(materialZipRequestedEnvelope.payload()).thenReturn(materialZipped);
        when(materialZipRequestedEnvelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());
        materialEventProcessor.handleMaterialZipped(materialZipRequestedEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata(), is(metadata().withName("public.material.events.material-zipped")));
    }

    @Test
    public void shouldHandleMaterialZipFailedEventMessage() throws IOException {
        final MaterialZipFailed materialZipFailed = new MaterialZipFailed(UUID.randomUUID(), Arrays.asList(randomUUID(), randomUUID()), emptyList(), "error message");
        when(materialZipFailedEnvelope.payload()).thenReturn(materialZipFailed);
        when(materialZipFailedEnvelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());
        materialEventProcessor.handleMaterialZipFailed(materialZipFailedEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata(), is(metadata().withName("public.material.events.material-zip-failed")));
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
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileServiceId.toString(), "fileServiceId")
                .build();

        final JsonObject fileMetadata = JsonObjects.createObjectBuilder()
                .add("fileName", htmlFileName)
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
        assertThat(jsonObject.getString("materialId"), is(materialId.toString()));
        assertThat(jsonObject.getString("fileName"), is(pdfFileName));
        assertThat(jsonObject.getJsonObject("document").getString("fileReference"), is(alfrescoId));
        assertThat(jsonObject.getJsonObject("document").getString("mimeType"), is(pdfMediaType));

        verify(inputStream).close();
    }
}