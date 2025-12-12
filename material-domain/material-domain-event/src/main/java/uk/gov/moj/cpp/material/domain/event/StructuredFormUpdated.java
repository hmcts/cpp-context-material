package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.material.domain.UpdatedBy;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@Event("material.events.structured-form-updated")
public class StructuredFormUpdated implements Serializable {
    private static final long serialVersionUID = 7129860470713712730L;

    private final String structuredFormData;

    private final UUID structuredFormId;

    private final UpdatedBy updatedBy;

    private final ZonedDateTime lastUpdated;

    public StructuredFormUpdated(final UUID structuredFormId, final String structuredFormData, final UpdatedBy updatedBy, final ZonedDateTime lastUpdated) {
        this.structuredFormId = structuredFormId;
        this.structuredFormData = structuredFormData;
        this.updatedBy = updatedBy;
        this.lastUpdated = lastUpdated;
    }

    public String getStructuredFormData() {
        return structuredFormData;
    }

    public UUID getStructuredFormId() {
        return structuredFormId;
    }

    public UpdatedBy getUpdatedBy() {
        return updatedBy;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public static Builder structuredFormUpdated() {
        return new Builder();
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {

        private String structuredFormData;

        private UUID structuredFormId;

        private UpdatedBy updatedBy;

        private ZonedDateTime lastUpdated;

        public Builder withStructuredFormData(final String structuredFormData) {
            this.structuredFormData = structuredFormData;
            return this;
        }

        public Builder withStructuredFormId(final UUID structuredFormId) {
            this.structuredFormId = structuredFormId;
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

        public Builder withValuesFrom(final StructuredFormUpdated structuredFormUpdated) {
            this.structuredFormData = structuredFormUpdated.getStructuredFormData();
            this.structuredFormId = structuredFormUpdated.getStructuredFormId();
            this.updatedBy = structuredFormUpdated.getUpdatedBy();
            this.lastUpdated = structuredFormUpdated.getLastUpdated();
            return this;
        }

        public StructuredFormUpdated build() {
            return new StructuredFormUpdated(structuredFormId, structuredFormData, updatedBy, lastUpdated);
        }
    }
}
