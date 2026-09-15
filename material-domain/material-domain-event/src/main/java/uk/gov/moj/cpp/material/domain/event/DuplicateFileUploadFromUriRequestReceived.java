package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("material.events.duplicate-file-upload-from-uri-request-received")
public class DuplicateFileUploadFromUriRequestReceived {
    private final UUID materialId;
    private final String fileUri;

    public DuplicateFileUploadFromUriRequestReceived(final UUID materialId, final String fileUri) {
        this.materialId = materialId;
        this.fileUri = fileUri;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getFileUri() {
        return fileUri;
    }
}
