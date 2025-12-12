package uk.gov.moj.cpp.material.query.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.material.query.view.StructuredFormChangeHistoryQueryView;
import uk.gov.moj.cpp.material.query.view.StructuredFormQueryView;

import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StructuredFormQueryApiTest {

    @Mock
    private StructuredFormQueryView structuredFormQueryView;

    @Mock
    private StructuredFormChangeHistoryQueryView structuredFormChangeHistoryQueryView;

    @Mock
    private Requester requester;

    @InjectMocks
    private StructuredFormQueryApi structuredFormQueryApi;

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @BeforeEach
    public void setup() {
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldReturnStructuredFormById() {

        final String structuredFormId = randomUUID().toString();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("material.query.structured-form"), createObjectBuilder().build());

        when(structuredFormQueryView.getStructuredForm(any())).thenReturn(envelope);

        final JsonEnvelope query = EnvelopeFactory.createEnvelope("material.query.structured-form", createObjectBuilder()
                .add("structuredFormId", structuredFormId)
                .build());

        final JsonEnvelope result = structuredFormQueryApi.getStructuredForm(query);
        assertThat(result, is(envelope));
    }

    @Test
    public void shouldReturnStructuredFormChangeHistory() {
        final UUID uuid = randomUUID();
        final String structuredFormId = randomUUID().toString();
        final String firstName = "firstName";
        final String lastName = "Chapman";
        final JsonArrayBuilder defentants = createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("id", "8f8fe782-a287-11eb-bcbc-0242ac135302")
                                .add("updatedBy", createObjectBuilder()
                                                .add("id", "8f8fe782-a287-11eb-bcbc-0242ac135303")
                                                .add("firstName", firstName)
                                                .add("lastName", lastName)
                                                .build()
                                )
                );

        final JsonObject payload = createObjectBuilder()
                .add("structuredFormChangeHistory", defentants)
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataBuilder().withId(uuid)
                .withName("material.query.structured-form-change-history"), payload);

        when(structuredFormChangeHistoryQueryView.getStructuredFormChangeHistory(any())).thenReturn(envelope);

        final JsonObject jsonObjectPayload = createObjectBuilder()
                .add("userId", "1e2f843e-d639-40b3-8611-8015f3a18958")
                .add("firstName", firstName)
                .add("lastName", lastName)
                .build();

        final JsonEnvelope query = EnvelopeFactory.createEnvelope("material.query.structured-form-change-history", createObjectBuilder()
                .add("structuredFormId", structuredFormId)
                .build());

        final JsonEnvelope result = structuredFormQueryApi.getStructuredFormChangeHistory(query);
        final JsonArray structuredFormChangeHistoryArray = result.payloadAsJsonObject().getJsonArray("structuredFormChangeHistory");
        final JsonObject structuredFormChangeHistory1 = structuredFormChangeHistoryArray.getJsonObject(0);

        final JsonArray structuredFormChangeHistoryArrayMatcher = envelope.payloadAsJsonObject().getJsonArray("structuredFormChangeHistory");
        final JsonObject structuredFormChangeHistory1Matcher = structuredFormChangeHistoryArrayMatcher.getJsonObject(0);

        assertThat(structuredFormChangeHistory1.getString("id"), is(structuredFormChangeHistory1Matcher.getString("id")));
        assertThat(structuredFormChangeHistory1.getJsonObject("updatedBy"), is(notNullValue()));
        assertThat(structuredFormChangeHistory1.getJsonObject("updatedBy").getString("firstName"), is(firstName));
        assertThat(structuredFormChangeHistory1.getJsonObject("updatedBy").getString("lastName"), is(lastName));
    }

    @Test
    public void shouldReturnStructuredFormDefendantUser() {

        final String structuredFormId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("material.query.structured-form-defendant-user"), createObjectBuilder().build());

        when(structuredFormQueryView.getStructuredFormDefendantUser(any())).thenReturn(envelope);

        final JsonEnvelope query = EnvelopeFactory.createEnvelope("material.query.structured-form-defendant-user", createObjectBuilder()
                .add("structuredFormId", structuredFormId)
                .add("defendantId", defendantId)
                .build());

        final JsonEnvelope result = structuredFormQueryApi.getStructuredFormDefendantUser(query);
        assertThat(result, is(envelope));
    }
}
