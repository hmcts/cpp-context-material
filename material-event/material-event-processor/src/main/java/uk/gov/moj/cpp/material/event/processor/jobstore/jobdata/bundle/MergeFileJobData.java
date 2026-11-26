package uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.json.JsonObject;

@SuppressWarnings("squid:S2384")
public class MergeFileJobData {

    private final UUID bundledMaterialId;
    private final String bundledMaterialName;
    private final List<UUID> materialIds;
    private final JsonObject eventMetadata;

    public MergeFileJobData(
            final UUID bundledMaterialId,
            final String bundledMaterialName,
            final List<UUID> materialIds,
            final JsonObject eventMetadata) {
        this.bundledMaterialId = bundledMaterialId;
        this.bundledMaterialName = bundledMaterialName;
        this.materialIds = materialIds;
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

    public JsonObject getEventMetadata() {
        return eventMetadata;
    }

    @Override
    public String toString() {
        return "MergeFileJobData{" +
                "bundledMaterialId=" + bundledMaterialId +
                ", bundledMaterialName='" + bundledMaterialName + '\'' +
                ", materialIds=" + materialIds +
                ", eventMetadata=" + eventMetadata +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        return reflectionEquals(this, object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bundledMaterialId, bundledMaterialName, materialIds, eventMetadata);
    }

    public static class MergeFileJobDataBuilder {

        private UUID bundledMaterialId;
        private String bundledMaterialName;
        private List<UUID> materialIds;
        private JsonObject eventMetadata;

        public static MergeFileJobDataBuilder mergeFileJobDataBuilder() {
            return new MergeFileJobDataBuilder();
        }

        public MergeFileJobDataBuilder withValuesFrom(final MergeFileJobData mergeFileJobData) {
            bundledMaterialId = mergeFileJobData.getBundledMaterialId();
            bundledMaterialName = mergeFileJobData.getBundledMaterialName();
            materialIds = mergeFileJobData.getMaterialIds();
            eventMetadata = mergeFileJobData.getEventMetadata();
            return this;
        }

        public MergeFileJobDataBuilder withBundledMaterialId(final UUID bundledMaterialId) {
            this.bundledMaterialId = bundledMaterialId;
            return this;
        }

        public MergeFileJobDataBuilder withBundledMaterialName(final String bundledMaterialName) {
            this.bundledMaterialName = bundledMaterialName;
            return this;
        }

        public MergeFileJobDataBuilder withMaterialIds(final List<UUID> materialIds) {
            this.materialIds = materialIds;
            return this;
        }

        public MergeFileJobDataBuilder withEventMetadata(final JsonObject eventMetadata) {
            this.eventMetadata = eventMetadata;
            return this;
        }

        public MergeFileJobData build() {
            return new MergeFileJobData(
                    bundledMaterialId,
                    bundledMaterialName,
                    materialIds,
                    eventMetadata
            );
        }
    }
}
