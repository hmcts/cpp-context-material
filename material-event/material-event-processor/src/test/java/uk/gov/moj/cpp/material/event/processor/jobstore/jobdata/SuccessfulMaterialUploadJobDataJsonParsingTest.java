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

public class SuccessfulMaterialUploadJobDataJsonParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Test
    public void shouldParseToAndFromJson() throws Exception {

        final JsonObject fileUploadedEventMetadata = createObjectBuilder()
                .add("name", "Fred")
                .add("surname", "Bloggs")
                .build();

        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final UUID alfrescoFileId = randomUUID();
        final boolean unbundledDocument = true;
        final String fileName = "file name";
        final String mediaType = "application/pdf";

        final SuccessfulMaterialUploadJobData sendMaterialToAlfrescoJobState = new SuccessfulMaterialUploadJobData(
                materialId,
                fileServiceId,"" ,
                alfrescoFileId,
                unbundledDocument,
                fileName,
                mediaType,
                fileUploadedEventMetadata);

        final String json = objectMapper.writeValueAsString(sendMaterialToAlfrescoJobState);

        final SuccessfulMaterialUploadJobData newSendMaterialToAlfrescoJobState = objectMapper
                .readerFor(SuccessfulMaterialUploadJobData.class)
                .readValue(json);

        assertThat(newSendMaterialToAlfrescoJobState.getMaterialId(), is(materialId));
        assertThat(newSendMaterialToAlfrescoJobState.getFileServiceId(), is(fileServiceId));
        assertThat(newSendMaterialToAlfrescoJobState.getAlfrescoFileId(), is(alfrescoFileId));
        assertThat(newSendMaterialToAlfrescoJobState.isUnbundledDocument(), is(unbundledDocument));

        final String fileUploadedEventMetadataJson = newSendMaterialToAlfrescoJobState.getFileUploadedEventMetadata().toString();

        with(fileUploadedEventMetadataJson)
                .assertThat("$.name", is("Fred"))
                .assertThat("$.surname", is("Bloggs"))
        ;
    }
}