package uk.gov.moj.cpp.material.query.view;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.material.query.view.StructuredFormQueryView.ZONE_DATETIME_FORMATTER;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus;
import uk.gov.moj.cpp.material.persistence.entity.StructuredForm;
import uk.gov.moj.cpp.material.persistence.repository.StructuredFormRepository;
import uk.gov.moj.cpp.material.query.view.utils.FileUtil;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StructuredFormQueryViewTest {

    @InjectMocks
    private StructuredFormQueryView structuredFormQueryView;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private StructuredFormRepository structuredFormRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldReturnStructuredForm() {
        final UUID structuredFormId = randomUUID();
        final UUID formId = randomUUID();
        final ZonedDateTime lastUpdated = now();
        final JsonObject data = createObjectBuilder()
                .add("firstName", "John")
                .add("lastName", "Doe")
                .build();
        when(structuredFormRepository.findBy(structuredFormId)).thenReturn(new StructuredForm(structuredFormId, formId, data.toString(), StructuredFormStatus.CREATED, lastUpdated));

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithDefaults().withName("material.query.structured-form"), createObjectBuilder().add("structuredFormId", structuredFormId.toString()).build());
        final JsonEnvelope result = structuredFormQueryView.getStructuredForm(envelope);

        final JsonObject expected = createStructuredFormResponse(structuredFormId, formId, createData("John", "Doe"), lastUpdated);

        assertThat(result.payloadAsJsonObject(), is(expected));
    }

    @Test
    public void shouldReturnStructuredFormDefendantUser() {
        final UUID defendantId = randomUUID();
        final UUID structuredFormId = randomUUID();
        final UUID formId = randomUUID();
        final ZonedDateTime lastUpdated = now();
        final String payload = FileUtil.getPayload("stub-data/structured-form-sample-data.json")
                .replaceAll("DEFENDANT_ID", defendantId.toString());

        when(structuredFormRepository.findBy(structuredFormId)).thenReturn(new StructuredForm(structuredFormId, formId, payload, StructuredFormStatus.CREATED, lastUpdated));

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithDefaults().withName("material.query.structured-form-defendant-user"), createObjectBuilder().add("structuredFormId", structuredFormId.toString()).add("defendantId", defendantId.toString()).build());
        final JsonEnvelope result = structuredFormQueryView.getStructuredFormDefendantUser(envelope);

        assertThat(result.payloadAsJsonObject().getJsonArray("defendantUser").getJsonObject(0).getString("id"), is(notNullValue()));
        assertThat(result.payloadAsJsonObject().getJsonArray("defendantUser").getJsonObject(0).getString("id"), is(defendantId.toString()));
    }

    private JsonObject createStructuredFormResponse(final UUID structuredFormId, final UUID formId, final JsonObject data, final ZonedDateTime lastUpdated) {
        return createObjectBuilder()
                .add("structuredFormId", structuredFormId.toString())
                .add("formId", formId.toString())
                .add("data", data.toString())
                .add("status", StructuredFormStatus.CREATED.toString())
                .add("lastUpdated", lastUpdated.format(ZONE_DATETIME_FORMATTER))
                .build();
    }

    private JsonObject createData(final String firstName, final String lastName) {
        return createObjectBuilder()
                .add("firstName", firstName)
                .add("lastName", lastName)
                .build();
    }
}
