package uk.gov.moj.cpp.material.persistence.repository;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus;
import uk.gov.moj.cpp.material.persistence.entity.StructuredFormChangeHistory;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StructuredFormChangeHistoryRepositoryTest {

    private static final String PERSISTENCE_UNIT = "material-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private StructuredFormChangeHistoryRepository repository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        repository = new StructuredFormChangeHistoryRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    void shouldSaveAndReadStructuredFormChangeHistory() {
        final UUID id = randomUUID();
        final StructuredFormChangeHistory structuredFormChangeHistory = new StructuredFormChangeHistory(
                id, randomUUID(), randomUUID(), null, now(), "John Wick", "{}", StructuredFormStatus.CREATED);
        repository.save(structuredFormChangeHistory);

        final StructuredFormChangeHistory persisted = repository.findBy(id);
        assertThat(persisted, is(notNullValue()));
        assertThat(persisted.getStructuredFormId(), is(structuredFormChangeHistory.getStructuredFormId()));
        assertThat(persisted.getFormId(), is(structuredFormChangeHistory.getFormId()));
        assertThat(persisted.getDate(), is(structuredFormChangeHistory.getDate()));
        assertThat(persisted.getUpdatedBy(), is(structuredFormChangeHistory.getUpdatedBy()));
        assertThat(persisted.getData(), is(structuredFormChangeHistory.getData()));
    }

    @Test
    void shouldFindByStructuredFormId() {
        final UUID sharedStructuredFormId = randomUUID();

        repository.save(new StructuredFormChangeHistory(randomUUID(), sharedStructuredFormId, randomUUID(), null, now(), "Alice", "{}", StructuredFormStatus.CREATED));
        repository.save(new StructuredFormChangeHistory(randomUUID(), sharedStructuredFormId, randomUUID(), null, now(), "Bob", "{}", StructuredFormStatus.CREATED));
        repository.save(new StructuredFormChangeHistory(randomUUID(), randomUUID(), randomUUID(), null, now(), "Carol", "{}", StructuredFormStatus.CREATED));

        final List<StructuredFormChangeHistory> results = repository.findByStructuredFormId(sharedStructuredFormId);

        assertThat(results, hasSize(2));
        assertThat(results.stream().allMatch(r -> r.getStructuredFormId().equals(sharedStructuredFormId)), is(true));
    }
}
