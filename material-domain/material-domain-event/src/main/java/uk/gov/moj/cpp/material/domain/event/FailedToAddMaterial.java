package uk.gov.moj.cpp.material.domain.event;


import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

@Event("material.events.failed-to-add-material")
public class FailedToAddMaterial {

    private final UUID materialId;
    private final UUID fileServiceId;
    private final ZonedDateTime failedTime;
    private final String errorMessage;

    public FailedToAddMaterial(final UUID materialId, final UUID fileServiceId, final ZonedDateTime failedTime, final String errorMessage) {
        this.materialId = materialId;
        this.fileServiceId = fileServiceId;
        this.failedTime = failedTime;
        this.errorMessage = errorMessage;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getFileServiceId() {
        return fileServiceId;
    }

    public ZonedDateTime getFailedTime() {
        return failedTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static class FailedToAddMaterialBuilder {
        private UUID materialId;
        private UUID fileServiceId;
        private ZonedDateTime failedTime;
        private String errorMessage;

        public FailedToAddMaterialBuilder withMaterialId(final UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public FailedToAddMaterialBuilder withFileServiceId(final UUID fileServiceId) {
            this.fileServiceId = fileServiceId;
            return this;
        }

        public FailedToAddMaterialBuilder withFailedTime(final ZonedDateTime failedTime) {
            this.failedTime = failedTime;
            return this;
        }

        public FailedToAddMaterialBuilder withErrorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public FailedToAddMaterialBuilder withValuesFrom(final FailedToAddMaterial failedToAddMaterial) {
            this.materialId = failedToAddMaterial.getMaterialId();
            this.fileServiceId = failedToAddMaterial.getFileServiceId();
            this.failedTime = failedToAddMaterial.getFailedTime();
            this.errorMessage = failedToAddMaterial.getErrorMessage();

            return this;
        }

        public FailedToAddMaterial build() {
            return new FailedToAddMaterial(materialId, fileServiceId, failedTime, errorMessage);
        }
    }
}
