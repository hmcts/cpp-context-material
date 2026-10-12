package uk.gov.moj.cpp.material.persistence.repository;

import uk.gov.moj.cpp.material.persistence.entity.Material;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class MaterialRepository {

    @PersistenceContext(unitName = "material-persistence-unit")
    EntityManager entityManager;

    public Material findBy(final UUID materialId) {
        return entityManager.find(Material.class, materialId);
    }

    public Material save(final Material material) {
        return entityManager.merge(material);
    }

    public void removeAndFlush(final Material material) {
        final Material managed = entityManager.contains(material) ? material : entityManager.merge(material);
        entityManager.remove(managed);
        entityManager.flush();
    }
}
