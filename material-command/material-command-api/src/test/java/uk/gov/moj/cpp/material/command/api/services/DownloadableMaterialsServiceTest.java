package uk.gov.moj.cpp.material.command.api.services;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.material.command.services.DownloadableMaterialsService;
import uk.gov.moj.cpp.material.persistence.MaterialUploadStatusRepository;
import uk.gov.moj.cpp.material.persistence.entity.Material;
import uk.gov.moj.cpp.material.persistence.entity.MaterialUploadStatus;
import uk.gov.moj.cpp.material.persistence.repository.MaterialRepository;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DownloadableMaterialsServiceTest {
    private static final UUID MATERIAL_ID = randomUUID();
    private static final String ALFRESCO_ID = "jhdfjksdhfdjksfgsdkg534534563";
    private static final String FILENAME = "test.txt";
    private static final String MIME_TYPE = TEXT_PLAIN;
    private static final String EXTERNAL_LINK = "http://something.com";
    private static final ZonedDateTime MATERIAL_ADDED_DATE = new UtcClock().now();
    private static final String UPLOAD_STATUS_QUEUED = "QUEUED";
    private static final String UPLOAD_STATUS_SUCCESS = "SUCCESS";

    @InjectMocks
    private DownloadableMaterialsService materialService;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialUploadStatusRepository materialUploadStatusRepository;


    @Test
    public void shouldReturnMaterialsViewForMaterialIds() {
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(materialUploadStatus(UPLOAD_STATUS_SUCCESS));

        Map<UUID, Boolean> result = materialService.getDownloadableMaterials(singletonList(MATERIAL_ID));

        assertThat(result.size(), equalTo(1));
        assertThat(result.get(MATERIAL_ID), equalTo(true));
    }

    @Test
    public void shouldReturnMaterialsViewForMaterialIdsWhenNotDownloadable() {
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(materialUploadStatus(UPLOAD_STATUS_QUEUED));

        Map<UUID, Boolean> result = materialService.getDownloadableMaterials(singletonList(MATERIAL_ID));

        assertThat(result.size(), equalTo(1));
        assertThat(result.get(MATERIAL_ID), equalTo(false));
    }

    @Test
    public void shouldReturnFalseWhenNoMaterialsDownloadStatusAndMaterialsFound() {
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(null);
        when(materialRepository.findBy(MATERIAL_ID)).thenReturn(null);

        Map<UUID, Boolean> result = materialService.getDownloadableMaterials(singletonList(MATERIAL_ID));

        assertThat(result.size(), equalTo(1));
        assertThat(result.get(MATERIAL_ID), equalTo(false));
    }

    @Test
    public void shouldReturnTrueWhenNoMaterialsDownloadStatusFoundAndMaterialAddedDateBeforeThresholdTime() {
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(null);

        Material material = getMaterial(ZonedDateTime.now().withYear(2021).withMonth(7));
        when(materialRepository.findBy(MATERIAL_ID)).thenReturn(material);

        Map<UUID, Boolean> result = materialService.getDownloadableMaterials(singletonList(MATERIAL_ID));

        assertThat(result.size(), equalTo(1));
        assertThat(result.get(MATERIAL_ID), equalTo(true));
    }

    @Test
    public void shouldReturnFalseWhenNoMaterialsDownloadStatusFoundAndMaterialAddedDateAfterThresholdTime() {
        when(materialUploadStatusRepository.findBy(MATERIAL_ID)).thenReturn(null);

        Material material = getMaterial(ZonedDateTime.now().withYear(2021).withMonth(9));
        when(materialRepository.findBy(MATERIAL_ID)).thenReturn(material);

        Map<UUID, Boolean> result = materialService.getDownloadableMaterials(singletonList(MATERIAL_ID));

        assertThat(result.size(), equalTo(1));
        assertThat(result.get(MATERIAL_ID), equalTo(false));
    }


    private Material getMaterial(ZonedDateTime materialAddedDate) {
        return new Material(MATERIAL_ID, ALFRESCO_ID, FILENAME, MIME_TYPE, materialAddedDate, EXTERNAL_LINK);
    }


    private MaterialUploadStatus materialUploadStatus(String status) {
        return new MaterialUploadStatus(MATERIAL_ID, randomUUID(), status, null, null, MATERIAL_ADDED_DATE);
    }
}
