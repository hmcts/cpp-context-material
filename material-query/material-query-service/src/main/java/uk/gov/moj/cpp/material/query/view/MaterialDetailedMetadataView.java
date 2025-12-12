package uk.gov.moj.cpp.material.query.view;

import java.util.UUID;

public class MaterialDetailedMetadataView {

    private final UUID materialId;
    private final String alfrescoAssetId;
    private final String fileName;
    private final int fileSize;

    public MaterialDetailedMetadataView(final int fileSize, final String fileName, final UUID materialId, final String alfrescoAssetId) {
        this.fileSize = fileSize;
        this.fileName = fileName;
        this.materialId = materialId;
        this.alfrescoAssetId = alfrescoAssetId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getAlfrescoAssetId() {
        return alfrescoAssetId;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileSize() {
        return fileSize;
    }


    public static class MaterialDetailedMetadataViewBuilder {
        private UUID materialId;
        private String alfrescoAssetId;
        private String fileName;
        private int fileSize;

        private MaterialDetailedMetadataViewBuilder() {
        }

        public static MaterialDetailedMetadataViewBuilder materialDetailedMetadataViewBuilder() {
            return new MaterialDetailedMetadataViewBuilder();
        }

        public MaterialDetailedMetadataViewBuilder withMaterialId(final UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public MaterialDetailedMetadataViewBuilder withAlfrescoFileId(final String alfrescoAssetId) {
            this.alfrescoAssetId = alfrescoAssetId;
            return this;
        }

        public MaterialDetailedMetadataViewBuilder withFileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public MaterialDetailedMetadataViewBuilder withFileSize(final int fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public MaterialDetailedMetadataView build() {
            return new MaterialDetailedMetadataView(
                    fileSize,
                    fileName,
                    materialId,
                    alfrescoAssetId
            );
        }
    }
}
