package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("material.events.duplicate-file-upload-request-received")
public class DuplicateFileUploadRequestReceived {
    private final UUID materialId;
    private final UUID fileServiceId;

    public DuplicateFileUploadRequestReceived(final UUID materialId, final UUID fileServiceId) {
        this.materialId = materialId;
        this.fileServiceId = fileServiceId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getFileServiceId() {
        return fileServiceId;
    }
}
