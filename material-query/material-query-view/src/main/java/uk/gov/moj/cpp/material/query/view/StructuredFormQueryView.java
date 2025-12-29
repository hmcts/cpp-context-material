package uk.gov.moj.cpp.material.query.view;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.persistence.entity.StructuredForm;
import uk.gov.moj.cpp.material.persistence.repository.StructuredFormRepository;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.jayway.jsonpath.JsonPath;

public class StructuredFormQueryView {
    public static final String STRUCTURED_FORM_ID = "structuredFormId";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String FORM_ID = "formId";
    public static final String STATUS = "status";
    public static final String DATA = "data";
    public static final String DEFENDANT_USER = "defendantUser";
    public static final String LAST_UPDATED = "lastUpdated";
    protected static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Inject
    private StructuredFormRepository structuredFormRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    public JsonEnvelope getStructuredForm(final JsonEnvelope envelope) {
        final UUID structuredFormId = fromString(envelope.asJsonObject().getString(STRUCTURED_FORM_ID));
        final StructuredForm structuredForm = structuredFormRepository.findBy(structuredFormId);
        final JsonObject resultJson = createObjectBuilder().add(STRUCTURED_FORM_ID, structuredForm.getId().toString())
                .add(FORM_ID, structuredForm.getFormId().toString())
                .add(DATA, structuredForm.getData())
                .add(STATUS, structuredForm.getStatus().toString())
                .add(LAST_UPDATED, structuredForm.getLastUpdated().format(ZONE_DATETIME_FORMATTER))
                .build();
        return envelopeFrom(envelope.metadata(), resultJson);
    }

    public JsonEnvelope getStructuredFormDefendantUser(final JsonEnvelope envelope) {
        final UUID structuredFormId = fromString(envelope.asJsonObject().getString(STRUCTURED_FORM_ID));
        final String defendantId = envelope.asJsonObject().getString(DEFENDANT_ID);
        final StructuredForm structuredForm = structuredFormRepository.findBy(structuredFormId);

        final Object updatedData = JsonPath.parse(structuredForm.getData())
                .read("$.data.defence.defendants[?(@.id == \"" + defendantId + "\")]");

        final JsonValue dataValue = objectToJsonValueConverter.convert(updatedData);

        final JsonObject resultJson = createObjectBuilder()
                .add(DEFENDANT_USER, dataValue)
                .build();
        return envelopeFrom(envelope.metadata(), resultJson);
    }

}
