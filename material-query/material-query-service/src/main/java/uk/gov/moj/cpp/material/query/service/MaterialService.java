package uk.gov.moj.cpp.material.query.service;

import static java.util.Arrays.asList;

import uk.gov.moj.cpp.material.persistence.MaterialUploadStatusRepository;
import uk.gov.moj.cpp.material.persistence.entity.Material;
import uk.gov.moj.cpp.material.persistence.entity.MaterialUploadStatus;
import uk.gov.moj.cpp.material.persistence.repository.MaterialRepository;
import uk.gov.moj.cpp.material.query.view.MaterialMetadataView;
import uk.gov.moj.cpp.material.query.view.MaterialsView;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;

public class MaterialService {

    private static final String UPLOAD_STATUS_SUCCESS = "SUCCESS";

    @Inject
    private MaterialRepository materialRepository;

    @Inject
    private MaterialUploadStatusRepository materialUploadStatusRepository;

    public MaterialMetadataView getMaterialMetadataByMaterialId(final UUID materialId) throws JsonProcessingException {
        return Optional.ofNullable(materialRepository.findBy(materialId)).map(MaterialMetadataView::new).orElse(null);
    }

    public MaterialsView getMaterialsDownloadStatusByMaterialIds(final String materialIds) {
        final HashMap<String, Boolean> materials = new HashMap<>();

        asList(materialIds.split(",")).forEach(m -> {
            final Optional<MaterialUploadStatus> materialUploadStatusOptional = Optional.ofNullable(materialUploadStatusRepository.findBy(UUID.fromString(m)));
            final boolean status;
            if (materialUploadStatusOptional.isPresent()) {
                status = UPLOAD_STATUS_SUCCESS.equals(materialUploadStatusOptional.get().getStatus());
            } else {
                final Optional<Material> materialOptional = Optional.ofNullable(materialRepository.findBy(UUID.fromString(m)));
                status = materialOptional.isPresent() && materialOptional.get().getDateMaterialAdded().toLocalDate().isBefore(LocalDate.of(2021, 8, 18));
            }
            materials.put(m, status);
        });
        return materials.isEmpty() ? null : new MaterialsView(materials);
    }
}