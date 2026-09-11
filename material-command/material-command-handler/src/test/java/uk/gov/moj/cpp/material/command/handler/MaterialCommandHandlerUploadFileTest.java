package uk.gov.moj.cpp.material.command.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.material.domain.aggregate.Material;
import uk.gov.moj.cpp.material.domain.event.CloudBlobFileUploaded;
import uk.gov.moj.cpp.material.domain.event.FileUploaded;
import uk.gov.moj.cpp.material.domain.event.FileUploadedAsPdf;
import uk.gov.moj.cpp.material.domain.event.FileUploadedFromUri;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialCommandHandlerUploadFileTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private Material material;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(FileUploaded.class, FileUploadedAsPdf.class, FileUploadedFromUri.class);

    @InjectMocks
    private MaterialCommandHandler materialCommandHandler;

    @Test
    public void shouldHandleFileUpload() throws EventStreamException {

        final UUID materialId = UUID.randomUUID();
        final UUID fileServiceId = UUID.randomUUID();
        final boolean isUnbundledDocument = true;

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("material.command.upload-file"))
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileServiceId.toString(), "fileServiceId")
                .withPayloadOf(isUnbundledDocument, "isUnbundledDocument")
                .build();

        final FileUploaded fileUploaded = new FileUploaded(materialId, fileServiceId, isUnbundledDocument);
        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(material.uploadFile(materialId, fileServiceId, isUnbundledDocument)).thenReturn(Stream.of(fileUploaded));

        materialCommandHandler.uploadFile(command);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("material.events.file-uploaded"),
                        payload().isJson(allOf(
                                withJsonPath("$.materialId", equalTo(materialId.toString())),
                                withJsonPath("$.isUnbundledDocument", equalTo(isUnbundledDocument)),
                                withJsonPath("$.fileServiceId", equalTo(fileServiceId.toString()))
                        ))
                ))
        );
    }

    @Test
    public void shouldHandleFileUploadAsPdf() throws EventStreamException {

        final UUID materialId = UUID.randomUUID();
        final UUID fileServiceId = UUID.randomUUID();
        final boolean isUnbundledDocument = true;

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("material.command.upload-file-as-pdf"))
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileServiceId.toString(), "fileServiceId")
                .withPayloadOf(isUnbundledDocument, "isUnbundledDocument")
                .build();

        final FileUploadedAsPdf fileUploaded = new FileUploadedAsPdf(materialId, fileServiceId, isUnbundledDocument);
        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(material.uploadFileAsPdf(materialId, fileServiceId, isUnbundledDocument)).thenReturn(Stream.of(fileUploaded));

        materialCommandHandler.uploadFileAsPdf(command);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("material.events.file-uploaded-as-pdf"),
                        payload().isJson(allOf(
                                withJsonPath("$.materialId", equalTo(materialId.toString())),
                                withJsonPath("$.isUnbundledDocument", equalTo(isUnbundledDocument)),
                                withJsonPath("$.fileServiceId", equalTo(fileServiceId.toString()))
                        ))
                ))
        );
    }

    @Test
    void shouldHandleCloudBlobFileUpload() throws EventStreamException {

        final UUID materialId = UUID.randomUUID();
        final String fileCloudLocation = "2789/test_1.pdf";
        final boolean isUnbundledDocument = false;

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("material.command.upload-file"))
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileCloudLocation, "fileCloudLocation")
                .withPayloadOf(isUnbundledDocument, "isUnbundledDocument")
                .build();

        final CloudBlobFileUploaded blobFileUploaded = new CloudBlobFileUploaded(materialId, fileCloudLocation);
        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(material.uploadCloudBlobFile(materialId, fileCloudLocation)).thenReturn(Stream.of(blobFileUploaded));

        materialCommandHandler.uploadFile(command);
        verify(material).uploadCloudBlobFile(materialId,fileCloudLocation);


    }

    @Test
    void shouldHandleFileUploadFromUri() throws EventStreamException {

        final UUID materialId = UUID.randomUUID();
        final String fileUri = "https://sastagingdvlafilestore.blob.core.windows.net/producer-container/payload/2789.json";
        final boolean isUnbundledDocument = true;

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("material.command.upload-file"))
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileUri, "fileUri")
                .withPayloadOf(isUnbundledDocument, "isUnbundledDocument")
                .build();

        final FileUploadedFromUri fileUploadedFromUri = new FileUploadedFromUri(materialId, fileUri, isUnbundledDocument);
        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(material.uploadFileFromUri(materialId, fileUri, isUnbundledDocument)).thenReturn(Stream.of(fileUploadedFromUri));

        materialCommandHandler.uploadFile(command);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("material.events.file-uploaded-from-uri"),
                        payload().isJson(allOf(
                                withJsonPath("$.materialId", equalTo(materialId.toString())),
                                withJsonPath("$.fileUri", equalTo(fileUri)),
                                withJsonPath("$.isUnbundledDocument", equalTo(isUnbundledDocument))
                        ))
                ))
        );
    }

    @Test
    void shouldRejectUploadFileCommandCarryingMoreThanOneFileReference() throws EventStreamException {

        final UUID materialId = UUID.randomUUID();
        final UUID fileServiceId = UUID.randomUUID();
        final String fileUri = "https://sastagingdvlafilestore.blob.core.windows.net/producer-container/payload/2789.json";

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("material.command.upload-file"))
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileServiceId.toString(), "fileServiceId")
                .withPayloadOf(fileUri, "fileUri")
                .build();

        assertThrows(IllegalArgumentException.class, () -> materialCommandHandler.uploadFile(command));

        verifyNoInteractions(eventSource);
        verify(material, never()).uploadFile(any(), any(), any());
        verify(material, never()).uploadCloudBlobFile(any(), any());
        verify(material, never()).uploadFileFromUri(any(), any(), any());
    }

    @Test
    void shouldRejectUploadFileCommandCarryingFileServiceIdAndFileCloudLocation() throws EventStreamException {

        final UUID materialId = UUID.randomUUID();
        final UUID fileServiceId = UUID.randomUUID();
        final String fileCloudLocation = "2789/test_1.pdf";

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("material.command.upload-file"))
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileServiceId.toString(), "fileServiceId")
                .withPayloadOf(fileCloudLocation, "fileCloudLocation")
                .build();

        assertThrows(IllegalArgumentException.class, () -> materialCommandHandler.uploadFile(command));

        verifyNoInteractions(eventSource);
        verify(material, never()).uploadFile(any(), any(), any());
        verify(material, never()).uploadCloudBlobFile(any(), any());
        verify(material, never()).uploadFileFromUri(any(), any(), any());
    }

    @Test
    void shouldRejectUploadFileCommandCarryingFileCloudLocationAndFileUri() throws EventStreamException {

        final UUID materialId = UUID.randomUUID();
        final String fileCloudLocation = "2789/test_1.pdf";
        final String fileUri = "https://sastagingdvlafilestore.blob.core.windows.net/producer-container/payload/2789.json";

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("material.command.upload-file"))
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileCloudLocation, "fileCloudLocation")
                .withPayloadOf(fileUri, "fileUri")
                .build();

        assertThrows(IllegalArgumentException.class, () -> materialCommandHandler.uploadFile(command));

        verifyNoInteractions(eventSource);
        verify(material, never()).uploadFile(any(), any(), any());
        verify(material, never()).uploadCloudBlobFile(any(), any());
        verify(material, never()).uploadFileFromUri(any(), any(), any());
    }

    @Test
    void shouldRejectUploadFileCommandCarryingAllThreeFileReferences() throws EventStreamException {

        final UUID materialId = UUID.randomUUID();
        final UUID fileServiceId = UUID.randomUUID();
        final String fileCloudLocation = "2789/test_1.pdf";
        final String fileUri = "https://sastagingdvlafilestore.blob.core.windows.net/producer-container/payload/2789.json";

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("material.command.upload-file"))
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileServiceId.toString(), "fileServiceId")
                .withPayloadOf(fileCloudLocation, "fileCloudLocation")
                .withPayloadOf(fileUri, "fileUri")
                .build();

        assertThrows(IllegalArgumentException.class, () -> materialCommandHandler.uploadFile(command));

        verifyNoInteractions(eventSource);
        verify(material, never()).uploadFile(any(), any(), any());
        verify(material, never()).uploadCloudBlobFile(any(), any());
        verify(material, never()).uploadFileFromUri(any(), any(), any());
    }

    @Test
    void shouldDefaultIsUnbundledDocumentToFalseWhenFileUploadFromUriOmitsIt() throws EventStreamException {

        final UUID materialId = UUID.randomUUID();
        final String fileUri = "https://sastagingdvlafilestore.blob.core.windows.net/producer-container/payload/2789.json";

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("material.command.upload-file"))
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileUri, "fileUri")
                .build();

        final FileUploadedFromUri fileUploadedFromUri = new FileUploadedFromUri(materialId, fileUri, false);
        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(material.uploadFileFromUri(materialId, fileUri, false)).thenReturn(Stream.of(fileUploadedFromUri));

        materialCommandHandler.uploadFile(command);

        verify(material).uploadFileFromUri(materialId, fileUri, false);
    }

    @Test
    void shouldTreatEmptyFileUriAsPresentAndDispatchUpload() throws EventStreamException {

        final UUID materialId = UUID.randomUUID();
        final String fileUri = "";
        final boolean isUnbundledDocument = false;

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("material.command.upload-file"))
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileUri, "fileUri")
                .withPayloadOf(isUnbundledDocument, "isUnbundledDocument")
                .build();

        final FileUploadedFromUri fileUploadedFromUri = new FileUploadedFromUri(materialId, fileUri, isUnbundledDocument);
        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        when(material.uploadFileFromUri(materialId, fileUri, isUnbundledDocument)).thenReturn(Stream.of(fileUploadedFromUri));

        materialCommandHandler.uploadFile(command);

        verify(material).uploadFileFromUri(materialId, fileUri, isUnbundledDocument);
        verify(eventStream).append(any());
    }

    @Test
    void shouldNotAppendAnyEventsWhenUploadFileCommandHasNoFileReference() throws EventStreamException {

        final UUID materialId = UUID.randomUUID();

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("material.command.upload-file"))
                .withPayloadOf(materialId.toString(), "materialId")
                .build();

        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);

        materialCommandHandler.uploadFile(command);

        verify(eventStream, never()).append(any());
        verify(material, never()).uploadFile(any(), any(), any());
        verify(material, never()).uploadCloudBlobFile(any(), any());
        verify(material, never()).uploadFileFromUri(any(), any(), any());
    }

}
