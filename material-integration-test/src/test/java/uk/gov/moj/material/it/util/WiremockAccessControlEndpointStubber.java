package uk.gov.moj.material.it.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.material.it.util.FileUtil.getPayload;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

/**
 * Utility class for setting stubs.
 */
public class WiremockAccessControlEndpointStubber {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final int HTTP_STATUS_OK = 200;

    public WiremockAccessControlEndpointStubber() {
        configureFor(HOST, 8080);
        reset();
    }


    public void stubUsersAndGroupsUserAsSystemUser(String userId) {

        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/" + userId + "/groups"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.get-groups-by-user.json"))));

    }

    public static void setupUsersGroupQueryStub() {
        stubFor(get(urlPathMatching("/usersgroups-service/query/api/rest/usersgroups/users/.*/groups"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.get-groups-by-user.json"))));
    }


    public void stubUsersAndGroupsForUserDetail(String userId) {
        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/" + userId))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.get-user-details.json")
                                .replace("USER_ID", UUID.randomUUID().toString()))));
    }


    public void setupLoggedInUsersPermissionQueryStub(final String userId) {

        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader(ID, userId)
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(resourceToString("stub-data/usersgroups.user-permissions.json"))));


    }

    public void setupLoggedInUsersPermissionQueryStubV1(final String userId) {

        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader(ID, userId)
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(resourceToString("stub-data/usersgroups.user-permissions-noncps.json"))));

    }

    public String resourceToString(final String path, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(path)) {
            return format(IOUtils.toString(systemResourceAsStream), placeholders);
        } catch (final IOException e) {
            fail("Error consuming file from location " + path);
            throw new UncheckedIOException(e);
        }
    }

    public void stubStructureAsCPSProsecutedCase() {
        stubStructureAsProsecutedBy("CPS");
    }

    public void stubStructureAsProsecutedBy(final String prosecutorName) {

        stubFor(get(urlPathEqualTo("/structure-service/query/api/rest/structure/search"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.structure.query.cases-search-by-material-id+json")
                        .withBody(getPayload("stub-data/structure.query.cases-search-by-material-id.json")
                                .replace("CASE_ID", UUID.randomUUID().toString())
                                .replace("PROSECUTING_AUTHORITY", prosecutorName))));
        stubFor(get(urlPathEqualTo("/sjp-service/query/api/rest/sjp/search"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.sjp.query.cases-search-by-material-id+json")
                        .withBody(getPayload("stub-data/structure.query.cases-search-by-material-id.json")
                                .replace("CASE_ID", UUID.randomUUID().toString())
                                .replace("PROSECUTING_AUTHORITY", prosecutorName))));
        stubFor(get(urlPathEqualTo("/hearing-service/query/api/rest/hearing/search"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.hearing.query.search-by-material-id+json")
                        .withBody(getPayload("stub-data/hearing.query.search-by-material-id.json"))));
    }
}
