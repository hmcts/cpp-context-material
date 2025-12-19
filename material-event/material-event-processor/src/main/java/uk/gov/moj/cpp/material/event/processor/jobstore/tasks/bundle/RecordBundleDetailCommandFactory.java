package uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class RecordBundleDetailCommandFactory {

    private static final String BUNDLED_MATERIAL_ID = "bundledMaterialId";
    private static final String FILE_SIZE = "fileSize";
    private static final String PAGE_COUNT = "pageCount";
    public static final String FILE_NAME = "fileName";
    private static final String FILE_REFERENCE = "fileReference";
    static final String MATERIAL_ID_ARRAY = "materialIds";
    private static final String MIME_TYPE = "mimeType";
    static final String ERROR_CODE = "errorCode";
    static final String ERROR_MESSAGE = "errorMessage";
    static final String FAILED_TIME = "failedTime";
    static final String FILE_SERVICE_ID = "fileServiceId";

    static final String MATERIAL_COMMAND_HANDLER_RECORD_BUNDLE_DETAILS = "material.command.handler.record-bundle-details";
    static final String MATERIAL_COMMAND_HANDLER_RECORD_BUNDLE_DETAILS_FAILURE = "material.command.handler.record-bundle-details-failure";


    public JsonEnvelope recordBundleCommand(UUID bundledMaterialId, String bundledMaterialName,
                                            UUID alfrescoFileId, String mediaType,
                                            Long fileSize, int pageCount, JsonObject eventMetadata) {

        final JsonObject recordBundleDetailCommandPayload = createObjectBuilder()
                .add(BUNDLED_MATERIAL_ID, bundledMaterialId.toString())
                .add(FILE_REFERENCE, alfrescoFileId.toString())
                .add(MIME_TYPE, mediaType)
                .add(FILE_NAME, bundledMaterialName)
                .add(FILE_SIZE, fileSize.toString())
                .add(PAGE_COUNT, pageCount)
                .build();

        return envelopeFrom(
                metadataFrom(eventMetadata)
                        .withName(MATERIAL_COMMAND_HANDLER_RECORD_BUNDLE_DETAILS),
                recordBundleDetailCommandPayload);
    }

    public JsonEnvelope recordBundleFailedCommand(UUID bundledMaterialId, List<UUID> materialIds, Optional<UUID> fileServiceId,
                                                  JsonObject eventMetadata, BundleErrorType errorType,
                                                  String errorMessage, ZonedDateTime failedTime) {

        final JsonArrayBuilder materialIdsJsonArray = createArrayBuilder();
        materialIds.forEach(m -> materialIdsJsonArray.add(m.toString()));

        final JsonObjectBuilder recordBundleDetailCommandBuilder = createObjectBuilder()
                .add(BUNDLED_MATERIAL_ID, bundledMaterialId.toString())
                .add(MATERIAL_ID_ARRAY, materialIdsJsonArray.build())
                .add(ERROR_CODE, errorType.name())
                .add(ERROR_MESSAGE, errorMessage)
                .add(FAILED_TIME, ZonedDateTimes.toString(failedTime));
        fileServiceId.ifPresent(id -> recordBundleDetailCommandBuilder.add(FILE_SERVICE_ID, id.toString()));

        return envelopeFrom(
                metadataFrom(eventMetadata)
                        .withName(MATERIAL_COMMAND_HANDLER_RECORD_BUNDLE_DETAILS_FAILURE),
                recordBundleDetailCommandBuilder.build());
    }
}
