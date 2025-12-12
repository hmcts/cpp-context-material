package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize"})
@Event("material.events.failed-to-zip-material")
public class MaterialZipFailed implements Serializable {

    private final UUID caseId;
    private final List<UUID> materialIds;
    private final List<UUID> fileIds;
    private final String errorMessage;

    public MaterialZipFailed(final UUID caseId, final List<UUID> materialIds, final List<UUID> fileIds, final String errorMessage) {
        this.materialIds = materialIds;
        this.fileIds = fileIds;
        this.errorMessage = errorMessage;
        this.caseId = caseId;
    }

    public List<UUID> getMaterialIds() {
        return materialIds;
    }

    public List<UUID> getFileIds() {
        return fileIds;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public UUID getCaseId() {
        return caseId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MaterialZipFailed that = (MaterialZipFailed) o;
        return Objects.equals(caseId, that.caseId) && Objects.equals(materialIds, that.materialIds) && Objects.equals(fileIds, that.fileIds) && Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(materialIds, fileIds, errorMessage, caseId);
    }

    @Override
    public String toString() {
        return "MaterialZipFailed{" +
                "caseId=" + caseId +
                ", materialIds=" + materialIds +
                ", fileIds=" + fileIds +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

    public static class Builder {
        private List<UUID> materialIds;
        private List<UUID> fileIds;
        private String errorMessage;
        private UUID caseId;

        public Builder withMaterialIds(final List<UUID> materialIds) {
            this.materialIds = materialIds;
            return this;
        }

        public Builder withFileIds(final List<UUID> fileIds) {
            this.fileIds = fileIds;
            return this;
        }

        public Builder withErrorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withValuesFrom(final MaterialZipFailed materialZipFailed) {
            this.fileIds = materialZipFailed.getFileIds();
            this.materialIds = materialZipFailed.getMaterialIds();
            this.errorMessage = materialZipFailed.getErrorMessage();
            this.caseId = materialZipFailed.getCaseId();
            return this;
        }

        public MaterialZipFailed build() {
            return new MaterialZipFailed(caseId, materialIds, fileIds, errorMessage);
        }
    }
}
