package uk.gov.moj.cpp.material.query.view;

import uk.gov.moj.cpp.material.persistence.entity.Material;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MaterialMetadataView {

    private final UUID materialId;
    private final String alfrescoAssetId;
    private final String fileName;
    private final String mimeType;
    private final String externalLink;
    private ZonedDateTime materialAddedDate;

    public MaterialMetadataView(final Material material) {
        this.materialId = material.getMaterialId();
        this.alfrescoAssetId = material.getAlfrescoId();
        this.fileName = material.getFilename();
        this.mimeType = material.getMimeType();
        this.materialAddedDate = material.getDateMaterialAdded();
        this.externalLink = material.getExternalLink();
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getAlfrescoAssetId() {
        return alfrescoAssetId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public ZonedDateTime getMaterialAddedDate() {
        return materialAddedDate;
    }

    public String getExternalLink() {
        return externalLink;
    }
}
