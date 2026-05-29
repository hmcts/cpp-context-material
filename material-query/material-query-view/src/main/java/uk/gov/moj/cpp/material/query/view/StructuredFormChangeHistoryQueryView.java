package uk.gov.moj.cpp.material.query.view;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.material.query.view.StructuredFormQueryView.ZONE_DATETIME_FORMATTER;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.persistence.entity.StructuredFormChangeHistory;
import uk.gov.moj.cpp.material.persistence.repository.StructuredFormChangeHistoryRepository;
import uk.gov.moj.cpp.material.query.view.response.StructuredFormChangeHistoryResponse;
import uk.gov.moj.cpp.material.query.view.response.UpdatedBy;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class StructuredFormChangeHistoryQueryView {
    public static final String STRUCTURED_FORM_ID = "structuredFormId";
    public static final String STRUCTURED_FORM_CHANGE_HISTORY = "structuredFormChangeHistory";
    public static final String ID = "id";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String NAME = "name";

    @Inject
    private StructuredFormChangeHistoryRepository structuredFormChangeHistoryRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    public JsonEnvelope getStructuredFormChangeHistory(JsonEnvelope envelope) {
        final UUID structuredFormId = fromString(envelope.asJsonObject().getString(STRUCTURED_FORM_ID));
        final List<StructuredFormChangeHistory> structuredFormChangeHistoryList = structuredFormChangeHistoryRepository.findByStructuredFormId(structuredFormId);

        final JsonArrayBuilder structuredFormChangeHistoryJsonArrayBuilder = createArrayBuilder();
        structuredFormChangeHistoryList.forEach(item -> structuredFormChangeHistoryJsonArrayBuilder.add(buildStructuredFormChangeHistoryResponse(item)));

        final JsonObject resultJson = createObjectBuilder()
                .add(STRUCTURED_FORM_CHANGE_HISTORY, structuredFormChangeHistoryJsonArrayBuilder)
                .build();

        return envelopeFrom(envelope.metadata(), resultJson);
    }

    private JsonObject buildStructuredFormChangeHistoryResponse(final StructuredFormChangeHistory item) {
        final JsonObject updatedByJsonObject = stringToJsonObjectConverter.convert(item.getUpdatedBy());
        final UpdatedBy.Builder updatedByBuilder = UpdatedBy.builder();
        final StructuredFormChangeHistoryResponse.Builder builder = StructuredFormChangeHistoryResponse.builder()
                .withId(item.getId())
                .withStructuredFormId(item.getStructuredFormId())
                .withFormId(item.getFormId())
                .withDate(item.getDate().format(ZONE_DATETIME_FORMATTER))
                .withData(item.getData())
                .withStatus(item.getStatus());

        if (nonNull(item.getMaterialId())) {
            builder.withMaterialId(item.getMaterialId());
        }

        if (updatedByJsonObject.containsKey(NAME)) {
            updatedByBuilder.withName(updatedByJsonObject.getString(NAME));
        } else {
            updatedByBuilder.withId(fromString(updatedByJsonObject.getString(ID)));
            updatedByBuilder.withFirstName(updatedByJsonObject.getString(FIRST_NAME));
            updatedByBuilder.withLastName(updatedByJsonObject.getString(LAST_NAME));
        }

        builder.withUpdatedBy(updatedByBuilder.build());

        final StructuredFormChangeHistoryResponse structuredFormChangeHistoryResponse = builder
                .build();

        return objectToJsonObjectConverter.convert(structuredFormChangeHistoryResponse);
    }
}
