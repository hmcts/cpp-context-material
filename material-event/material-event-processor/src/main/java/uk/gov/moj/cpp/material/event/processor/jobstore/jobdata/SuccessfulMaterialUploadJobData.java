package uk.gov.moj.cpp.material.event.processor.jobstore.jobdata;

import java.util.UUID;

import javax.json.JsonObject;

public class SuccessfulMaterialUploadJobData {

    private final UUID materialId;
    private final UUID fileServiceId;
    private final String fileCloudLocation;
    private final UUID alfrescoFileId;
    private final boolean unbundledDocument;
    private final String fileName;
    private final String mediaType;
    private final JsonObject fileUploadedEventMetadata;

    public SuccessfulMaterialUploadJobData(
            final UUID materialId,
            final UUID fileServiceId, String fileCloudLocation,
            final UUID alfrescoFileId,
            final boolean unbundledDocument,
            final String fileName,
            final String mediaType,
            final JsonObject fileUploadedEventMetadata) {
        this.materialId = materialId;
        this.fileServiceId = fileServiceId;
        this.fileCloudLocation = fileCloudLocation;
        this.alfrescoFileId = alfrescoFileId;
        this.unbundledDocument = unbundledDocument;
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.fileUploadedEventMetadata = fileUploadedEventMetadata;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getFileServiceId() {
        return fileServiceId;
    }

    public UUID getAlfrescoFileId() {
        return alfrescoFileId;
    }

    public boolean isUnbundledDocument() {
        return unbundledDocument;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getFileCloudLocation() {
        return fileCloudLocation;
    }

    public JsonObject getFileUploadedEventMetadata() {
        return fileUploadedEventMetadata;
    }

    public static class SuccessfulMaterialUploadJobDataBuilder {

        private UUID materialId;
        private UUID fileServiceId;
        private  String fileCloudLocation;
        private UUID alfrescoFileId;
        private boolean unbundledDocument;
        private String fileName;
        private String mediaType;
        private JsonObject fileUploadedEventMetadata;

        private SuccessfulMaterialUploadJobDataBuilder() {}

        public static SuccessfulMaterialUploadJobDataBuilder successfulMaterialUploadJobData() {
            return new SuccessfulMaterialUploadJobDataBuilder();
        }

        public SuccessfulMaterialUploadJobDataBuilder withMaterialId(final UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public SuccessfulMaterialUploadJobDataBuilder withFileServiceId(final UUID fileServiceId) {
            this.fileServiceId = fileServiceId;
            return this;
        }

        public SuccessfulMaterialUploadJobDataBuilder withAlfrescoFileId(final UUID alfrescoFileId) {
            this.alfrescoFileId = alfrescoFileId;
            return this;
        }

        public SuccessfulMaterialUploadJobDataBuilder withUnbundledDocument(final boolean unbundledDocument) {
            this.unbundledDocument = unbundledDocument;
            return this;
        }

        public SuccessfulMaterialUploadJobDataBuilder withFileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public SuccessfulMaterialUploadJobDataBuilder withFileCloudLocation(final String fileCloudLocation) {
            this.fileCloudLocation = fileCloudLocation;
            return this;
        }

        public SuccessfulMaterialUploadJobDataBuilder withMediaType(final String mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public SuccessfulMaterialUploadJobDataBuilder withFileUploadedEventMetadata(final JsonObject fileUploadedEventMetadata) {
            this.fileUploadedEventMetadata = fileUploadedEventMetadata;
            return this;
        }


        public SuccessfulMaterialUploadJobData build() {
            return new SuccessfulMaterialUploadJobData(
                    materialId,
                    fileServiceId,
                    fileCloudLocation,
                    alfrescoFileId,
                    unbundledDocument,
                    fileName,
                    mediaType,
                    fileUploadedEventMetadata);
        }

    }
}
