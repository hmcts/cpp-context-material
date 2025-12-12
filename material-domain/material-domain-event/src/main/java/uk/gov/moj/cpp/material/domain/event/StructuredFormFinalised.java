package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.material.domain.UpdatedBy;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@Event("material.events.structured-form-finalised")
public class StructuredFormFinalised implements Serializable {
    private static final long serialVersionUID = 7129860470713712730L;

    private final UUID structuredFormId;

    private final UUID materialId;

    private final UpdatedBy updatedBy;

    private final ZonedDateTime lastUpdated;

    public StructuredFormFinalised(final UUID structuredFormId, final UUID materialId, final UpdatedBy updatedBy, final ZonedDateTime lastUpdated) {
        this.structuredFormId = structuredFormId;
        this.materialId = materialId;
        this.updatedBy = updatedBy;
        this.lastUpdated = lastUpdated;
    }

    public UUID getMaterialId() {
        return materialId;
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

    public static Builder structuredFormFinalised() {
        return new Builder();
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {
        private UUID structuredFormId;
        private UUID materialId;
        private UpdatedBy updatedBy;
        private ZonedDateTime lastUpdated;

        public Builder withStructuredFormId(final UUID structuredFormId) {
            this.structuredFormId = structuredFormId;
            return this;
        }

        public Builder withMaterialId(final UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public Builder withUpdatedBy(final UpdatedBy userId) {
            this.updatedBy = userId;
            return this;
        }

        public Builder withLastUpdated(final ZonedDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder withValuesFrom(final StructuredFormFinalised petFormUpdated) {
            this.structuredFormId = petFormUpdated.getStructuredFormId();
            this.materialId = petFormUpdated.getMaterialId();
            this.updatedBy = petFormUpdated.getUpdatedBy();
            return this;
        }

        public StructuredFormFinalised build() {
            return new StructuredFormFinalised(structuredFormId, materialId, updatedBy, lastUpdated);
        }
    }
}
