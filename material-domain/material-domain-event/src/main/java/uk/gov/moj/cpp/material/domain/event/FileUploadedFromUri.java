package uk.gov.moj.cpp.material.domain.event;


import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("material.events.file-uploaded-from-uri")
public class FileUploadedFromUri {

    private final UUID materialId;
    private final String fileUri;
    private final Boolean isUnbundledDocument;

    @JsonCreator
    public FileUploadedFromUri(
            @JsonProperty("materialId") final UUID materialId,
            @JsonProperty("fileUri") final String fileUri,
            @JsonProperty("isUnbundledDocument") final Boolean isUnbundledDocument) {
        this.materialId = materialId;
        this.fileUri = fileUri;
        this.isUnbundledDocument = isUnbundledDocument;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getFileUri() {
        return fileUri;
    }

    public Boolean getIsUnbundledDocument() {
        return isUnbundledDocument;
    }
}
