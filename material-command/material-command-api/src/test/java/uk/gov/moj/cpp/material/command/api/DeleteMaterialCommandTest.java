package uk.gov.moj.cpp.material.command.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import uk.gov.justice.services.messaging.JsonObjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteMaterialCommandTest {

    @Mock
    Sender sender;

    @InjectMocks
    MaterialCommandApi materialCommandApi;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;


    @Test
    public void shouldCallHandlerForDelete() {
        final String materialId = randomUUID().toString();

        materialCommandApi.deleteMaterial(deleteMaterialCommand(materialId));

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata(),
                is(metadata()
                        .withName("material.command.handler.delete-material")));
    }

    private JsonEnvelope deleteMaterialCommand(final String materialId) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithDefaults().withName("any-name"),
                JsonObjects.createObjectBuilder()
                        .add("materialId", materialId)
                        .build()
        );
    }
}
