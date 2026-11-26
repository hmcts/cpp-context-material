package uk.gov.moj.cpp.material.query.api;


import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.query.view.MaterialDetailedMetadataView;
import uk.gov.moj.cpp.material.query.view.MaterialMetadataView;
import uk.gov.moj.cpp.material.query.view.MaterialQueryView;

import java.io.IOException;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;

@ServiceComponent(QUERY_API)
public class MaterialQueryApi {

    @Inject
    private MaterialQueryView materialQueryView;

    @Handles("material.query.material-metadata")
    public Envelope<MaterialMetadataView> findMaterialMetadata(final JsonEnvelope query) throws JsonProcessingException {
        return materialQueryView.findMaterialMetadata(query);
    }

    @Handles("material.query.material-metadata-details")
    public Envelope<MaterialDetailedMetadataView> findMaterialMetadataDetails(final JsonEnvelope query) throws IOException {
        return materialQueryView.findMaterialMetadataDetails(query);
    }

    @Handles("material.query.is-downloadable-materials")
    public JsonEnvelope findDownloadableMaterials(final JsonEnvelope query) {
        return materialQueryView.findDownloadableMaterials(query);
    }

}
