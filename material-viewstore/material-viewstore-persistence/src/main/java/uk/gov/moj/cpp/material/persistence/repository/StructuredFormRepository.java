package uk.gov.moj.cpp.material.persistence.repository;


import uk.gov.moj.cpp.material.persistence.entity.StructuredForm;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface StructuredFormRepository extends EntityRepository<StructuredForm, UUID> {
}

