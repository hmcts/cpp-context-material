package uk.gov.moj.cpp.material.client;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.material.MaterialUrls.BASE_URI;
import static uk.gov.moj.cpp.material.MaterialUrls.BUNDLE_MATERIAL_REQUEST_PATH;
import static uk.gov.moj.cpp.material.MaterialUrls.COMMAND_BASE_URI;
import static uk.gov.moj.cpp.material.MaterialUrls.MATERIAL_METADATA_REQUEST_PATH;
import static uk.gov.moj.cpp.material.MaterialUrls.MATERIAL_REQUEST_PATH;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MaterialClient {

    public static final String REQUEST_PARAM_ADD_INLINE_CONTENT_DISPOSITION_HEADER = "addInlineContentDispositionHeader";
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialClient.class);
    private static final String GET_MATERIAL_AS_PDF = "application/vnd.material.query.material+json";
    private static final String QUERY_MATERIAL_METADATA_DETAILS = "application/vnd.material.query.material-metadata-details+json";
    private static final String REMOVE_MATERIAL = "application/vnd.material.command.delete-material+json";
    private static final String CREATE_BUNDLE_MATERIAL = "application/vnd.material.command.create-material-bundle+json";
    private static final String CREATE_BUNDLE_MATERIAL_NAME = "material.command.create-material-bundle";
    private static final String REQUEST_PARAM_STREAM = "stream";
    private static final String REQUEST_PARAM_REQUEST_PDF = "requestPdf";
    private static final String BUNDLED_MATERIAL_ID = "bundledMaterialId";
    private static final String BUNDLED_MATERIAL_NAME = "bundledMaterialName";
    private static final String MATERIAL_IDS = "materialIds";


    public Response getMaterialAsPdf(final String materialId, final String userId) {
        return getMaterial(materialId, userId, false, true, true);
    }

    public Response getMaterialAsPdf(final UUID materialId, final UUID userId) {
        return getMaterial(materialId.toString(), userId.toString(), true, true, false);
    }

    public Response getMaterial(final UUID materialId, final UUID userId) {
        return getMaterial(materialId.toString(), userId.toString(), true, false, false);
    }

    public Response getMaterialWithHeader(final UUID materialId, final UUID userId) {
        return getMaterial(materialId.toString(), userId.toString(), true, false, true);
    }

    public Response getMaterialAsPdfAttachment(final String materialId, final String userId) {
        return getMaterial(materialId, userId, false, true, false);
    }

    public Response removeMaterial(final UUID materialId, final UUID userId, JsonObject body) {
        return removeMaterial(materialId.toString(), userId.toString(), body);
    }

    private Response getMaterial(final String materialId, final String userId, final boolean asStream, final boolean asPdf, final boolean addInlineContentDispositionHeader) {

        final Invocation.Builder builder = getClient()
                .target(BASE_URI)
                .path(MATERIAL_REQUEST_PATH + materialId)
                .queryParam(REQUEST_PARAM_STREAM, asStream)
                .queryParam(REQUEST_PARAM_REQUEST_PDF, asPdf)
                .queryParam(REQUEST_PARAM_ADD_INLINE_CONTENT_DISPOSITION_HEADER, addInlineContentDispositionHeader)
                .request()
                .header(USER_ID, userId)
                .accept(GET_MATERIAL_AS_PDF);

        LOGGER.info("Invoking call to material context");
        return builder.get();
    }

    private Response removeMaterial(final String materialId, final String userId, JsonObject body) {

        final Invocation.Builder builder = getClient()
                .target(COMMAND_BASE_URI)
                .path(MATERIAL_REQUEST_PATH + materialId)
                .request()
                .header(USER_ID, userId)
                .accept(REMOVE_MATERIAL);

        LOGGER.info("Invoking call to material context");
        return builder.post(Entity.entity(body, REMOVE_MATERIAL));
    }

    public Response createMaterialBundle(final UUID bundleMaterialId, final String bundleMaterialName, final List<UUID> materialIds, final String userId, final Map<String, String> headerParameters, final Metadata metaData) {

        final Invocation.Builder builder = getClient()
                .target(COMMAND_BASE_URI)
                .path(BUNDLE_MATERIAL_REQUEST_PATH)
                .request()
                .header(USER_ID, userId)
                .accept(CREATE_BUNDLE_MATERIAL);


        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();
        materialIds.stream().map(materialId -> jsonArrayBuilder.add(materialId.toString())).collect(toList());
        final JsonObject jsonObject = createObjectBuilder()
                .add(BUNDLED_MATERIAL_ID, bundleMaterialId.toString())
                .add(BUNDLED_MATERIAL_NAME, bundleMaterialName)
                .add(MATERIAL_IDS, jsonArrayBuilder.build())
                .add(JsonEnvelope.METADATA, getMetaData(metaData, headerParameters, userId))
                .build();

        LOGGER.info("Invoking call to material context - to create bundle");
        return builder.post(Entity.entity(jsonObject, CREATE_BUNDLE_MATERIAL));
    }

    private JsonObject getMetaData(final Metadata metadata, final Map<String, String> headerParameters, final String userId) {

        final Metadata updatedMetadata = metadataFrom(metadata)
                .withName(CREATE_BUNDLE_MATERIAL_NAME)
                .withUserId(userId)
                .build();

        final JsonObjectBuilder metadataBuilder = createObjectBuilderFrom(updatedMetadata.asJsonObject());

        if (nonNull(headerParameters)) {
            headerParameters.forEach(metadataBuilder::add);
        }
        return metadataBuilder.build();
    }

    public Response getMetadataDetails(final String materialId, final String userId) {
        final Invocation.Builder builder = getClient()
                .target(BASE_URI)
                .path(format(MATERIAL_METADATA_REQUEST_PATH,materialId))
                .request()
                .header(USER_ID, userId)
                .accept(QUERY_MATERIAL_METADATA_DETAILS);

        LOGGER.info("Invoking call to material metadata details");
        return builder.get();
    }

    /**
     * Returns Material Client. Workaround is done so that the client can be mocked in the unit
     * test. As we don't use powermock, static methods are difficult to test.
     */
    Client getClient() {
        return newClient();
    }

    private JsonObjectBuilder createObjectBuilderFrom(final JsonObject source) {
        final JsonObjectBuilder builder = createObjectBuilder();
        source.forEach(builder::add);
        return builder;
    }
}