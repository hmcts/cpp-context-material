package uk.gov.moj.material.it.helper;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


public class MaterialRestClient {

    protected final String URL = getBaseUri() + "/material-command-api/command/api/rest/material/material-reference";

    private final RestClient restClient = new RestClient();

    public void postCommand(final UUID userId, final String commandName, final JsonObject payload) {

        final Metadata metadata = metadataWithRandomUUID(commandName)
                .withUserId(userId.toString())
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);

        final String requestPayload = jsonEnvelope.toDebugStringPrettyPrint();
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, userId);

        final String contentType = "application/vnd." + commandName + "+json";

        final Response response = restClient.postCommand(URL, contentType, requestPayload, headers);
        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));

    }
}
