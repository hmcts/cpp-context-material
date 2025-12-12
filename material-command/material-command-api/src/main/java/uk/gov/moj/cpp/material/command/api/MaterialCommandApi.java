package uk.gov.moj.cpp.material.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class MaterialCommandApi {

    @Inject
    private Sender sender;

    @Handles("material.command.add-material")
    public void addMaterial(final JsonEnvelope command) {
        sender.send(command);
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

    @Handles("material.command.delete-material")
    public void deleteMaterial(final JsonEnvelope command) {
        sender.send(Enveloper.envelop(command.payloadAsJsonObject())
                .withName("material.command.handler.delete-material")
                .withMetadataFrom(command));
    }

    @Handles("material.command.create-material-bundle")
    public void createMaterialBundle(final JsonEnvelope command) {
        sender.send(Enveloper.envelop(command.payloadAsJsonObject())
                .withName("material.command.handler.create-material-bundle")
                .withMetadataFrom(command));
    }

    @Handles("material.command.zip-material")
    public void zipMaterial(final JsonEnvelope command) {
        sender.send(Enveloper.envelop(command.payloadAsJsonObject())
                .withName("material.command.handler.zip-material")
                .withMetadataFrom(command));
    }
}
