package uk.gov.moj.cpp.material.event.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.domain.StructuredFormStatus;
import uk.gov.moj.cpp.material.domain.event.StructuredFormCreated;
import uk.gov.moj.cpp.material.domain.event.StructuredFormUpdated;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class FormEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormEventProcessor.class);
    private static final String COMMAND_CREATE_FORM = "material.command.handler.create-structured-form";
    private static final String COMMAND_UPDATE_FORM = "material.command.handler.update-structured-form";
    private static final String PUBLIC_STRUCTURED_FORM_SUCCESS_EVENT = "public.material.structured-form-operation-successful";
    private static final String COMMAND_FINALISE_STRUCTURED_FORM = "material.command.handler.finalise-structured-form";
    private static final String STRUCTURED_FORM_ID = "structuredFormId";
    private static final String FORM_ID = "formId";
    private static final String STRUCTURED_FORM_DATA = "structuredFormData";
    private static final String FORM_DATA = "formData";
    private static final String STATUS = "status";
    private static final String COURT_FORM_ID = "courtFormId";
    private static final String MATERIAL_ID = "materialId";
    private static final String DOCUMENT_META_DATA = "documentMetaData";
    private static final String COMMAND = "command";
    private static final String UPDATED_BY = "updatedBy";
    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("public.progression.form-created")
    public void handleFormCreated(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final String courtFormId = payload.getString(COURT_FORM_ID);
        LOGGER.info("Received public.progression.form-created for courtFormId: {}", courtFormId);

        final JsonObject structuredFormCreatedPayload = createObjectBuilder()
                .add(STRUCTURED_FORM_ID, courtFormId)
                .add(FORM_ID, payload.getString(FORM_ID))
                .add(STRUCTURED_FORM_DATA, payload.getString(FORM_DATA))
                .add(STATUS, StructuredFormStatus.CREATED.name())
                .add(UPDATED_BY, payload.getJsonObject(UPDATED_BY))
                .build();

        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_CREATE_FORM),
                structuredFormCreatedPayload
        ));
    }

    @Handles("public.progression.form-updated")
    public void handleFormUpdated(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final String courtFormId = payload.getString(COURT_FORM_ID);
        LOGGER.info("Received public.progression.form-created for courtFormId: {}", courtFormId);

        final JsonObject structuredFormUpdatedPayload = createObjectBuilder()
                .add(STRUCTURED_FORM_ID, courtFormId)
                .add(STRUCTURED_FORM_DATA, payload.getString(FORM_DATA))
                .add(UPDATED_BY, payload.getJsonObject(UPDATED_BY))
                .build();

        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_FORM),
                structuredFormUpdatedPayload
        ));
    }


    @Handles("public.progression.form-finalised")
    public void handleFormFinalised(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final String courtFormId = payload.getString(COURT_FORM_ID);
        LOGGER.info("Received public.progression.form-finalised for courtFormId: {}", courtFormId);
        payload.getJsonArray(DOCUMENT_META_DATA).forEach(documentMetaData -> {
            final JsonObject metaData = (JsonObject) documentMetaData;
            final JsonObject finaliseStructuredFormPayload = createObjectBuilder()
                    .add(STRUCTURED_FORM_ID, payload.getString(COURT_FORM_ID))
                    .add(MATERIAL_ID, metaData.getString(MATERIAL_ID))
                    .add(UPDATED_BY, payload.getJsonObject(UPDATED_BY))
                    .build();

            sender.send(envelopeFrom(
                    metadataFrom(envelope.metadata()).withName(COMMAND_FINALISE_STRUCTURED_FORM),
                    finaliseStructuredFormPayload
            ));
        });

    }

    @Handles("material.events.structured-form-created")
    public void raisePublicEventForFormCreated(final JsonEnvelope event) {

        final StructuredFormCreated structuredFormCreated = jsonObjectConverter.convert(
                event.payloadAsJsonObject(),
                StructuredFormCreated.class);

        final JsonObject structuredFormId = createObjectBuilder()
                .add(STRUCTURED_FORM_ID, structuredFormCreated.getStructuredFormId().toString())
                .add(COMMAND, "structured-form-created")
                .add(UPDATED_BY, objectToJsonObjectConverter.convert(structuredFormCreated.getUpdatedBy()))
                .build();

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_STRUCTURED_FORM_SUCCESS_EVENT),
                structuredFormId
        ));
    }

    @Handles("material.events.structured-form-updated")
    public void raisePublicEventForFormUpdated(final JsonEnvelope event) {

        final StructuredFormUpdated structuredFormUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), StructuredFormUpdated.class);

        final JsonObject structuredFormId = createObjectBuilder()
                .add(COMMAND, "structured-form-updated")
                .add(STRUCTURED_FORM_ID, structuredFormUpdated.getStructuredFormId().toString())
                .add(UPDATED_BY, objectToJsonObjectConverter.convert(structuredFormUpdated.getUpdatedBy()))
                .build();

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_STRUCTURED_FORM_SUCCESS_EVENT),
                structuredFormId
        ));
    }
}
