package uk.gov.moj.cpp.material.domain.aggregate;

import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.material.domain.event.MaterialZipFailed;
import uk.gov.moj.cpp.material.domain.event.MaterialZipped;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class CaseAggregate implements Aggregate {

    private static final long serialVersionUID = -1837632618531502420L;

    public Stream<Object> materialZipFailed(final UUID caseId, final List<UUID> materialIds, final List<UUID> fileIds, final String errorMessage) {
        return apply(of(new MaterialZipFailed(caseId, materialIds, fileIds, errorMessage)));
    }

    public Stream<Object> materialZipped(final UUID caseId, final String caseURN, final List<UUID> materialIds, final List<UUID> fileIds) {
        return apply(of(new MaterialZipped(caseId, caseURN, materialIds, fileIds)));
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                otherwiseDoNothing()
        );
    }

}
