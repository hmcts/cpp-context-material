package uk.gov.moj.cpp.material.domain.event;


import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("material.events.cloud-blob-file-uploaded")
public class CloudBlobFileUploaded {

    private final UUID materialId;
    private final String fileCloudLocation;
    @JsonProperty("isUnbundledDocument")
    private final Boolean isUnbundledDocument = false;

    @JsonCreator
    public CloudBlobFileUploaded(
            @JsonProperty("materialId") final UUID materialId,
            @JsonProperty("fileCloudLocation") final String fileCloudLocation
            ) {
        this.materialId = materialId;
        this.fileCloudLocation = fileCloudLocation;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getFileCloudLocation() {
        return fileCloudLocation;
    }

    public Boolean getIsUnbundledDocument() {
        return isUnbundledDocument;
    }
}
