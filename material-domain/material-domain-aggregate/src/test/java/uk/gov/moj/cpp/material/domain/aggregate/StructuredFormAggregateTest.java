package uk.gov.moj.cpp.material.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.material.domain.StructuredFormStatus;
import uk.gov.moj.cpp.material.domain.UpdatedBy;
import uk.gov.moj.cpp.material.domain.event.StructuredFormCreated;
import uk.gov.moj.cpp.material.domain.event.StructuredFormFinalised;
import uk.gov.moj.cpp.material.domain.event.StructuredFormPublished;
import uk.gov.moj.cpp.material.domain.event.StructuredFormUpdated;
import uk.gov.moj.cpp.material.domain.event.StructuredFormUpdatedForDefendant;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StructuredFormAggregateTest {

    @InjectMocks
    private StructuredFormAggregate aggregate;

    @BeforeEach
    public void setUp() {
        aggregate = new StructuredFormAggregate();
    }

    @Test
    public void shouldGenerateStructuredFormCreatedWhenDefendantAndOffenceIsValid() {
        final UUID formId = randomUUID();
        final String formData = "test data";
        final UUID structuredFormId = randomUUID();
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());
        final StructuredFormStatus status = StructuredFormStatus.CREATED;

        final List<Object> eventStream = aggregate.createStructuredForm(structuredFormId, formId, formData, status, updatedBy).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(StructuredFormCreated.class)));
    }

    @Test
    public void shouldGenerateStructuredFormUpdated() {
        final String formData = "test data";
        final UUID structuredFormId = randomUUID();
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());

        final List<Object> eventStream = aggregate.updateStructuredForm(structuredFormId, formData, updatedBy).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(StructuredFormUpdated.class)));
    }

    @Test
    public void shouldGenerateStructuredFormUpdatedForDefendant() {
        final String defendantData = "test data";
        final UUID structuredFormId = randomUUID();
        final UUID defendantId = randomUUID();
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());

        final List<Object> eventStream = aggregate.updateStructuredFormForDefendant(structuredFormId, defendantId, defendantData, updatedBy).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(StructuredFormUpdatedForDefendant.class)));
    }

    @Test
    public void shouldGenerateStructuredFormPublished() {
        final UUID structuredFormId = randomUUID();
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());

        final List<Object> eventStream = aggregate.publishStructuredForm(structuredFormId, updatedBy).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(StructuredFormPublished.class)));
    }

    @Test
    public void shouldGenerateStructuredFormFinalised() {
        final UUID structuredFormId = randomUUID();
        final UUID materialId = randomUUID();
        final UpdatedBy updatedBy = new UpdatedBy(randomUUID());

        final List<Object> eventStream = aggregate.finaliseStructuredForm(structuredFormId, materialId, updatedBy).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(StructuredFormFinalised.class)));
    }

}

