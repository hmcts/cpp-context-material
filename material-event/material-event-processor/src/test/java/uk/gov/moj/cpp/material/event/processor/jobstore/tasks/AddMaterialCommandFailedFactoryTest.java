package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.FailedMaterialUploadJobData;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddMaterialCommandFailedFactoryTest {
    private static final String EVENT_SOURCE = "material";

    @InjectMocks
    private AddMaterialCommandFailedFactory addMaterialCommandFailedFactory;

    @Test
    public void shouldCreateAddMaterialCommandEnvelopeWithExistingMetadata() {

        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final UUID userId = randomUUID();
        final String errorMessage = "error";
        final ZonedDateTime failedTime = new UtcClock().now();

        final JsonObject fileUploadedEventMetadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(materialId)
                .withUserId(userId.toString())
                .withName("someEventName")
                .withSource(EVENT_SOURCE)
                .build()
                .asJsonObject();

        final FailedMaterialUploadJobData jobState = new FailedMaterialUploadJobData(
                materialId,
                fileServiceId,
                "",
                fileUploadedEventMetadata,
                errorMessage,
                failedTime);

        final JsonEnvelope commandEnvelope = addMaterialCommandFailedFactory.createCommandEnvelope(jobState);

        final Metadata metadata = commandEnvelope.metadata();
        assertThat(metadata.name(), is("material.record-add-material-failed"));
        assertThat(metadata.streamId(), is(of(materialId)));
        assertThat(metadata.userId(), is(of(userId.toString())));
        assertThat(metadata.source(), is(of("material")));

        final JsonObject payload = commandEnvelope.payloadAsJsonObject();
        assertThat(payload.getString("materialId"), is(materialId.toString()));
        assertThat(payload.getString("fileServiceId"), is(fileServiceId.toString()));
        assertThat(payload.getString("errorMessage"), is(errorMessage));
        assertThat(payload.getString("failedTime"), is(failedTime.truncatedTo(ChronoUnit.MILLIS).toString()));
    }
}