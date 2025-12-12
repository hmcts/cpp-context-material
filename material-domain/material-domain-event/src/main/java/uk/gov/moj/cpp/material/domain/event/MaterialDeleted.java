package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

@Event("material.events.material-deleted")
public class MaterialDeleted {

    private final UUID materialId;
    private final String alfrescoId;
    private final UUID fileServiceId;

    public MaterialDeleted(final UUID materialId, final String alfrescoId, final UUID fileServiceId) {
        this.materialId = materialId;
        this.alfrescoId = alfrescoId;
        this.fileServiceId = fileServiceId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getAlfrescoId() {
        return alfrescoId;
    }

    public UUID getFileServiceId() {
        return fileServiceId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MaterialDeleted that = (MaterialDeleted) o;
        return Objects.equals(materialId, that.materialId) &&
                Objects.equals(alfrescoId, that.alfrescoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(materialId, alfrescoId);
    }

    @Override
    public String toString() {
        return "MaterialDeleted{" +
                "materialId=" + materialId +
                ", alfrescoId='" + alfrescoId + '\'' +
                '}';
    }
}
