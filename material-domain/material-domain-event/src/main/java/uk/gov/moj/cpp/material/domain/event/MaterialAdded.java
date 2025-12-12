package uk.gov.moj.cpp.material.domain.event;


import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.material.domain.FileDetails;

import java.time.ZonedDateTime;
import java.util.UUID;

@Event("material.events.material-added")
public class MaterialAdded {

    private final UUID materialId;
    private final FileDetails fileDetails;
    private final ZonedDateTime materialAddedDate;
    private final Boolean isUnbundledDocument;

    public MaterialAdded(final UUID materialId, final FileDetails fileDetails, final ZonedDateTime materialAddedDate, final Boolean isUnbundledDocument) {
        this.materialId = materialId;
        this.fileDetails = fileDetails;
        this.materialAddedDate = materialAddedDate;
        this.isUnbundledDocument = isUnbundledDocument;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public FileDetails getFileDetails() {
        return fileDetails;
    }

    public ZonedDateTime getMaterialAddedDate() {
        return materialAddedDate;
    }

    public Boolean getIsUnbundledDocument() {
        return isUnbundledDocument;
    }

    public static class Builder {
        private UUID materialId;
        private FileDetails fileDetails;
        private ZonedDateTime materialAddedDate;
        private Boolean isUnbundledDocument;

        public Builder withMaterialId(final UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public Builder withFileDetails(final FileDetails fileDetails) {
            this.fileDetails = fileDetails;
            return this;
        }

        public Builder withMaterialAddedDate(final ZonedDateTime materialAddedDate) {
            this.materialAddedDate = materialAddedDate;
            return this;
        }

        public Builder withIsUnbundledDocument(final Boolean isUnbundledDocument) {
            this.isUnbundledDocument = isUnbundledDocument;
            return this;
        }

        public Builder withValuesFrom(final MaterialAdded materialAdded) {
            this.materialId = materialAdded.getMaterialId();
            this.fileDetails = materialAdded.getFileDetails();
            this.materialAddedDate = materialAdded.getMaterialAddedDate();
            this.isUnbundledDocument = materialAdded.getIsUnbundledDocument();
            return this;
        }

        public MaterialAdded build() {
            return new MaterialAdded(materialId, fileDetails, materialAddedDate, isUnbundledDocument);
        }
    }
}
