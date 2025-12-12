package uk.gov.moj.cpp.material.command.handler.alfresco;

import java.util.Objects;

public class AlfrescoStatus {

    private final Integer code;
    private final String name;
    private final String description;

    public AlfrescoStatus(final Integer code, final String name, final String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AlfrescoStatus that = (AlfrescoStatus) o;
        return Objects.equals(getCode(), that.getCode()) &&
                Objects.equals(getName(), that.getName()) &&
                Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCode(), getName(), getDescription());
    }
}
