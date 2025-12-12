package uk.gov.moj.cpp.material.domain.event;


import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Event("material.events.material-bundling-failed")
@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize"})
public class MaterialBundlingFailed {

    private final UUID bundledMaterialId;
    private final List<UUID> materialIds;
    private Optional<UUID> fileServiceId;
    private final String errorType;
    private final String errorMessage;
    private final ZonedDateTime failedTime;

    public MaterialBundlingFailed(final UUID bundledMaterialId, final List<UUID> materialIds,
                                  final Optional<UUID> fileServiceId, final String errorType,
                                  final String errorMessage, final ZonedDateTime failedTime) {
        this.bundledMaterialId = bundledMaterialId;
        this.materialIds = materialIds;
        this.errorType = errorType;
        this.fileServiceId = fileServiceId;
        this.errorMessage = errorMessage;
        this.failedTime = failedTime;
    }

    public UUID getBundledMaterialId() {
        return bundledMaterialId;
    }

    public List<UUID> getMaterialIds() {
        return materialIds;
    }

    public Optional<UUID> getFileServiceId() {
        return fileServiceId;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ZonedDateTime getFailedTime() {
        return failedTime;
    }

}
