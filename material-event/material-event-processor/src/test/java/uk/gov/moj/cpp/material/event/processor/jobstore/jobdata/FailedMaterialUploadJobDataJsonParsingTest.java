package uk.gov.moj.cpp.material.event.processor.jobstore.jobdata;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class FailedMaterialUploadJobDataJsonParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Test
    public void shouldParseToAndFromJson() throws Exception {

        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final ZonedDateTime failedTime = new UtcClock().now();
        final JsonObject fileUploadedEventMetadata = createObjectBuilder()
                .add("name", "Fred")
                .add("surname", "Bloggs")
                .build();

        final String errorMessage = "help help it all went wrong";
        final FailedMaterialUploadJobData sendMaterialToAlfrescoJobState = new FailedMaterialUploadJobData(
                materialId,
                fileServiceId,
                "",
                fileUploadedEventMetadata,
                errorMessage,
                failedTime
        );

        final String json = objectMapper.writeValueAsString(sendMaterialToAlfrescoJobState);

        final FailedMaterialUploadJobData newFailedMaterialUploadJobData = objectMapper
                .readerFor(FailedMaterialUploadJobData.class)
                .readValue(json);

        assertThat(newFailedMaterialUploadJobData.getMaterialId(), is(materialId));
        assertThat(newFailedMaterialUploadJobData.getFileServiceId(), is(fileServiceId));
        assertThat(newFailedMaterialUploadJobData.getErrorMessage(), is(errorMessage));
        assertThat(newFailedMaterialUploadJobData.getFailedTime().truncatedTo(ChronoUnit.MILLIS).toInstant(), is(failedTime.truncatedTo(ChronoUnit.MILLIS).toInstant()));

        final String fileUploadedEventMetadataJson = newFailedMaterialUploadJobData.getFileUploadedEventMetadata().toString();

        with(fileUploadedEventMetadataJson)
                .assertThat("$.name", is("Fred"))
                .assertThat("$.surname", is("Bloggs"))
        ;
    }
}