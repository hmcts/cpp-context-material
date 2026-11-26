package uk.gov.moj.cpp.material.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.domain.event.CloudBlobFileUploaded;
import uk.gov.moj.cpp.material.domain.event.FailedToAddMaterial;
import uk.gov.moj.cpp.material.domain.event.FileUploaded;
import uk.gov.moj.cpp.material.domain.event.MaterialAdded;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleDetailsRecorded;
import uk.gov.moj.cpp.material.domain.event.MaterialDeleted;
import uk.gov.moj.cpp.material.event.listener.converter.MaterialAddedToMaterialConverter;
import uk.gov.moj.cpp.material.event.listener.converter.MaterialBundleDetailsRecordedToMaterialConverter;
import uk.gov.moj.cpp.material.persistence.entity.Material;
import uk.gov.moj.cpp.material.persistence.entity.MaterialUploadStatus;
import uk.gov.moj.cpp.material.persistence.repository.MaterialRepository;
import uk.gov.moj.cpp.material.persistence.repository.MaterialUploadStatusRepository;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

@ServiceComponent(EVENT_LISTENER)
public class MaterialEventListener {

    private static final String UPLOAD_STATUS_QUEUED = "QUEUED";
    private static final String UPLOAD_STATUS_SUCCESS = "SUCCESS";
    private static final String UPLOAD_STATUS_FAILED = "FAILED";

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private MaterialAddedToMaterialConverter materialAddedToMaterialConverter;

    @Inject
    private MaterialRepository materialRepository;

    @Inject
    private MaterialUploadStatusRepository materialUploadStatusRepository;

    @Inject
    private UtcClock clock;

    @Inject
    MaterialBundleDetailsRecordedToMaterialConverter materialBundleDetailsRecordedToMaterialConverter;

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @Handles("material.events.material-added")
    public void materialAdded(final JsonEnvelope event) {
        final MaterialAdded materialAdded = jsonObjectConverter.convert(
                event.payloadAsJsonObject(),
                MaterialAdded.class);

        final Material material = materialAddedToMaterialConverter.convert(
                materialAdded);

        materialRepository.save(material);

        final MaterialUploadStatus materialUploadStatus = materialUploadStatusRepository.findBy(material.getMaterialId());

        if (materialUploadStatus == null) {
            logger.warn("Failed to upload status of materialId {} to {} as it was not found.", material.getMaterialId(), UPLOAD_STATUS_SUCCESS);
        } else {
            materialUploadStatus.setStatus(UPLOAD_STATUS_SUCCESS);
            materialUploadStatus.setLastModified(clock.now());
            materialUploadStatusRepository.save(materialUploadStatus);
        }
    }

    @Handles("material.events.material-deleted")
    public void materialDeleted(final JsonEnvelope event) {
        final MaterialDeleted materialDeleted = jsonObjectConverter.convert(
                event.payloadAsJsonObject(),
                MaterialDeleted.class);

        final Material material = materialRepository.findBy(materialDeleted.getMaterialId());
        materialRepository.removeAndFlush(material);
    }

    @Handles("material.events.file-uploaded")
    public void fileUploaded(final JsonEnvelope event) {
        final FileUploaded fileUploaded = jsonObjectConverter.convert(
                event.payloadAsJsonObject(),
                FileUploaded.class);

        final MaterialUploadStatus materialUploadStatus = materialUploadStatusRepository.findBy(fileUploaded.getMaterialId());

        if (materialUploadStatus == null) {
            materialUploadStatusRepository.save(new MaterialUploadStatus(fileUploaded.getMaterialId(), fileUploaded.getFileServiceId(), UPLOAD_STATUS_QUEUED, null, null, clock.now()));
        } else {
            logger.warn("Failed to add new upload status for materialId {} as it already exists.", fileUploaded.getMaterialId());
        }
    }

    @Handles("material.events.cloud-blob-file-uploaded")
    public void cloudBlobFileUploaded(final JsonEnvelope event) {
        final CloudBlobFileUploaded fileUploaded = jsonObjectConverter.convert(
                event.payloadAsJsonObject(),
                CloudBlobFileUploaded.class);

        final MaterialUploadStatus materialUploadStatus = materialUploadStatusRepository.findBy(fileUploaded.getMaterialId());

        if (materialUploadStatus == null) {
            materialUploadStatusRepository.save(new MaterialUploadStatus(fileUploaded.getMaterialId(), UUID.nameUUIDFromBytes(fileUploaded.getFileCloudLocation().getBytes()), UPLOAD_STATUS_QUEUED, null, null, clock.now()));
        } else {
            logger.warn("Failed to add new upload status for materialId {} as it already exists.", fileUploaded.getMaterialId());
        }
    }

    @Handles("material.events.failed-to-add-material")
    public void failedToAddMaterial(final JsonEnvelope event) {
        final FailedToAddMaterial failedToAddMaterial = jsonObjectConverter.convert(
                event.payloadAsJsonObject(),
                FailedToAddMaterial.class);

        final MaterialUploadStatus materialUploadStatus = materialUploadStatusRepository.findBy(failedToAddMaterial.getMaterialId());

        if (materialUploadStatus == null) {
            logger.warn("Failed to upload status of materialId {} to {} as it was not found.", failedToAddMaterial.getMaterialId(), UPLOAD_STATUS_FAILED);
        } else {
            materialUploadStatus.setStatus(UPLOAD_STATUS_FAILED);
            materialUploadStatus.setLastModified(clock.now());
            materialUploadStatus.setErrorMessage(failedToAddMaterial.getErrorMessage());
            materialUploadStatus.setFailedTime(failedToAddMaterial.getFailedTime());
            materialUploadStatusRepository.save(materialUploadStatus);
        }
    }

    @Handles("material.events.material-bundle-details-recorded")
    public void materialBundleDetailsRecorded(final Envelope<MaterialBundleDetailsRecorded> event) {
        final MaterialBundleDetailsRecorded materialBundleDetailsRecorded = event.payload();

        final Material material = materialBundleDetailsRecordedToMaterialConverter.convert(materialBundleDetailsRecorded);

        materialRepository.save(material);
    }

}
