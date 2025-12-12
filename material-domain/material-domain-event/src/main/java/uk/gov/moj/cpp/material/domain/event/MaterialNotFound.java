package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

@Event("material.events.material-not-found")
public class MaterialNotFound {

    private final UUID materialId;

    public MaterialNotFound(final UUID materialId) {
        this.materialId = materialId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MaterialNotFound that = (MaterialNotFound) o;
        return Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(materialId);
    }

    @Override
    public String toString() {
        return "MaterialNotFound{" +
                "materialId=" + materialId +
                '}';
    }
}
