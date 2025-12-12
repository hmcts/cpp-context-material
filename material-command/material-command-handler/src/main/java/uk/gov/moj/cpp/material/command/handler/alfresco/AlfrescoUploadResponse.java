package uk.gov.moj.cpp.material.command.handler.alfresco;

import java.util.Objects;

public class AlfrescoUploadResponse {

    private final String nodeRef;
    private final String fileName;
    private final String fileMimeType;
    private final AlfrescoStatus status;

    public AlfrescoUploadResponse(final String nodeRef, final String fileName, final String fileMimeType, final AlfrescoStatus status) {
        this.nodeRef = nodeRef;
        this.fileName = fileName;
        this.fileMimeType = fileMimeType;
        this.status = status;
    }

    public String getNodeRef() {
        return nodeRef;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileMimeType() {
        return fileMimeType;
    }

    public AlfrescoStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AlfrescoUploadResponse that = (AlfrescoUploadResponse) o;
        return Objects.equals(getNodeRef(), that.getNodeRef()) &&
                Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getFileMimeType(), that.getFileMimeType()) &&
                Objects.equals(getStatus(), that.getStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNodeRef(), getFileName(), getFileMimeType(), getStatus());
    }
}
