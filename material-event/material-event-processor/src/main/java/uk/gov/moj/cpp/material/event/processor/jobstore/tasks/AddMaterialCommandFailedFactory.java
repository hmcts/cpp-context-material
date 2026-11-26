package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.FailedMaterialUploadJobData;

import javax.json.JsonObject;

public class AddMaterialCommandFailedFactory {

    private static final String RECORD_ADD_MATERIAL_FAILED_COMMAND_NAME = "material.record-add-material-failed";

    public JsonEnvelope createCommandEnvelope(final FailedMaterialUploadJobData failedMaterialUploadJobData) {

        final JsonObject fileUploadedEventMetadata = failedMaterialUploadJobData.getFileUploadedEventMetadata();

        final JsonObject recordAddMaterialFailedCommandPayload = createObjectBuilder()
                .add("materialId", failedMaterialUploadJobData.getMaterialId().toString())
                .add("fileServiceId", failedMaterialUploadJobData.getFileServiceId().toString())
                .add("errorMessage", failedMaterialUploadJobData.getErrorMessage())
                .add("failedTime", ZonedDateTimes.toString(failedMaterialUploadJobData.getFailedTime()))
                .build();



        return envelopeFrom(
                metadataFrom(fileUploadedEventMetadata).withName(RECORD_ADD_MATERIAL_FAILED_COMMAND_NAME),
                recordAddMaterialFailedCommandPayload);
    }
}
