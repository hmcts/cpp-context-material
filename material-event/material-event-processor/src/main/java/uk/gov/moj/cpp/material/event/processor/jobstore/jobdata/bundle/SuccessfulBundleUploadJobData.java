package uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle;

import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;

import java.util.UUID;

import javax.json.JsonObject;

public class SuccessfulBundleUploadJobData extends SuccessfulMaterialUploadJobData {

    private Long fileSize;
    private int pageCount;

    public SuccessfulBundleUploadJobData(UUID materialId, UUID fileServiceId, UUID alfrescoFileId,
                                         boolean unbundledDocument, String fileName, String mediaType,
                                         Long fileSize, int pageCount,
                                         JsonObject fileUploadedEventMetadata) {
        super(materialId, fileServiceId, "", alfrescoFileId, unbundledDocument, fileName, mediaType, fileUploadedEventMetadata);
        this.fileSize = fileSize;
        this.pageCount = pageCount;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public int getPageCount() {
        return pageCount;
    }
}
