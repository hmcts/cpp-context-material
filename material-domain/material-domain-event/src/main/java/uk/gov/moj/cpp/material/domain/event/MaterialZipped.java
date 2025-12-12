package uk.gov.moj.cpp.material.domain.event;


import uk.gov.justice.domain.annotation.Event;

import java.util.List;
import java.util.UUID;

@Event("material.events.material-zipped")
@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize"})
public class MaterialZipped {

    private final UUID caseId;
    private final List<UUID> materialIds;
    private final String caseURN;
    private final List<UUID> fileIds;

    public MaterialZipped(final UUID caseId, final String caseURN, final List<UUID> materialIds, final List<UUID> fileIds) {
        this.caseId = caseId;
        this.materialIds = materialIds;
        this.caseURN = caseURN;
        this.fileIds = fileIds;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<UUID> getMaterialIds() {
        return materialIds;
    }

    public String getCaseURN() {
        return caseURN;
    }

    public List<UUID> getFileIds() {
        return fileIds;
    }

    public static class Builder {
        private UUID caseId;
        private List<UUID> materialIds;
        private String caseURN;

        private List<UUID> fileIds;

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withCaseURN(final String caseURN) {
            this.caseURN = caseURN;
            return this;
        }

        public Builder withMaterialIds(final List<UUID> materialIds) {
            this.materialIds = materialIds;
            return this;
        }

        public Builder withFileIds(final List<UUID> fileIds) {
            this.fileIds = fileIds;
            return this;
        }

        public Builder withValuesFrom(final MaterialZipped materialZipped) {
            this.caseId = materialZipped.getCaseId();
            this.materialIds = materialZipped.getMaterialIds();
            this.caseURN = materialZipped.getCaseURN();
            this.fileIds = materialZipped.getFileIds();
            return this;
        }

        public MaterialZipped build() {
            return new MaterialZipped(caseId, caseURN, materialIds, fileIds);
        }
    }
}
