package uk.gov.moj.cpp.material.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.material.domain.FileDetails;
import uk.gov.moj.cpp.material.domain.aggregate.Material;
import uk.gov.moj.cpp.material.domain.event.MaterialDeleted;
import uk.gov.moj.cpp.material.domain.event.MaterialNotFound;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteMaterialCommandHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    MaterialCommandHandler materialCommandHandler;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> envelopeCaptor;

    @BeforeEach
    public void setup() throws Exception {
        createEnveloperWithEvents(MaterialDeleted.class, MaterialNotFound.class);
    }

    @Test
    public void shouldDeleteMaterial_AndRaiseEvent() throws EventStreamException {
        //given
        final UUID materialId = randomUUID();
        final UUID alfrescoId = randomUUID();
        final Material material = spy(Material.class);
        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        //ensure the material has been created first
        material.addFileReference(materialId, new FileDetails(alfrescoId.toString(), "mime", "test.pdf"), now(), false);
        //when
        final JsonEnvelope command = deleteMaterialCommand(materialId.toString());
        materialCommandHandler.deleteMaterial(command);
        //then
        verify(eventStream).append(envelopeCaptor.capture());
        // assertThat(envelopeCaptor.getValue(), streamContaining(is(new MaterialDeleted(materialId, alfrescoId.toString()))));
        assertThat(envelopeCaptor.getValue(), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(command)
                                .withName("material.events.material-deleted"),
                        payloadIsJson(allOf(
                                withJsonPath("$.materialId", equalTo(materialId.toString())),
                                withJsonPath("$.alfrescoId", equalTo(alfrescoId.toString()))
                        )))
        ));
    }

    @Test
    public void shouldRaiseMaterialNotFound() throws EventStreamException {
        //given
        final UUID materialId = randomUUID();
        final UUID alfrescoId = randomUUID();
        final Material material = spy(Material.class);
        when(eventSource.getStreamById(materialId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, Material.class)).thenReturn(material);
        //when
        final JsonEnvelope command = deleteMaterialCommand(materialId.toString());
        materialCommandHandler.deleteMaterial(command);
        //then
        verify(eventStream).append(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue(), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(command)
                                .withName("material.events.material-not-found"),
                        payloadIsJson(
                                allOf(
                                        withJsonPath("$.materialId", equalTo(materialId.toString()))
                                )))
        ));
    }

    private JsonEnvelope deleteMaterialCommand(final String materialId) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithDefaults().withName("material.command.handler.delete-material"),
                Json.createObjectBuilder()
                        .add("materialId", materialId)
                        .build()
        );
    }
}
