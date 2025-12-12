package uk.gov.justice.api.resource;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.messaging.JsonEnvelope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class MaterialEnvelopeFactoryTest {


    @InjectMocks
    private MaterialEnvelopeFactory materialEnvelopeFactory;

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateTheAccessControlRequestEnvelope() throws Exception {

        final String userId = randomUUID().toString();
        final String materialId = randomUUID().toString();
        final JsonEnvelope envelope = materialEnvelopeFactory.buildEnvelope(
                userId,
                materialId);

        assertThat(envelope, jsonEnvelope(
                metadata()
                        .withUserId(userId)
                        .withName("material.query.material"),
                payloadIsJson(allOf(
                        withJsonPath("$.materialId", is(materialId))
                ))));
    }
}
