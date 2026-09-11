package uk.gov.moj.cpp.material.event.processor.jobstore.jobdata;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.UploadMaterialToAlfrescoJobData.UploadMaterialToAlfrescoJobDataBuilder.uploadMaterialToAlfrescoJobData;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

class UploadMaterialToAlfrescoJobDataTest {

    private final JsonObject eventMetadata = createObjectBuilder().add("name", "material.events.file-uploaded-from-uri").build();

    @Test
    void shouldGiveEqualHashCodesForEqualObjectsIncludingCloudLocationAndFileUri() {
        final UUID materialId = randomUUID();

        final UploadMaterialToAlfrescoJobData first = new UploadMaterialToAlfrescoJobData(
                materialId, null, false, eventMetadata, null, "https://example.blob.core.windows.net/container/file.pdf");
        final UploadMaterialToAlfrescoJobData second = new UploadMaterialToAlfrescoJobData(
                materialId, null, false, eventMetadata, null, "https://example.blob.core.windows.net/container/file.pdf");

        assertThat(first, is(equalTo(second)));
        assertThat(first.hashCode(), is(equalTo(second.hashCode())));
    }

    @Test
    void shouldGiveDifferentHashCodesWhenOnlyFileUriDiffers() {
        final UUID materialId = randomUUID();

        final UploadMaterialToAlfrescoJobData first = new UploadMaterialToAlfrescoJobData(
                materialId, null, false, eventMetadata, null, "https://example.blob.core.windows.net/container/one.pdf");
        final UploadMaterialToAlfrescoJobData second = new UploadMaterialToAlfrescoJobData(
                materialId, null, false, eventMetadata, null, "https://example.blob.core.windows.net/container/two.pdf");

        assertThat(first, is(not(equalTo(second))));
        assertThat(first.hashCode(), is(not(equalTo(second.hashCode()))));
    }

    @Test
    void shouldCopyCloudLocationAndFileUriFromTheSourceObjectWhenBuildingWithValuesFrom() {
        final UUID materialId = randomUUID();
        final UUID fileServiceId = randomUUID();
        final String cloudLocation = "2789/test_1.pdf";
        final String fileUri = "https://example.blob.core.windows.net/container/file.pdf";

        final UploadMaterialToAlfrescoJobData source = new UploadMaterialToAlfrescoJobData(
                materialId, fileServiceId, true, eventMetadata, cloudLocation, fileUri);

        final UploadMaterialToAlfrescoJobData copy = uploadMaterialToAlfrescoJobData()
                .withValuesFrom(source)
                .build();

        assertThat(copy.getMaterialId(), is(equalTo(materialId)));
        assertThat(copy.getFileServiceId(), is(equalTo(fileServiceId)));
        assertThat(copy.isUnbundledDocument(), is(true));
        assertThat(copy.getFileUploadedEventMetadata(), is(equalTo(eventMetadata)));
        assertThat(copy.getCloudLocation(), is(equalTo(cloudLocation)));
        assertThat(copy.getFileUri(), is(equalTo(fileUri)));
    }
}
