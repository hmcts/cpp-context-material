package uk.gov.moj.cpp.material.command.api;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.command.services.DownloadableMaterialsService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

@ServiceComponent(COMMAND_API)
public class DownloadableMaterialCommandApi {

    @Inject
    private Sender sender;



    @Inject
    private DownloadableMaterialsService downloadableMaterialsService;

    @Handles("material.command.publish-is-downloadable-materials")
    public Envelope publishDownloadableMaterials(final JsonEnvelope command){

        final List<UUID> metarialIds = command.payloadAsJsonObject().getJsonArray("materialIds").stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .map(UUID::fromString)
                .collect(Collectors.toList());

        final Map<UUID, Boolean> materials = downloadableMaterialsService.getDownloadableMaterials(metarialIds);
        final JsonObjectBuilder builder = createObjectBuilder();
        materials.forEach((k, v) -> builder.add(k.toString(), v.toString()));

        return envelopeFrom(
                command.metadata(),
                createObjectBuilder().add("materials", builder.build()).build());
    }

}
