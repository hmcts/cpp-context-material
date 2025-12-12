package uk.gov.moj.cpp.material.query.view.response;

import uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus;

import java.util.UUID;

public class StructuredFormChangeHistoryResponse {
    private UUID id;
    private UUID structuredFormId;
    private UUID formId;
    private UUID materialId;
    private String date;
    private UpdatedBy updatedBy;
    private String data;
    private StructuredFormStatus status;

    @SuppressWarnings("java:S107")
    private StructuredFormChangeHistoryResponse(UUID id, UUID structuredFormId, UUID formId, UUID materialId, String date, UpdatedBy updatedBy, String data, StructuredFormStatus status) {
        this.id = id;
        this.structuredFormId = structuredFormId;
        this.formId = formId;
        this.materialId = materialId;
        this.date = date;
        this.updatedBy = updatedBy;
        this.data = data;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStructuredFormId() {
        return structuredFormId;
    }

    public UUID getFormId() {
        return formId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getDate() {
        return date;
    }

    public UpdatedBy getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final UpdatedBy updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getData() {
        return data;
    }

    public StructuredFormStatus getStatus() {
        return status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private UUID structuredFormId;
        private UUID formId;
        private UUID materialId;
        private String date;
        private UpdatedBy updatedBy;
        private String data;
        private StructuredFormStatus status;

        private Builder() {
        }

        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withStructuredFormId(UUID structuredFormId) {
            this.structuredFormId = structuredFormId;
            return this;
        }

        public Builder withFormId(UUID formId) {
            this.formId = formId;
            return this;
        }

        public Builder withMaterialId(UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public Builder withDate(String date) {
            this.date = date;
            return this;
        }

        public Builder withUpdatedBy(UpdatedBy updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public Builder withData(String data) {
            this.data = data;
            return this;
        }

        public Builder withStatus(StructuredFormStatus status) {
            this.status = status;
            return this;
        }

        public StructuredFormChangeHistoryResponse build() {
            return new StructuredFormChangeHistoryResponse(id, structuredFormId, formId, materialId, date, updatedBy, data, status);
        }
    }
}
