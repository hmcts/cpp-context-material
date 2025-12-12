package uk.gov.moj.cpp.material.persistence.entity;


import static javax.persistence.EnumType.STRING;

import uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "structured_form_change_history")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class StructuredFormChangeHistory {
    private static final long serialVersionUID = 3193931477424914096L;

    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "structured_form_id")
    private UUID structuredFormId;
    @Column(name = "form_id")
    private UUID formId;
    @Column(name = "material_id")
    private UUID materialId;
    @Column(name = "date")
    private ZonedDateTime date;
    @Column(name = "updated_by")
    private String updatedBy;
    @Column(name = "data")
    private String data;
    @Column(name = "status")
    @Enumerated(STRING)
    private StructuredFormStatus status;

    public StructuredFormChangeHistory() {
    }

    @SuppressWarnings("java:S107")
    public StructuredFormChangeHistory(final UUID id, final UUID structuredFormId, final UUID formId, final UUID materialId, final ZonedDateTime date, final String updatedBy, final String data, final StructuredFormStatus status) {
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

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getStructuredFormId() {
        return structuredFormId;
    }

    public void setStructuredFormId(UUID structuredFormId) {
        this.structuredFormId = structuredFormId;
    }

    public UUID getFormId() {
        return formId;
    }

    public void setFormId(UUID formId) {
        this.formId = formId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(UUID materialId) {
        this.materialId = materialId;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public StructuredFormStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final StructuredFormChangeHistory structuredFormChangeHistory = (StructuredFormChangeHistory) o;
        return id.equals(structuredFormChangeHistory.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static StructuredFormChangeHistory.Builder builder() {
        return new StructuredFormChangeHistory.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID structuredFormId;
        private UUID formId;
        private UUID materialId;
        private ZonedDateTime date;
        private String updatedBy;
        private String data;
        private StructuredFormStatus status;

        public StructuredFormChangeHistory.Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public StructuredFormChangeHistory.Builder withStructuredFormId(final UUID structuredFormId) {
            this.structuredFormId = structuredFormId;
            return this;
        }

        public StructuredFormChangeHistory.Builder withFormId(final UUID formId) {
            this.formId = formId;
            return this;
        }

        public StructuredFormChangeHistory.Builder withMaterialId(final UUID materialId) {
            this.materialId = materialId;
            return this;
        }


        public StructuredFormChangeHistory.Builder withDate(final ZonedDateTime date) {
            this.date = date;
            return this;
        }

        public StructuredFormChangeHistory.Builder withUpdatedBy(final String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public StructuredFormChangeHistory.Builder withData(final String data) {
            this.data = data;
            return this;
        }

        public StructuredFormChangeHistory.Builder withStatus(final StructuredFormStatus status) {
            this.status = status;
            return this;
        }

        public StructuredFormChangeHistory build() {
            return new StructuredFormChangeHistory(id, structuredFormId, formId, materialId, date, updatedBy, data, status);
        }
    }
}
