package uk.gov.moj.cpp.material.command.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.verify;
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
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(FileUploaded.class, FileUploadedAsPdf.class);

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

}
