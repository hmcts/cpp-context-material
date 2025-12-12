package uk.gov.moj.cpp.material.domain.aggregate;

import static com.google.common.collect.ImmutableList.of;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import uk.gov.moj.cpp.material.domain.event.MaterialAdded;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleDetailsRecorded;
import uk.gov.moj.cpp.material.domain.event.MaterialBundleRequested;
import uk.gov.moj.cpp.material.domain.event.MaterialDeleted;
import uk.gov.moj.cpp.material.domain.event.MaterialNotFound;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MaterialTest {

    private static final String MIME_TYPE = "application/pdf";
    private static final String FILE_NAME = "Boys' Bumper Book of Facts";
    private static final String FILE_SIZE = "1234";
    private static final String BUNDLED_MATERIAL_NAME = "Barkingside Magistrates' Court 17072021.pdf";
    private Material material;

    @BeforeEach
    public void setup() {
        material = new Material();
    }

    @Test
    public void shouldGenerateMaterialAddedEventWhenAddingExternalMaterials() {
        final UUID materialId = randomUUID();
        final ZonedDateTime currentDateTime = ZonedDateTime.now(UTC);

        final Stream<Object> events = material.addExternalMaterial(
                materialId,
                "fileName",
                "externalLink",
                currentDateTime);

        final Optional<Object> event = events.findFirst();

        assertThat(event.isPresent(), is(true));
        assertThat(event.get(), is(instanceOf(MaterialAdded.class)));

        final MaterialAdded materialAdded = (MaterialAdded) event.get();

        assertThat(materialAdded.getMaterialId(), is(equalTo(materialId)));
        assertThat(materialAdded.getMaterialAddedDate(), is(currentDateTime));
        assertThat(materialAdded.getFileDetails().getFileName(), is(equalTo("fileName")));
        assertThat(materialAdded.getFileDetails().getExternalLink(), is(equalTo("externalLink")));
    }

    @Test
    public void shouldRaiseDuplicateMaterialEventIfAddingMultipleExternalMaterialsWithSameId() {
        final UUID materialId = randomUUID();
        final ZonedDateTime currentDateTime = ZonedDateTime.now(UTC);

        material.addExternalMaterial(materialId, "fileName", "externalLink", currentDateTime);
        final Stream<Object> events = material.addExternalMaterial(materialId, "fileName", "externalLink", currentDateTime);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        assertThat(event.get(), is(instanceOf(DuplicateMaterialNotCreated.class)));
        assertThat(((DuplicateMaterialNotCreated) event.get()).getMaterialId(), is(equalTo(materialId)));
    }

    @Test
    public void shouldRaiseFailedToAddMaterialEvent() {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final ZonedDateTime failedTime = ZonedDateTime.now(UTC);
        final String errorMessage = "Error";

        final Stream<Object> events = material.recordAddMaterialFailed(materialId, fileServiceId, failedTime, errorMessage);

        final Optional<Object> event = events.findFirst();

        assertThat(event.isPresent(), is(true));
        assertThat(event.get(), is(instanceOf(FailedToAddMaterial.class)));

        final FailedToAddMaterial failedToAddMaterial = (FailedToAddMaterial) event.get();

        assertThat(failedToAddMaterial.getMaterialId(), is(materialId));
        assertThat(failedToAddMaterial.getFileServiceId(), is(fileServiceId));
        assertThat(failedToAddMaterial.getErrorMessage(), is(errorMessage));
        assertThat(failedToAddMaterial.getFailedTime(), is(failedTime));
    }

    @Test
    public void shouldGenerateMaterialAddedEventWhenAddingUploadedFile() {
        final UUID materialId = randomUUID();
        final ZonedDateTime currentDateTime = ZonedDateTime.now(UTC);

        final Stream<Object> events = material.addUploadedFile(
                materialId,
                "fileName",
                new UploadedMaterial("externalId", MIME_TYPE),
                currentDateTime);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        assertThat(event.get(), is(instanceOf(MaterialAdded.class)));

        MaterialAdded materialAdded = (MaterialAdded) event.get();
        assertThat(materialAdded.getMaterialId(), is(equalTo(materialId)));
        assertThat(materialAdded.getMaterialAddedDate(), is(currentDateTime));
        assertThat(materialAdded.getFileDetails().getFileName(), is(equalTo("fileName")));
        assertThat(materialAdded.getFileDetails().getAlfrescoAssetId(), is(equalTo("externalId")));
        assertThat(materialAdded.getFileDetails().getMimeType(), is(equalTo("application/pdf")));
    }

    @Test
    public void shouldRaiseDuplicateMaterialEventIfAddingMultipleUploadedFilesWithSameId() {
        final UUID materialId = randomUUID();
        final ZonedDateTime currentDateTime = ZonedDateTime.now(UTC);

        material.addUploadedFile(
                materialId,
                "fileName2",
                new UploadedMaterial("externalId", MIME_TYPE),
                currentDateTime);

        final Stream<Object> events = material.addUploadedFile(
                materialId,
                FILE_NAME,
                new UploadedMaterial("externalId", MIME_TYPE),
                currentDateTime);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        assertThat(event.get(), is(instanceOf(DuplicateMaterialNotCreated.class)));
        assertThat(((DuplicateMaterialNotCreated) event.get()).getMaterialId(), is(equalTo(materialId)));
    }

    @Test
    public void shouldGenerateMaterialAddedEventWhenAddingFileReference() {

        final UUID materialId = randomUUID();
        final UUID fileId = randomUUID();
        final ZonedDateTime currentDateTime = ZonedDateTime.now(UTC);

        final FileDetails fileDetails = new FileDetails(fileId.toString(), MIME_TYPE, FILE_NAME);

        final Stream<Object> events = material.addFileReference(materialId, fileDetails, currentDateTime, false);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        assertThat(event.get(), is(instanceOf(MaterialAdded.class)));

        final MaterialAdded materialAdded = (MaterialAdded) event.get();
        assertThat(materialAdded.getMaterialId(), is(equalTo(materialId)));
        assertThat(materialAdded.getMaterialAddedDate(), is(currentDateTime));
        assertThat(materialAdded.getFileDetails().getFileName(), is(equalTo(FILE_NAME)));
        assertThat(materialAdded.getFileDetails().getAlfrescoAssetId(), is(equalTo(fileId.toString())));
        assertThat(materialAdded.getFileDetails().getMimeType(), is(equalTo(MIME_TYPE)));
    }

    @Test
    public void shouldRaiseDuplicateMaterialEventIfAddingMultipleFileReferencesWithSameId() {

        final UUID materialId = randomUUID();
        final ZonedDateTime currentDateTime = ZonedDateTime.now(UTC);

        material.addFileReference(
                materialId,
                new FileDetails(
                        randomUUID().toString(),
                        MIME_TYPE,
                        FILE_NAME
                ),
                currentDateTime,
                false
        );
        final Stream<Object> events = material.addFileReference(
                materialId,
                new FileDetails(
                        randomUUID().toString(),
                        MIME_TYPE,
                        "1974 Blue StructuredFormer Annual"
                ),
                currentDateTime,
                false
        );

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        assertThat(event.get(), is(instanceOf(DuplicateMaterialNotCreated.class)));
        assertThat(((DuplicateMaterialNotCreated) event.get()).getMaterialId(), is(equalTo(materialId)));
    }

    @Test
    public void shouldSerializeMaterialObjectGraph() {
        byte[] result = serialize(material);
        assertNotNull(result);
    }

    @Test
    public void shouldCreateFileUploadEvent() {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final boolean isUnbundledDocument = false;

        final Stream<Object> events = material.uploadFile(materialId, fileServiceId, isUnbundledDocument);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        final FileUploaded fileUploaded = (FileUploaded) event.get();
        assertThat(fileUploaded.getMaterialId(), is(equalTo(materialId)));
        assertThat(fileUploaded.getFileServiceId(), is(equalTo(fileServiceId)));
    }

    @Test
    public void shouldNotCreateFileUploadEventForDuplicateSubmit() {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final boolean isUnbundledDocument = false;

        Stream<Object> events = material.uploadFile(materialId, fileServiceId, isUnbundledDocument);

        Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        final FileUploaded fileUploaded = (FileUploaded) event.get();
        assertThat(fileUploaded.getMaterialId(), is(equalTo(materialId)));
        assertThat(fileUploaded.getFileServiceId(), is(equalTo(fileServiceId)));

        events = material.uploadFile(materialId, fileServiceId, isUnbundledDocument);
        event = events.findFirst();

        final DuplicateFileUploadRequestReceived duplicateFileUploadRequestReceived = (DuplicateFileUploadRequestReceived) event.get();
        assertThat(duplicateFileUploadRequestReceived.getMaterialId(), is(equalTo(materialId)));
        assertThat(duplicateFileUploadRequestReceived.getFileServiceId(), is(equalTo(fileServiceId)));

    }

    @Test
    public void shouldGenerateMaterialDelete() {

        final UUID materialId = randomUUID();
        final UUID alfrescoId = randomUUID();

        givenAMaterialHasBeenAdded(materialId, alfrescoId);

        final Stream<Object> events = deleteMaterial(materialId);

        thenMaterialDeletedEventIssued(events, materialId, alfrescoId);
    }

    @Test
    public void shouldNotGenerateMaterialDelete() {

        final UUID materialId = randomUUID();
        final UUID alfrescoId = randomUUID();

        givenAMaterialHasBeenAddedAndThenDeleted(materialId, alfrescoId);

        //when
        final Stream<Object> events = deleteMaterial(materialId);

        thenMaterialNotFoundEventIssued(events, materialId);
    }


    @Test
    public void shouldRaiseBundleMaterialDetailsRecordedEventWhenBundleMaterialsAdded() {

        final UUID materialId = randomUUID();
        final UUID fileId = randomUUID();
        final int pageCount = 1;
        final ZonedDateTime currentDateTime = ZonedDateTime.now(UTC);

        final FileDetails fileDetails = new FileDetails(fileId.toString(), MIME_TYPE, FILE_NAME);

        final Stream<Object> events = material.recordBundleDetails(materialId, fileDetails, FILE_SIZE, pageCount, currentDateTime);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        assertThat(event.get(), is(instanceOf(MaterialBundleDetailsRecorded.class)));

        final MaterialBundleDetailsRecorded materialAdded = (MaterialBundleDetailsRecorded) event.get();
        assertThat(materialAdded.getMaterialId(), is(equalTo(materialId)));
        assertThat(materialAdded.getMaterialBundleDetailsRecordedDate(), is(currentDateTime));
        assertThat(materialAdded.getFileDetails().getFileName(), is(equalTo(FILE_NAME)));
        assertThat(materialAdded.getFileDetails().getAlfrescoAssetId(), is(equalTo(fileId.toString())));
        assertThat(materialAdded.getFileDetails().getMimeType(), is(equalTo(MIME_TYPE)));
    }

    @Test
    public void shouldRaiseDuplicateRecordBundleDetailsRequestedEventWhenMultipleRecordBundleDetailsRequestReceived() {

        final UUID materialId = randomUUID();
        final UUID fileId = randomUUID();
        final int pageCount = 1;
        final ZonedDateTime currentDateTime = ZonedDateTime.now(UTC);

        final FileDetails fileDetails = new FileDetails(fileId.toString(), MIME_TYPE, FILE_NAME);

        material.recordBundleDetails(materialId, fileDetails, FILE_SIZE, pageCount, currentDateTime);

        final Stream<Object> events = material.recordBundleDetails(materialId, fileDetails, FILE_SIZE, pageCount, currentDateTime);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        assertThat(event.get(), is(instanceOf(DuplicateRecordBundleDetailsRequested.class)));
        assertThat(((DuplicateRecordBundleDetailsRequested) event.get()).getBundledMaterialId(), is(equalTo(materialId)));
    }


    @Test
    public void shouldRaiseBundleMaterialRequestedEventWhenBundleMaterialsRequested() {

        final UUID materialId = randomUUID();
        List<UUID> materialIds = of(randomUUID(), randomUUID());

        final Stream<Object> events = material.createMaterialBundle(materialId, materialIds, BUNDLED_MATERIAL_NAME);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        assertThat(event.get(), is(instanceOf(MaterialBundleRequested.class)));

        final MaterialBundleRequested materialBundleRequested = (MaterialBundleRequested) event.get();
        assertThat(materialBundleRequested.getBundledMaterialId(), is(equalTo(materialId)));
        assertThat(materialBundleRequested.getBundledMaterialName(), is(equalTo(BUNDLED_MATERIAL_NAME)));
        assertThat(materialBundleRequested.getMaterialIds(), is(materialIds));
    }

    @Test
    public void shouldRaiseBundleMaterialRequestedFailedEventWhenDuplicateBundleRequested() {

        final UUID materialId = randomUUID();
        List<UUID> materialIds = of(randomUUID(), randomUUID());

        material.createMaterialBundle(materialId, materialIds, BUNDLED_MATERIAL_NAME);
        final String fileSize = "1234";
        final int pageCount = 1;
        final FileDetails fileDetails = new FileDetails(randomUUID().toString(), "application/pdf", "Boys' Bumper Book of Facts");
        material.recordBundleDetails(materialId, fileDetails, fileSize, pageCount, ZonedDateTime.now(UTC));
        final Stream<Object> events = material.createMaterialBundle(materialId, materialIds, BUNDLED_MATERIAL_NAME);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        assertThat(event.get(), is(instanceOf(DuplicateMaterialBundleNotCreated.class)));

        final DuplicateMaterialBundleNotCreated duplicateMaterialBundleNotCreated = (DuplicateMaterialBundleNotCreated) event.get();
        assertThat(duplicateMaterialBundleNotCreated.getBundledMaterialId(), is(equalTo(materialId)));
        assertThat(duplicateMaterialBundleNotCreated.getFailedCommand(), is("Create Material Bundle"));
    }


    @Test
    void shouldRaiseCloudBlobFileUploaded(){
        final UUID materialId = randomUUID();
        final String fileCloudLocation = "2017/testcase2.pdf";

        final Stream<Object> events = material.uploadCloudBlobFile(materialId, fileCloudLocation);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        final CloudBlobFileUploaded cloudBlobFileUploaded = (CloudBlobFileUploaded) event.get();
        assertThat(cloudBlobFileUploaded.getMaterialId(), is(equalTo(materialId)));
        assertThat(cloudBlobFileUploaded.getFileCloudLocation(), is(equalTo(fileCloudLocation)));

    }

    @Test
    void shouldRaiseDuplicateCloudBlobFileUploadedRequestReceived(){
        final UUID materialId = randomUUID();
        final String fileCloudLocation = "2017/testcase2.pdf";

        material.uploadCloudBlobFile(materialId, fileCloudLocation);
        final Stream<Object> events = material.uploadCloudBlobFile(materialId, fileCloudLocation);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));

        final DuplicateCloudBlobFileUploadedRequestReceived cloudBlobFileUploaded = (DuplicateCloudBlobFileUploadedRequestReceived) event.get();
        assertThat(cloudBlobFileUploaded.getMaterialId(), is(equalTo(materialId)));
        assertThat(cloudBlobFileUploaded.getFileCloudLocation(), is(equalTo(fileCloudLocation)));

    }

    private void thenMaterialNotFoundEventIssued(final Stream<Object> events, final UUID materialId) {
        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));
        assertThat(event.get(), is(instanceOf(MaterialNotFound.class)));
        assertThat(material.isHasBeenCreated(), is(false));
        MaterialNotFound materialNotFound = (MaterialNotFound) event.get();
        assertThat(materialNotFound.getMaterialId(), is(equalTo(materialId)));
    }

    private void givenAMaterialHasBeenAddedAndThenDeleted(final UUID materialId, final UUID alfrescoId) {
        givenAMaterialHasBeenAdded(materialId, alfrescoId);
        deleteMaterial(materialId);
    }

    private void thenMaterialDeletedEventIssued(final Stream<Object> events, final UUID materialId, final UUID alfrescoId) {
        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));
        assertThat(event.get(), is(instanceOf(MaterialDeleted.class)));
        assertThat(material.isHasBeenCreated(), is(false));
        MaterialDeleted materialDeleted = (MaterialDeleted) event.get();
        assertThat(materialDeleted.getMaterialId(), is(equalTo(materialId)));
        assertThat(materialDeleted.getAlfrescoId(), is(equalTo(alfrescoId.toString())));
    }

    private Stream<Object> deleteMaterial(final UUID materialId) {
        return material.deleteMaterial(materialId);
    }

    private void givenAMaterialHasBeenAdded(final UUID materialId, final UUID alfrescoId) {
        final ZonedDateTime currentDateTime = ZonedDateTime.now(UTC);
        final Stream<Object> events = material.addUploadedFile(
                materialId,
                "fileName",
                new UploadedMaterial(alfrescoId.toString(), "mimeType"),
                currentDateTime);

        final Optional<Object> event = events.findFirst();
        assertThat(event.isPresent(), is(true));
        assertThat(event.get(), is(instanceOf(MaterialAdded.class)));
        assertThat(material.isHasBeenCreated(), is(true));
        MaterialAdded materialAdded = (MaterialAdded) event.get();
        assertThat(materialAdded.getMaterialId(), is(equalTo(materialId)));
        assertThat(materialAdded.getFileDetails().getAlfrescoAssetId(), is(equalTo(alfrescoId.toString())));
    }

}
