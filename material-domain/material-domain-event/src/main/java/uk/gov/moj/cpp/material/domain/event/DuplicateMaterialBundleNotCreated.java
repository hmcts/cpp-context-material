package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("material.events.duplicate-material-bundle-not-created")
public class DuplicateMaterialBundleNotCreated {
    private final UUID bundledMaterialId;
    private final String failedCommand;

    public DuplicateMaterialBundleNotCreated(UUID bundledMaterialId, String failedCommand) {
        this.bundledMaterialId = bundledMaterialId;
        this.failedCommand = failedCommand;
    }

    public UUID getBundledMaterialId() {
        return bundledMaterialId;
    }

    public String getFailedCommand() {
        return failedCommand;
    }
}
