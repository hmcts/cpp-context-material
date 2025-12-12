package uk.gov.moj.cpp.material.command.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.material.command.handler.alfresco.AlfrescoUploadService;
import uk.gov.moj.cpp.material.command.handler.azure.service.AzureArchiveBlobClientService;
import uk.gov.moj.cpp.material.domain.FileDetails;
import uk.gov.moj.cpp.material.domain.UploadedMaterial;
import uk.gov.moj.cpp.material.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.material.domain.aggregate.Material;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1135", "squid:S1133"})
@ServiceComponent(COMMAND_HANDLER)
public class MaterialCommandHandler {

    private static final String MATERIAL_ID = "materialId";
    private static final String MATERIAL_IDS = "materialIds";
    private static final String BUNDLED_MATERIAL_ID = "bundledMaterialId";
    private static final String BUNDLED_MATERIAL_NAME = "bundledMaterialName";
    private static final String FILE_REFERENCE = "fileReference";
    private static final String MIME_TYPE = "mimeType";
    private static final String DOCUMENT_ROOT = "document";
    private static final String FILE_NAME = "fileName";
    public static final String FILE_SIZE = "fileSize";
    public static final String PAGE_COUNT = "pageCount";
    private static final String EXTERNAL_LINK = "externalLink";
    private static final String IS_UNBUNDLED_DOCUMENT = "isUnbundledDocument";
    private static final String FAILED_TIME = "failedTime";
    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String FILE_SERVICE_ID = "fileServiceId";
    private static final String FILE_CLOUD_LOCATION = "fileCloudLocation";

    @Inject
    private EventSource eventSource;

    @Inject
    private AlfrescoUploadService alfrescoUploadService;

    @Inject
    private AzureArchiveBlobClientService azureArchiveBlobClientService;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private UtcClock utcClock;

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialCommandHandler.class);

    @Handles("material.add-material")
    public void addMaterialReference(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID materialId = fromString(payload.getString(MATERIAL_ID));
        final Boolean isUnbundledDocument = payload.getBoolean(IS_UNBUNDLED_DOCUMENT, false);
        final String fileName = payload.getString(FILE_NAME);
        final JsonObject document = payload.getJsonObject(DOCUMENT_ROOT);
        final String fileReference = document.getString(FILE_REFERENCE);
        final String mimeType = document.getString(MIME_TYPE);

        final EventStream eventStream = eventSource.getStreamById(materialId);
        final Material material = aggregateService.get(eventStream, Material.class);

        final FileDetails fileDetails = new FileDetails(fileReference, mimeType, fileName);

        LOGGER.info("adding material for materialId={}", materialId);
        final Stream<Object> events = material.addFileReference(materialId, fileDetails, utcClock.now(), isUnbundledDocument);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    @Handles("material.record-add-material-failed")
    public void recordAddMaterialFailed(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID materialId = fromString(payload.getString(MATERIAL_ID));
        final UUID fileServiceId = fromString(payload.getString(FILE_SERVICE_ID));
        final ZonedDateTime failedTime = ZonedDateTimes.fromString(payload.getString(FAILED_TIME));
        final String errorMessage = payload.getString(ERROR_MESSAGE);

        final EventStream eventStream = eventSource.getStreamById(materialId);
        final Material material = aggregateService.get(eventStream, Material.class);

        final Stream<Object> events = material.recordAddMaterialFailed(materialId, fileServiceId, failedTime, errorMessage);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    @Handles("material.command.upload-file")
    public void uploadFile(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();

        final UUID materialId = fromString(payload.getString(MATERIAL_ID));
        final Boolean isUnbundledDocument = payload.getBoolean(IS_UNBUNDLED_DOCUMENT, false);

        final EventStream eventStream = eventSource.getStreamById(materialId);
        final Material material = aggregateService.get(eventStream, Material.class);

        Stream<Object> events = null;
        if (payload.containsKey(FILE_CLOUD_LOCATION) && !payload.isNull(FILE_CLOUD_LOCATION)) {
            events= material.uploadCloudBlobFile(materialId, payload.getString(FILE_CLOUD_LOCATION));
        }

        if (payload.containsKey(FILE_SERVICE_ID) && !payload.isNull(FILE_SERVICE_ID)) {
            events= material.uploadFile(materialId, fromString(payload.getString(FILE_SERVICE_ID)), isUnbundledDocument);
        }

        if(null != events){
            eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
        }else {
            LOGGER.info("Nothing to Upload");
        }


    }

    @Handles("material.command.upload-file-as-pdf")
    public void uploadFileAsPdf(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();

        final UUID materialId = fromString(payload.getString(MATERIAL_ID));
        final UUID fileServiceId = fromString(payload.getString(FILE_SERVICE_ID));
        final Boolean isUnbundledDocument = payload.getBoolean(IS_UNBUNDLED_DOCUMENT, false);

        final EventStream eventStream = eventSource.getStreamById(materialId);
        final Material material = aggregateService.get(eventStream, Material.class);

        final Stream<Object> events = material.uploadFileAsPdf(materialId, fileServiceId, isUnbundledDocument);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    /**
     * Deprecated. Should use the new command 'material.add-material' {@link
     * MaterialCommandHandler#addMaterialReference} instead.
     *
     * @deprecated TODO: remove this method and associated classes once other contexts are upgraded
     * to use the new command.
     */
    @Deprecated(since = "11.0.0")
    @Handles("material.command.add-material")
    public void addMaterial(final JsonEnvelope command) {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID materialId = fromString(payload.getString(MATERIAL_ID));

        final EventStream eventStream = eventSource.getStreamById(materialId);
        final Material material = aggregateService.get(eventStream, Material.class);
        @SuppressWarnings("squid:S3655") final String fileName = JsonObjects.getString(payload, FILE_NAME).get();

        try {
            final UploadedMaterial uploadedMaterial = alfrescoUploadService.uploadFile(payload);
            final Stream<Object> events = material.addUploadedFile(materialId, fileName, uploadedMaterial, utcClock.now());
            eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
        } catch (final IOException | EventStreamException ex) {
            throw new MaterialException(ex);
        }
    }

    @Handles("material.command.add-external-material")
    public void addExternalMaterial(final JsonEnvelope command) {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID materialId = fromString(payload.getString(MATERIAL_ID));

        final EventStream eventStream = eventSource.getStreamById(materialId);
        final Material material = aggregateService.get(eventStream, Material.class);
        @SuppressWarnings("squid:S3655") final String fileName = JsonObjects.getString(payload, FILE_NAME).get();

        final String externalLink = payload.getJsonObject(DOCUMENT_ROOT).getJsonString(EXTERNAL_LINK).getString();
        final Stream<Object> events = material.addExternalMaterial(materialId, fileName, externalLink, utcClock.now());

        try {
            eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
        } catch (final EventStreamException ex) {
            throw new MaterialException(ex);
        }
    }

    @Handles("material.command.handler.delete-material")
    public void deleteMaterial(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID materialId = fromString(payload.getString(MATERIAL_ID));

        final EventStream eventStream = eventSource.getStreamById(materialId);
        final Material material = aggregateService.get(eventStream, Material.class);

        final Stream<Object> events = material.deleteMaterial(materialId);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    @Handles("material.command.handler.create-material-bundle")
    public void createMaterialBundle(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID bundledMaterialId = fromString(payload.getString(BUNDLED_MATERIAL_ID));
        final List<UUID> materialIds = convertToList(payload.getJsonArray(MATERIAL_IDS));
        final String bundledMaterialName = payload.getString(BUNDLED_MATERIAL_NAME);

        final EventStream eventStream = eventSource.getStreamById(bundledMaterialId);
        final Material material = aggregateService.get(eventStream, Material.class);

        final Stream<Object> events = material.createMaterialBundle(bundledMaterialId, materialIds, bundledMaterialName);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    @Handles("material.command.handler.zip-material")
    public void createMaterialZip(final Envelope<ZipMaterial> command) throws EventStreamException {
        final ZipMaterial zipMaterial = command.payload();
        final EventStream eventStream = eventSource.getStreamById(zipMaterial.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        Stream<Object> events = null;
        try {
            azureArchiveBlobClientService.createAndUploadZip(zipMaterial);
        } catch (Exception e) {
            LOGGER.error("Create Material zip failed exception {} ", e);
            final String message = nonNull(e.getMessage()) ? e.getMessage() : "No Exception message available";
            events = caseAggregate.materialZipFailed(zipMaterial.getCaseId(), zipMaterial.getMaterialIds(), zipMaterial.getFileIds(), message);
        }

        if (isZippingAndUploadingSuccess(events)) {
            events = caseAggregate.materialZipped(zipMaterial.getCaseId(), zipMaterial.getCaseURN(), zipMaterial.getMaterialIds(), zipMaterial.getFileIds());
        }

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    private boolean isZippingAndUploadingSuccess(final Stream<Object> events) {
        return isNull(events);
    }

    @Handles("material.command.handler.record-bundle-details")
    public void recordBundleDetails(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID bundledMaterialId = fromString(payload.getString(BUNDLED_MATERIAL_ID));
        final String fileReference = payload.getString(FILE_REFERENCE);
        final String fileSize = payload.getString(FILE_SIZE);
        final int pageCount = payload.getInt(PAGE_COUNT);
        final String fileName = JsonObjects.getString(payload, FILE_NAME).orElse(null);
        final String mimeType = JsonObjects.getString(payload, MIME_TYPE).orElse(null);

        LOGGER.info("recordBundleDetails for bundledMaterialId={}", bundledMaterialId);

        final EventStream eventStream = eventSource.getStreamById(bundledMaterialId);
        final Material material = aggregateService.get(eventStream, Material.class);
        final FileDetails fileDetails = new FileDetails(fileReference, mimeType, fileName);
        final Stream<Object> events = material.recordBundleDetails(bundledMaterialId, fileDetails, fileSize, pageCount, utcClock.now());
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));

    }

    @Handles("material.command.handler.record-bundle-details-failure")
    public void recordBundleDetailsFailure(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID bundledMaterialId = fromString(payload.getString(BUNDLED_MATERIAL_ID));
        final Optional<UUID> fileServiceId = nonNull(payload.get(FILE_SERVICE_ID))
                ? Optional.of(fromString(payload.getString(FILE_SERVICE_ID))) : Optional.empty();
        final List<UUID> materialIds = convertToList(payload.getJsonArray(MATERIAL_IDS));
        final String errorCode = payload.getString(ERROR_CODE);
        final String errorMessage = payload.getString(ERROR_MESSAGE);
        final ZonedDateTime failedTime = ZonedDateTimes.fromString(payload.getString(FAILED_TIME));

        final EventStream eventStream = eventSource.getStreamById(bundledMaterialId);
        final Material material = aggregateService.get(eventStream, Material.class);

        final Stream<Object> events = material.recordBundleDetailsFailure(bundledMaterialId, materialIds, fileServiceId, errorCode, errorMessage, failedTime);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
    }

    private List<UUID> convertToList(final JsonArray jsonArray) {
        final List<UUID> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(UUID.fromString(jsonArray.getString(i)));
        }
        return list;
    }
}
