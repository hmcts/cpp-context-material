package uk.gov.moj.material.it.test;


import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.material.it.helper.BaseMaterialTestHelper.READ_ENDPOINT;
import static uk.gov.moj.material.it.helper.BaseMaterialTestHelper.WRITE_ENDPOINT;
import static uk.gov.moj.material.it.util.FileUtil.getDocumentBytesFromFile;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.material.it.helper.FileServiceClient;
import uk.gov.moj.material.it.util.WiremockAccessControlEndpointStubber;

import java.sql.SQLException;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matchers;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UploadFileIT extends BaseIT {

    private static final RestClient restClient = new RestClient();
    private final WiremockAccessControlEndpointStubber accessControlStub = new WiremockAccessControlEndpointStubber();
    private MultivaluedMap<String, Object> headers;
    private UUID materialId, userId;

    private static final String IS_UNBUNDLED_DOCUMENT = "isUnbundledDocument";

    @BeforeEach
    public void init() {
        materialId = randomUUID();
        userId = randomUUID();
        headers = new MultivaluedMapImpl<>();
        headers.add(HeaderConstants.USER_ID, userId);
    }

    @Test
    public void readsFileServiceAndAddsNewMaterial() throws Exception {
        readsFileServiceAndAddsNewMaterialFor(false);
    }

    @Test
    public void readsFileServiceAndAddsNewMaterialIfDocumentIsUnbundled() throws Exception {
        readsFileServiceAndAddsNewMaterialFor(true);
    }

    // NOTE: this test will not be complete and verifying until the file status table is correctly implemented in the viewstore
    @Test
    public void shouldFailAndSkipRetryWhenPermanentFailureSuchAsInvalidFileServiceId() {
        accessControlStub.stubUsersAndGroupsUserAsSystemUser(userId.toString());
        accessControlStub.stubStructureAsProsecutedBy("TFL");
        accessControlStub.setupLoggedInUsersPermissionQueryStub(userId.toString());


        final UUID nonExistentFileServiceId = randomUUID();

        final JsonObjectBuilder uploadFilePayloadBuilder = JsonObjects.createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("fileServiceId", nonExistentFileServiceId.toString())
                .add(IS_UNBUNDLED_DOCUMENT, true);

        final JsonObject uploadFilePayload = uploadFilePayloadBuilder.build();

        final JmsMessageConsumerClient publicMessageConsumer = newPublicJmsMessageConsumerClientProvider()
                .withEventNames("public.events.material.failed-to-add-material")
                .getMessageConsumerClient();

        restClient.postCommand(WRITE_ENDPOINT, "application/vnd.material.command.upload-file+json",
                uploadFilePayload.toString(), headers);

        final JsonPath publicEvent = new JsonPath(publicMessageConsumer.retrieveMessage(60000L).get());

        assertThat(publicEvent.get("materialId"), equalTo(materialId.toString()));

    }

    private void readsFileServiceAndAddsNewMaterialFor(final boolean isUnbundledDocument) throws SQLException, FileServiceException {
        accessControlStub.stubUsersAndGroupsUserAsSystemUser(userId.toString());
        accessControlStub.stubStructureAsProsecutedBy("TFL");
        accessControlStub.setupLoggedInUsersPermissionQueryStub(userId.toString());


        final byte[] documentContent = getDocumentBytesFromFile("upload_samples/sample.txt");
        final UUID fileServiceId = FileServiceClient.create("sample.txt", "plain/text", documentContent);

        final JsonObjectBuilder uploadFilePayloadBuilder = JsonObjects.createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("fileServiceId", fileServiceId.toString());

        if (isUnbundledDocument) {
            uploadFilePayloadBuilder.add(IS_UNBUNDLED_DOCUMENT, true);
        }

        final JsonObject uploadFilePayload = uploadFilePayloadBuilder.build();

        final JmsMessageConsumerClient publicMessageConsumer = newPublicJmsMessageConsumerClientProvider()
                .withEventNames("material.material-added")
                .getMessageConsumerClient();


        restClient.postCommand(WRITE_ENDPOINT, "application/vnd.material.command.upload-file+json",
                uploadFilePayload.toString(), headers);

        final JsonPath publicEvent = new JsonPath(publicMessageConsumer.retrieveMessage(30000L).get());

        assertThat(publicEvent.get("materialId"), equalTo(materialId.toString()));
        assertThat(publicEvent.get(IS_UNBUNDLED_DOCUMENT), equalTo(isUnbundledDocument));

        final Response response = pollForMaterial(materialId);

        final String azureContentUrl = response.readEntity(String.class);

        assertThat(azureContentUrl, containsString("https://sastelargefiles.blob.core.windows.net/largefiles-blob-container/"));
    }

    private Response pollForMaterial(final UUID materialId) {
        return await().pollInterval(50, MILLISECONDS).until(() -> getMaterial(materialId), Matchers.hasProperty("status", equalTo(OK.getStatusCode())));
    }

    private Response getMaterial(final UUID materialId) {
        return restClient.query(READ_ENDPOINT + "/" + materialId.toString(), "application/vnd.material.query.material+json", headers);
    }

}
