package uk.gov.moj.cpp.material.persistence.repository;

import uk.gov.moj.cpp.material.persistence.entity.Material;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

/**
 * Repository for {@link Material}
 */
@Repository
public interface MaterialRepository extends EntityRepository<Material, UUID> {
}
