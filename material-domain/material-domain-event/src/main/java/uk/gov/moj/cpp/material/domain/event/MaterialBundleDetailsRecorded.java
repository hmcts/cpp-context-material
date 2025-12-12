package uk.gov.moj.cpp.material.domain.event;


import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.material.domain.FileDetails;

import java.time.ZonedDateTime;
import java.util.UUID;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
@Event("material.events.material-bundle-details-recorded")
public class MaterialBundleDetailsRecorded {

    private final UUID materialId;
    private final FileDetails fileDetails;
    private final String fileSize;
    private final int pageCount;
    private final ZonedDateTime materialBundleDetailsRecordedDate;

    public MaterialBundleDetailsRecorded(final UUID materialId, final FileDetails fileDetails, final String fileSize, final int pageCount, final ZonedDateTime materialBundleDetailsRecordedDate) {
        this.materialId = materialId;
        this.fileDetails = fileDetails;
        this.fileSize =fileSize;
        this.pageCount = pageCount;
        this.materialBundleDetailsRecordedDate = materialBundleDetailsRecordedDate;
    }

    public static Builder materialBundleDetailsRecorded() {
        return new MaterialBundleDetailsRecorded.Builder();
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public FileDetails getFileDetails() {
        return fileDetails;
    }

    public ZonedDateTime getMaterialBundleDetailsRecordedDate() {
        return materialBundleDetailsRecordedDate;
    }

    public String getFileSize() {
        return fileSize;
    }

    public int getPageCount() {
        return pageCount;
    }

    public static class Builder {
        private UUID materialId;
        private FileDetails fileDetails;
        private ZonedDateTime materialBundleDetailsRecorded;
        private String fileSize;
        private int pageCount;

        public MaterialBundleDetailsRecorded.Builder withMaterialId(final UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public MaterialBundleDetailsRecorded.Builder withFileSize(final String fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public MaterialBundleDetailsRecorded.Builder withPageCount(final int pageCount) {
            this.pageCount = pageCount;
            return this;
        }

        public MaterialBundleDetailsRecorded.Builder withFileDetails(final FileDetails fileDetails) {
            this.fileDetails = fileDetails;
            return this;
        }

        public MaterialBundleDetailsRecorded.Builder withMaterialBundleDetailsRecordedDate(final ZonedDateTime materialBundleDetailsRecorded) {
            this.materialBundleDetailsRecorded = materialBundleDetailsRecorded;
            return this;
        }


        public MaterialBundleDetailsRecorded.Builder withValuesFrom(final MaterialBundleDetailsRecorded materialBundleDetailsRecorded) {
            this.materialId = materialBundleDetailsRecorded.getMaterialId();
            this.fileDetails = materialBundleDetailsRecorded.getFileDetails();
            this.materialBundleDetailsRecorded = materialBundleDetailsRecorded.getMaterialBundleDetailsRecordedDate();
            return this;
        }

        public MaterialBundleDetailsRecorded build() {
            return new MaterialBundleDetailsRecorded(materialId, fileDetails, fileSize, pageCount, materialBundleDetailsRecorded);
        }
    }
}
