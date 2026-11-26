package uk.gov.moj.cpp.material.persistence.entity;

import static java.util.Objects.hash;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "material_upload_status")
@Access(value = AccessType.FIELD)
public class MaterialUploadStatus {

    @Id
    @Column(name = "material_id")
    private UUID materialId;

    @Column(name = "file_service_id")
    private UUID fileServiceId;

    @Column(name = "status")
    private String status;

    @Column(name = "failed_time")
    private ZonedDateTime failedTime;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "last_modified")
    private ZonedDateTime lastModified;

    public MaterialUploadStatus() {
        //default constructor
    }

    public MaterialUploadStatus(final UUID materialId, final UUID fileServiceId, final String status, final ZonedDateTime failedTime, final String errorMessage, final ZonedDateTime lastModified) {
        this.materialId = materialId;
        this.fileServiceId = fileServiceId;
        this.status = status;
        this.failedTime = failedTime;
        this.errorMessage = errorMessage;
        this.lastModified = lastModified;
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

    public String getStatus() {
        return status;
    }

    public ZonedDateTime getLastModified() {
        return lastModified;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setFailedTime(final ZonedDateTime failedTime) {
        this.failedTime = failedTime;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setLastModified(final ZonedDateTime lastModified) {
        this.lastModified = lastModified;
    }

    @SuppressWarnings("squid:S1067")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MaterialUploadStatus that = (MaterialUploadStatus) o;
        return Objects.equals(materialId, that.materialId) &&
                Objects.equals(fileServiceId, that.fileServiceId) &&
                Objects.equals(status, that.status) &&
                Objects.equals(failedTime, that.failedTime) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(lastModified, that.lastModified);
    }

    @Override
    public int hashCode() {
        return hash(materialId, fileServiceId, status, failedTime, errorMessage, lastModified);
    }
}