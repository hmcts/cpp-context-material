package uk.gov.moj.material.it.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.material.it.helper.BaseMaterialTestHelper.ZIP_ENDPOINT;
import static uk.gov.moj.material.it.util.WiremockAccessControlEndpointStubber.setupUsersGroupQueryStub;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.material.it.util.WiremockAccessControlEndpointStubber;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MultivaluedMap;

import io.restassured.path.json.JsonPath;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class ZipMaterialIT extends BaseIT {
    private final RestClient restClient = new RestClient();
    private final WiremockAccessControlEndpointStubber accessControlStub = new WiremockAccessControlEndpointStubber();
    private MultivaluedMap<String, Object> headers;
    private List<UUID> materialIds;
    private List<UUID> fileIds;
    private UUID userId;

    private final String USER_ID_FOR_META_DATA = randomUUID().toString();
    private final JmsMessageConsumerClient publicMaterialZipFailedMessageConsumer = newPublicJmsMessageConsumerClientProvider()
            .withEventNames("public.material.events.material-zip-failed")
            .getMessageConsumerClient();

    @BeforeEach
    public void init() {
        materialIds = new ArrayList<>();
        materialIds.add(randomUUID());
        materialIds.add(randomUUID());
        userId = randomUUID();
        fileIds = new ArrayList<>();
        fileIds.add(randomUUID());
        fileIds.add(randomUUID());
        accessControlStub.setupLoggedInUsersPermissionQueryStub(userId.toString());
        accessControlStub.stubUsersAndGroupsForUserDetail(USER_ID_FOR_META_DATA);
        headers = new MultivaluedMapImpl<>();
        headers.add(HeaderConstants.USER_ID, userId);
        setupUsersGroupQueryStub();
    }

    @Test
    public void shouldRaiseMaterialZipFailedWhenAnErrorOccurred() {
        accessControlStub.stubUsersAndGroupsUserAsSystemUser(userId.toString());
        accessControlStub.stubStructureAsProsecutedBy("TFL");

        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        materialIds.forEach(materialId ->
                jsonArrayBuilder.add(materialId.toString()));

        final JsonArrayBuilder jsonFileIDArrayBuilder = Json.createArrayBuilder();
        fileIds.forEach(fileId ->
                jsonFileIDArrayBuilder.add(fileId.toString()));
        final JsonObjectBuilder uploadFilePayloadBuilder = Json.createObjectBuilder().add("materialIds", jsonArrayBuilder.build()).add("fileIds", jsonFileIDArrayBuilder.build()).add("caseURN", "test").add("caseId", randomUUID().toString());
        final JsonObject zipMaterial = uploadFilePayloadBuilder.build();

        restClient.postCommand(ZIP_ENDPOINT, "application/vnd.material.command.zip-material+json",
                zipMaterial.toString(), headers);

        final JsonPath publicEvent = new JsonPath(publicMaterialZipFailedMessageConsumer.retrieveMessage(60000L).get());
        assertThat(publicEvent.get("materialIds").toString(), equalTo(materialIds.toString()));
    }

    @Test
    public void shouldRaiseMaterialZipFailedWhenAnErrorOccurredForNonCps() {
        accessControlStub.setupLoggedInUsersPermissionQueryStub(userId.toString());
        accessControlStub.stubUsersAndGroupsForUserDetail(USER_ID_FOR_META_DATA);
        accessControlStub.stubUsersAndGroupsUserAsSystemUser(userId.toString());
        accessControlStub.stubStructureAsProsecutedBy("TFL");

        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        materialIds.forEach(materialId ->
                jsonArrayBuilder.add(materialId.toString()));

        final JsonArrayBuilder jsonFileIDArrayBuilder = Json.createArrayBuilder();
        fileIds.forEach(fileId ->
                jsonFileIDArrayBuilder.add(fileId.toString()));
        final JsonObjectBuilder uploadFilePayloadBuilder = Json.createObjectBuilder().add("materialIds", jsonArrayBuilder.build()).add("fileIds", jsonFileIDArrayBuilder.build()).add("caseURN", "test").add("caseId", randomUUID().toString());
        final JsonObject zipMaterial = uploadFilePayloadBuilder.build();
        restClient.postCommand(ZIP_ENDPOINT, "application/vnd.material.command.zip-material+json",
                zipMaterial.toString(), headers);

        final JsonPath publicEvent = new JsonPath(publicMaterialZipFailedMessageConsumer.retrieveMessage(60000L).get());
        assertThat(publicEvent.get("materialIds").toString(), equalTo(materialIds.toString()));
    }
}
