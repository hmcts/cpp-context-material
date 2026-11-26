package uk.gov.moj.cpp.material.query.view.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

public class StructuredFormResponse {
    private UUID structuredFormId;
    private UUID formId;
    private List<StructuredFormDefendantResponse> defendants;
    private JsonObject data;

    private StructuredFormResponse(UUID structuredFormId, UUID formId, List<StructuredFormDefendantResponse> defendants, JsonObject data) {
        this.structuredFormId = structuredFormId;
        this.formId = formId;
        defendants = new ArrayList<>(defendants);
        this.defendants = Collections.unmodifiableList(defendants);
        this.data = data;
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

    public List<StructuredFormDefendantResponse> getDefendants() {
        return new ArrayList<>(defendants);
    }

    public void setDefendants(List<StructuredFormDefendantResponse> defendants) {
        defendants = new ArrayList<>(defendants);
        this.defendants = Collections.unmodifiableList(defendants);
    }

    public JsonObject getData() {
        return data;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID structuredFormId;
        private UUID formId;
        private List<StructuredFormDefendantResponse> defendants = Collections.emptyList();
        private JsonObject data;

        private Builder() {
        }

        public static Builder aStructuredFormResponse() {
            return new Builder();
        }

        public Builder withStructuredFormId(UUID structuredFormId) {
            this.structuredFormId = structuredFormId;
            return this;
        }

        public Builder withFormId(UUID formId) {
            this.formId = formId;
            return this;
        }

        public Builder withDefendants(List<StructuredFormDefendantResponse> defendants) {
            defendants = new ArrayList<>(defendants);
            this.defendants = Collections.unmodifiableList(defendants);
            return this;
        }

        public Builder withData(JsonObject data) {
            this.data = data;
            return this;
        }

        public StructuredFormResponse build() {
            return new StructuredFormResponse(structuredFormId, formId, defendants, data);
        }
    }
}
