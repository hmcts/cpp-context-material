package uk.gov.moj.cpp.material.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StructuredFormStatus {
    DRAFTED("DRAFTED"),
    CREATED("CREATED"),
    PUBLISHED("PUBLISHED");

    private final String value;

    @JsonCreator
    StructuredFormStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static StructuredFormStatus valueFor(final String value) {
        if (DRAFTED.value.equals(value)) {
            return DRAFTED;
        }
        if (CREATED.value.equals(value)) {
            return CREATED;
        }
        if (PUBLISHED.value.equals(value)) {
            return PUBLISHED;
        }
        return null;
    }
}
