package uk.gov.moj.cpp.material.persistence.repository;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus;
import uk.gov.moj.cpp.material.persistence.entity.StructuredForm;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class StructuredFormRepositoryTest {

    @Inject
    private StructuredFormRepository repository;


    @Test
    public void shouldSaveAndReadStructuredForm() {
        final UUID id = randomUUID();
        final ZonedDateTime lastUpdate = now();
        final StructuredForm structuredForm = new StructuredForm(id, randomUUID(), "{}", StructuredFormStatus.CREATED, lastUpdate);
        repository.save(structuredForm);

        final StructuredForm persistedStructuredForm = repository.findBy(id);
        assertThat(persistedStructuredForm, is(notNullValue()));
        assertThat(persistedStructuredForm.getId(), is(id));
        assertThat(persistedStructuredForm.getFormId(), is(structuredForm.getFormId()));
        assertThat(persistedStructuredForm.getData(), is(structuredForm.getData()));
        assertThat(persistedStructuredForm.getStatus(), is(StructuredFormStatus.CREATED));
        assertThat(persistedStructuredForm.getLastUpdated(), is(lastUpdate));
    }

}
