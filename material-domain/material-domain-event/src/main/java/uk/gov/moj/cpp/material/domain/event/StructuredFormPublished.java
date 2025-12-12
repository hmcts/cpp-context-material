package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.material.domain.UpdatedBy;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@Event("material.events.structured-form-published")
public class StructuredFormPublished implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID structuredFormId;

    private final UpdatedBy updatedBy;

    private final ZonedDateTime lastUpdated;

    public StructuredFormPublished(final UUID structuredFormId, final UpdatedBy updatedBy, final ZonedDateTime lastUpdated) {
        this.structuredFormId = structuredFormId;
        this.updatedBy = updatedBy;
        this.lastUpdated = lastUpdated;
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

    public static Builder structuredFormPublished() {
        return new Builder();
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {

        private UUID structuredFormId;

        private UpdatedBy updatedBy;

        private ZonedDateTime lastUpdated;

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

        public Builder withValuesFrom(final StructuredFormPublished structuredFormPublished) {
            this.structuredFormId = structuredFormPublished.getStructuredFormId();
            this.updatedBy = structuredFormPublished.getUpdatedBy();
            this.lastUpdated = structuredFormPublished.getLastUpdated();
            return this;
        }

        public StructuredFormPublished build() {
            return new StructuredFormPublished(structuredFormId, updatedBy, lastUpdated);
        }
    }
}
