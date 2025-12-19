package uk.gov.moj.cpp.material.command.controller;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialCommandControllerTest {

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private MaterialCommandController materialCommandController;

    @Test
    public void shouldSendAddMaterial() throws Exception {
        // given
        JsonEnvelope command = envelopeFrom(
                metadataOf(randomUUID(), "material.command.add-material"),
                createObjectBuilder()
                        .add("document", createObjectBuilder()
                                .add("content", RandomGenerator.STRING.next()))
                        .build());
        // when
        materialCommandController.addMaterial(command);

        // then
        verify(sender).send(command);
    }

    @Test
    public void shouldSendAddExternalMaterial() throws Exception {
        // given
        String name = "material.command.add-external-material";
        JsonEnvelope command = envelopeFrom(
                metadataOf(randomUUID(), name),
                createObjectBuilder().build());
        // and
        ArgumentCaptor<DefaultEnvelope> argumentCaptor = ArgumentCaptor.forClass(DefaultEnvelope.class);

        // when
        materialCommandController.addMaterial(command);

        // then
        verify(sender).send(argumentCaptor.capture());
        // and
        var envelope = argumentCaptor.getValue();
        // and
        assertThat(envelope.metadata().name(), is(name));
    }

    @Test
    public void shouldHandleCommands() {
        assertThat(MaterialCommandController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("addMaterial")
                        .thatHandles("material.command.add-material"))
                .with(method("uploadFile")
                        .thatHandles("material.command.upload-file")
                        .withSenderPassThrough())
                .with(method("uploadFileAsPdf")
                        .thatHandles("material.command.upload-file-as-pdf")
                        .withSenderPassThrough())
                .with(method("deleteMaterial")
                        .thatHandles("material.command.handler.delete-file")
                        .withSenderPassThrough())
                .with(method("addMaterialReference")
                        .thatHandles("material.add-material")
                        .withSenderPassThrough())
        );
    }

}
