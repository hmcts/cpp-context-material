package uk.gov.moj.cpp.material.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.material.domain.FileDetails;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleDetailsRecorded;
import uk.gov.moj.cpp.material.persistence.entity.Material;

public class MaterialBundleDetailsRecordedToMaterialConverter implements Converter<MaterialBundleDetailsRecorded, Material> {

    @Override
    public Material convert(MaterialBundleDetailsRecorded event) {
        final FileDetails fileDetails = event.getFileDetails();

        return new Material(event.getMaterialId(), getAlfrescoAssetId(fileDetails), getFilename(fileDetails), getMimeType(fileDetails), event.getMaterialBundleDetailsRecordedDate(), getExternalLink(fileDetails));
    }

    private String getExternalLink(FileDetails fileDetails) {
        return fileDetails != null ? fileDetails.getExternalLink() : null;
    }

    private String getMimeType(FileDetails fileDetails) {
        return fileDetails != null ? fileDetails.getMimeType() : null;
    }

    private String getAlfrescoAssetId(FileDetails fileDetails) {
        return fileDetails != null ? fileDetails.getAlfrescoAssetId() : null;
    }

    private String getFilename(FileDetails fileDetails) {
        return fileDetails != null ? fileDetails.getFileName() : null;
    }

}
