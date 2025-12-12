package uk.gov.moj.cpp.material.query.view;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.valueOf;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.file.alfresco.Headers.headersWithUserId;
import static uk.gov.moj.cpp.material.query.view.MaterialDetailedMetadataView.MaterialDetailedMetadataViewBuilder.materialDetailedMetadataViewBuilder;

import uk.gov.justice.services.common.configuration.GlobalValue;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.file.alfresco.AlfrescoRestClient;
import uk.gov.justice.services.file.api.FileOperationException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.query.service.MaterialService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;

public class MaterialQueryView {

    static final String FIELD_ID = "materialId";
    static final String MATERIAL_IDS = "materialIds";
    static final String MATERIAL_QUERY_MATERIAL_METADATA_RESPONSE = "material.query.material-metadata-response";
    static final String MATERIAL_QUERY_IS_DOWNLOADABLE_MATERIALS_RESPONSE = "material.query.is-downloadable-materials-response";
    static final String MATERIAL_QUERY_MATERIAL_METADATA_DETAILS_RESPONSE = "material.query.material-metadata-details-response";

    @Inject
    Enveloper enveloper;

    @Inject
    MaterialService materialService;

    @Inject
    AlfrescoRestClient restClient;

    @Inject
    @GlobalValue(key = "alfrescoReadUser")
    String alfrescoReadUser;

    @Inject
    @GlobalValue(key = "alfrescoWorkspacePath", defaultValue = "/service/slingshot/doclib2/node/workspace/SpacesStore/")
    String alfrescoWorkspacePath;

    @Handles("material.query.material-metadata")
    public Envelope<MaterialMetadataView> findMaterialMetadata(final JsonEnvelope query) throws JsonProcessingException {
        return envelop(materialService.getMaterialMetadataByMaterialId(UUID.fromString(query.payloadAsJsonObject().getString(FIELD_ID))))
                .withName(MATERIAL_QUERY_MATERIAL_METADATA_RESPONSE)
                .withMetadataFrom(query);
    }

    @Handles("material.query.material-metadata-details")
    @SuppressWarnings("squid:S1166")
    public Envelope<MaterialDetailedMetadataView> findMaterialMetadataDetails(final JsonEnvelope query) throws IOException {

        final MaterialMetadataView materialMetadataView = materialService.getMaterialMetadataByMaterialId(UUID.fromString(query.payloadAsJsonObject().getString(FIELD_ID)));
        if(isNull(materialMetadataView)){
            return envelop(fileDataFrom(null, materialMetadataView))
                    .withName(MATERIAL_QUERY_MATERIAL_METADATA_DETAILS_RESPONSE)
                    .withMetadataFrom(query);
        }

        final Optional<InputStream> response = request(materialMetadataView.getAlfrescoAssetId(), materialMetadataView.getMimeType());

        if (!response.isPresent()) {
            throw new FileOperationException(format("No file from Alfresco with fileId:%s ", materialMetadataView.getAlfrescoAssetId()));
        }

        final String responseString = IOUtils.toString(response.get());

        return envelop(fileDataFrom(responseString, materialMetadataView))
                .withName(MATERIAL_QUERY_MATERIAL_METADATA_DETAILS_RESPONSE)
                .withMetadataFrom(query);
    }

    public Optional<InputStream> request(final String fileId, final String fileMimeType) {
        try {
            return ofNullable(restClient.getAsInputStream(alfrescoUriOf(fileId),
                    valueOf(fileMimeType), headersWithUserId(alfrescoReadUser)));
        } catch (final ProcessingException | InternalServerErrorException ex) {
            throw new FileOperationException(format("Error fetching file from Alfresco with fileId = %s", fileId), ex);
        }
    }

    private String alfrescoUriOf(final String fileId) {
        return format("%s%s", alfrescoWorkspacePath, fileId);
    }

    private MaterialDetailedMetadataView fileDataFrom(final String responseEntity, final MaterialMetadataView materialMetadataView) {
        if(isNull(materialMetadataView)){
            return null;
        }
        final Object responseDocument = defaultConfiguration().jsonProvider().parse(responseEntity);
        final int size = JsonPath.read(responseDocument, "$.item.node.size");
        return materialDetailedMetadataViewBuilder()
                .withAlfrescoFileId(materialMetadataView.getAlfrescoAssetId())
                .withFileName(materialMetadataView.getFileName())
                .withFileSize(size)
                .withMaterialId(materialMetadataView.getMaterialId())
                .build();
    }

    @Handles("material.query.is-downloadable-materials")
    public JsonEnvelope findDownloadableMaterials(final JsonEnvelope query) {
        return enveloper.withMetadataFrom(query, MATERIAL_QUERY_IS_DOWNLOADABLE_MATERIALS_RESPONSE).apply(
                materialService.getMaterialsDownloadStatusByMaterialIds(query.payloadAsJsonObject().getString(MATERIAL_IDS)));
    }
}