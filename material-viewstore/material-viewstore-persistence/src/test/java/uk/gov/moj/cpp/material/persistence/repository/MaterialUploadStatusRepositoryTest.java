package uk.gov.moj.cpp.material.persistence;


import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.material.persistence.entity.MaterialUploadStatus;
import uk.gov.moj.cpp.material.persistence.repository.MaterialUploadStatusRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class MaterialUploadStatusRepositoryTest {

    private static final UUID MATERIAL_ID_A = randomUUID();
    private static final UUID FILE_SERVICE_ID_A = randomUUID();
    private static final ZonedDateTime LAST_MODIFIED_A = new UtcClock().now();

    private static final UUID MATERIAL_ID_B = randomUUID();
    private static final UUID FILE_SERVICE_ID_B = randomUUID();
    private static final ZonedDateTime LAST_MODIFIED_B = new UtcClock().now().plusSeconds(2);
    private static final ZonedDateTime FAILED_TIME_B = new UtcClock().now().plusSeconds(5);

    @Inject
    private MaterialUploadStatusRepository materialUploadStatusRepository;

    @Before
    public void setup() {
        MaterialUploadStatus materialUploadStatusA = new MaterialUploadStatus(MATERIAL_ID_A, FILE_SERVICE_ID_A, "QUEUED", null, null, LAST_MODIFIED_A);
        MaterialUploadStatus materialUploadStatusB = new MaterialUploadStatus(MATERIAL_ID_B, FILE_SERVICE_ID_B, "SUCCESS", FAILED_TIME_B, "Error", LAST_MODIFIED_B);
        materialUploadStatusRepository.save(materialUploadStatusA);
        materialUploadStatusRepository.save(materialUploadStatusB);
    }

    @Test
    public void shouldFindMaterialUploadStatusById() {
        final MaterialUploadStatus materialUploadStatusA = materialUploadStatusRepository.findBy(MATERIAL_ID_A);

        assertThat(materialUploadStatusA, is(notNullValue()));
        assertThat(materialUploadStatusA.getMaterialId(), equalTo(MATERIAL_ID_A));
        assertThat(materialUploadStatusA.getFileServiceId(), equalTo(FILE_SERVICE_ID_A));
        assertThat(materialUploadStatusA.getStatus(), equalTo("QUEUED"));
        assertThat(materialUploadStatusA.getLastModified(), equalTo(LAST_MODIFIED_A));
        assertThat(materialUploadStatusA.getErrorMessage(), is(nullValue()));
        assertThat(materialUploadStatusA.getFailedTime(), is(nullValue()));

        final MaterialUploadStatus materialUploadStatusB = materialUploadStatusRepository.findBy(MATERIAL_ID_B);

        assertThat(materialUploadStatusB, is(notNullValue()));
        assertThat(materialUploadStatusB.getMaterialId(), equalTo(MATERIAL_ID_B));
        assertThat(materialUploadStatusB.getFileServiceId(), equalTo(FILE_SERVICE_ID_B));
        assertThat(materialUploadStatusB.getStatus(), equalTo("SUCCESS"));
        assertThat(materialUploadStatusB.getLastModified(), equalTo(LAST_MODIFIED_B));
        assertThat(materialUploadStatusB.getErrorMessage(), is("Error"));
        assertThat(materialUploadStatusB.getFailedTime(), is(FAILED_TIME_B));
    }

    @Test
    public void shouldReturnNullIfMaterialNotFound() {
        MaterialUploadStatus materialUploadStatus = materialUploadStatusRepository.findBy(randomUUID());

        assertThat(materialUploadStatus, is(nullValue()));
    }
}