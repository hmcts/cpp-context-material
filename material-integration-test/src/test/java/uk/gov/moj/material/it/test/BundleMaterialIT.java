package uk.gov.moj.material.it.test;

import static com.google.common.collect.ImmutableList.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.material.it.util.WiremockAccessControlEndpointStubber.setupUsersGroupQueryStub;

import uk.gov.moj.material.it.helper.MaterialTestHelper;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for bundling materials
 */
public class BundleMaterialIT extends BaseIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleMaterialIT.class);

    public static final int BUNDLE_COUNT = 2;
    private static final String FILENAME_PDF = "MaterialFullStackTestFile.pdf";
    private static final String FILENAME_PDF2 = "MaterialFullStackTestFile2.pdf";
    private static final String MIME_TYPE_PDF = "application/pdf";
    private static final String FILE_PATH_PDF = "upload_samples/sample.pdf";
    private static MaterialTestHelper testHelper;


    @BeforeAll
    public static void beforeAll() {
        setupUsersGroupQueryStub();
        testHelper = new MaterialTestHelper();
        testHelper.setup();
    }

    @Test
    public void shouldCreateBundleMaterial() throws Exception {
        final String materialId1 = randomUUID().toString();
        addMaterialAndVerifyMaterialAspdf(FILENAME_PDF, materialId1);
        final String materialId2 = randomUUID().toString();
        addMaterialAndVerifyMaterialAspdf(FILENAME_PDF, materialId2);

        testHelper.setUploadFileProperties(FILE_PATH_PDF, FILENAME_PDF, MIME_TYPE_PDF);
        final String bundleMaterialId = randomUUID().toString();


        final String request = testHelper.buildCreateMaterialBundleRequest(bundleMaterialId, of(materialId1, materialId2));

        testHelper.startConsumerForMaterialBundleCreated();
        testHelper.createMaterialBundle(request);
        testHelper.verifyInPublicTopic(bundleMaterialId);
        testHelper.verifyBundleMaterialAdded(bundleMaterialId);
    }

    @Test
    public void shouldProcessMultipleBundleCreationRequests() throws Exception {
        final String materialId1 = randomUUID().toString();
        addMaterialAndVerifyMaterialAspdf(FILENAME_PDF, materialId1);
        final String materialId2 = randomUUID().toString();
        addMaterialAndVerifyMaterialAspdf(FILENAME_PDF2, materialId2);

        testHelper.setUploadFileProperties(FILE_PATH_PDF, FILENAME_PDF2, MIME_TYPE_PDF);
        testHelper.startConsumerForMaterialBundleCreated();

        final List<String> bundleMaterialIdList = new ArrayList<>();
        range(0, BUNDLE_COUNT).mapToObj(i -> randomUUID().toString()).forEachOrdered(bundleMaterialId -> {
            String request = testHelper.buildCreateMaterialBundleRequest(bundleMaterialId, of(materialId1, materialId2));
            testHelper.createMaterialBundle(request);
            final String bundleMaterialIdFromTopic = testHelper.getMaterialIdFromPublicTopic();
            assertThat(bundleMaterialIdFromTopic, is(bundleMaterialId));
            bundleMaterialIdList.add(bundleMaterialId);
        });

        bundleMaterialIdList.forEach(bundleMaterialId -> testHelper.deleteMaterial(bundleMaterialId, true));
    }

    @Test
    public void shouldRaiseMaterialBundleCreationFailedEventWhenMaterialIdNotFoundInFileStore() throws Exception {
        String materialId1 = randomUUID().toString();
        addMaterialAndVerifyMaterialAspdf(FILENAME_PDF, materialId1);

        String materialId2 = randomUUID().toString();
        String bundleMaterialId = randomUUID().toString();
        testHelper.setUploadFileProperties(FILE_PATH_PDF, FILENAME_PDF, MIME_TYPE_PDF);

        LOGGER.info("bundleId={} with materialIds={} and {}", bundleMaterialId, materialId1, materialId2);
        String request = testHelper.buildCreateMaterialBundleRequest(bundleMaterialId, of(materialId1, materialId2));
        testHelper.startConsumerForMaterialBundleCreationFailed();
        testHelper.createMaterialBundle(request);
        testHelper.verifyBundleInPublicTopic(bundleMaterialId);
    }


    public void addMaterialAndVerifyMaterialAspdf(final String fileName, final String materialId) throws Exception {

        testHelper.setUploadFileProperties(FILE_PATH_PDF, fileName, MIME_TYPE_PDF);
        testHelper.addMaterial(materialId);
        verifyMaterial(testHelper, materialId);
    }

    private void verifyMaterial(MaterialTestHelper testHelper, String materialId) throws Exception {
        testHelper.verifyMetadataAdded(materialId);
        testHelper.verifyMaterialAdded(materialId, true);
    }
}