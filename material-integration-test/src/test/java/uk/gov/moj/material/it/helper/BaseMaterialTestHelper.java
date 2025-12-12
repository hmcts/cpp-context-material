package uk.gov.moj.material.it.helper;


import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.material.it.util.WiremockAccessControlEndpointStubber.setupUsersGroupQueryStub;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.material.it.util.WiremockAccessControlEndpointStubber;

import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

public class BaseMaterialTestHelper {

    private final static WiremockAccessControlEndpointStubber wiremockAccessControlEndpointStubber = new WiremockAccessControlEndpointStubber();

    protected static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    public static final String MATERIAL_ADDED_EVENT = "material.events.material-added";

    public static final String WRITE_ENDPOINT = "http://" + HOST + ":8080/material-command-api/command/api/rest/material/material";

    public static final String ZIP_ENDPOINT = "http://" + HOST + ":8080/material-command-api/command/api/rest/material/zip-material";

    public static final String BUNDLE_WRITE_ENDPOINT = "http://" + HOST + ":8080/material-command-api/command/api/rest/material/create-material-bundle";
    public static final String DELETE_ENDPOINT = "http://" + HOST + ":8080/material-command-api/command/api/rest/material/material/%s";
    public static final String READ_ENDPOINT = "http://" + HOST + ":8080/material-query-api/query/api/rest/material/material";
    public static final String IS_DOWNLOADABLE_ENDPOINT = "http://" + HOST + ":8080/material-command-api/command/api/rest/material/downloadable-materials";
    public static final String MATERIAL_ID = "materialId";
    public static final String FILE_SERVICE_ID = "fileServiceId";
    public static final String ALFRESCO_ASSET_ID = "alfrescoAssetId";
    public static final String ALFRESCO_ID = "alfrescoId";
    public static final String EXTERNAL_LINK = "externalLink";
    public static final String FILE_NAME = "fileName";
    public static final String FILE_SIZE = "fileSize";
    public static final String DOCUMENT = "document";
    public static final String MIME_TYPE = "mimeType";
    public static final String CONTEXT_NAME = "material";
    public static final String USER_ID_FOR_META_DATA = UUID.randomUUID().toString();


    protected JmsMessageConsumerClient materialAddedEventConsumer;


    protected StringToJsonObjectConverter stringToJsonObjectConverter;
    protected RestClient restClient;
    protected MultivaluedMap<String, Object> headers;

    private static final UUID userId = randomUUID();

    static {
        setupUsersGroupQueryStub();
    }

    public void setup() {
        wiremockAccessControlEndpointStubber.stubUsersAndGroupsUserAsSystemUser(userId.toString());
        wiremockAccessControlEndpointStubber.stubStructureAsCPSProsecutedCase();
        wiremockAccessControlEndpointStubber.setupLoggedInUsersPermissionQueryStub(userId.toString());
        wiremockAccessControlEndpointStubber.stubUsersAndGroupsForUserDetail(USER_ID_FOR_META_DATA);
        headers = new MultivaluedMapImpl<>();
        headers.add(HeaderConstants.USER_ID, userId);
        stringToJsonObjectConverter = new StringToJsonObjectConverter();
        restClient = new RestClient();
        materialAddedEventConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                .withEventNames(MATERIAL_ADDED_EVENT)
                .getMessageConsumerClient();
    }

}
