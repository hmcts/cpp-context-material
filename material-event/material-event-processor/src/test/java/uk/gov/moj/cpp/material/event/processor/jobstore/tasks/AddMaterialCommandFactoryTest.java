package uk.gov.moj.cpp.material.event.processor.jobstore.tasks;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.SuccessfulMaterialUploadJobData;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddMaterialCommandFactoryTest {

    private static final String EVENT_SOURCE = "material";

    @InjectMocks
    private AddMaterialCommandFactory addMaterialCommandFactory;

    @Test
    public void shouldCreateAddMaterialCommandEnvelopeWithExistingMetadata() {

        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final UUID alfrescoFileId = randomUUID();
        final UUID userId = randomUUID();
        final boolean unbundledDocument = false;
        final String fileName = "alfresco01.pdf";
        final String mediaType = "application/pdf";

        final JsonObject fileUploadedEventMetadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(materialId)
                .withUserId(userId.toString())
                .withName("someEventName")
                .withSource(EVENT_SOURCE)
                .build()
                .asJsonObject();

        final SuccessfulMaterialUploadJobData jobState = new SuccessfulMaterialUploadJobData(
                materialId,
                fileServiceId,"" ,
                alfrescoFileId,
                unbundledDocument,
                fileName,
                mediaType, fileUploadedEventMetadata);

        final JsonEnvelope commandEnvelope = addMaterialCommandFactory.createCommandEnvelope(jobState);

        final Metadata metadata = commandEnvelope.metadata();
        assertThat(metadata.name(), is("material.add-material"));
        assertThat(metadata.streamId(), is(of(materialId)));
        assertThat(metadata.userId(), is(of(userId.toString())));
        assertThat(metadata.source(), is(of("material")));

        final JsonObject payload = commandEnvelope.payloadAsJsonObject();
        assertThat(payload.getString("materialId"), is(materialId.toString()));
        assertThat(payload.getString("fileName"), is(fileName));
        assertThat(payload.getBoolean("isUnbundledDocument"), is(unbundledDocument));
        assertThat(payload.getJsonObject("document").getString("fileReference"), is(alfrescoFileId.toString()));
        assertThat(payload.getJsonObject("document").getString("mimeType"), is(mediaType));
    }
}