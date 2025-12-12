package uk.gov.moj.material.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;

import java.util.Optional;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class TestHelper {

    private static final StringToJsonObjectConverter STRING_TO_JSON_CONVERTER = new StringToJsonObjectConverter();

    @SafeVarargs
    public static void postMessageToTopicAndVerify(final String payload, final String eventName, final String commandName,
                                                   final boolean verify, final Matcher<? super ReadContext>... matchers) {


        final JmsMessageConsumerClient caseManagement = newPrivateJmsMessageConsumerClientProvider("material")
                .withEventNames(eventName)
                .getMessageConsumerClient();
        final JmsMessageProducerClient JMS_PRODUCER_CLIENT = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        JMS_PRODUCER_CLIENT.sendMessage(commandName, STRING_TO_JSON_CONVERTER.convert(payload));
        if (verify) {
            Optional<String> message = caseManagement.retrieveMessage(30000L);
            assertThat(eventName + " message not found in jms.topic.material.events topic", message.isPresent(), is(true));
            assertThat(message.get(), isJson(allOf(matchers)));
        }
    }
}
