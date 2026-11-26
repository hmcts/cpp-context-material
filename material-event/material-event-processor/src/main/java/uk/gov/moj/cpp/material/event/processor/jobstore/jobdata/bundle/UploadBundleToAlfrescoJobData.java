package uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.json.JsonObject;

@SuppressWarnings("squid:S2384")
public class UploadBundleToAlfrescoJobData {

    private final UUID bundledMaterialId;
    private final String bundledMaterialName;
    private final List<UUID> materialIds;
    private final UUID fileServiceId;
    private final Long fileSize;
    private final int pageCount;
    private final JsonObject eventMetadata;

    public UploadBundleToAlfrescoJobData(UUID bundledMaterialId, String bundledMaterialName, List<UUID> materialIds,
                                         UUID fileServiceId, Long fileSize, int pageCount,
                                         JsonObject eventMetadata) {

        this.bundledMaterialId = bundledMaterialId;
        this.bundledMaterialName = bundledMaterialName;
        this.materialIds = materialIds;
        this.fileServiceId = fileServiceId;
        this.fileSize = fileSize;
        this.pageCount = pageCount;
        this.eventMetadata = eventMetadata;
    }

    public UUID getBundledMaterialId() {
        return bundledMaterialId;
    }

    public String getBundledMaterialName() {
        return bundledMaterialName;
    }

    public List<UUID> getMaterialIds() {
        return materialIds;
    }

    public UUID getFileServiceId() {
        return fileServiceId;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public int getPageCount() {
        return pageCount;
    }

    public JsonObject getEventMetadata() {
        return eventMetadata;
    }

    @Override
    public boolean equals(Object object) {
        return reflectionEquals(this, object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bundledMaterialId, bundledMaterialName, materialIds, fileServiceId, fileSize, pageCount, eventMetadata);
    }

    public static class UploadBundleToAlfrescoJobDataBuilder {

        private UUID bundledMaterialId;
        private String bundledMaterialName;
        private List<UUID> materialIds;
        private UUID fileServiceId;
        private Long fileSize;
        private int pageCount;
        private JsonObject eventMetadata;

        public static UploadBundleToAlfrescoJobDataBuilder uploadBundleToAlfrescoJobData() {
            return new UploadBundleToAlfrescoJobDataBuilder();
        }

        public UploadBundleToAlfrescoJobDataBuilder withValuesFrom(final UploadBundleToAlfrescoJobData uploadBundleToAlfrescoJobData) {

            bundledMaterialId = uploadBundleToAlfrescoJobData.getBundledMaterialId();
            bundledMaterialName = uploadBundleToAlfrescoJobData.getBundledMaterialName();
            materialIds = uploadBundleToAlfrescoJobData.getMaterialIds();
            fileServiceId = uploadBundleToAlfrescoJobData.getFileServiceId();
            fileSize = uploadBundleToAlfrescoJobData.getFileSize();
            pageCount = uploadBundleToAlfrescoJobData.getPageCount();
            eventMetadata = uploadBundleToAlfrescoJobData.getEventMetadata();

            return this;
        }

        public UploadBundleToAlfrescoJobData build() {
            return new UploadBundleToAlfrescoJobData(
                    bundledMaterialId,
                    bundledMaterialName,
                    materialIds,
                    fileServiceId,
                    fileSize,
                    pageCount,
                    eventMetadata
            );
        }
    }
}
