package uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;

import uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle.BundleErrorType;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.lang3.builder.HashCodeBuilder;

@SuppressWarnings("squid:S2384")
public class FailedBundleUploadJobData {

    private final UUID bundledMaterialId;
    private final List<UUID> materialIds;
    private final Optional<UUID> fileServiceId;
    private final JsonObject eventMetadata;
    private final BundleErrorType errorType;
    private final String errorMessage;
    private final ZonedDateTime failedTime;

    public FailedBundleUploadJobData(
            final UUID bundledMaterialId,
            final List<UUID> materialIds,
            final Optional<UUID> fileServiceId,
            final JsonObject eventMetadata,
            final BundleErrorType errorType,
            final String errorMessage,
            final ZonedDateTime failedTime) {
        this.bundledMaterialId = bundledMaterialId;
        this.materialIds = materialIds;
        this.fileServiceId = fileServiceId;
        this.eventMetadata = eventMetadata;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.failedTime = failedTime;
    }

    public UUID getBundledMaterialId() {
        return bundledMaterialId;
    }

    public List<UUID> getMaterialIds() {
        return materialIds;
    }

    public Optional<UUID> getFileServiceId() {
        return fileServiceId;
    }

    public JsonObject getEventMetadata() {
        return eventMetadata;
    }

    public BundleErrorType getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ZonedDateTime getFailedTime() {
        return failedTime;
    }

    @Override
    public boolean equals(final Object object) {
        return reflectionEquals(this, object);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(bundledMaterialId).append(materialIds).append(fileServiceId).append(errorMessage).append(failedTime).toHashCode();
    }

    @Override
    public String toString() {
        return "FailedBundleUploadJobData{" +
                "bundledMaterialId=" + bundledMaterialId +
                ", materialIds=" + materialIds +
                ", fileServiceId=" + fileServiceId +
                ", eventMetadata=" + eventMetadata +
                ", errorType=" + errorType +
                ", errorMessage='" + errorMessage + '\'' +
                ", failedTime=" + failedTime +
                '}';
    }
}
