package uk.gov.moj.cpp.material.persistence.repository;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.fail;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.material.persistence.entity.Material;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * J17/J25 parity characterisation (J17 golden master) for the viewstore repositories.
 *
 * On J17 the four repositories are DeltaSpike EntityRepository interfaces using the framework's EntityManager. The
 * J25 upgrade rewrites them by hand on a container-managed @PersistenceContext and Hibernate 6. Every assertion
 * below encodes the J17 result, for the repository methods production calls.
 *
 * Harness notes: hbm2ddl does not create the Liquibase column types, so the test that reads a timestamp changes the
 * column first (material.date_material_added is TIMESTAMP WITH TIME ZONE). The structured_form.last_updated rendering
 * is pinned in StructuredFormQueryViewTest, and its round trip through PostgreSQL in JavaUpgradeParityIT.maI01.
 * Under HSQLDialect on H2 (J17) a UUID primary key does not round-trip after the persistence context is cleared, so
 * the tests read back by a non-id column, and nothing here binds a UUID query parameter (findByStructuredFormId and
 * the removal of a stored entity are left to the integration tests, against PostgreSQL).
 */
public class RepositoryParityTest {

    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    @RegisterExtension
    static final HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("material-test-persistence-unit");

    private final MaterialRepository materialRepository = new MaterialRepository();

    private final MaterialUploadStatusRepository materialUploadStatusRepository = new MaterialUploadStatusRepository();

    private final StructuredFormRepository structuredFormRepository = new StructuredFormRepository();

    private final StructuredFormChangeHistoryRepository structuredFormChangeHistoryRepository = new StructuredFormChangeHistoryRepository();

    private EntityManager entityManager;

    private TimeZone originalDefaultTimeZone;

    @BeforeEach
    public void rememberDefaultTimeZone() {
        hibernateTestEntityManagerProvider.injectEntityManagerInto(materialRepository);
        hibernateTestEntityManagerProvider.injectEntityManagerInto(materialUploadStatusRepository);
        hibernateTestEntityManagerProvider.injectEntityManagerInto(structuredFormRepository);
        hibernateTestEntityManagerProvider.injectEntityManagerInto(structuredFormChangeHistoryRepository);
        entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        originalDefaultTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    public void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    // MA-P01 (BC-01/02): findBy is the only single-result finder; an unknown id gives null on every repository.
    @Test
    public void maP01_findByUnknownIdShouldReturnNull() {
        assertThat(materialRepository.findBy(randomUUID()), is(nullValue()));
        assertThat(materialUploadStatusRepository.findBy(randomUUID()), is(nullValue()));
        assertThat(structuredFormRepository.findBy(randomUUID()), is(nullValue()));
        assertThat(structuredFormChangeHistoryRepository.findBy(randomUUID()), is(nullValue()));
    }

    // MA-P02: on J17 the framework EntityManagerProducer set the JVM default time zone to UTC when it created the
    // EntityManager the DeltaSpike repositories used. J25 repositories no longer go through that producer, so using them
    // leaves the JVM default time zone unchanged and the server must run in UTC itself.
    @Test
    public void maP02_repositoryUseShouldLeaveTheJvmDefaultTimeZoneUnchanged() {
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));

        materialRepository.save(material("P02.pdf", ZonedDateTime.now(LONDON)));

        assertThat(TimeZone.getDefault().getID(), is("Europe/London"));
    }

    // MA-P03: save() of a new entity returns a managed entity with the same field values. DeltaSpike (J17) returned the
    // same instance; the J25 merge() returns a managed copy. No caller relies on the identity.
    @Test
    public void maP03_saveOfNewEntityShouldReturnAManagedEntityWithTheSameFields() {
        final Material material = material("P03.pdf", ZonedDateTime.now(LONDON));

        final Material saved = materialRepository.save(material);

        assertThat(saved, samePropertyValuesAs(material));
        assertThat(entityManager.contains(saved), is(true));
    }

    // MA-P05 (BC-08): MaterialService / DownloadableMaterialsService treat a material without an upload status as
    // downloadable when dateMaterialAdded.toLocalDate() is before 2021-08-18, so near midnight the read-back zone
    // decides the answer. material.date_material_added is TIMESTAMP WITH TIME ZONE. The value is compared as local
    // date-time and offset: J17 reads it back with the zone id UTC, J25 with the offset Z, and neither changes the answer.
    @Test
    public void maP05_dateMaterialAddedShouldBeReadBackWithTheJ17LocalDate() {
        entityManager.createNativeQuery("alter table material alter column date_material_added set data type timestamp with time zone").executeUpdate();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        materialRepository.save(material("P05.pdf", ZonedDateTime.of(2021, 8, 17, 23, 30, 0, 0, ZoneId.of("UTC"))));
        entityManager.flush();
        entityManager.clear();

        final Material reloaded = entityManager
                .createQuery("select m from Material m where m.filename = :filename", Material.class)
                .setParameter("filename", "P05.pdf")
                .getSingleResult();
        assertThat(reloaded.getDateMaterialAdded().format(ISO_OFFSET_DATE_TIME) + " downloadable="
                + reloaded.getDateMaterialAdded().toLocalDate().isBefore(LocalDate.of(2021, 8, 18)), is("2021-08-17T23:30:00Z downloadable=true"));
    }

    // MA-P06: material-deleted calls removeAndFlush(findBy(id)), so a delete for an unknown material passes null.
    // J17 throws (DeltaSpike wraps it in a QueryInvocationException), so the listener fails and the framework
    // redelivers. The exception type necessarily differs once DeltaSpike is gone; the behaviour pinned is that it throws.
    @Test
    public void maP06_removeAndFlushOfNullShouldThrowAsOnJ17() {
        try {
            materialRepository.removeAndFlush(null);
            fail("removeAndFlush(null) did not throw");
        } catch (final RuntimeException expected) {
            // J17 behaviour
        }
    }


    private static Material material(final String filename, final ZonedDateTime dateMaterialAdded) {
        return new Material(randomUUID(), "alfresco-" + filename, filename, "application/pdf", dateMaterialAdded, null);
    }

}
