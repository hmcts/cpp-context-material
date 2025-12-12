package uk.gov.moj.cpp.material.event.processor.jobstore.jobdata;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;

import java.util.Objects;
import java.util.UUID;

import javax.json.JsonObject;

public class UploadMaterialToAlfrescoJobData {

    private final UUID materialId;
    private final UUID fileServiceId;
    private final String cloudLocation;



    private final boolean unbundledDocument;
    private final JsonObject fileUploadedEventMetadata;

    public UploadMaterialToAlfrescoJobData(
            final UUID materialId,
            final UUID fileServiceId,
            final boolean unbundledDocument,
            final JsonObject fileUploadedEventMetadata, String cloudLocation) {
        this.materialId = materialId;
        this.fileServiceId = fileServiceId;
        this.unbundledDocument = unbundledDocument;
        this.fileUploadedEventMetadata = fileUploadedEventMetadata;
        this.cloudLocation = cloudLocation;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getFileServiceId() {
        return fileServiceId;
    }

    public String getCloudLocation() {
        return cloudLocation;
    }

    public boolean isUnbundledDocument() {
        return unbundledDocument;
    }

    public JsonObject getFileUploadedEventMetadata() {
        return fileUploadedEventMetadata;
    }

    @Override
    public String toString() {
        return "UploadMaterialToAlfrescoJobState{" +
                "materialId=" + materialId +
                ", fileServiceId=" + fileServiceId +
                ", unbundledDocument=" + unbundledDocument +
                ", fileUploadedEventMetadata=" + fileUploadedEventMetadata +
                '}';
    }

    @Override
    public boolean equals(final Object object) {
        return reflectionEquals(this, object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(materialId, fileServiceId, unbundledDocument, fileUploadedEventMetadata);
    }

    public static class UploadMaterialToAlfrescoJobDataBuilder {

        private UUID materialId;
        private UUID fileServiceId;
        private  String cloudLocation;
        private boolean unbundledDocument;
        private JsonObject fileUploadedEventMetadata;

        public static UploadMaterialToAlfrescoJobDataBuilder uploadMaterialToAlfrescoJobData() {
            return new UploadMaterialToAlfrescoJobDataBuilder();
        }

        public UploadMaterialToAlfrescoJobDataBuilder withValuesFrom(final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData) {

            materialId = uploadMaterialToAlfrescoJobData.getMaterialId();
            fileServiceId = uploadMaterialToAlfrescoJobData.getFileServiceId();
            unbundledDocument = uploadMaterialToAlfrescoJobData.isUnbundledDocument();
            fileUploadedEventMetadata = uploadMaterialToAlfrescoJobData.getFileUploadedEventMetadata();
            cloudLocation = uploadMaterialToAlfrescoJobData().build().getCloudLocation();

            return this;
        }

        public UploadMaterialToAlfrescoJobData build() {
            return new UploadMaterialToAlfrescoJobData(
                    materialId,
                    fileServiceId,
                    unbundledDocument,
                    fileUploadedEventMetadata,
                    cloudLocation
            );
        }
    }
}
