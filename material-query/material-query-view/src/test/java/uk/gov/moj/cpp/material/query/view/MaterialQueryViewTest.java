package uk.gov.moj.cpp.material.query.view;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.file.alfresco.AlfrescoRestClient;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.persistence.entity.Material;
import uk.gov.moj.cpp.material.query.service.MaterialService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialQueryViewTest {

    private static final String FIELD_ID = "materialId";
    private static final String MATERIAL_IDS = "materialIds";
    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private static final String ALFRESCO_ID = "jhdfjksdhfdjksfgsdkg534534563";
    private static final String FILENAME = "test.txt";
    private static final String MIME_TYPE = "text/plain";
    private static final ZonedDateTime MATERIAL_ADDED_DATE = new UtcClock().now();
    private static final String EXTERNAL_LINK = "http://something.com";

    private static final String MATERIAL_QUERY_MATERIAL_METADATA_RESPONSE = "material.query.material-metadata-response";
    private static final String MATERIAL_QUERY_IS_DOWNLOADABLE_MATERIALS_RESPONSE = "material.query.is-downloadable-materials-response";
    private static final String MATERIAL_QUERY_MATERIAL_METADATA_DETAILS_RESPONSE = "material.query.material-metadata-details-response";


    @InjectMocks
    private MaterialQueryView materialQueryView;

    @Mock
    private JsonEnvelope query;

    @Mock
    private MaterialService materialService;

    @Mock
    private JsonObject queryAsJsonObject;

    @Mock
    private MaterialMetadataView expectedMaterialView;

    @Mock
    Enveloper enveloper;

    @Mock
    Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonEnvelope expectedEnvelope;

    @Mock
    AlfrescoRestClient restClient;

    @Test
    public void shouldFindMaterialMetadata() throws Exception {
        when(query.payloadAsJsonObject()).thenReturn(queryAsJsonObject);
        when(query.metadata()).thenReturn(metadataWithRandomUUID(MATERIAL_QUERY_MATERIAL_METADATA_RESPONSE).build());
        when(queryAsJsonObject.getString(FIELD_ID)).thenReturn(MATERIAL_ID.toString());
        MaterialMetadataView expectedMaterialMetadataView = expectedMaterialMetadataView();
        when(materialService.getMaterialMetadataByMaterialId(MATERIAL_ID)).thenReturn(expectedMaterialMetadataView);

        Envelope<MaterialMetadataView> envelope = materialQueryView.findMaterialMetadata(query);

        assertThat(envelope.payload(), equalTo(expectedMaterialMetadataView));
    }

    @Test
    public void shouldFindMaterialMetadataDetail() throws Exception {
        when(query.payloadAsJsonObject()).thenReturn(queryAsJsonObject);
        when(query.metadata()).thenReturn(metadataWithRandomUUID(MATERIAL_QUERY_MATERIAL_METADATA_DETAILS_RESPONSE).build());
        when(queryAsJsonObject.getString(FIELD_ID)).thenReturn(MATERIAL_ID.toString());
        MaterialMetadataView expectedMaterialMetadataView = expectedMaterialMetadataView();
        when(materialService.getMaterialMetadataByMaterialId(MATERIAL_ID)).thenReturn(expectedMaterialMetadataView);

        final byte[] alfrescoResponse = readFileToString(new File(this.getClass().getClassLoader().getResource("alfresco.json").getFile())).getBytes();
        InputStream response = new ByteArrayInputStream(alfrescoResponse);
        when(restClient.getAsInputStream(any(), eq(MediaType.valueOf(MIME_TYPE)), any())).thenReturn(response);

        Envelope<MaterialDetailedMetadataView> envelope = materialQueryView.findMaterialMetadataDetails(query);
        assertThat(envelope.payload().getAlfrescoAssetId(), equalTo(expectedMaterialMetadataView.getAlfrescoAssetId()));
        assertThat(envelope.payload().getMaterialId(), equalTo(expectedMaterialMetadataView.getMaterialId()));
        assertThat(envelope.payload().getFileName(), equalTo(expectedMaterialMetadataView.getFileName()));
        assertThat(envelope.payload().getFileSize(), equalTo(708784));

    }

    private MaterialMetadataView expectedMaterialMetadataView() {
        return new MaterialMetadataView(new Material(MATERIAL_ID, ALFRESCO_ID, FILENAME, MIME_TYPE, MATERIAL_ADDED_DATE, EXTERNAL_LINK));
    }

    @Test
    public void shouldFindMaterialsIsDownloadableStatus() throws Exception {
        when(query.payloadAsJsonObject()).thenReturn(queryAsJsonObject);
        when(queryAsJsonObject.getString(MATERIAL_IDS)).thenReturn(MATERIAL_ID.toString());
        MaterialsView materialsView = expectedMaterialsView();
        when(materialService.getMaterialsDownloadStatusByMaterialIds(MATERIAL_ID.toString())).thenReturn(materialsView);
        when(enveloper.withMetadataFrom(query, MATERIAL_QUERY_IS_DOWNLOADABLE_MATERIALS_RESPONSE)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(materialsView)).thenReturn(expectedEnvelope);

        JsonEnvelope envelope = materialQueryView.findDownloadableMaterials(query);

        assertThat(envelope, equalTo(expectedEnvelope));
    }

    private MaterialsView expectedMaterialsView() {
        return new MaterialsView(Collections.singletonMap(MATERIAL_ID.toString(), true));
    }
}