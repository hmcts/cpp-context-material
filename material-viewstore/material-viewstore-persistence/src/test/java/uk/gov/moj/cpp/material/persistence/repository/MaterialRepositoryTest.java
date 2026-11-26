package uk.gov.moj.cpp.material.persistence.repository;


import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.material.persistence.entity.Material;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class MaterialRepositoryTest {

    private static final UUID MATERIAL_ID_A = UUID.randomUUID();
    private static final String ALFRESCO_ID_A = "alfrescoA";
    private static final String FILENAME_A = "filenameA.txt";
    private static final String MIME_TYPE_A = TEXT_PLAIN;
    private static final ZonedDateTime DATE_ADDED_A = new UtcClock().now();

    private static final UUID MATERIAL_ID_B = UUID.randomUUID();
    private static final String ALFRESCO_ID_B = "alfrescoB";
    private static final String FILENAME_B = "filenameB.txt";
    private static final String MIME_TYPE_B = APPLICATION_XML;
    private static final ZonedDateTime DATE_ADDED_B = new UtcClock().now().minusDays(1);

    @Inject
    private MaterialRepository materialRepository;

    @Before
    public void setup() {
        Material materialA = new Material(MATERIAL_ID_A, ALFRESCO_ID_A, FILENAME_A, MIME_TYPE_A, DATE_ADDED_A, null);
        materialRepository.save(materialA);
        Material materialB = new Material(MATERIAL_ID_B, ALFRESCO_ID_B, FILENAME_B, MIME_TYPE_B, DATE_ADDED_B, null);
        materialRepository.save(materialB);
    }

    @Test
    public void shouldFindMaterialById() {
        Material materialA = materialRepository.findBy(MATERIAL_ID_A);
        Material materialB = materialRepository.findBy(MATERIAL_ID_B);

        assertThat(materialA, is(notNullValue()));
        assertThat(materialA.getMaterialId(), equalTo(MATERIAL_ID_A));
        assertThat(materialA.getAlfrescoId(), equalTo(ALFRESCO_ID_A));
        assertThat(materialA.getFilename(), equalTo(FILENAME_A));
        assertThat(materialA.getMimeType(), equalTo(MIME_TYPE_A));
        assertThat(materialA.getDateMaterialAdded(), equalTo(DATE_ADDED_A));
        assertThat(materialA.getExternalLink(), is(nullValue()));

        assertThat(materialB, is(notNullValue()));
        assertThat(materialB.getMaterialId(), equalTo(MATERIAL_ID_B));
        assertThat(materialB.getAlfrescoId(), equalTo(ALFRESCO_ID_B));
        assertThat(materialB.getFilename(), equalTo(FILENAME_B));
        assertThat(materialB.getMimeType(), equalTo(MIME_TYPE_B));
        assertThat(materialB.getDateMaterialAdded(), equalTo(DATE_ADDED_B));
        assertThat(materialB.getExternalLink(), is(nullValue()));
    }

    @Test
    public void shouldReturnNullIfMaterialNotFound() {
        Material material = materialRepository.findBy(UUID.randomUUID());

        assertThat(material, is(nullValue()));
    }
}