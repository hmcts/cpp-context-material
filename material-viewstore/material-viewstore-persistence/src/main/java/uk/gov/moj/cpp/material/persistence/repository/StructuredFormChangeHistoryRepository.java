package uk.gov.moj.cpp.material.persistence.repository;


import uk.gov.moj.cpp.material.persistence.entity.StructuredFormChangeHistory;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface StructuredFormChangeHistoryRepository extends EntityRepository<StructuredFormChangeHistory, UUID> {
    List<StructuredFormChangeHistory> findByStructuredFormId(UUID structuredFormId);
}
