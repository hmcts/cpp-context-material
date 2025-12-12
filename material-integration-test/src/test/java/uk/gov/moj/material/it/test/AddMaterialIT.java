package uk.gov.moj.material.it.test;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.material.it.util.FileUtil.getDocumentBytesFromFile;

import uk.gov.moj.material.it.helper.FileServiceClient;
import uk.gov.moj.material.it.helper.MaterialTestHelper;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for adding materials of different types.
 */

public class AddMaterialIT extends BaseIT {

    private static final String FILENAME_TXT = "MaterialFullStackTestFile.txt";
    private static final String MIME_TYPE_TXT = "text/plain";
    private static final String FILE_PATH_TXT = "upload_samples/sample.txt";

    private static final String FILENAME_PDF = "MaterialFullStackTestFile.pdf";
    private static final String MIME_TYPE_PDF = "application/pdf";
    private static final String FILE_PATH_PDF = "upload_samples/sample.pdf";

    private static final String FILENAME_DOCX = "MaterialFullStackTestFile.docx";
    private static final String MIME_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String FILE_PATH_DOCX = "upload_samples/sample.docx";

    private static final String FILENAME_JPG = "MaterialFullStackTestFile.jpg";
    private static final String MIME_TYPE_JPG = "image/jpeg";
    private static final String FILE_PATH_JPG = "upload_samples/sample.jpg";
    private static final String EXTERNAL_LINK_JPG = "http://something.com/test.jpg";

    private static MaterialTestHelper testHelper;
    private String materialId;


    @BeforeAll
    public static void setup() {
        testHelper = new MaterialTestHelper();
        testHelper.setup();
    }

    @BeforeEach
    public void setUp() {
        materialId = randomUUID().toString();

    }

    @Test
    public void addMaterialShouldRaisePrivateAndPublicEvents() {
        testHelper.setUploadFileProperties(FILE_PATH_TXT, FILENAME_TXT, MIME_TYPE_TXT);
        testHelper.addMaterial(materialId);

        testHelper.verifyInPublicTopic(materialId);
    }

    @Disabled("temporarily ignored for 23.13 release")
    @Test
    public void addMaterialAndVerifyMaterialAndItsMetadataAdded_txt() throws Exception {
        testHelper.setUploadFileProperties(FILE_PATH_TXT, FILENAME_TXT, MIME_TYPE_TXT);
        testHelper.addMaterial(materialId);

        testHelper.verifyInActiveMQ(materialId);
        testHelper.verifyMetadataAdded(materialId);
        testHelper.verifyMaterialAddedAsPlainText(materialId);
    }

    @Test
    public void addMaterialAndVerifyMaterialAndItsMetadataAdded_pdf() throws Exception {
        testHelper.setUploadFileProperties(FILE_PATH_PDF, FILENAME_PDF, MIME_TYPE_PDF);

        testHelper.addMaterial(materialId);
        verifyMaterial(true, materialId);
    }


    @Test
    public void shouldReturnResponseFromAlfresco() throws Exception {
        testHelper.setUploadFileProperties(FILE_PATH_PDF, FILENAME_PDF, MIME_TYPE_PDF);
        testHelper.addMaterial(materialId);

        verifyMaterialMetadataDetail(materialId);
    }

    @Test
    public void shouldGet404ResponseWhenMaterialIdNotFound() {
        testHelper.setUploadFileProperties(FILE_PATH_PDF, FILENAME_PDF, MIME_TYPE_PDF);
        testHelper.verifyMetadataDetailForNotFound(randomUUID().toString());
    }

    @Test
    public void addMaterialAndVerifyMaterialAndItsMetadataAdded_docx() throws Exception {
        testHelper.setUploadFileProperties(FILE_PATH_DOCX, FILENAME_DOCX, MIME_TYPE_DOCX);
        testHelper.addMaterial(materialId);
        verifyMaterial(true, materialId);
    }

    @Test
    public void addMaterialAndVerifyMaterialAndItsMetadataAdded_jpg() throws Exception {
        testHelper.setUploadFileProperties(FILE_PATH_JPG, FILENAME_JPG, MIME_TYPE_JPG);
        testHelper.addMaterial(materialId);
        verifyMaterial(true, materialId);
    }

    @Test
    public void addMaterialAndVerifyMaterialAndItsMetadataAdded_externalLink() throws Exception {
        testHelper.setExternalFileProperties(EXTERNAL_LINK_JPG, FILENAME_JPG);
        testHelper.addMaterial(materialId);
        verifyMaterial(false, materialId);
    }

    @Disabled("CPI-733 - Flaky IT, temporarily ignored for release")
    @Test
    public void uploadFileAndVerifyIsNotDownloadable() {
        testHelper.setUploadFileProperties(FILE_PATH_JPG, FILENAME_JPG, MIME_TYPE_JPG);
        testHelper.uploadFile(UUID.randomUUID(), materialId);
        verifyMaterialIsDownloadable(materialId);
    }

    @Test
    public void addMaterialAndVerifyMaterialAndItsMetadataAddedAndIsDownloadable() throws Exception {

        final byte[] documentContent = getDocumentBytesFromFile(FILE_PATH_JPG);
        final UUID fileServiceId = FileServiceClient.create(FILENAME_JPG, MIME_TYPE_JPG, documentContent);
        testHelper.setUploadFileProperties(FILE_PATH_JPG, FILENAME_JPG, MIME_TYPE_JPG);
        testHelper.uploadFile(fileServiceId, materialId);
        testHelper.verifyInActiveMQ(materialId);
        verifyMaterial(true, materialId);
        verifyMaterialIsDownloadable(materialId);
    }

    @Test
    public void addFileNameMatchingAlfrescoFileNameValidationAndVerifyMaterialAndItsMetadataAddedAndIsDownloadable() throws Exception {
        final String fileName = "Download extract showing SIM contact location 19 mobile number 0123456789 has been stored as dad. ";
        final byte[] documentContent = getDocumentBytesFromFile(FILE_PATH_PDF);
        final UUID fileServiceId = FileServiceClient.create(fileName, MIME_TYPE_PDF, documentContent);

        testHelper.setUploadFileProperties(FILE_PATH_PDF, fileName, MIME_TYPE_PDF);

        testHelper.uploadFile(fileServiceId, materialId);

        testHelper.verifyInActiveMQ(materialId);
        testHelper.verifyMetadataAdded(materialId);
        testHelper.verifyMaterialAdded(materialId, false);

        verifyMaterialIsDownloadable(materialId);
    }

    private void verifyMaterialIsDownloadable(final String materialId) {
        testHelper.verifyMaterialsIsDownloadable(materialId);
    }

    private void verifyMaterial(final boolean isUpload, final String materialId) throws Exception {
        testHelper.verifyMetadataAdded(materialId);

        // material not added (ie. uploaded to alfresco) for external links
        if (isUpload) {
            testHelper.verifyMaterialAdded(materialId, true);
        }
    }

    private void verifyMaterialMetadataDetail(final String materialId) throws Exception {
        testHelper.verifyMetadataDetailAdded(materialId);

        // material not added (ie. uploaded to alfresco) for external links
        testHelper.verifyMaterialAdded(materialId, true);
    }
}