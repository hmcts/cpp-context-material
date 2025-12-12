package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.material.domain.UpdatedBy;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@Event("material.events.structured-form-updated-for-defendant")
public class StructuredFormUpdatedForDefendant implements Serializable {
    private static final long serialVersionUID = 7129860470713712730L;

    private final UUID structuredFormId;

    private final UUID defendantId;

    private final String defendantData;

    private final UpdatedBy updatedBy;

    private final ZonedDateTime lastUpdated;

    public StructuredFormUpdatedForDefendant(final UUID structuredFormId, final UUID defendantId, final String defendantData, final UpdatedBy updatedBy, final ZonedDateTime lastUpdated) {
        this.structuredFormId = structuredFormId;
        this.defendantId = defendantId;
        this.defendantData = defendantData;
        this.updatedBy = updatedBy;
        this.lastUpdated = lastUpdated;
    }

    public String getDefendantData() {
        return defendantData;
    }

    public UUID getStructuredFormId() {
        return structuredFormId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UpdatedBy getUpdatedBy() {
        return updatedBy;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public static Builder structuredFormUpdatedForDefendant() {
        return new Builder();
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {

        private String defendantData;

        private UUID structuredFormId;

        private UUID defendantId;

        private UpdatedBy updatedBy;

        private ZonedDateTime lastUpdated;

        public Builder withDefendantData(final String defendantData) {
            this.defendantData = defendantData;
            return this;
        }

        public Builder withStructuredFormId(final UUID structuredFormId) {
            this.structuredFormId = structuredFormId;
            return this;
        }

        public Builder withDefendantId(final UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public Builder withUpdatedBy(final UpdatedBy updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public Builder withLastUpdated(final ZonedDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder withValuesFrom(final StructuredFormUpdatedForDefendant structuredFormUpdatedForDefendant) {
            this.defendantData = structuredFormUpdatedForDefendant.getDefendantData();
            this.defendantId = structuredFormUpdatedForDefendant.getDefendantId();
            this.structuredFormId = structuredFormUpdatedForDefendant.getStructuredFormId();
            this.updatedBy = structuredFormUpdatedForDefendant.getUpdatedBy();
            this.lastUpdated = structuredFormUpdatedForDefendant.getLastUpdated();
            return this;
        }

        public StructuredFormUpdatedForDefendant build() {
            return new StructuredFormUpdatedForDefendant(structuredFormId, defendantId, defendantData, updatedBy, lastUpdated);
        }
    }
}
