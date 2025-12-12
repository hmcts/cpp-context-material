package uk.gov.moj.cpp.material.domain.aggregate;

import static java.util.Objects.isNull;
import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.material.domain.event.MaterialBundleDetailsRecorded.materialBundleDetailsRecorded;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.material.domain.FileDetails;
import uk.gov.moj.cpp.material.domain.UploadedMaterial;
import uk.gov.moj.cpp.material.domain.event.CloudBlobFileUploaded;
import uk.gov.moj.cpp.material.domain.event.DuplicateCloudBlobFileUploadedRequestReceived;
import uk.gov.moj.cpp.material.domain.event.DuplicateFileUploadRequestReceived;
import uk.gov.moj.cpp.material.domain.event.DuplicateMaterialBundleNotCreated;
import uk.gov.moj.cpp.material.domain.event.DuplicateMaterialNotCreated;
import uk.gov.moj.cpp.material.domain.event.DuplicateRecordBundleDetailsRequested;
import uk.gov.moj.cpp.material.domain.event.FailedToAddMaterial;
import uk.gov.moj.cpp.material.domain.event.FileUploaded;
import uk.gov.moj.cpp.material.domain.event.FileUploadedAsPdf;
import uk.gov.moj.cpp.material.domain.event.MaterialAdded;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleDetailsRecorded;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleRequested;
import uk.gov.moj.cpp.material.domain.event.MaterialBundlingFailed;
import uk.gov.moj.cpp.material.domain.event.MaterialDeleted;
import uk.gov.moj.cpp.material.domain.event.MaterialNotFound;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings({"squid:S1068", "PMD.BeanMembersShouldSerialize"})
public class Material implements Aggregate {

    private static final long serialVersionUID = -1837632618531502420L;
    private static final String ADD_MATERIAL = "Add Material";
    private static final String RECORD_BUNDLE_DETAILS = "Record Bundle Details";
    private static final String CREATE_MATERIAL_BUNDLE = "Create Material Bundle";
    private boolean hasBeenCreated;
    private boolean hasBeenBundled;
    private String alfrescoFileId;
    private String filename;
    private UUID fileServiceId;
    private String fileCloudLocation;

    public boolean isHasBeenCreated() {
        return hasBeenCreated;
    }


    public String getFilename() {
        return filename;
    }

    public UUID getFileServiceId() {
        return this.fileServiceId;
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(MaterialAdded.class).apply(e -> onMaterialAdded(e.getFileDetails().getAlfrescoAssetId(), e.getFileDetails().getFileName())),
                when(MaterialBundleDetailsRecorded.class).apply(e -> onMaterialBundled(e.getFileDetails().getAlfrescoAssetId(), e.getFileDetails().getFileName())),
                when(MaterialDeleted.class).apply(e -> onMaterialDeleted()),
                when(FileUploaded.class).apply(e -> onFileUpload(e.getFileServiceId())),
                when(MaterialBundleRequested.class).apply(e -> doNothing()),
                when(MaterialBundlingFailed.class).apply(e -> doNothing()),
                when(FileUploadedAsPdf.class).apply(e -> onFileUpload(e.getFileServiceId())),
                when(DuplicateMaterialNotCreated.class).apply(e -> doNothing()),
                when(DuplicateMaterialBundleNotCreated.class).apply(e -> doNothing()),
                when(DuplicateRecordBundleDetailsRequested.class).apply(e -> doNothing()),
                when(FailedToAddMaterial.class).apply(e -> doNothing()),
                when(MaterialNotFound.class).apply(e -> doNothing()),
                when(DuplicateFileUploadRequestReceived.class).apply(e -> doNothing()),
                when(CloudBlobFileUploaded.class).apply(e -> onCloudBlobFileUpload(e.getFileCloudLocation())),
                when(DuplicateCloudBlobFileUploadedRequestReceived.class).apply(e -> doNothing())
        );
    }

    public Stream<Object> addFileReference(
            final UUID materialId,
            final FileDetails fileDetails,
            final ZonedDateTime currentDateTime,
            final Boolean isUnbundledDocument) {

        if (hasBeenCreated) {
            return apply(of(new DuplicateMaterialNotCreated(materialId, ADD_MATERIAL)));
        }

        final MaterialAdded materialAdded = new MaterialAdded.Builder()
                .withMaterialId(materialId)
                .withFileDetails(fileDetails)
                .withMaterialAddedDate(currentDateTime)
                .withIsUnbundledDocument(isUnbundledDocument)
                .build();

        return apply(of(materialAdded));
    }

    public Stream<Object> recordAddMaterialFailed(
            final UUID materialId,
            final UUID fileServiceId,
            final ZonedDateTime failedTime,
            final String errorMessage) {

        return apply(of(new FailedToAddMaterial(materialId, fileServiceId, failedTime, errorMessage)));
    }

    public Stream<Object> deleteMaterial(final UUID materialId) {

        if (!hasBeenCreated) {
            return apply(of(new MaterialNotFound(materialId)));
        }

        return apply(of(new MaterialDeleted(
                materialId, this.alfrescoFileId, this.fileServiceId)));
    }

    public Stream<Object> createMaterialBundle(final UUID bundledMaterialId, final List<UUID> materialIds, final String bundledMaterialName) {

        if (hasBeenBundled) {
            return apply(of(new DuplicateMaterialBundleNotCreated(bundledMaterialId, CREATE_MATERIAL_BUNDLE)));
        }

        return apply(of(new MaterialBundleRequested(
                bundledMaterialId, materialIds, bundledMaterialName)));
    }

    public Stream<Object> recordBundleDetails(
            final UUID materialId,
            final FileDetails fileDetails,
            final String fileSize,
            final int pageCount,
            final ZonedDateTime currentDateTime) {

        if (hasBeenBundled) {
            return apply(of(new DuplicateRecordBundleDetailsRequested(materialId, RECORD_BUNDLE_DETAILS)));
        }

        final MaterialBundleDetailsRecorded materialBundleDetailsRecorded = materialBundleDetailsRecorded()
                .withMaterialId(materialId)
                .withFileDetails(fileDetails)
                .withMaterialBundleDetailsRecordedDate(currentDateTime)
                .withFileSize(fileSize)
                .withPageCount(pageCount)
                .build();

        return apply(of(materialBundleDetailsRecorded));
    }

    public Stream<Object> recordBundleDetailsFailure(
            final UUID bundledMaterialId,
            final List<UUID> materialIds,
            final Optional<UUID> fileServiceId,
            String errorType, String errorMessage,
            ZonedDateTime failedTime) {

        return apply(of(new MaterialBundlingFailed(bundledMaterialId, materialIds, fileServiceId, errorType, errorMessage, failedTime)));
    }

    public Stream<Object> uploadFile(final UUID materialId, final UUID fileServiceId, final Boolean isUnbundledDocument) {
        if (isNull(this.fileServiceId)) { // if fileServiceId is not null, it is duplicate submit of material
            return apply(of(new FileUploaded(materialId, fileServiceId, isUnbundledDocument)));
        } else {
            return apply(of(new DuplicateFileUploadRequestReceived(materialId, fileServiceId)));
        }
    }

    public Stream<Object> uploadCloudBlobFile(final UUID materialId,  final String fileCloudLocation ) {
        if(!fileCloudLocation.equals(this.fileCloudLocation)){
            return apply(of(new CloudBlobFileUploaded(materialId, fileCloudLocation)));
        }else {
            return apply(of(new DuplicateCloudBlobFileUploadedRequestReceived(materialId, fileCloudLocation)));
        }

    }

    public Stream<Object> uploadFileAsPdf(final UUID materialId, final UUID fileServiceId, final Boolean isUnbundledDocument) {
        final FileUploadedAsPdf fileUploaded = new FileUploadedAsPdf(materialId, fileServiceId, isUnbundledDocument);
        return apply(of(fileUploaded));
    }

    public Stream<Object> addUploadedFile(
            final UUID materialId,
            final String fileName,
            final UploadedMaterial uploadedMaterial,
            final ZonedDateTime currentDateTime) {

        if (hasBeenCreated) {
            return apply(of(new DuplicateMaterialNotCreated(materialId, ADD_MATERIAL)));
        }

        final MaterialAdded materialAdded = new MaterialAdded.Builder()
                .withMaterialId(materialId)
                .withFileDetails(new FileDetails(uploadedMaterial.getExternalId(), uploadedMaterial.getMimeType(), fileName))
                .withMaterialAddedDate(currentDateTime)
                .build();

        return apply(of(materialAdded));
    }


    public Stream<Object> addExternalMaterial(
            final UUID materialId,
            final String fileName,
            final String externalLink,
            final ZonedDateTime currentDateTime) {
        if (hasBeenCreated) {
            return apply(of(new DuplicateMaterialNotCreated(materialId, ADD_MATERIAL)));
        }

        final MaterialAdded materialAdded = new MaterialAdded.Builder()
                .withMaterialId(materialId)
                .withFileDetails(new FileDetails(externalLink, fileName))
                .withMaterialAddedDate(currentDateTime)
                .build();

        return apply(of(materialAdded));
    }
    private void onMaterialAdded(final String alfrescoFileId, final String filename) {
        this.hasBeenCreated = true;
        this.alfrescoFileId = alfrescoFileId;
        this.filename = filename;
    }

    private void onMaterialBundled(final String alfrescoFileId, final String filename) {
        this.hasBeenBundled = true;
        this.alfrescoFileId = alfrescoFileId;
        this.filename = filename;
    }

    private void onFileUpload(final UUID fileServiceId) {
        this.fileServiceId = fileServiceId;
    }

    private void onCloudBlobFileUpload(final String  fileCloudLocation) {
        this.fileCloudLocation = fileCloudLocation;
    }

    private void onMaterialDeleted() {
        this.hasBeenCreated = false;
        this.alfrescoFileId = "";
        this.filename = "";
    }
}
