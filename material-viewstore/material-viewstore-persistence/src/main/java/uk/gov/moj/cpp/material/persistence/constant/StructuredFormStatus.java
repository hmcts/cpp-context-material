package uk.gov.moj.cpp.material.persistence.constant;

public enum StructuredFormStatus {
    DRAFTED("DRAFTED"),
    CREATED("CREATED"),
    UPDATED("UPDATED"),
    PUBLISHED("PUBLISHED"),
    FINALISED("FINALISED");

    private final String value;

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
        if (UPDATED.value.equals(value)) {
            return UPDATED;
        }
        if (PUBLISHED.value.equals(value)) {
            return PUBLISHED;
        }
        if (FINALISED.value.equals(value)) {
            return FINALISED;
        }
        return null;
    }
}
