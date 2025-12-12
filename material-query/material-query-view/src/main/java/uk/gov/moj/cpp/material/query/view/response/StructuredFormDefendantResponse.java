package uk.gov.moj.cpp.material.query.view.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class StructuredFormDefendantResponse {
    private UUID defendantId;
    private UUID caseId;
    private List<UUID> offences;

    private StructuredFormDefendantResponse(final UUID defendantId, final UUID caseId, final List<UUID> offences) {
        this.defendantId = defendantId;
        this.caseId = caseId;
        this.offences = offences;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public List<UUID> getOffences() {
        return new ArrayList<>(offences);
    }

    public void setOffences(List<UUID> offences) {
        offences = new ArrayList<>(offences);
        this.offences = Collections.unmodifiableList(offences);
    }

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {
        private UUID defendantId;
        private UUID caseId;
        private List<UUID> offences = Collections.emptyList();

        private Builder() {
        }

        public Builder withDefendantId(UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public Builder withCaseId(UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withOffences(List<UUID> offences) {
            offences = new ArrayList<>(offences);
            this.offences = Collections.unmodifiableList(offences);
            return this;
        }

        public StructuredFormDefendantResponse build() {
            return new StructuredFormDefendantResponse(defendantId, caseId, offences);
        }
    }
}
