package uk.gov.moj.cpp.material.event.processor.jobstore.jobdata;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class UploadMaterialToAlfrescoJobDataJsonParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Test
    public void shouldParseToAndFromJson() throws Exception {

        final JsonObject fileUploadedEventMetadata = createObjectBuilder()
                .add("name", "Fred")
                .add("surname", "Bloggs")
                .build();

        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final boolean unbundledDocument = true;

        final UploadMaterialToAlfrescoJobData uploadMaterialToAlfrescoJobData = new UploadMaterialToAlfrescoJobData(
                materialId,
                fileServiceId,
                unbundledDocument,
                fileUploadedEventMetadata, "");

        final String json = objectMapper.writeValueAsString(uploadMaterialToAlfrescoJobData);

        final UploadMaterialToAlfrescoJobData newUploadMaterialToAlfrescoJobData = objectMapper
                .readerFor(UploadMaterialToAlfrescoJobData.class)
                .readValue(json);

        assertThat(newUploadMaterialToAlfrescoJobData.getMaterialId(), is(materialId));
        assertThat(newUploadMaterialToAlfrescoJobData.getFileServiceId(), is(fileServiceId));
        assertThat(newUploadMaterialToAlfrescoJobData.isUnbundledDocument(), is(unbundledDocument));

        final String fileUploadedEventMetadataJson = newUploadMaterialToAlfrescoJobData.getFileUploadedEventMetadata().toString();

        with(fileUploadedEventMetadataJson)
                .assertThat("$.name", is("Fred"))
                .assertThat("$.surname", is("Bloggs"))
        ;
    }
}