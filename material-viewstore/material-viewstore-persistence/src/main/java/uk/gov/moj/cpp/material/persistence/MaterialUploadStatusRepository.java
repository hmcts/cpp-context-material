package uk.gov.moj.cpp.material.persistence;

import uk.gov.moj.cpp.material.persistence.entity.MaterialUploadStatus;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class MaterialUploadStatusRepository {

    @PersistenceContext(unitName = "material-persistence-unit")
    EntityManager entityManager;

    public MaterialUploadStatus findBy(final UUID materialId) {
        return entityManager.find(MaterialUploadStatus.class, materialId);
    }

    public MaterialUploadStatus save(final MaterialUploadStatus materialUploadStatus) {
        return entityManager.merge(materialUploadStatus);
    }
}
