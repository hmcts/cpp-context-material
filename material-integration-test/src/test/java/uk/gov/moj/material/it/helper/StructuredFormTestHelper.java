package uk.gov.moj.material.it.helper;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import java.util.UUID;

import javax.json.JsonObject;


public class StructuredFormTestHelper extends BaseMaterialTestHelper {
    public static final String USER_ID = randomUUID().toString();
    public static final String FINALISED = "FINALISED";
    public static final String QUERY_STRUCTURED_FORM_ENDPOINT = "http://" + HOST + ":8080/material-query-api/query/api/rest/material/structured-form/%s";
    public static final String QUERY_STRUCTURED_FORM_DEFENDANT_USER_ENDPOINT = "http://" + HOST + ":8080/material-query-api/query/api/rest/material/structured-form/%s/defendant/%s";
    public static final String QUERY_STRUCTURED_FORM_HISTORY_ENDPOINT = "http://" + HOST + ":8080/material-query-api/query/api/rest/material/structured-form/%s/history";

    public void queryAndVerifyStructuredFormDetail(final UUID structuredFormId, final UUID formId, final int defendantSize) {
        final String payload = poll(requestParams(String.format(QUERY_STRUCTURED_FORM_ENDPOINT, structuredFormId), "application/vnd.material.query.structured-form+json")
                .withHeader("CJSCPPUID", USER_ID))
                .pollInterval(50, MILLISECONDS)
                .timeout(30, SECONDS)
                .until(status().is(OK),
                        payload().isJson(withJsonPath("$.formId", equalTo(formId.toString()))))
                .getPayload();

        assertThat(payload, hasJsonPath("$.data"));

        final String dataStringFromPayload = stringToJsonObjectConverter.convert(payload).getString("data");
        with(dataStringFromPayload).assertThat("$.defendants", hasSize(defendantSize));
    }

    public void queryAndVerifyStructuredFormDetailAfterUpdate(final UUID structuredFormId, final UUID formId, final int defendantSize) {
        final String payload = poll(requestParams(String.format(QUERY_STRUCTURED_FORM_ENDPOINT, structuredFormId), "application/vnd.material.query.structured-form+json")
                .withHeader("CJSCPPUID", USER_ID))
                .pollInterval(50, MILLISECONDS)
                .timeout(30, SECONDS)
                .until(status().is(OK),
                        payload().isJson(withJsonPath("$.formId", equalTo(formId.toString()))))
                .getPayload();

        assertThat(payload, hasJsonPath("$.data"));

        final String dataStringFromPayload = stringToJsonObjectConverter.convert(payload).getString("data");
        with(dataStringFromPayload).assertThat("$.data.defence.defendants", hasSize(defendantSize));
        with(dataStringFromPayload).assertThat("$.petFormUpdatedForTest", is(true));
    }

    public void queryAndVerifyStructuredFormDetailAfterDefendantUpdate(final UUID structuredFormId, final UUID formId) {
        final String payload = poll(requestParams(String.format(QUERY_STRUCTURED_FORM_ENDPOINT, structuredFormId), "application/vnd.material.query.structured-form+json")

                .withHeader("CJSCPPUID", USER_ID))
                .pollInterval(50, MILLISECONDS)
                .timeout(30, SECONDS)
                .until(status().is(OK),
                        payload().isJson(withJsonPath("$.formId", equalTo(formId.toString()))))
                .getPayload();

        assertThat(payload, hasJsonPath("$.data"));

        final String dataStringFromPayload = stringToJsonObjectConverter.convert(payload).getString("data");
        assertThat(dataStringFromPayload, hasJsonPath("$.data.defence.defendants"));
        with(dataStringFromPayload).assertThat("$.data.defence.defendants[0].defenceDynamicFormAnswers", hasKey("someField"));

    }

    public void queryAndVerifyStructuredFormDetailAfterFormUpdate(final UUID structuredFormId, final UUID formId) {
        final String payload = poll(requestParams(format(QUERY_STRUCTURED_FORM_ENDPOINT, structuredFormId), "application/vnd.material.query.structured-form+json")
                .withHeader("CJSCPPUID", USER_ID))
                .pollInterval(50, MILLISECONDS)
                .timeout(30, SECONDS)
                .until(status().is(OK),
                        payload().isJson(withJsonPath("$.formId", equalTo(formId.toString()))))
                .getPayload();

        assertThat(payload, hasJsonPath("$.data"));
        final String dataStringFromPayload = stringToJsonObjectConverter.convert(payload).getString("data");
        with(dataStringFromPayload).assertThat("$.formId", equalTo("FORM_UPDATED_FOR_TEST"));
    }

    public void queryAndVerifyStructuredFormDefendantUser(final UUID structuredFormId, final UUID defendantId) {
        poll(requestParams(String.format(QUERY_STRUCTURED_FORM_DEFENDANT_USER_ENDPOINT, structuredFormId, defendantId), "application/vnd.material.query.structured-form-defendant-user+json")
                .withHeader("CJSCPPUID", USER_ID))
                .pollInterval(50, MILLISECONDS)
                .timeout(30, SECONDS)
                .until(status().is(OK),
                        payload().isJson(withJsonPath("$.defendantUser[0].id", equalTo(defendantId.toString()))))
                .getPayload();
    }

    public JsonObject queryStructuredFormHistory(final UUID structuredFormId) {
        final String payload = poll(requestParams(String.format(QUERY_STRUCTURED_FORM_HISTORY_ENDPOINT, structuredFormId), "application/vnd.material.query.structured-form-change-history+json")
                .withHeader("CJSCPPUID", USER_ID))
                .pollInterval(50, MILLISECONDS)
                .timeout(30, SECONDS)
                .until(status().is(OK))
                .getPayload();

        assertThat(payload, hasJsonPath("$.structuredFormChangeHistory"));
        return stringToJsonObjectConverter.convert(payload);
    }

    public void queryAndVerifyStructuredFormDetailAfterFormFinalised(UUID structuredFormId, UUID formId) {
        final String payload = poll(requestParams(format(QUERY_STRUCTURED_FORM_ENDPOINT, structuredFormId), "application/vnd.material.query.structured-form+json")
                .withHeader("CJSCPPUID", USER_ID))
                .pollInterval(50, MILLISECONDS)
                .timeout(30, SECONDS)
                .until(status().is(OK),
                        payload().isJson(withJsonPath("$.formId", equalTo(formId.toString()))))
                .getPayload();

        assertThat(payload, hasJsonPath("$.data"));

        final String dataStringFromPayload = stringToJsonObjectConverter.convert(payload).getString("data");
        with(dataStringFromPayload).assertThat("$.formId", equalTo("FORM_UPDATED_FOR_TEST"));
    }
}
