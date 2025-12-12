package uk.gov.moj.cpp.material.command.services;

import uk.gov.moj.cpp.material.persistence.MaterialUploadStatusRepository;
import uk.gov.moj.cpp.material.persistence.entity.Material;
import uk.gov.moj.cpp.material.persistence.entity.MaterialUploadStatus;
import uk.gov.moj.cpp.material.persistence.repository.MaterialRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

public class DownloadableMaterialsService {

    private static final String UPLOAD_STATUS_SUCCESS = "SUCCESS";

    @Inject
    private MaterialUploadStatusRepository materialUploadStatusRepository;

    @Inject
    private MaterialRepository materialRepository;

    public Map<UUID, Boolean> getDownloadableMaterials(final List<UUID> metarialIds ){
        final HashMap<UUID, Boolean> materials = new HashMap<>();

        metarialIds.forEach(m -> {
            final Optional<MaterialUploadStatus> materialUploadStatusOptional = Optional.ofNullable(materialUploadStatusRepository.findBy(m));
            final boolean status;
            if (materialUploadStatusOptional.isPresent()) {
                status = UPLOAD_STATUS_SUCCESS.equals(materialUploadStatusOptional.get().getStatus());
            } else {
                final Optional<Material> materialOptional = Optional.ofNullable(materialRepository.findBy(m));
                status = materialOptional.isPresent() && materialOptional.get().getDateMaterialAdded().toLocalDate().isBefore(LocalDate.of(2021, 8, 18));
            }
            materials.put(m, status);
        });
        return materials;
    }

}
