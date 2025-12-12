package uk.gov.moj.material.it.helper;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static java.lang.String.format;
import static java.util.List.of;
import static java.util.Map.Entry;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParamswithHeaders;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseHeadersMatcher.headers;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.material.it.util.FileUtil.getDocumentBytesFromFile;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MaterialTestHelper extends BaseMaterialTestHelper {

    public static final String TEXT_URI_LIST = "text/uri-list";
    private String fileName;
    private String mimeType;
    private String externalLink;
    private JsonObject document;
    private boolean isUploadFileTest;
    private JmsMessageConsumerClient publicMessageConsumer;

    public static final long TIMEOUT_MILI_SECONDS = 30000L;
    private static final String IS_UNBUNDLED_DOCUMENT = "isUnbundledDocument";
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MaterialTestHelper.class.getCanonicalName());

    public void setUploadFileProperties(String filePath, String fileName, String mimeType) {

        this.document = Json.createObjectBuilder()
                .add("content", Base64.getEncoder().encodeToString(getDocumentBytesFromFile(filePath)))
                .build();
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.isUploadFileTest = true;
    }

    public void setExternalFileProperties(String externalLink, String fileName) {
        this.externalLink = externalLink;
        this.fileName = fileName;
        this.document = Json.createObjectBuilder().add("externalLink", externalLink).build();
        this.isUploadFileTest = false;
    }

    private JmsMessageConsumerClient createConsumerForPublicEvent(final String eventName) {
        return newPublicJmsMessageConsumerClientProvider()
                .withEventNames(eventName)
                .getMessageConsumerClient();
    }

    public void addMaterial(final String materialId) {
        final String request = buildAddMaterialRequest(materialId);
        publicMessageConsumer = createConsumerForPublicEvent("material.material-added");
        final Response writeResponse = restClient.postCommand(WRITE_ENDPOINT,
                "application/vnd.material.command.add-material+json", request, headers);
        assertThat(writeResponse.getStatus(), is(ACCEPTED.getStatusCode()));
    }

    public void startConsumerForMaterialBundleCreated() {
        publicMessageConsumer = createConsumerForPublicEvent("public.material.material-bundle-created");
    }

    public void startConsumerForMaterialBundleCreationFailed() {
        publicMessageConsumer = createConsumerForPublicEvent("public.material.material-bundle-creation-failed");
    }

    public void createMaterialBundle(String request) {

        final Response writeResponse = restClient.postCommand(BUNDLE_WRITE_ENDPOINT,
                "application/vnd.material.command.create-material-bundle+json", request, headers);
        assertThat(writeResponse.getStatus(), is(ACCEPTED.getStatusCode()));
    }

    public void deleteMaterial(final String materialId, final boolean materialExists) {
        if (materialExists) {
            publicMessageConsumer = createConsumerForPublicEvent("public.material.material-deleted");
        } else {
            publicMessageConsumer = createConsumerForPublicEvent("public.material.material-not-found");
        }

        final Response writeResponse = restClient.postCommand(format(DELETE_ENDPOINT, materialId),
                "application/vnd.material.command.delete-material+json", "{}", headers);
        assertThat(writeResponse.getStatus(), is(ACCEPTED.getStatusCode()));
    }


    public void verifyInActiveMQ(final String materialId) {
        String message = await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .ignoreExceptions()
                .until(() -> materialAddedEventConsumer.retrieveMessage().orElse(null),
                        Objects::nonNull);

        assertMessagePublished(message, materialId);
    }

    public void verifyInPublicTopic(final String materialId) {
        String data = await()
                .atMost(10, SECONDS)
                .pollInterval(200, MILLISECONDS)
                .ignoreExceptions()
                .until(() -> publicMessageConsumer.retrieveMessage().orElse(null),
                        Objects::nonNull);

        JsonPath response = new JsonPath(data);
        assertThat(response.get("materialId"), equalTo(materialId));
    }

    public void verifyBundleInPublicTopic(final String materialId) {
        String data = await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .ignoreExceptions()
                .until(() -> publicMessageConsumer.retrieveMessage().orElse(null),
                        Objects::nonNull);

        JsonPath response = new JsonPath(data);
        assertThat(response.get("bundledMaterialId"), equalTo(materialId));
    }

    public String verifyInPublicTopicAndExtractAlfrescoId(final String materialId) {
        String data = publicMessageConsumer.retrieveMessage().get();
        JsonPath response = new JsonPath(data);
        assertThat(response.get(MATERIAL_ID), equalTo(materialId));
        final String alfrescoAssetId = response.getString("fileDetails.alfrescoAssetId");
        assertThat(alfrescoAssetId, notNullValue());
        return alfrescoAssetId;
    }


    @SuppressWarnings("squid:S2925")
    public void verifyMetadataAdded(final String materialId) throws Exception {
        Response readMetadataResponse = await()
                .atMost(1, SECONDS)
                .pollInterval(100, MILLISECONDS)
                .until(() -> makeMetadataQueryCall(materialId),
                        response -> response.getStatus() != NOT_FOUND.getStatusCode());

        assertMetadata(readMetadataResponse, materialId, fileName, mimeType);
    }

    @SuppressWarnings("squid:S2925")
    public void verifyMetadataDetailAdded(final String materialId) throws Exception {
        Response readMetadataResponse = await()
                .atMost(10, SECONDS)
                .pollInterval(100, MILLISECONDS)
                .until(() -> makeMetadataDetailQueryCall(materialId),
                        response -> response.getStatus() != NOT_FOUND.getStatusCode());

        assertMetadataDetail(readMetadataResponse, materialId, fileName, 6879);
    }

    public void verifyMetadataDetailForNotFound(final String materialId) {
        Response readMetadataResponse;
        readMetadataResponse = makeMetadataDetailQueryCall(materialId);
        assertThat(readMetadataResponse.getStatus(), is(NOT_FOUND.getStatusCode()));
    }

    public void verifyMaterialAdded(final String materialId, final boolean requestPdf) {
        final Response readMaterialResponse =
                restClient.query(READ_ENDPOINT + "/" + materialId,
                        "application/vnd.material.query.material+json", headers);
        assertMaterial(readMaterialResponse);


        RequestParamsBuilder materialPdf = requestParamswithHeaders(
                READ_ENDPOINT + "/" + materialId + "?requestPdf=" + requestPdf,
                "application/vnd.material.query.material+json", headers);


        poll(materialPdf).pollInterval(50, MILLISECONDS)
                .until(status().is(OK), headers().hasHeader(equalToIgnoringCase("Content-Type"),
                        equalTo(of(TEXT_URI_LIST + ";charset=UTF-8"))));

    }

    public void verifyBundleMaterialAdded(final String materialId) {
        final Response readMaterialResponse =
                restClient.query(READ_ENDPOINT + "/" + materialId,
                        "application/vnd.material.query.material+json", headers);
        assertThat(readMaterialResponse.getStatus(), is(OK.getStatusCode()));
        assertThat(readMaterialResponse.getHeaderString(CONTENT_TYPE), startsWith(TEXT_URI_LIST));

        RequestParamsBuilder materialPdf = requestParamswithHeaders(
                READ_ENDPOINT + "/" + materialId + "?requestPdf=true",
                "application/vnd.material.query.material+json", headers);


        poll(materialPdf).until(status().is(OK), headers().hasHeader(equalToIgnoringCase("Content-Type"),
                equalTo(of(TEXT_URI_LIST + ";charset=UTF-8"))));

    }

    public void verifyMaterialsIsDownloadable(final String materialId) {
        final AtomicBoolean result = new AtomicBoolean(false);
        await().atMost(5, SECONDS)
                .pollInterval(100, MILLISECONDS)
                .until(() -> {
                    Response response = pollIsMaterialDownloadable(materialId);
                    JsonObject metadataBodyResponse = stringToJsonObjectConverter.convert(response.readEntity(String.class));
                    result.set(metadataBodyResponse.getJsonObject("materials").getString(materialId).equals("true"));
                    return result.get();
                });


        assertThat(result.get(), is(true));
    }

    private Response pollIsMaterialDownloadable(final String materialId) {
        return await().until(() -> isMaterialDownloadable(materialId), Matchers.hasProperty("status", equalTo(ACCEPTED.getStatusCode())));
    }

    private Response isMaterialDownloadable(final String materialId) {
        return restClient.postCommand(IS_DOWNLOADABLE_ENDPOINT, "application/vnd.material.command.publish-is-downloadable-materials+json",
                Json.createObjectBuilder().add("materialIds", Json.createArrayBuilder().add(materialId).build()).build().toString(), headers);
    }

    public void verifyMaterialExists(final String materialId, final Matcher... matchers) {
        verifyMaterial(materialId, addMatcherToMatcherArray(status().is(OK), matchers));
    }

    public void verifyMaterialDoesNotExist(final String materialId, final Matcher... matchers) {
        verifyMaterial(materialId, addMatcherToMatcherArray(status().is(NOT_FOUND), matchers));
    }

    private Matcher[] addMatcherToMatcherArray(final Matcher matcher, final Matcher[] matchers) {
        final List<Matcher> list = new ArrayList<>();
        list.add(matcher);
        list.addAll(Arrays.asList(matchers));
        return list.toArray(Matcher[]::new);
    }

    public void verifyMaterial(final String materialId, final Matcher... matchers) {

        poll(requestParams(format("%smaterial/%s", getGetQueryBaseUrl(), materialId), "application/vnd.material.query.material+json")
                .withHeaders(toSimpleMap(headers))
                .build())
                .until(matchers).getPayload();
    }

    public void verifyMaterialMetadata(final String materialId, final Matcher... matchers) {

        poll(requestParams(format("%smaterial/%s/metadata", getGetQueryBaseUrl(), materialId),
                "application/vnd.material.query.material-metadata+json")

                .withHeaders(toSimpleMap(headers))

                .build())
                .until(matchers).getPayload();
    }

    private Map<String, Object> toSimpleMap(final MultivaluedMap<String, Object> headers) {

        return headers.entrySet().stream().collect(Collectors.toMap(Entry::getKey, o -> o.getValue().get(0)));

    }

    private String getGetQueryBaseUrl() {
        return format("%s%s", getBaseUri(), "/material-query-api/query/api/rest/material/");
    }


    public void verifyMaterialAddedAsPlainText(final String materialId) {
        final Response readMaterialResponse =
                restClient.query(READ_ENDPOINT + "/" + materialId,
                        "application/vnd.material.query.material+json", headers);
        assertMaterial(readMaterialResponse);


        RequestParamsBuilder materialPdf = requestParamswithHeaders(
                READ_ENDPOINT + "/" + materialId,
                "application/vnd.material.query.material+json", headers);

        poll(materialPdf).until(status().is(OK), headers().hasHeader(equalToIgnoringCase("Content-Type"),
                equalTo(of(TEXT_URI_LIST + ";charset=UTF-8"))));
    }

    private Response makeMetadataQueryCall(final String materialId) {
        return restClient.query(READ_ENDPOINT + "/" + materialId + "/metadata",
                "application/vnd.material.query.material-metadata+json", headers);
    }

    private Response makeMetadataDetailQueryCall(final String materialId) {
        return restClient.query(READ_ENDPOINT + "/" + materialId + "/metadata",
                "application/vnd.material.query.material-metadata-details+json", headers);
    }

    private String buildAddMaterialRequest(final String materialId) {
        JsonObjectBuilder materialBuilder = Json.createObjectBuilder();
        materialBuilder.add(MATERIAL_ID, materialId);
        materialBuilder.add(DOCUMENT, document);
        materialBuilder.add(FILE_NAME, fileName);

        return materialBuilder.build().toString();
    }


    public String buildCreateMaterialBundleRequest(String bundleMaterialId, List<String> materialIds) {
        JsonObjectBuilder materialBuilder = Json.createObjectBuilder();
        materialBuilder.add("bundledMaterialId", bundleMaterialId);
        materialBuilder.add("bundledMaterialName", "Barkingside Magistrates' Court 17072021.pdf");
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        materialIds.stream().map(jsonArrayBuilder::add).toList();
        materialBuilder.add("materialIds", jsonArrayBuilder.build());

        return materialBuilder.build().toString();
    }

    private void assertMessagePublished(final String message, final String expectedMaterialId) {
        JsonObject responsePayload = stringToJsonObjectConverter.convert(message);
        assertThat(responsePayload.getString(MATERIAL_ID), equalTo(expectedMaterialId));

        final JsonObject fileDetails = responsePayload.getJsonObject("fileDetails");

        if (isUploadFileTest) {
            assertMaterialContainingUploadableFile(fileDetails);
        } else {
            assertMaterialContainingExternalLink(fileDetails);
        }
    }

    private void assertMetadata(final Response readMetadataResponse,
                                final String expectedMaterialId, final String expectedFilename,
                                final String expectedMimeType) {
        assertThat(readMetadataResponse.getStatus(), is(OK.getStatusCode()));
        JsonObject metadataBodyResponse = stringToJsonObjectConverter
                .convert(readMetadataResponse.readEntity(String.class));
        assertThat(metadataBodyResponse.getString(FILE_NAME), equalTo(expectedFilename));
        assertThat(metadataBodyResponse.getString(MATERIAL_ID), equalTo(expectedMaterialId));
        assertThat(metadataBodyResponse.getString("materialAddedDate"), is(notNullValue()));

        if (isUploadFileTest) {
            assertMaterialContainingUploadableFile(metadataBodyResponse);
            assertThat(metadataBodyResponse.getString("mimeType"), is(expectedMimeType));
        } else {
            assertMaterialContainingExternalLink(metadataBodyResponse);
        }
    }

    private void assertMetadataDetail(final Response readMetadataResponse,
                                      final String expectedMaterialId, final String expectedFilename, final int expectedFileSize) {
        assertThat(readMetadataResponse.getStatus(), is(OK.getStatusCode()));
        JsonObject metadataBodyResponse = stringToJsonObjectConverter
                .convert(readMetadataResponse.readEntity(String.class));
        assertThat(metadataBodyResponse.getString(FILE_NAME), equalTo(expectedFilename));
        assertThat(metadataBodyResponse.getString(MATERIAL_ID), equalTo(expectedMaterialId));
        assertThat(metadataBodyResponse.getInt(FILE_SIZE), equalTo(expectedFileSize));
    }

    private void assertMaterial(final Response readMaterialResponse) {
        assertThat(readMaterialResponse.getStatus(), is(OK.getStatusCode()));
        assertThat(readMaterialResponse.getHeaderString(CONTENT_TYPE), startsWith(MaterialTestHelper.TEXT_URI_LIST));

        final String actualDocument = readMaterialResponse.readEntity(String.class);

        assertThat("Document content", actualDocument, containsString("https://sastelargefiles.blob.core.windows.net/largefiles-blob-container/"));
    }

    private void assertMaterialContainingExternalLink(JsonObject payload) {
        assertThat(payload.getString(EXTERNAL_LINK), is(externalLink));
        assertThat(payload.get(ALFRESCO_ASSET_ID), is(nullValue()));
        assertThat(payload.get(MIME_TYPE), is(nullValue()));
    }

    private void assertMaterialContainingUploadableFile(JsonObject payload) {
        assertThat(payload.getString(MIME_TYPE), is(mimeType));
        assertThat(payload.getString(ALFRESCO_ASSET_ID), is(notNullValue()));
        assertThat(payload.get(EXTERNAL_LINK), is(nullValue()));
    }

    public void verifyMaterialDeletedEventInPublicTopic(final String alfrescoId, final String materialId) {
        String data = publicMessageConsumer.retrieveMessage().get();
        LOGGER.info("Expected materialId: \n\n\t{}", materialId);

        JsonPath response = new JsonPath(data);
        assertThat(response.get(MATERIAL_ID), equalTo(materialId));
        assertThat(response.get(ALFRESCO_ID), equalTo(alfrescoId));
    }

    public void verifyEventInPublicTopic(final Matcher matcher) {
        String data = publicMessageConsumer.retrieveMessage().get();

        assertThat(data, isJson(matcher));
    }

    public String getMaterialIdFromPublicTopic() {
        String data = publicMessageConsumer.retrieveMessage(TIMEOUT_MILI_SECONDS).get();
        JsonPath response = new JsonPath(data);
        return response.get(MATERIAL_ID);
    }

    public void uploadFile(final UUID fileServiceId, final String materialId) {
        final JsonObjectBuilder uploadFilePayloadBuilder = Json.createObjectBuilder()
                .add("materialId", materialId)
                .add("fileServiceId", fileServiceId.toString())
                .add(MATERIAL_ID, materialId)
                .add(FILE_SERVICE_ID, fileServiceId.toString())
                .add(IS_UNBUNDLED_DOCUMENT, true);

        final JsonObject uploadFilePayload = uploadFilePayloadBuilder.build();
        restClient.postCommand(WRITE_ENDPOINT, "application/vnd.material.command.upload-file+json",
                uploadFilePayload.toString(), headers);
    }


}
