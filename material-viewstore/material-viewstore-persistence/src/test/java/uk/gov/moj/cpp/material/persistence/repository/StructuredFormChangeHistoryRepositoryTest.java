package uk.gov.moj.cpp.material.persistence.repository;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.cpp.material.persistence.constant.StructuredFormStatus;
import uk.gov.moj.cpp.material.persistence.entity.StructuredFormChangeHistory;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class StructuredFormChangeHistoryRepositoryTest {

    @Inject
    private StructuredFormChangeHistoryRepository repository;


    @Test
    public void shouldSaveAndReadStructuredFormChangeHistory() {
        final UUID id = randomUUID();
        final StructuredFormChangeHistory structuredFormChangeHistory = new StructuredFormChangeHistory(id, randomUUID(), randomUUID(), null, now(), "John Wick", "{}", StructuredFormStatus.CREATED);
        repository.save(structuredFormChangeHistory);

        final StructuredFormChangeHistory persistedStructuredFormChangeHistory = repository.findBy(id);
        assertThat(persistedStructuredFormChangeHistory, is(notNullValue()));
        assertThat(persistedStructuredFormChangeHistory.getStructuredFormId(), is(structuredFormChangeHistory.getStructuredFormId()));
        assertThat(persistedStructuredFormChangeHistory.getFormId(), is(structuredFormChangeHistory.getFormId()));
        assertThat(persistedStructuredFormChangeHistory.getDate(), is(structuredFormChangeHistory.getDate()));
        assertThat(persistedStructuredFormChangeHistory.getUpdatedBy(), is(structuredFormChangeHistory.getUpdatedBy()));
        assertThat(persistedStructuredFormChangeHistory.getData(), is(structuredFormChangeHistory.getData()));
    }

}
