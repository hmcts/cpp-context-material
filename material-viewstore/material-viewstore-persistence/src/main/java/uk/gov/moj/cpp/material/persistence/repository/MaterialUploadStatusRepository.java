package uk.gov.moj.cpp.material.persistence.repository;

import uk.gov.moj.cpp.material.persistence.entity.MaterialUploadStatus;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

/**
 * Repository for {@link uk.gov.moj.cpp.material.persistence.entity.MaterialUploadStatus}
 */
@Repository
public interface MaterialUploadStatusRepository extends EntityRepository<MaterialUploadStatus, UUID> {
}