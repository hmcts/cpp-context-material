package uk.gov.moj.cpp.material.persistence.repository;

import uk.gov.moj.cpp.material.persistence.entity.StructuredForm;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class StructuredFormRepository {

    @PersistenceContext(unitName = "material-persistence-unit")
    EntityManager entityManager;

    public StructuredForm findBy(final UUID id) {
        return entityManager.find(StructuredForm.class, id);
    }

    public StructuredForm save(final StructuredForm structuredForm) {
        return entityManager.merge(structuredForm);
    }
}
