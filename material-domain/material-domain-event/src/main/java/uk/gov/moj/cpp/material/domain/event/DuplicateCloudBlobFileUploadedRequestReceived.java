package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("material.events.duplicate-cloud-file-upload-request-received")
public class DuplicateCloudBlobFileUploadedRequestReceived {
    private final UUID materialId;
    private final String fileCloudLocation;

    public DuplicateCloudBlobFileUploadedRequestReceived(final UUID materialId, final String fileCloudLocation) {
        this.materialId = materialId;
        this.fileCloudLocation = fileCloudLocation;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getFileCloudLocation() {
        return fileCloudLocation;
    }
}
