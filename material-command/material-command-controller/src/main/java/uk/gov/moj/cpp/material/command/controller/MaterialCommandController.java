package uk.gov.moj.cpp.material.command.controller;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_CONTROLLER)
public class MaterialCommandController {

    private static final String DOCUMENT_ROOT = "document";
    private static final String CONTENT = "content";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("material.command.add-material")
    public void addMaterial(final JsonEnvelope command) {
        if (containsUploadableDocument(command.payloadAsJsonObject())) {
            sender.send(command);
        } else {
            final Metadata metadata = metadataFrom(command.metadata())
                    .withName("material.command.add-external-material")
                    .build();
            sender.send(envelopeFrom(metadata, command.payloadAsJsonObject()));
        }
    }

    @Handles("material.add-material")
    public void addMaterialReference(final JsonEnvelope command) {
        sender.send(command);
    }

    @Handles("material.command.upload-file")
    public void uploadFile(final JsonEnvelope command) {
        sender.send(command);
    }

    @Handles("material.command.upload-file-as-pdf")
    public void uploadFileAsPdf(final JsonEnvelope command) {
        sender.send(command);
    }

    @Handles("material.command.handler.delete-material")
    public void deleteMaterial(final JsonEnvelope command) {
        sender.send(command);
    }

    @Handles("material.command.handler.create-material-bundle")
    public void createMaterialBundle(final JsonEnvelope command) {
        sender.send(command);
    }

    @Handles("material.command.handler.zip-material")
    public void createMaterialZip(final JsonEnvelope command) { sender.send(command); }

    private boolean containsUploadableDocument(final JsonObject command) {
        return command.containsKey(DOCUMENT_ROOT) && command.getJsonObject(DOCUMENT_ROOT).getJsonString(CONTENT) != null;
    }
}
