package uk.gov.moj.cpp.material.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.domain.event.FailedToAddMaterial;
import uk.gov.moj.cpp.material.domain.event.MaterialNotFound;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class MaterialEventProcessorMaterialNotFoundTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
    private static final ObjectToJsonValueConverter pojoToJsonconverter = new ObjectToJsonValueConverter(OBJECT_MAPPER);

    @Mock
    private Sender sender;

    @Mock
    private Logger logger;

    @InjectMocks
    private MaterialEventProcessor materialEventListener;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @Test
    public void shouldHandleMaterialDeleted() {
        final UUID materialId = randomUUID();

        final MaterialNotFound materialNotFound = new MaterialNotFound(materialId);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("material.events.material-not-found"),
                pojoToJsonconverter.convert(materialNotFound)
        );

        materialEventListener.materialNotFound(envelope);

        verify(sender).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.material.material-not-found"));
        assertThat(envelopeCaptor.getValue().payload(), payload().isJson(withJsonPath("$.materialId", is(materialId.toString()))));
    }

    @Test
    public void shouldHandleFailedToAddMaterial() {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final String errorMessage = "error";
        final ZonedDateTime failedTime = new UtcClock().now();

        final FailedToAddMaterial failedToAddMaterial = new FailedToAddMaterial(materialId, fileServiceId, failedTime, errorMessage);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("material.events.failed-to-add-material"),
                pojoToJsonconverter.convert(failedToAddMaterial)
        );

        materialEventListener.failedToAddMaterial(envelope);

        verify(sender).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.events.material.failed-to-add-material"));
        assertThat(envelopeCaptor.getValue().payload(), payload()
                .isJson(CoreMatchers.allOf(
                        withJsonPath("$.materialId", is(materialId.toString())),
                        withJsonPath("$.fileServiceId", is(fileServiceId.toString())),
                        withJsonPath("$.failedTime", is(failedTime.truncatedTo(ChronoUnit.MILLIS).toString())),
                        withJsonPath("$.errorMessage", is(errorMessage))
                )));
    }
}
