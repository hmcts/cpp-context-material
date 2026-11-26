package uk.gov.moj.cpp.material.persistence.entity;

import static javax.persistence.EnumType.STRING;

import uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "structured_form")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class StructuredForm implements Serializable {
    private static final long serialVersionUID = 2801203406435125940L;

    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "form_id")
    private UUID formId;
    @Column(name = "data")
    private String data;
    @Column(name = "status")
    @Enumerated(STRING)
    private StructuredFormStatus status;
    @Column(name = "last_updated")
    private ZonedDateTime lastUpdated;

    public StructuredForm() {
    }

    public StructuredForm(UUID id, UUID formId, String data, StructuredFormStatus status, ZonedDateTime lastUpdated) {
        this.id = id;
        this.formId = formId;
        this.data = data;
        this.status = status;
        this.lastUpdated = lastUpdated;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID structuredFormId) {
        this.id = structuredFormId;
    }

    public UUID getFormId() {
        return formId;
    }

    public void setFormId(UUID formId) {
        this.formId = formId;
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

    public void setStatus(final StructuredFormStatus status) {
        this.status = status;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(final ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final StructuredForm structuredForm = (StructuredForm) o;
        return id.equals(structuredForm.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static StructuredForm.Builder builder() {
        return new StructuredForm.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID formId;
        private String data;
        private StructuredFormStatus status;
        private ZonedDateTime lastUpdated;

        public StructuredForm.Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public StructuredForm.Builder withFormId(final UUID formId) {
            this.formId = formId;
            return this;
        }

        public StructuredForm.Builder withData(final String data) {
            this.data = data;
            return this;
        }

        public StructuredForm.Builder withStatus(final StructuredFormStatus status) {
            this.status = status;
            return this;
        }

        public StructuredForm.Builder withLastUpdated(final ZonedDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public StructuredForm build() {
            return new StructuredForm(id, formId, data, status, lastUpdated);
        }
    }
}
