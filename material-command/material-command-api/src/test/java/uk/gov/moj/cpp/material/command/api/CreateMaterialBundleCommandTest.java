package uk.gov.moj.cpp.material.command.api;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import uk.gov.justice.services.messaging.JsonObjects;
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

@ExtendWith(MockitoExtension.class)
public class CreateMaterialBundleCommandTest {

    final String materialId1 = randomUUID().toString();
    final String materialId2 = randomUUID().toString();

    final String fileId1 = randomUUID().toString();

    final String fileId2 = randomUUID().toString();
    @Mock
    Sender sender;

    @InjectMocks
    MaterialCommandApi materialCommandApi;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Spy
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @BeforeEach
    public void init() {
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
        setField(this.listToJsonArrayConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldCallHandlerForCreateMaterialBundle() {


        materialCommandApi.createMaterialBundle(createMaterialBundleCommand());

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> jsonObjectEnvelope = envelopeCaptor.getValue();

        assertThat(jsonObjectEnvelope.metadata(),
                is(metadata()
                        .withName("material.command.handler.create-material-bundle")));
    }

    @Test
    public void shouldCallHandlerForZipMaterial() {
        materialCommandApi.zipMaterial(createZipMaterialCommand());
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> jsonObjectEnvelope = envelopeCaptor.getValue();

        assertThat(jsonObjectEnvelope.metadata(),
                is(metadata()
                        .withName("material.command.handler.zip-material")));
    }

    private JsonEnvelope createMaterialBundleCommand() {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithDefaults().withName("any-name"),
                JsonObjects.createObjectBuilder()
                        .add("materialIds", createArrayBuilder().add(materialId1)
                                .add(materialId2)
                                .build())

        );
    }

    private JsonEnvelope createZipMaterialCommand() {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithDefaults().withName("any-name"),
                JsonObjects.createObjectBuilder()
                        .add("materialIds", createArrayBuilder().add(materialId1)
                                .add(materialId2)
                                .build())
                        .add("fileIds", createArrayBuilder().add(fileId1)
                                .add(fileId2)
                                .build())
                        .build()

        );
    }
}
