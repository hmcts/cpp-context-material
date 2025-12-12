package uk.gov.moj.material.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.moj.material.it.helper.MaterialTestHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for adding materials of different types.
 */

public class DeleteMaterialIT extends BaseIT {

    private static final String FILENAME_TXT = "MaterialFullStackTestFile.txt";
    private static final String MIME_TYPE_TXT = "text/plain";
    private static final String FILE_PATH_TXT = "upload_samples/sample.txt";

    private MaterialTestHelper testHelper;
    private String materialId;

    @BeforeEach
    public void setUp() {
        materialId = randomUUID().toString();
        testHelper = new MaterialTestHelper();
        testHelper.setup();
    }

    @Test
    public void deleteMaterialAndPublicEvents() {
        testHelper.setUploadFileProperties(FILE_PATH_TXT, FILENAME_TXT, MIME_TYPE_TXT);
        testHelper.addMaterial(materialId);


        final String alfrescoId = testHelper.verifyInPublicTopicAndExtractAlfrescoId(materialId);
        testHelper.verifyMaterialExists(materialId, anything());
        testHelper.verifyMaterialMetadata(materialId, payload().isJson(allOf(
                withJsonPath("$.materialId", equalTo(materialId)),
                withJsonPath("$.alfrescoAssetId", equalTo(alfrescoId))
        )));

        //when
        testHelper.deleteMaterial(materialId, true);

        //then
        testHelper.verifyMaterialDeletedEventInPublicTopic(alfrescoId, materialId);

        testHelper.verifyMaterialDoesNotExist(materialId);
        testHelper.verifyMaterialMetadata(materialId, status().is(NOT_FOUND));

    }

    @Test
    public void delete_material_which_doesnt_exist() {
        //given a non existing material
        final String nonExistingMaterialId = randomUUID().toString();
        //when
        testHelper.deleteMaterial(nonExistingMaterialId, false);
        //then
        testHelper.verifyEventInPublicTopic(
                allOf(
                        withJsonPath("$._metadata.name", equalTo("public.material.material-not-found")),
                        withJsonPath("$.materialId", equalTo(nonExistingMaterialId))
                )
        );
    }

}