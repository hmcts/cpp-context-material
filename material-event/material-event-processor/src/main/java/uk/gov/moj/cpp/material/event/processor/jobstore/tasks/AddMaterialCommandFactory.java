package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class AddMaterialCommandFactory {

    private static final String MATERIAL_FIELD_NAME = "materialId";
    private static final String ADD_MATERIAL_COMMAND_NAME = "material.add-material";

    public JsonEnvelope createCommandEnvelope(final SuccessfulMaterialUploadJobData sendMaterialToAlfrescoJobState) {

        final JsonObject fileUploadedEventMetadata = sendMaterialToAlfrescoJobState.getFileUploadedEventMetadata();

        final JsonObjectBuilder document = createObjectBuilder()
                .add("fileReference", sendMaterialToAlfrescoJobState.getAlfrescoFileId().toString())
                .add("mimeType", sendMaterialToAlfrescoJobState.getMediaType());

        final JsonObject addMaterialCommandPayload = createObjectBuilder()
                .add(MATERIAL_FIELD_NAME, sendMaterialToAlfrescoJobState.getMaterialId().toString())
                .add("fileName", sendMaterialToAlfrescoJobState.getFileName())
                .add("document", document)
                .add("isUnbundledDocument", sendMaterialToAlfrescoJobState.isUnbundledDocument())
                .build();

        return envelopeFrom(
                metadataFrom(fileUploadedEventMetadata).withName(ADD_MATERIAL_COMMAND_NAME),
                addMaterialCommandPayload);
    }
}
