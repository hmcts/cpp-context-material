package uk.gov.moj.cpp.material.persistence.repository;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus;
import uk.gov.moj.cpp.material.persistence.entity.StructuredForm;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StructuredFormRepositoryTest {

    private static final String PERSISTENCE_UNIT = "material-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private StructuredFormRepository repository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        repository = new StructuredFormRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    void shouldSaveAndReadStructuredForm() {
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
