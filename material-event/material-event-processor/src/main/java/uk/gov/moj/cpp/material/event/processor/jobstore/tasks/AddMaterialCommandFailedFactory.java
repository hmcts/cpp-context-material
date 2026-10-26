package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.FailedMaterialUploadJobData;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class AddMaterialCommandFailedFactory {

    private static final String RECORD_ADD_MATERIAL_FAILED_COMMAND_NAME = "material.record-add-material-failed";

    public JsonEnvelope createCommandEnvelope(final FailedMaterialUploadJobData failedMaterialUploadJobData) {

        final JsonObject fileUploadedEventMetadata = failedMaterialUploadJobData.getFileUploadedEventMetadata();

        final JsonObjectBuilder recordAddMaterialFailedCommandPayload = createObjectBuilder()
                .add("materialId", failedMaterialUploadJobData.getMaterialId().toString())
                .add("errorMessage", failedMaterialUploadJobData.getErrorMessage())
                .add("failedTime", ZonedDateTimes.toString(failedMaterialUploadJobData.getFailedTime()));

        // only file service uploads have a fileServiceId; blob (fileCloudLocation / fileUri) uploads do not
        if (nonNull(failedMaterialUploadJobData.getFileServiceId())) {
            recordAddMaterialFailedCommandPayload.add("fileServiceId", failedMaterialUploadJobData.getFileServiceId().toString());
        }

        return envelopeFrom(
                metadataFrom(fileUploadedEventMetadata).withName(RECORD_ADD_MATERIAL_FAILED_COMMAND_NAME),
                recordAddMaterialFailedCommandPayload.build());
    }
}
