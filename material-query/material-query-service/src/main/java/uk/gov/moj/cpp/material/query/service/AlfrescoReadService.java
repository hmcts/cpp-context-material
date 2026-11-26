package uk.gov.moj.cpp.material.query.service;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import uk.gov.justice.services.file.api.requester.FileRequester;
import uk.gov.moj.cpp.material.query.service.exception.AlfrescoReadException;
import uk.gov.moj.cpp.material.query.view.MaterialMetadataView;
import uk.gov.moj.cpp.material.query.view.MaterialView;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;

public class AlfrescoReadService {

    @Inject
    private MaterialService materialService;

    @Inject
    private FileRequester fileRequester;

    public Optional<MaterialView> getDataById(final UUID materialId) {


        final MaterialMetadataView materialMetadataView = getMaterialMetadata(materialId);

        if (materialMetadataView == null) {
            return empty();
        }

        return documentFromAlfrescoUsing(materialMetadataView);
    }

    public Optional<MaterialView> getDataAsPdfById(final UUID materialId) {


        final MaterialMetadataView materialMetadataView = getMaterialMetadata(materialId);

        if (materialMetadataView == null) {
            return empty();
        }

        return documentAsPdfFromAlfrescoUsing(materialMetadataView);
    }

    private MaterialMetadataView getMaterialMetadata(final UUID materialId) {

        try {
            return materialService.getMaterialMetadataByMaterialId(materialId);

        } catch (final JsonProcessingException e) {
            throw new AlfrescoReadException(format("Json Error while fetching material for id %s", materialId), e);
        }
    }

    private Optional<MaterialView> documentFromAlfrescoUsing(final MaterialMetadataView materialMetadataView) throws AlfrescoReadException {

        final String alfrescoAssetId = materialMetadataView.getAlfrescoAssetId();
        final String mimeType = materialMetadataView.getMimeType();
        final String fileName = materialMetadataView.getFileName();

        final Optional<InputStream> document = fileRequester.request(alfrescoAssetId, mimeType, fileName);

        if (document.isPresent()) {
            return of(new MaterialView(fileName, document.get(), mimeType));
        }

        final String message = format("No document found while fetching document from alfresco. " +
                        "alfrescoAssetId: '%s', mimeType: '%s', fileName: '%s'",
                alfrescoAssetId,
                mimeType,
                fileName);

        throw new AlfrescoReadException(message);
    }

    private Optional<MaterialView> documentAsPdfFromAlfrescoUsing(final MaterialMetadataView materialMetadataView) {

        final String alfrescoAssetId = materialMetadataView.getAlfrescoAssetId();
        final String mimeType = "application/pdf";
        final String fileName = materialMetadataView.getFileName();

        final Optional<InputStream> document = fileRequester.requestPdf(alfrescoAssetId, fileName);

        if (document.isPresent()) {
            return of(new MaterialView(fileName, document.get(), mimeType));
        }

        final String message = format("No document found while fetching document from alfresco. " +
                        "alfrescoAssetId: '%s', mimeType: '%s', fileName: '%s'",
                alfrescoAssetId,
                mimeType,
                fileName);

        throw new AlfrescoReadException(message);
    }
}
