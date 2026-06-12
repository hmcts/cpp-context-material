package uk.gov.moj.cpp.material.persistence.repository;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.material.persistence.entity.Material;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MaterialRepositoryTest {

    private static final String PERSISTENCE_UNIT = "material-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private MaterialRepository materialRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        materialRepository = new MaterialRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(materialRepository);
    }

    @Test
    void shouldFindMaterialById() {
        final UUID materialIdA = UUID.randomUUID();
        final String alfrescoIdA = "alfrescoA";
        final String filenameA = "filenameA.txt";
        final ZonedDateTime dateAddedA = new UtcClock().now();

        final UUID materialIdB = UUID.randomUUID();
        final String alfrescoIdB = "alfrescoB";
        final String filenameB = "filenameB.txt";
        final ZonedDateTime dateAddedB = new UtcClock().now().minusDays(1);

        materialRepository.save(new Material(materialIdA, alfrescoIdA, filenameA, TEXT_PLAIN, dateAddedA, null));
        materialRepository.save(new Material(materialIdB, alfrescoIdB, filenameB, APPLICATION_XML, dateAddedB, null));

        final Material materialA = materialRepository.findBy(materialIdA);
        assertThat(materialA, is(notNullValue()));
        assertThat(materialA.getMaterialId(), equalTo(materialIdA));
        assertThat(materialA.getAlfrescoId(), equalTo(alfrescoIdA));
        assertThat(materialA.getFilename(), equalTo(filenameA));
        assertThat(materialA.getMimeType(), equalTo(TEXT_PLAIN));
        assertThat(materialA.getDateMaterialAdded(), equalTo(dateAddedA));
        assertThat(materialA.getExternalLink(), is(nullValue()));

        final Material materialB = materialRepository.findBy(materialIdB);
        assertThat(materialB, is(notNullValue()));
        assertThat(materialB.getMaterialId(), equalTo(materialIdB));
        assertThat(materialB.getAlfrescoId(), equalTo(alfrescoIdB));
        assertThat(materialB.getFilename(), equalTo(filenameB));
        assertThat(materialB.getMimeType(), equalTo(APPLICATION_XML));
        assertThat(materialB.getDateMaterialAdded(), equalTo(dateAddedB));
        assertThat(materialB.getExternalLink(), is(nullValue()));
    }

    @Test
    void shouldReturnNullIfMaterialNotFound() {
        final Material material = materialRepository.findBy(UUID.randomUUID());
        assertThat(material, is(nullValue()));
    }
}
