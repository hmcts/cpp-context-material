package uk.gov.moj.cpp.material.domain.event;


import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("material.events.file-uploaded-as-pdf")
public class FileUploadedAsPdf {

    private final UUID materialId;
    private final UUID fileServiceId;
    private final Boolean isUnbundledDocument;

    @JsonCreator
    public FileUploadedAsPdf(
            @JsonProperty("materialId") final UUID materialId,
            @JsonProperty("fileServiceId") final UUID fileServiceId,
            @JsonProperty("isUnbundledDocument") final Boolean isUnbundledDocument) {
        this.materialId = materialId;
        this.fileServiceId = fileServiceId;
        this.isUnbundledDocument = isUnbundledDocument;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getFileServiceId() {
        return fileServiceId;
    }

    public Boolean getIsUnbundledDocument() {
        return isUnbundledDocument;
    }
}
