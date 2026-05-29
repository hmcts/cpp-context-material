package uk.gov.moj.cpp.material.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import java.time.ZonedDateTime;

import javax.json.JsonObject;

import org.hamcrest.Matchers;
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
public class FormEventProcessorTest {

    private static final String userId = randomUUID().toString();
    private static final String CASE_ID = "caseId";
    private static final String COURT_FORM_ID = "courtFormId";
    private static final String FORM_ID = "formId";
    private static final String FORM_DATA = "formData";
    private static final String SAMPLE_DATA = "sample data";
    private static final String CPS_SAMPLE_DATA = "Cps sample data";
    private static final String USER_ID = "userId";
    private static final String MATERIAL_ID = "materialId";
    private static final String DOCUMENT_META_DATA = "documentMetaData";
    private static final String UPDATED_BY = "updatedBy";
    private static final String STRUCTURED_FORM_ID = "structuredFormId";
    private static final String STRUCTURED_FORM_DATA = "structuredFormData";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String CPS_NAME = "cps user full name";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String MATERIAL_COMMAND_CREATE_FORM = "material.command.handler.create-structured-form";
    private static final String HEARING_DATE_TIME = "hearingDateTime";


    @InjectMocks
    private FormEventProcessor formEventProcessor;

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
    public void shouldHandleFormCreated() {

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.form-created").withUserId(userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, randomUUID().toString())
                        .add(FORM_ID, randomUUID().toString())
                        .add(CASE_ID, randomUUID().toString())
                        .add(FORM_DATA, SAMPLE_DATA)
                        .add(UPDATED_BY, createObjectBuilder()
                                .add(ID, randomUUID().toString())
                                .add(FIRST_NAME, FIRST_NAME)
                                .add(LAST_NAME, LAST_NAME))
        );

        formEventProcessor.handleFormCreated(requestEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        final JsonEnvelope value = envelopeArgumentCaptor.getValue();
        assertFormCreatedFromEnvelope(value, false);

    }

    @Test
    public void shouldHandleCpsFormCreated() {

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.form-created").withUserId(userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, randomUUID().toString())
                        .add(FORM_ID, randomUUID().toString())
                        .add(CASE_ID, randomUUID().toString())
                        .add(FORM_DATA, CPS_SAMPLE_DATA)
                        .add(UPDATED_BY, createObjectBuilder()
                                .add(NAME, CPS_NAME)
                                .build())

        );

        formEventProcessor.handleFormCreated(requestEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        final JsonEnvelope value = envelopeArgumentCaptor.getValue();
        assertFormCreatedFromEnvelope(value, true);
    }

    @Test
    public void shouldHandleFormUpdated() {

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.form-updated").withUserId(userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, randomUUID().toString())
                        .add(CASE_ID, randomUUID().toString())
                        .add(FORM_DATA, SAMPLE_DATA)
                        .add(UPDATED_BY, createObjectBuilder()
                                .add(ID, randomUUID().toString())
                                .add(FIRST_NAME, FIRST_NAME)
                                .add(LAST_NAME, LAST_NAME))
        );

        formEventProcessor.handleFormUpdated(requestEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        final JsonEnvelope value = envelopeArgumentCaptor.getValue();

        assertThat(value.metadata().name(), is("material.command.handler.update-structured-form"));
    }


    @Test
    public void shouldHandleFormFinalised() {
        final ZonedDateTime hearingDateTime = ZonedDateTime.parse("2025-12-28T22:23:12.414Z");
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("public.progression.form-finalised").withUserId(userId),
                createObjectBuilder()
                        .add(COURT_FORM_ID, randomUUID().toString())
                        .add(USER_ID, randomUUID().toString())
                        .add(HEARING_DATE_TIME, hearingDateTime.toString())
                        .add(DOCUMENT_META_DATA, createArrayBuilder()
                                .add(createObjectBuilder().add(MATERIAL_ID, randomUUID().toString()))
                                .add(createObjectBuilder().add(MATERIAL_ID, randomUUID().toString()))
                                .add(createObjectBuilder().add(MATERIAL_ID, randomUUID().toString()))
                        )
                        .add(UPDATED_BY, createObjectBuilder()
                                .add(ID, randomUUID().toString())
                                .add(FIRST_NAME, FIRST_NAME)
                                .add(LAST_NAME, LAST_NAME)
                                .build())
        );

        formEventProcessor.handleFormFinalised(requestEnvelope);

        verify(sender, times(3)).send(envelopeArgumentCaptor.capture());
        final JsonEnvelope value = envelopeArgumentCaptor.getValue();

        assertThat(value.metadata().name(), is("material.command.handler.finalise-structured-form"));
    }

    private void assertFormCreatedFromEnvelope(final JsonEnvelope envelope, final boolean cpsFlag) {
        assertThat(envelope.metadata().name(), Matchers.is(MATERIAL_COMMAND_CREATE_FORM));
        final JsonObject payload = envelope.payloadAsJsonObject();
        assertThat(payload.getString(STRUCTURED_FORM_ID), is(notNullValue()));
        assertThat(payload.getString(FORM_ID), is(notNullValue()));
        assertThat(payload.getString(STRUCTURED_FORM_DATA), is(notNullValue()));
        if (cpsFlag) {
            assertThat(payload.getJsonObject(UPDATED_BY).getString(NAME), is(notNullValue()));

        } else {
            assertThat(payload.getJsonObject(UPDATED_BY).getString(FIRST_NAME), is(notNullValue()));
            assertThat(payload.getJsonObject(UPDATED_BY).getString(LAST_NAME), is(notNullValue()));
        }
    }

}
