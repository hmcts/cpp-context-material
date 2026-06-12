package uk.gov.moj.cpp.material.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.material.persistence.entity.MaterialUploadStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MaterialUploadStatusRepositoryTest {

    private static final String PERSISTENCE_UNIT = "material-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private MaterialUploadStatusRepository materialUploadStatusRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        materialUploadStatusRepository = new MaterialUploadStatusRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(materialUploadStatusRepository);
    }

    @Test
    void shouldFindMaterialUploadStatusById() {
        final UUID materialIdA = randomUUID();
        final UUID fileServiceIdA = randomUUID();
        final ZonedDateTime lastModifiedA = new UtcClock().now();

        final UUID materialIdB = randomUUID();
        final UUID fileServiceIdB = randomUUID();
        final ZonedDateTime lastModifiedB = new UtcClock().now().plusSeconds(2);
        final ZonedDateTime failedTimeB = new UtcClock().now().plusSeconds(5);

        materialUploadStatusRepository.save(new MaterialUploadStatus(materialIdA, fileServiceIdA, "QUEUED", null, null, lastModifiedA));
        materialUploadStatusRepository.save(new MaterialUploadStatus(materialIdB, fileServiceIdB, "SUCCESS", failedTimeB, "Error", lastModifiedB));

        final MaterialUploadStatus materialUploadStatusA = materialUploadStatusRepository.findBy(materialIdA);
        assertThat(materialUploadStatusA, is(notNullValue()));
        assertThat(materialUploadStatusA.getMaterialId(), equalTo(materialIdA));
        assertThat(materialUploadStatusA.getFileServiceId(), equalTo(fileServiceIdA));
        assertThat(materialUploadStatusA.getStatus(), equalTo("QUEUED"));
        assertThat(materialUploadStatusA.getLastModified(), equalTo(lastModifiedA));
        assertThat(materialUploadStatusA.getErrorMessage(), is(nullValue()));
        assertThat(materialUploadStatusA.getFailedTime(), is(nullValue()));

        final MaterialUploadStatus materialUploadStatusB = materialUploadStatusRepository.findBy(materialIdB);
        assertThat(materialUploadStatusB, is(notNullValue()));
        assertThat(materialUploadStatusB.getMaterialId(), equalTo(materialIdB));
        assertThat(materialUploadStatusB.getFileServiceId(), equalTo(fileServiceIdB));
        assertThat(materialUploadStatusB.getStatus(), equalTo("SUCCESS"));
        assertThat(materialUploadStatusB.getLastModified(), equalTo(lastModifiedB));
        assertThat(materialUploadStatusB.getErrorMessage(), is("Error"));
        assertThat(materialUploadStatusB.getFailedTime(), is(failedTimeB));
    }

    @Test
    void shouldReturnNullIfMaterialNotFound() {
        final MaterialUploadStatus materialUploadStatus = materialUploadStatusRepository.findBy(randomUUID());
        assertThat(materialUploadStatus, is(nullValue()));
    }
}
