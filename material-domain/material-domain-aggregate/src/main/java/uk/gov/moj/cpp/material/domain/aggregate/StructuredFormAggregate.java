package uk.gov.moj.cpp.material.domain.aggregate;

import static java.time.ZonedDateTime.now;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.material.domain.StructuredFormStatus;
import uk.gov.moj.cpp.material.domain.UpdatedBy;
import uk.gov.moj.cpp.material.domain.event.StructuredFormCreated;
import uk.gov.moj.cpp.material.domain.event.StructuredFormFinalised;
import uk.gov.moj.cpp.material.domain.event.StructuredFormPublished;
import uk.gov.moj.cpp.material.domain.event.StructuredFormUpdated;
import uk.gov.moj.cpp.material.domain.event.StructuredFormUpdatedForDefendant;

import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings({"squid:S1948", "PMD.BeanMembersShouldSerialize"})
public class StructuredFormAggregate implements Aggregate {

    private static final long serialVersionUID = 3565662835288990225L;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(StructuredFormCreated.class).apply(e -> {
                        }
                ),
                when(StructuredFormUpdated.class).apply(e -> {
                    //do nothing
                }),
                when(StructuredFormUpdatedForDefendant.class).apply(e -> {
                    //do nothing
                }),
                otherwiseDoNothing());

    }


    public Stream<Object> createStructuredForm(final UUID structuredFormId, final UUID formId, final String structuredFormData, final StructuredFormStatus status, final UpdatedBy updatedBy) {

        return apply(Stream.of(StructuredFormCreated.structuredFormCreated()
                .withStructuredFormId(structuredFormId)
                .withFormId(formId)
                .withStructuredFormData(structuredFormData)
                .withStatus(status)
                .withUpdatedBy(updatedBy)
                .withLastUpdated(now())
                .build()));
    }

    public Stream<Object> updateStructuredForm(final UUID structuredFormId, final String structuredFormData, final UpdatedBy updatedBy) {
        return apply(Stream.of(StructuredFormUpdated.structuredFormUpdated()
                .withStructuredFormData(structuredFormData)
                .withStructuredFormId(structuredFormId)
                .withUpdatedBy(updatedBy)
                .withLastUpdated(now())
                .build()));
    }

    public Stream<Object> updateStructuredFormForDefendant(final UUID structuredFormId, final UUID defendantId, final String defendant, final UpdatedBy updatedBy) {
        return apply(Stream.of(StructuredFormUpdatedForDefendant.structuredFormUpdatedForDefendant()
                .withDefendantData(defendant)
                .withDefendantId(defendantId)
                .withStructuredFormId(structuredFormId)
                .withUpdatedBy(updatedBy)
                .withLastUpdated(now())
                .build()));
    }

    public Stream<Object> publishStructuredForm(final UUID structuredFormId, final UpdatedBy updatedBy) {
        return apply(Stream.of(StructuredFormPublished.structuredFormPublished()
                .withStructuredFormId(structuredFormId)
                .withUpdatedBy(updatedBy)
                .withLastUpdated(now())
                .build()));
    }

    public Stream<Object> finaliseStructuredForm(final UUID structuredFormId, final UUID materialId, final UpdatedBy updatedBy) {
        return apply(Stream.of(StructuredFormFinalised.structuredFormFinalised()
                .withStructuredFormId(structuredFormId)
                .withMaterialId(materialId)
                .withLastUpdated(now())
                .withUpdatedBy(updatedBy)
                .build()));
    }

}
