package uk.gov.moj.material.it.dataaccess;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MaterialReference {

    private final UUID materialId;
    private final String fileReference;
    private final String fileName;
    private final String mimeType;
    private final String externalLink;
    private final ZonedDateTime dateAdded;


    public MaterialReference(
            final UUID materialId,
            final String fileReference,
            final String fileName,
            final String mimeType,
            final String externalLink,
            final ZonedDateTime dateAdded) {
        this.materialId = materialId;
        this.fileReference = fileReference;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.externalLink = externalLink;
        this.dateAdded = dateAdded;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getFileReference() {
        return fileReference;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public ZonedDateTime getDateAdded() {
        return dateAdded;
    }
}
