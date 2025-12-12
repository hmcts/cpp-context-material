package uk.gov.moj.cpp.material.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("material.events.duplicate-material-not-created")
public class DuplicateMaterialNotCreated {
    private final UUID materialId;
    private final String failedCommand;

    public DuplicateMaterialNotCreated(UUID materialId, String failedCommand) {
        this.materialId = materialId;
        this.failedCommand = failedCommand;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getFailedCommand() {
        return failedCommand;
    }
}
