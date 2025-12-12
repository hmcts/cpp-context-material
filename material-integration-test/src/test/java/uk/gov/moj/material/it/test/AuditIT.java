package uk.gov.moj.material.it.test;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.material.it.helper.MaterialTestHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuditIT extends BaseIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditIT.class);

    private static final String FILENAME_TXT = "MaterialFullStackTestFile.txt";
    private static final String MIME_TYPE_TXT = "text/plain";
    private static final String FILE_PATH_TXT = "upload_samples/sample.txt";

    private static JmsMessageConsumerClient auditMessageConsumerClient;
    private MaterialTestHelper testHelper;
    private String materialId;


    @BeforeAll
    public static void beforeAll() {
        auditMessageConsumerClient = newPrivateJmsMessageConsumerClientProvider("auditing")
                .withEventNames("audit.events.audit-recorded")
                .getMessageConsumerClient();

    }

    @BeforeEach
    public void setup() {
        materialId = randomUUID().toString();
        testHelper = new MaterialTestHelper();
        testHelper.setup();
    }

    @Test
    public void shouldAddEntriesToAuditTopic_whenMaterialIsCreated_AndQueriedFor() throws Exception {
        testHelper.setUploadFileProperties(FILE_PATH_TXT, FILENAME_TXT, MIME_TYPE_TXT);
        // invoke the command api
        testHelper.addMaterial(materialId);
        verifyAuditRecordedForCommand(materialId);

        verifyNoMoreMessagesAreOnTopic();

        // invoke the query api
        testHelper.verifyMetadataAdded(materialId);

        verifyAuditRecordedForQuery(materialId);

        verifyAuditRecordedForQueryResponses(materialId);

        verifyNoMoreMessagesAreOnTopic();
    }

    private void verifyAuditRecordedForCommand(final String materialId) {
        String message = retrieveLatestMessage();

        LOGGER.info("Command: {} consuming message {}", "COMMAND_API", message);
        assertThat(message, notNullValue());

        with(message)
                .assertThat("$.content", notNullValue())
                .assertThat("$.content.materialId", is(materialId))
                .assertThat("$.content.fileName", is(FILENAME_TXT))
                .assertThat("$.content.document.content", notNullValue())
                .assertThat("$.content._metadata.name", is("material.command.add-material"))
                .assertThat("$.component", is("COMMAND_API"))
                .assertThat("$.timestamp", startsWith(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
    }

    private void verifyAuditRecordedForQuery(final String materialId) {
        String message = retrieveLatestMessage();

        LOGGER.info("Query: {} consuming message {}", "QUERY_API", message);
        assertThat(message, notNullValue());

        with(message)
                .assertThat("$.content", notNullValue())
                .assertThat("$.content.materialId", is(materialId))
                .assertThat("$.content._metadata.name", is("material.query.material-metadata"))
                .assertThat("$.component", is("QUERY_API"))
                .assertThat("$.timestamp", startsWith(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
    }

    private void verifyAuditRecordedForQueryResponses(final String materialId) {
        // retrieve messages until topic is empty.
        List<String> messages = new ArrayList<>();
        String message;

        while ((message = retrieveLatestMessage()) != null) {
            messages.add(message);
        }

        // grab the last entry which happen to be the outbound review response messages that we are interested in
        final List<String> responses = messages.subList(messages.size() - 1, messages.size());

        assertThat(responses.size(), is(1));

        verifyResponseQuery(responses.get(0), materialId);
    }

    private void verifyResponseQuery(final String response, final String materialId) {
        LOGGER.info("Query Response: {} consuming message {}", "QUERY_API", response);

        with(response)
                .assertThat("$.content", notNullValue())
                .assertThat("$.content.materialId", is(materialId))
                .assertThat("$.content._metadata.name", is("material.query.material-metadata-response"))
                .assertThat("$.component", is("QUERY_API"))
                .assertThat("$.timestamp", startsWith(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));

    }

    private void verifyNoMoreMessagesAreOnTopic() {
        assertThat(retrieveLatestMessage(), nullValue());
    }

    private String retrieveLatestMessage() {
        return auditMessageConsumerClient.retrieveMessage(500).orElse(null);
    }
}
