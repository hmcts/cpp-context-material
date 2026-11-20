package uk.gov.moj.cpp.material.persistence.repository;

import uk.gov.moj.cpp.material.persistence.entity.StructuredFormChangeHistory;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class StructuredFormChangeHistoryRepository {

    @PersistenceContext(unitName = "material-persistence-unit")
    EntityManager entityManager;

    public StructuredFormChangeHistory findBy(final UUID id) {
        return entityManager.find(StructuredFormChangeHistory.class, id);
    }

    public List<StructuredFormChangeHistory> findByStructuredFormId(final UUID structuredFormId) {
        return entityManager.createQuery(
                        "SELECT s FROM StructuredFormChangeHistory s WHERE s.structuredFormId = :structuredFormId",
                        StructuredFormChangeHistory.class)
                .setParameter("structuredFormId", structuredFormId)
                .getResultList();
    }

    public StructuredFormChangeHistory save(final StructuredFormChangeHistory entity) {
        return entityManager.merge(entity);
    }
}
