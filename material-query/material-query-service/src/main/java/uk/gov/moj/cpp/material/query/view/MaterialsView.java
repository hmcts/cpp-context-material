package uk.gov.moj.cpp.material.query.view;

import java.util.Map;

public class MaterialsView {

    private final Map<String, Boolean> materials;

    public MaterialsView(final Map<String, Boolean> materials) {
        this.materials = materials;
    }

    public Map<String, Boolean> getMaterials() {
        return materials;
    }
}
