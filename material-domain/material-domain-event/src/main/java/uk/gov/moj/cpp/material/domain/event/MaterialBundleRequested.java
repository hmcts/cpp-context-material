package uk.gov.moj.cpp.material.domain.event;


import uk.gov.justice.domain.annotation.Event;

import java.util.List;
import java.util.UUID;

@Event("material.events.material-bundle-requested")
@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize"})
public class MaterialBundleRequested {

    private final UUID bundledMaterialId;
    private final List<UUID> materialIds;
    private final String bundledMaterialName;

    public MaterialBundleRequested(final UUID bundledMaterialId, final List<UUID> materialIds, final String bundledMaterialName) {
        this.bundledMaterialId = bundledMaterialId;
        this.materialIds = materialIds;
        this.bundledMaterialName = bundledMaterialName;
    }

    public UUID getBundledMaterialId() {
        return bundledMaterialId;
    }

    public List<UUID> getMaterialIds() {
        return materialIds;
    }

    public String getBundledMaterialName() {
        return bundledMaterialName;
    }

    public static class Builder {
        private UUID bundledMaterialId;
        private List<UUID> materialIds;
        private String bundledMaterialName;

        public Builder withBundledMaterialId(final UUID bundledMaterialId) {
            this.bundledMaterialId = bundledMaterialId;
            return this;
        }

        public Builder withBundledMaterialName(final String bundledMaterialName) {
            this.bundledMaterialName = bundledMaterialName;
            return this;
        }

        public Builder withMaterialIds(final List<UUID> materialIds) {
            this.materialIds = materialIds;
            return this;
        }

        public Builder withValuesFrom(final MaterialBundleRequested materialBundleCreated) {
            this.bundledMaterialId = materialBundleCreated.getBundledMaterialId();
            this.materialIds = materialBundleCreated.getMaterialIds();
            return this;
        }

        public MaterialBundleRequested build() {
            return new MaterialBundleRequested(bundledMaterialId, materialIds, bundledMaterialName);
        }
    }
}
