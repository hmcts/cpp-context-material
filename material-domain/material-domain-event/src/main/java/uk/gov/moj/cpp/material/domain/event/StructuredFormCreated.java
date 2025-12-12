package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.material.domain.StructuredFormStatus;
import uk.gov.moj.cpp.material.domain.UpdatedBy;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@Event("material.events.structured-form-created")
public class StructuredFormCreated implements Serializable {
    private static final long serialVersionUID = 6266060005598657240L;

    private final UUID structuredFormId;

    private final UUID formId;

    private final String structuredFormData;

    private final UpdatedBy updatedBy;

    private final StructuredFormStatus status;

    private final ZonedDateTime lastUpdated;

    public StructuredFormCreated(final UUID structuredFormId, final UUID formId, final String structuredFormData, final UpdatedBy updatedBy, final StructuredFormStatus status, final ZonedDateTime lastUpdated) {
        this.formId = formId;
        this.structuredFormData = structuredFormData;
        this.structuredFormId = structuredFormId;
        this.updatedBy = updatedBy;
        this.status = status;
        this.lastUpdated = lastUpdated;
    }

    public UUID getFormId() {
        return formId;
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

    public StructuredFormStatus getStatus() {
        return status;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public static Builder structuredFormCreated() {
        return new Builder();
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {
        private UUID formId;

        private String structuredFormData;

        private UUID structuredFormId;

        private UpdatedBy updatedBy;

        private StructuredFormStatus status;

        private ZonedDateTime lastUpdated;

        public Builder withFormId(final UUID formId) {
            this.formId = formId;
            return this;
        }

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

        public Builder withStatus(final StructuredFormStatus status) {
            this.status = status;
            return this;
        }

        public Builder withLastUpdated(final ZonedDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder withValuesFrom(final StructuredFormCreated structuredFormCreated) {
            this.formId = structuredFormCreated.getFormId();
            this.structuredFormData = structuredFormCreated.getStructuredFormData();
            this.structuredFormId = structuredFormCreated.getStructuredFormId();
            this.updatedBy = structuredFormCreated.getUpdatedBy();
            this.status = structuredFormCreated.getStatus();
            this.lastUpdated = structuredFormCreated.getLastUpdated();
            return this;
        }

        public StructuredFormCreated build() {
            return new StructuredFormCreated(structuredFormId, formId, structuredFormData, updatedBy, status, lastUpdated);
        }
    }
}
