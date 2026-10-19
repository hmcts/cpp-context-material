package uk.gov.moj.material.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.matchesPattern;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.material.it.helper.StructuredFormTestHelper.QUERY_STRUCTURED_FORM_ENDPOINT;
import static uk.gov.moj.material.it.helper.StructuredFormTestHelper.USER_ID;
import static uk.gov.moj.material.it.helper.TestHelper.postMessageToTopicAndVerify;
import static uk.gov.moj.material.it.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.material.it.helper.MaterialTestHelper;
import uk.gov.moj.material.it.helper.StructuredFormTestHelper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;

/**
 * J17/J25 parity characterisation (J17 golden master) against the deployed service.
 *
 * MA-I01 (BC-08, MA-P04): the structured-form and structured-form-history queries format timestamps read back from
 * PostgreSQL with a literal 'Z' after the local fields, so a value read back in a non-UTC zone would be returned
 * with the wrong hour. Pin the J17 format and that the value is the UTC time the form was written.
 * MA-I02 (BC-08, MA-P05): the material-metadata query returns dateMaterialAdded through the framework mapper.
 * MA-I03 (BC-16): the framework metrics endpoints under /internal/metrics.
 */
public class JavaUpgradeParityIT extends BaseIT {

    private static final String FORMATTED_UTC = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";
    private static final DateTimeFormatter LITERAL_Z_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final String METRICS_PING_URL = "http://localhost:8080/material-service/internal/metrics/ping";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Test
    public void maI01_structuredFormTimestampsShouldBeTheUtcWriteTimeInTheJ17Format() {
        final UUID structuredFormId = randomUUID();
        final UUID formId = randomUUID();
        final ZonedDateTime before = ZonedDateTime.now(UTC);
        final String petFormCreated = getPayload("stub-data/received-public-events/public.progression.pet-form-created.json")
                .replaceAll("CASE_ID", randomUUID().toString())
                .replaceAll("PET_ID", structuredFormId.toString())
                .replaceAll("FORM_ID", formId.toString())
                .replaceAll("DEFENDANT_ID", randomUUID().toString())
                .replaceAll("USER_ID", USER_ID);
        postMessageToTopicAndVerify(petFormCreated, "material.events.structured-form-created", "public.progression.pet-form-created", true);

        final JsonObject structuredForm = stringToJsonObjectConverter.convert(
                poll(requestParams(String.format(QUERY_STRUCTURED_FORM_ENDPOINT, structuredFormId), "application/vnd.material.query.structured-form+json")
                        .withHeader("CJSCPPUID", USER_ID))
                        .pollInterval(50, MILLISECONDS)
                        .timeout(30, SECONDS)
                        .until(status().is(OK), payload().isJson(withJsonPath("$.formId", equalTo(formId.toString()))))
                        .getPayload());
        final StructuredFormTestHelper structuredFormTestHelper = new StructuredFormTestHelper();
        structuredFormTestHelper.setup();
        final JsonObject history = structuredFormTestHelper.queryStructuredFormHistory(structuredFormId);

        final String lastUpdated = structuredForm.getString("lastUpdated");
        final String historyDate = history.getJsonArray("structuredFormChangeHistory").getJsonObject(0).getString("date");
        assertThat(lastUpdated, matchesPattern(FORMATTED_UTC));
        assertThat(historyDate, matchesPattern(FORMATTED_UTC));
        assertThat(minutesFrom(before, lastUpdated), lessThan(5L));
        assertThat(minutesFrom(before, historyDate), lessThan(5L));
    }

    @Test
    public void maI02_materialMetadataShouldReturnDateMaterialAddedInTheJ17Format() {
        final String materialId = randomUUID().toString();
        final MaterialTestHelper materialTestHelper = new MaterialTestHelper();
        materialTestHelper.setup();
        materialTestHelper.setUploadFileProperties("upload_samples/sample.pdf", "MaterialFullStackTestFile.pdf", "application/pdf");
        materialTestHelper.addMaterial(materialId);

        materialTestHelper.verifyMaterialMetadata(materialId,
                status().is(OK),
                payload().isJson(withJsonPath("$.materialAddedDate", matchesPattern(FORMATTED_UTC))));
    }

    @Test
    public void maI03_metricsPingShouldAnswerWithTheJ17Status() throws IOException, InterruptedException {
        final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(METRICS_PING_URL)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(200));
    }

    // the response time has a literal 'Z'; read it as UTC and compare with when the test wrote the form
    private static long minutesFrom(final ZonedDateTime before, final String formatted) {
        return Math.abs(Duration.between(before, LocalDateTime.parse(formatted, LITERAL_Z_FORMAT).atZone(UTC)).toMinutes());
    }
}
