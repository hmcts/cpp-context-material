package uk.gov.moj.cpp.material.query.service;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.material.persistence.MaterialUploadStatusRepository;
import uk.gov.moj.cpp.material.persistence.entity.Material;
import uk.gov.moj.cpp.material.persistence.entity.MaterialUploadStatus;
import uk.gov.moj.cpp.material.persistence.repository.MaterialRepository;
import uk.gov.moj.cpp.material.query.view.MaterialMetadataView;
import uk.gov.moj.cpp.material.query.view.MaterialsView;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialServiceTest {

    private static final UUID MATERIAL_ID = randomUUID();
    private static final String ALFRESCO_ID = "jhdfjksdhfdjksfgsdkg534534563";
    private static final String FILENAME = "test.txt";
    private static final String MIME_TYPE = TEXT_PLAIN;
    private static final String EXTERNAL_LINK = "http://something.com";
    private static final ZonedDateTime MATERIAL_ADDED_DATE = new UtcClock().now();
    private static final String UPLOAD_STATUS_QUEUED = "QUEUED";
    private static final String UPLOAD_STATUS_SUCCESS = "SUCCESS";

    @InjectMocks
    private MaterialService materialService;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialMetadataView materialMetadata;

    @Mock
    private MaterialUploadStatusRepository materialUploadStatusRepository;

    @Test
    public void shouldReturnMaterialMetadataByMaterialId() throws Exception {
        when(materialRepository.findBy(MATERIAL_ID)).thenReturn(getMaterial());

        MaterialMetadataView materialMetadataView = materialService.getMaterialMetadataByMaterialId(MATERIAL_ID);

        assertThat(materialMetadataView.getMaterialId(), equalTo(MATERIAL_ID));
        assertThat(materialMetadataView.getAlfrescoAssetId(), equalTo(ALFRESCO_ID));
        assertThat(materialMetadataView.getFileName(), equalTo(FILENAME));
        assertThat(materialMetadataView.getMimeType(), equalTo(MIME_TYPE));
        assertThat(materialMetadataView.getMaterialAddedDate(), equalTo(MATERIAL_ADDED_DATE));
        assertThat(materialMetadataView.getExternalLink(), equalTo(EXTERNAL_LINK));
    }

    @Test
    public void shouldReturnNullWhenMaterialMetadataNotFoundByMaterialId() throws Exception {
        when(materialRepository.findBy(MATERIAL_ID)).thenReturn(null);
        MaterialMetadataView materialMetadataView = materialService.getMaterialMetadataByMaterialId(MATERIAL_ID);

        assertThat(materialMetadataView, nullValue());
    }

    @Test
    public void shouldReturnMaterialsViewForMaterialIds() {
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(materialUploadStatus(UPLOAD_STATUS_SUCCESS));

        MaterialsView materialsView = materialService.getMaterialsDownloadStatusByMaterialIds(MATERIAL_ID.toString());

        assertThat(materialsView.getMaterials().size(), equalTo(1));
        assertThat(materialsView.getMaterials().get(MATERIAL_ID.toString()), equalTo(true));
    }

    @Test
    public void shouldReturnMaterialsViewForMaterialIdsWhenNotDownloadable() {
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(materialUploadStatus(UPLOAD_STATUS_QUEUED));

        MaterialsView materialsView = materialService.getMaterialsDownloadStatusByMaterialIds(MATERIAL_ID.toString());

        assertThat(materialsView.getMaterials().size(), equalTo(1));
        assertThat(materialsView.getMaterials().get(MATERIAL_ID.toString()), equalTo(false));
    }

    @Test
    public void shouldReturnFalseWhenNoMaterialsDownloadStatusAndMaterialsFound() {
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(null);
        when(materialRepository.findBy(MATERIAL_ID)).thenReturn(null);

        MaterialsView materialsView = materialService.getMaterialsDownloadStatusByMaterialIds(MATERIAL_ID.toString());

        assertThat(materialsView.getMaterials().size(), equalTo(1));
        assertThat(materialsView.getMaterials().get(MATERIAL_ID.toString()), equalTo(false));
    }

    @Test
    public void shouldReturnTrueWhenNoMaterialsDownloadStatusFoundAndMaterialAddedDateBeforeThresholdTime() {
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(null);

        Material material = getMaterial(ZonedDateTime.now().withYear(2021).withMonth(7));
        when(materialRepository.findBy(MATERIAL_ID)).thenReturn(material);

        MaterialsView materialsView = materialService.getMaterialsDownloadStatusByMaterialIds(MATERIAL_ID.toString());

        assertThat(materialsView.getMaterials().size(), equalTo(1));
        assertThat(materialsView.getMaterials().get(MATERIAL_ID.toString()), equalTo(true));
    }

    @Test
    public void shouldReturnFalseWhenNoMaterialsDownloadStatusFoundAndMaterialAddedDateAfterThresholdTime() {
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(null);

        Material material = getMaterial(ZonedDateTime.now().withYear(2021).withMonth(9));
        when(materialRepository.findBy(MATERIAL_ID)).thenReturn(material);

        MaterialsView materialsView = materialService.getMaterialsDownloadStatusByMaterialIds(MATERIAL_ID.toString());

        assertThat(materialsView.getMaterials().size(), equalTo(1));
        assertThat(materialsView.getMaterials().get(MATERIAL_ID.toString()), equalTo(false));
    }

    private Material getMaterial() {
        return getMaterial(MATERIAL_ADDED_DATE);
    }

    private Material getMaterial(ZonedDateTime materialAddedDate) {
        return new Material(MATERIAL_ID, ALFRESCO_ID, FILENAME, MIME_TYPE, materialAddedDate, EXTERNAL_LINK);
    }


    private MaterialUploadStatus materialUploadStatus(String status) {
        return new MaterialUploadStatus(MATERIAL_ID, randomUUID(), status, null, null, MATERIAL_ADDED_DATE);
    }
}