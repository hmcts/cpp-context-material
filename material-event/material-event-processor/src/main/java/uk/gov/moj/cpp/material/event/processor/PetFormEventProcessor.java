package uk.gov.moj.cpp.material.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
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
import uk.gov.moj.cpp.material.domain.event.StructuredFormFinalised;
import uk.gov.moj.cpp.material.domain.event.StructuredFormPublished;
import uk.gov.moj.cpp.material.domain.event.StructuredFormUpdatedForDefendant;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class PetFormEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PetFormEventProcessor.class);

    private static final String COMMAND_CREATE_FORM = "material.command.handler.create-structured-form";
    private static final String COMMAND_UPDATE_FORM = "material.command.handler.update-structured-form";
    private static final String COMMAND_UPDATE_FORM_FOR_DEFENDANT = "material.command.handler.update-structured-form-for-defendant";
    private static final String COMMAND_FINALISE_STRUCTURED_FORM = "material.command.handler.finalise-structured-form";
    private static final String PUBLIC_STRUCTURED_FORM_SUCCESS_EVENT = "public.material.structured-form-operation-successful";

    public static final String PET_ID = "petId";
    public static final String STRUCTURED_FORM_ID = "structuredFormId";
    public static final String FORM_ID = "formId";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String DEFENDANT_DATA = "defendantData";
    public static final String STRUCTURED_FORM_DATA = "structuredFormData";
    public static final String PET_FORM_DATA = "petFormData";
    public static final String STATUS = "status";
    public static final String USER_ID = "userId";
    public static final String MATERIAL_ID = "materialId";
    private static final String COMMAND = "command";
    private static final String UPDATED_BY = "updatedBy";

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("public.progression.pet-form-created")
    public void handlePetFormCreated(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final String petId = payload.getString(PET_ID);
        LOGGER.info("Received public.progression.pet-form-created for petId: {}", petId);

        final JsonObject structuredFormCreatedPayload = createObjectBuilder()
                .add(STRUCTURED_FORM_ID, payload.getString(PET_ID))
                .add(FORM_ID, payload.getString(FORM_ID))
                .add(STRUCTURED_FORM_DATA, payload.getString(PET_FORM_DATA))
                .add(STATUS, StructuredFormStatus.CREATED.name())
                .add(UPDATED_BY, payload.getJsonObject(UPDATED_BY))
                .build();

        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_CREATE_FORM),
                structuredFormCreatedPayload
        ));
    }

    @Handles("public.progression.pet-form-updated")
    public void handlePetFormUpdated(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final String petId = payload.getString(PET_ID);
        LOGGER.info("Received public.progression.pet-form-updated for petId: {}", petId);

        final JsonObject structuredFormUpdatedPayload = createObjectBuilder()
                .add(STRUCTURED_FORM_ID, payload.getString(PET_ID))
                .add(STRUCTURED_FORM_DATA, payload.getString(PET_FORM_DATA))
                .add(UPDATED_BY, payload.getJsonObject(UPDATED_BY))
                .build();

        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_FORM),
                structuredFormUpdatedPayload
        ));
    }

    @Handles("public.progression.pet-form-defendant-updated")
    public void handlePetFormDefendantUpdated(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final String petId = payload.getString(PET_ID);
        LOGGER.info("Received public.progression.pet-form-defendant-updated for petId: {}", petId);

        final JsonObject structuredFormDefendantUpdatedPayload = createObjectBuilder()
                .add(STRUCTURED_FORM_ID, payload.getString(PET_ID))
                .add(DEFENDANT_ID, payload.getString(DEFENDANT_ID))
                .add(DEFENDANT_DATA, payload.getString(DEFENDANT_DATA))
                .add(UPDATED_BY, payload.getJsonObject(UPDATED_BY))
                .build();

        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_FORM_FOR_DEFENDANT),
                structuredFormDefendantUpdatedPayload
        ));
    }

    @Handles("public.progression.pet-form-finalised")
    public void handlePetFormFinalised(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final String petId = payload.getString(PET_ID);
        LOGGER.info("Received public.progression.pet-form-finalised for petId: {}", petId);

        final JsonObject finaliseStructuredFormPayload = createObjectBuilder()
                .add(STRUCTURED_FORM_ID, payload.getString(PET_ID))
                .add(MATERIAL_ID, payload.getString(MATERIAL_ID))
                .add(UPDATED_BY, payload.getJsonObject(UPDATED_BY))
                .build();

        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName(COMMAND_FINALISE_STRUCTURED_FORM),
                finaliseStructuredFormPayload
        ));
    }

    @Handles("material.events.structured-form-updated-for-defendant")
    public void raisePublicEventForPetFormUpdatedForDefendant(final JsonEnvelope event) {

        final StructuredFormUpdatedForDefendant structuredFormUpdatedForDefendant = jsonObjectConverter.convert(event.payloadAsJsonObject(), StructuredFormUpdatedForDefendant.class);

        final JsonObject structuredFormId = createObjectBuilder()
                .add(COMMAND, "structured-form-updated-for-defendant")
                .add(STRUCTURED_FORM_ID, structuredFormUpdatedForDefendant.getStructuredFormId().toString())
                .add(UPDATED_BY, objectToJsonObjectConverter.convert(structuredFormUpdatedForDefendant.getUpdatedBy()))
                .build();

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_STRUCTURED_FORM_SUCCESS_EVENT),
                structuredFormId
        ));
    }

    @Handles("material.events.structured-form-finalised")
    public void raisePublicEventForPetFormFinalised(final JsonEnvelope event) {

        final StructuredFormFinalised structuredFormFinalised = jsonObjectConverter.convert(event.payloadAsJsonObject(), StructuredFormFinalised.class);

        final JsonObject structuredFormId = createObjectBuilder()
                .add(COMMAND, "structured-form-finalised")
                .add(STRUCTURED_FORM_ID, structuredFormFinalised.getStructuredFormId().toString())
                .add(UPDATED_BY, objectToJsonObjectConverter.convert(structuredFormFinalised.getUpdatedBy()))
                .build();

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_STRUCTURED_FORM_SUCCESS_EVENT),
                structuredFormId
        ));
    }

    @Handles("material.events.structured-form-published")
    public void raisePublicEventForPetFormPublished(final JsonEnvelope event) {

        final StructuredFormPublished structuredFormPublished = jsonObjectConverter.convert(event.payloadAsJsonObject(), StructuredFormPublished.class);

        final JsonObject structuredFormId = createObjectBuilder()
                .add(COMMAND, "structured-form-published")
                .add(STRUCTURED_FORM_ID, structuredFormPublished.getStructuredFormId().toString())
                .add(UPDATED_BY, objectToJsonObjectConverter.convert(structuredFormPublished.getUpdatedBy()))
                .build();

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_STRUCTURED_FORM_SUCCESS_EVENT),
                structuredFormId
        ));
    }
}
