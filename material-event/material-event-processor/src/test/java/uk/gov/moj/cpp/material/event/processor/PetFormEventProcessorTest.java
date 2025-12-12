package uk.gov.moj.cpp.material.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.material.event.processor.PetFormEventProcessor.DEFENDANT_DATA;
import static uk.gov.moj.cpp.material.event.processor.PetFormEventProcessor.DEFENDANT_ID;
import static uk.gov.moj.cpp.material.event.processor.PetFormEventProcessor.FORM_ID;
import static uk.gov.moj.cpp.material.event.processor.PetFormEventProcessor.MATERIAL_ID;
import static uk.gov.moj.cpp.material.event.processor.PetFormEventProcessor.PET_FORM_DATA;
import static uk.gov.moj.cpp.material.event.processor.PetFormEventProcessor.PET_ID;
import static uk.gov.moj.cpp.material.event.processor.PetFormEventProcessor.STATUS;
import static uk.gov.moj.cpp.material.event.processor.PetFormEventProcessor.USER_ID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.domain.StructuredFormStatus;

import java.util.UUID;

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
public class PetFormEventProcessorTest {

    private static final String userId = UUID.randomUUID().toString();
    private static final String UPDATED_BY = "updatedBy";

    @InjectMocks
    PetFormEventProcessor petEventProcessor;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandlePetFormCreated() {

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.structured-form-created").withUserId(userId),
                createObjectBuilder()
                        .add(PET_ID, randomUUID().toString())
                        .add(FORM_ID, randomUUID().toString())
                        .add(PET_FORM_DATA, "sample data")
                        .add(STATUS, StructuredFormStatus.CREATED.name())
                        .add(UPDATED_BY, createObjectBuilder()
                                .add("name", "cps user full name")
                                .build())
        );

        petEventProcessor.handlePetFormCreated(requestEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        final JsonEnvelope value = envelopeArgumentCaptor.getValue();

        assertThat(value.metadata().name(), is("material.command.handler.create-structured-form"));
    }

    @Test
    public void shouldHandlePetFormUpdated() {

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.structured-form-updated").withUserId(userId),
                createObjectBuilder().add(PET_ID, randomUUID().toString())
                        .add(PET_FORM_DATA, "sample data")
                        .add(UPDATED_BY, createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("firstName", "firstName")
                                .add("lastName", "lastName")
                                .build())
        );

        petEventProcessor.handlePetFormUpdated(requestEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        final JsonEnvelope value = envelopeArgumentCaptor.getValue();

        assertThat(value.metadata().name(), is("material.command.handler.update-structured-form"));
    }

    @Test
    public void shouldHandlePetFormDefendantUpdated() {

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.structured-form-defendant-updated").withUserId(userId),
                createObjectBuilder().add(PET_ID, randomUUID().toString())
                        .add(DEFENDANT_ID, randomUUID().toString())
                        .add(DEFENDANT_DATA, "sample data")
                        .add(UPDATED_BY, createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("firstName", "firstName")
                                .add("lastName", "lastName")
                                .build())
        );

        petEventProcessor.handlePetFormDefendantUpdated(requestEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        final JsonEnvelope value = envelopeArgumentCaptor.getValue();

        assertThat(value.metadata().name(), is("material.command.handler.update-structured-form-for-defendant"));
    }

    @Test
    public void shouldHandlePetFormFinalised() {

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.pet-form-finalised").withUserId(userId),
                createObjectBuilder()
                        .add(PET_ID, randomUUID().toString())
                        .add(USER_ID, randomUUID().toString())
                        .add(MATERIAL_ID, randomUUID().toString())
                        .add(UPDATED_BY, createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("firstName", "firstName")
                                .add("lastName", "lastName")
                                .build())

        );

        petEventProcessor.handlePetFormFinalised(requestEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        final JsonEnvelope value = envelopeArgumentCaptor.getValue();

        assertThat(value.metadata().name(), is("material.command.handler.finalise-structured-form"));
    }
}
