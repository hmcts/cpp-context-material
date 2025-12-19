package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

public class MaterialEnvelopeFactory {

    private static final String QUERY_ACTION_NAME = "material.query.material";

    public JsonEnvelope buildEnvelope(final String userId, final String materialId) {

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(QUERY_ACTION_NAME)
                .withUserId(userId);

        return envelopeFrom(
                metadataBuilder,
                createObjectBuilder()
                        .add("materialId", materialId)
                        .build());
    }
}
