package uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle.RecordBundleDetailCommandFactory.ERROR_CODE;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle.RecordBundleDetailCommandFactory.MATERIAL_COMMAND_HANDLER_RECORD_BUNDLE_DETAILS;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle.RecordBundleDetailCommandFactory.MATERIAL_COMMAND_HANDLER_RECORD_BUNDLE_DETAILS_FAILURE;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class RecordBundleDetailCommandFactoryTest {

    private RecordBundleDetailCommandFactory recordBundleDetailCommandFactory = new RecordBundleDetailCommandFactory();

    private UUID bundledMaterialId = UUID.randomUUID();
    private String bundledMaterialName = "BUNDLE_MATERIAL_NAME";
    private UUID alfrescoFileId = UUID.randomUUID();
    private String mediaType = "mediaType";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String FILE_SERVICE_ID = "fileServiceId";
    public static final String FILE_REFERENCE = "fileReference";
    public static final String FILE_NAME = "fileName";
    public static final String FILE_SIZE = "fileSize";
    public static final String MATERIAL_IDS = "materialIds";
    public static final String MIME_TYPE = "mimeType";


    private Long fileSize = 12345L;
    private int pageCount = 12;
    private JsonObject eventMetadata = metadataWithRandomUUIDAndName().build().asJsonObject();
    private List<UUID> materialIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());


    @Test
    public void shouldBuildRecordBundleCommand() {
        JsonEnvelope recordBundleCommand = recordBundleDetailCommandFactory.recordBundleCommand(bundledMaterialId, bundledMaterialName,
                alfrescoFileId, mediaType, fileSize, pageCount, eventMetadata);

        assertThat(recordBundleCommand.asJsonObject().getString("bundledMaterialId"), is(bundledMaterialId.toString()));
        assertThat(recordBundleCommand.asJsonObject().getString(FILE_NAME), is(bundledMaterialName));
        assertThat(recordBundleCommand.asJsonObject().getString(FILE_REFERENCE), is(alfrescoFileId.toString()));
        assertThat(recordBundleCommand.asJsonObject().getString(MIME_TYPE), is(mediaType));
        assertThat(recordBundleCommand.asJsonObject().getString(FILE_SIZE), is(fileSize.toString()));
        assertThat(recordBundleCommand.asJsonObject().getInt("pageCount"), is(pageCount));
        assertThat(recordBundleCommand.asJsonObject().getJsonObject("_metadata").getString("name"), is(MATERIAL_COMMAND_HANDLER_RECORD_BUNDLE_DETAILS));
    }

    @Test
    public void shouldBuildRecordBundleFailedCommand() {

        final ZonedDateTime failedTime = new UtcClock().now();
        JsonEnvelope recordBundleCommand = recordBundleDetailCommandFactory.recordBundleFailedCommand(bundledMaterialId, materialIds, Optional.empty(),
                eventMetadata, BundleErrorType.MERGE_FILE_ERROR, ERROR_MESSAGE, failedTime);

        assertThat(recordBundleCommand.asJsonObject().getString("bundledMaterialId"), is(bundledMaterialId.toString()));
        assertThat(recordBundleCommand.asJsonObject().getJsonArray(MATERIAL_IDS).getString(0), is(materialIds.get(0).toString()));
        assertThat(recordBundleCommand.asJsonObject().getJsonArray(MATERIAL_IDS).getString(1), is(materialIds.get(1).toString()));
        assertThat(recordBundleCommand.asJsonObject().getString(ERROR_CODE), is(BundleErrorType.MERGE_FILE_ERROR.name()));
        assertThat(recordBundleCommand.asJsonObject().getString(ERROR_MESSAGE), is(ERROR_MESSAGE));
        assertThat(recordBundleCommand.asJsonObject().get(FILE_SERVICE_ID), is(nullValue()));
        assertThat(recordBundleCommand.asJsonObject().getJsonObject("_metadata").getString("name"), is(MATERIAL_COMMAND_HANDLER_RECORD_BUNDLE_DETAILS_FAILURE));
    }

    @Test
    public void shouldBuildRecordBundleFailedCommandWithFileServiceId() {

        final ZonedDateTime failedTime = new UtcClock().now();
        UUID fileServiceId = UUID.randomUUID();
        JsonEnvelope recordBundleCommand = recordBundleDetailCommandFactory.recordBundleFailedCommand(bundledMaterialId, materialIds, Optional.of(fileServiceId),
                eventMetadata, BundleErrorType.MERGE_FILE_ERROR, ERROR_MESSAGE, failedTime);

        assertThat(recordBundleCommand.asJsonObject().getString("bundledMaterialId"), is(bundledMaterialId.toString()));
        assertThat(recordBundleCommand.asJsonObject().getJsonArray(MATERIAL_IDS).getString(0), is(materialIds.get(0).toString()));
        assertThat(recordBundleCommand.asJsonObject().getJsonArray(MATERIAL_IDS).getString(1), is(materialIds.get(1).toString()));
        assertThat(recordBundleCommand.asJsonObject().getString(ERROR_CODE), is(BundleErrorType.MERGE_FILE_ERROR.name()));
        assertThat(recordBundleCommand.asJsonObject().getString(ERROR_MESSAGE), is(ERROR_MESSAGE));
        assertThat(recordBundleCommand.asJsonObject().getString(FILE_SERVICE_ID), is(fileServiceId.toString()));
        assertThat(recordBundleCommand.asJsonObject().getJsonObject("_metadata").getString("name"), is(MATERIAL_COMMAND_HANDLER_RECORD_BUNDLE_DETAILS_FAILURE));
    }
}