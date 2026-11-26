package uk.gov.moj.cpp.material.query.api;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.query.view.StructuredFormChangeHistoryQueryView;
import uk.gov.moj.cpp.material.query.view.StructuredFormQueryView;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_API)
public class StructuredFormQueryApi {

    public static final String STRUCTURED_FORM_CHANGE_HISTORY = "structuredFormChangeHistory";

    @Inject
    private StructuredFormQueryView structuredFormQueryView;

    @Inject
    private StructuredFormChangeHistoryQueryView structuredFormChangeHistoryQueryView;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Requester requester;

    @Handles("material.query.structured-form")
    public JsonEnvelope getStructuredForm(final JsonEnvelope query) {
        return structuredFormQueryView.getStructuredForm(query);
    }

    @Handles("material.query.structured-form-change-history")
    public JsonEnvelope getStructuredFormChangeHistory(final JsonEnvelope query) {

        final JsonEnvelope structuredFormChangeHistory = structuredFormChangeHistoryQueryView.getStructuredFormChangeHistory(query);
        final JsonObject resultJson = createObjectBuilder()
                .add(STRUCTURED_FORM_CHANGE_HISTORY, structuredFormChangeHistory.payloadAsJsonObject().getJsonArray(STRUCTURED_FORM_CHANGE_HISTORY))
                .build();

        return envelopeFrom(query.metadata(), resultJson);
    }

    @Handles("material.query.structured-form-defendant-user")
    public JsonEnvelope getStructuredFormDefendantUser(final JsonEnvelope query) {
        return structuredFormQueryView.getStructuredFormDefendantUser(query);
    }
}
