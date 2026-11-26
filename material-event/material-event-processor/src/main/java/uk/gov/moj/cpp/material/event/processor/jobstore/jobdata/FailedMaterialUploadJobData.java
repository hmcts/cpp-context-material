package uk.gov.moj.cpp.material.event.processor.jobstore.jobdata;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.base.Objects;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class FailedMaterialUploadJobData {

    private final UUID materialId;
    private final UUID fileServiceId;
    private final String fileCloudLocation;
    private final JsonObject fileUploadedEventMetadata;
    private final String errorMessage;
    private final ZonedDateTime failedTime;

    public FailedMaterialUploadJobData(
            final UUID materialId,
            final UUID fileServiceId, String fileCloudLocation,
            final JsonObject fileUploadedEventMetadata,
            final String errorMessage,
            final ZonedDateTime failedTime) {
        this.materialId = materialId;
        this.fileServiceId = fileServiceId;
        this.fileCloudLocation = fileCloudLocation;
        this.fileUploadedEventMetadata = fileUploadedEventMetadata;
        this.errorMessage = errorMessage;
        this.failedTime = failedTime;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getFileServiceId() {
        return fileServiceId;
    }

    public String getFileCloudLocation() {
        return fileCloudLocation;
    }

    public JsonObject getFileUploadedEventMetadata() {
        return fileUploadedEventMetadata;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ZonedDateTime getFailedTime() {
        return failedTime;
    }

    @Override
    @SuppressWarnings("{squid:S00121, squid:S00122, squid:S1067}")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FailedMaterialUploadJobData)) {
            return false;
        }
        final FailedMaterialUploadJobData that = (FailedMaterialUploadJobData) o;
        return Objects.equal(materialId, that.materialId) && Objects.equal(fileServiceId, that.fileServiceId) && Objects.equal(fileUploadedEventMetadata, that.fileUploadedEventMetadata) && Objects.equal(errorMessage, that.errorMessage) && Objects.equal(failedTime, that.failedTime);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(materialId).append(fileServiceId).append(errorMessage).append(failedTime).toHashCode();
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public String toString() {
        return "FailedMaterialUploadJobData{" +
                "materialId=" + materialId +
                ", fileServiceId=" + fileServiceId +
                ", fileUploadedEventMetadata=" + fileUploadedEventMetadata +
                ", errorMessage='" + errorMessage + '\'' +
                ", failedTime=" + failedTime +
                '}';
    }
}
