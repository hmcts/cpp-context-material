package uk.gov.moj.cpp.material.event.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.STARTED;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.MERGE_FILE_TASK;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.UPLOAD_FILE_TO_ALFRESCO_TASK;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.file.api.remover.FileRemover;
import uk.gov.justice.services.file.api.requester.FileRequester;
import uk.gov.justice.services.file.api.sender.FileData;
import uk.gov.justice.services.file.api.sender.FileSender;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.jobstore.api.ExecutionService;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.persistence.Priority;
import uk.gov.moj.cpp.material.domain.event.MaterialAdded;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleDetailsRecorded;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleRequested;
import uk.gov.moj.cpp.material.domain.event.MaterialBundlingFailed;
import uk.gov.moj.cpp.material.domain.event.MaterialDeleted;
import uk.gov.moj.cpp.material.domain.event.MaterialZipFailed;
import uk.gov.moj.cpp.material.domain.event.MaterialZipped;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.MergeFileJobData;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

@ServiceComponent(EVENT_PROCESSOR)
public class MaterialEventProcessor {

    public static final String MEDIA_TYPE_PDF = "application/pdf";
    public static final String EXTENSION_PDF = ".pdf";
    public static final String FILE_NAME = "fileName";
    public static final String MEDIA_TYPE = "mediaType";
    public static final String FILE_SERVICE_ID = "fileServiceId";
    public static final String FILE_CLOUD_LOCATION = "fileCloudLocation";
    public static final String FILE_REFERENCE = "fileReference";
    public static final String MIME_TYPE = "mimeType";
    public static final String DOCUMENT = "document";
    static final String PUBLIC_MATERIAL_MATERIAL_BUNDLE_CREATED = "public.material.material-bundle-created";
    static final String PUBLIC_MATERIAL_MATERIAL_BUNDLE_CREATION_FAILED = "public.material.material-bundle-creation-failed";
    private static final String MATERIAL_ADDED_PUB_EVENT = "material.material-added";
    private static final String MATERIAL_DELETED_PUB_EVENT = "public.material.material-deleted";
    private static final String DUPLICATE_MATERIAL_PUB_EVENT = "material.duplicate-material-not-created";
    private static final String MATERIAL_FIELD_NAME = "materialId";
    private static final String IS_UNBUNDLED_DOCUMENT = "isUnbundledDocument";
    private static final String PUBLIC_MATERIAL_NOT_FOUND = "public.material.material-not-found";
    private static final String PUBLIC_FAILED_TO_ADD_MATERIAL = "public.events.material.failed-to-add-material";
    private static final String ADD_MATERIAL_COMMAND_NAME = "material.add-material";

    private static final String PUBLIC_MATERIAL_MATERIAL_ZIPPED = "public.material.events.material-zipped";
    private static final String PUBLIC_MATERIAL_EVENTS_MATERIAL_ZIP_FAILED = "public.material.events.material-zip-failed";

    @Inject
    private FileRequester fileRequester;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private FileService fileService;

    @Inject
    private FileSender fileSender;

    @Inject
    private FileRemover fileRemover;

    @Inject
    private ExecutionService executionService;

    @Inject
    private UtcClock clock;

    @SuppressWarnings({"squid:S1312"})
    @Inject
    private Logger logger;

    @Inject
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Handles("material.events.material-added")
    public void handleMaterialAdded(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final String materialId = payload.getString(MATERIAL_FIELD_NAME);
        final boolean isUnbundledDocument = payload.getBoolean(IS_UNBUNDLED_DOCUMENT, false);
        logger.debug("Received Material added for materialId: {} , isUnbundled : {}", materialId, isUnbundledDocument);

        final MaterialAdded event = jsonObjectConverter.convert(payload, MaterialAdded.class);
        logger.debug("MaterialAdded event  is {} ", event);
        sender.send(envelop(event)
                .withName(MATERIAL_ADDED_PUB_EVENT)
                .withMetadataFrom(envelope));
    }

    @Handles("material.events.duplicate-material-not-created")
    public void handleDuplicateMaterialNotCreated(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final String materialId = payload.getString(MATERIAL_FIELD_NAME);
        logger.debug("Received duplicate Material added for materialId: {}", materialId);
        sender.send(envelop(payload)
                .withName(DUPLICATE_MATERIAL_PUB_EVENT)
                .withMetadataFrom(envelope));
    }

    @Handles("material.events.file-uploaded")
    @SuppressWarnings({"squid:S00112", "squid:S2629"})
    public void handleFileUploaded(final JsonEnvelope fileUploadedEvent) {

        fileUploaded(fileUploadedEvent);
    }

    private void fileUploaded(final JsonEnvelope fileUploadedEvent) {
        final JsonObject fileUploadedPayload = fileUploadedEvent.payloadAsJsonObject();

        final UUID materialId = fromString(fileUploadedPayload.getString(MATERIAL_FIELD_NAME));
        final UUID fileServiceId =  Optional.ofNullable(fileUploadedPayload.getString(FILE_SERVICE_ID, null)).map(UUID::fromString).orElse(null);

        final String fileCloudLocation = fileUploadedPayload.getString(FILE_CLOUD_LOCATION, null);
        final boolean isUnbundledDocument = fileUploadedPayload.getBoolean(IS_UNBUNDLED_DOCUMENT, false);


        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId,
                fileServiceId,
                isUnbundledDocument,
                fileUploadedEvent.metadata().asJsonObject(), fileCloudLocation);

        final ExecutionInfo executionInfo = new ExecutionInfo(
                objectToJsonObjectConverter.convert(uploadMaterialToAlfrescoJobData),
                UPLOAD_FILE_TO_ALFRESCO_TASK,
                clock.now(),
                STARTED,
                Priority.MEDIUM);

        executionService.executeWith(executionInfo);

        logger.info("Added Alfresco file upload to the jobstore: task '{}', materialId '{}'", UPLOAD_FILE_TO_ALFRESCO_TASK, materialId);
    }

    @Handles("material.events.cloud-blob-file-uploaded")
    @SuppressWarnings({"squid:S00112", "squid:S2629"})
    public void handleCloudBlobFileUploaded(final JsonEnvelope fileUploadedEvent) {

        fileUploaded(fileUploadedEvent);
    }

    @Handles("material.events.file-uploaded-as-pdf")
    @SuppressWarnings("squid:S00112")
    public void handleFileUploadedAsPdf(final JsonEnvelope fileUploadedEvent) throws Exception {
        final JsonObject fileUploadedPayload = fileUploadedEvent.payloadAsJsonObject();

        final UUID materialId = fromString(fileUploadedPayload.getString(MATERIAL_FIELD_NAME));
        final UUID fileServiceId = fromString(fileUploadedPayload.getString(FILE_SERVICE_ID));
        final boolean isUnbundledDocument = fileUploadedPayload.getBoolean(IS_UNBUNDLED_DOCUMENT, false);
        try (@SuppressWarnings("squid:S3655") final FileReference fileReference = fileService.retrieve(fileServiceId).get()) {
            String fileName = fileReference.getMetadata().getString(FILE_NAME);
            String mediaType = fileReference.getMetadata().getString(MEDIA_TYPE);
            //send original file to alfresco
            FileData fileData = fileSender.send(fileName, fileReference.getContentStream());
            //get back the file as pdf
            final Optional<InputStream> pdfDocument = fileRequester.requestPdf(fileData.fileId(), fileName);

            if (pdfDocument.isPresent()) {
                //send converted pdf file to alfresco
                fileName = FilenameUtils.getBaseName(fileName).concat(EXTENSION_PDF);
                mediaType = MEDIA_TYPE_PDF;
                fileData = fileSender.send(fileName, pdfDocument.get());
            }

            final JsonObject addMaterialCommandPayload = getAddMaterialCommandPayload(materialId, isUnbundledDocument, fileName, mediaType, fileData);

            sender.send(envelop(addMaterialCommandPayload)
                    .withName(ADD_MATERIAL_COMMAND_NAME)
                    .withMetadataFrom(fileUploadedEvent));
        }
    }

    private JsonObject getAddMaterialCommandPayload(final UUID materialId, final boolean isUnbundledDocument, final String fileName, final String mediaType, final FileData fileData) {
        final JsonObjectBuilder document = createObjectBuilder()
                .add(FILE_REFERENCE, fileData.fileId())
                .add(MIME_TYPE, mediaType);

        return createObjectBuilder()
                .add(MATERIAL_FIELD_NAME, materialId.toString())
                .add(FILE_NAME, fileName)
                .add(DOCUMENT, document)
                .add(IS_UNBUNDLED_DOCUMENT, isUnbundledDocument)
                .build();
    }

    @Handles("material.events.material-deleted")
    public void materialDeleted(final JsonEnvelope event) throws Exception {
        final MaterialDeleted materialDeleted = jsonObjectConverter.convert(
                event.payloadAsJsonObject(),
                MaterialDeleted.class);

        checkAndDeleteFromFileService(materialDeleted);
        fileRemover.remove(materialDeleted.getAlfrescoId());

        sender.send(envelop(materialDeleted)
                .withName(MATERIAL_DELETED_PUB_EVENT)
                .withMetadataFrom(event));
    }

    @Handles("material.events.material-not-found")
    public void materialNotFound(final JsonEnvelope event) {
        sender.send(envelop(event.payload())
                .withName(PUBLIC_MATERIAL_NOT_FOUND)
                .withMetadataFrom(event));
    }

    @Handles("material.events.failed-to-add-material")
    public void failedToAddMaterial(final JsonEnvelope event) {
        sender.send(envelop(event.payload())
                .withName(PUBLIC_FAILED_TO_ADD_MATERIAL)
                .withMetadataFrom(event));
    }

    private void checkAndDeleteFromFileService(final MaterialDeleted materialDeleted) throws Exception {
        if (nonNull(materialDeleted.getFileServiceId())) {
            final Optional<FileReference> fileReferenceOptional = fileService.retrieve(materialDeleted.getFileServiceId());
            if (fileReferenceOptional.isPresent()) {
                fileReferenceOptional.get().close();
                fileService.delete(materialDeleted.getFileServiceId());
            }
        }
    }

    @Handles("material.events.material-bundle-requested")
    public void materialBundleCreated(final Envelope<MaterialBundleRequested> event) {

        final MaterialBundleRequested materialBundleRequested = event.payload();
        final MergeFileJobData mergeFileJobData = new MergeFileJobData(
                materialBundleRequested.getBundledMaterialId(),
                materialBundleRequested.getBundledMaterialName(),
                materialBundleRequested.getMaterialIds(),
                event.metadata().asJsonObject());

        final ExecutionInfo executionInfo = new ExecutionInfo(
                objectToJsonObjectConverter.convert(mergeFileJobData),
                MERGE_FILE_TASK,
                clock.now(),
                STARTED,
                Priority.MEDIUM);

        executionService.executeWith(executionInfo);
    }

    @Handles("material.events.material-bundle-details-recorded")
    public void handleMaterialBundleDetailsRecorded(final Envelope<MaterialBundleDetailsRecorded> envelope) {

        final MaterialBundleDetailsRecorded materialBundleDetailsRecorded = envelope.payload();

        final JsonObjectBuilder document = createObjectBuilder()
                .add(FILE_REFERENCE, materialBundleDetailsRecorded.getFileDetails().getAlfrescoAssetId())
                .add(MIME_TYPE, materialBundleDetailsRecorded.getFileDetails().getMimeType());

        final JsonObject addMaterialCommandPayload = createObjectBuilder()
                .add(MATERIAL_FIELD_NAME, materialBundleDetailsRecorded.getMaterialId().toString())
                .add(FILE_NAME, materialBundleDetailsRecorded.getFileDetails().getFileName())
                .add(DOCUMENT, document)
                .add(IS_UNBUNDLED_DOCUMENT, false)
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(ADD_MATERIAL_COMMAND_NAME),
                addMaterialCommandPayload));


        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PUBLIC_MATERIAL_MATERIAL_BUNDLE_CREATED)
                .build();
        sender.send(envelopeFrom(metadata, materialBundleDetailsRecorded));
    }

    @Handles("material.events.material-bundling-failed")
    public void handleMaterialBundleCreationFailed(final Envelope<MaterialBundlingFailed> envelope) {

        final MaterialBundlingFailed materialBundlingFailed = envelope.payload();


        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PUBLIC_MATERIAL_MATERIAL_BUNDLE_CREATION_FAILED)
                .build();
        sender.send(envelopeFrom(metadata, materialBundlingFailed));
    }

    @Handles("material.events.material-zipped")
    public void handleMaterialZipped(final Envelope<MaterialZipped> envelope) {
        final MaterialZipped materialZipped = envelope.payload();
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PUBLIC_MATERIAL_MATERIAL_ZIPPED)
                .build();
        sender.send(envelopeFrom(metadata, materialZipped));
    }

    @Handles("material.events.failed-to-zip-material")
    public void handleMaterialZipFailed(final Envelope<MaterialZipFailed> envelope) {
        final MaterialZipFailed materialZipFailed = envelope.payload();
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PUBLIC_MATERIAL_EVENTS_MATERIAL_ZIP_FAILED)
                .build();
        sender.send(envelopeFrom(metadata, materialZipFailed));
    }

}
