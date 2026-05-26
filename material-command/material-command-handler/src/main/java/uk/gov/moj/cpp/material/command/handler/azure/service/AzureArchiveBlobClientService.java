package uk.gov.moj.cpp.material.command.handler.azure.service;

import static com.azure.core.util.Context.NONE;
import static java.lang.String.format;
import static java.nio.file.Files.delete;
import static java.util.Objects.nonNull;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.material.command.handler.ZipMaterial;
import uk.gov.moj.cpp.material.command.handler.azure.service.exception.AzureBlobClientException;
import uk.gov.moj.cpp.material.command.handler.azure.service.exception.FileRetrieveException;
import uk.gov.moj.cpp.material.command.handler.azure.service.exception.MaterialNotFoundException;
import uk.gov.moj.cpp.material.command.handler.azure.service.exception.ResourceNotFoundException;
import uk.gov.moj.cpp.material.command.handler.azure.service.exception.TempFileCreationException;
import uk.gov.moj.cpp.material.filestore.azure.ArchiveAzureBlobConfiguration;
import uk.gov.moj.cpp.material.filestore.azure.ArchiveBlobContainer;
import uk.gov.moj.cpp.material.filestore.azure.StorageFileRetriever;
import uk.gov.moj.cpp.material.filestore.azure.StoragePath;
import uk.gov.moj.cpp.material.filestore.azure.StoredFile;
import uk.gov.moj.cpp.material.query.service.AlfrescoReadService;
import uk.gov.moj.cpp.material.query.view.MaterialView;
import uk.gov.moj.cpp.platform.data.utils.service.ZipCreator;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class AzureArchiveBlobClientService {

    private static final DateTimeFormatter ARCHIVE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String METADATA_FILENAME = "filename";

    public static final String CASE_ARCHIVE_FILE_PREFIX = "CaseArchive-";
    public static final String CONTENT_TYPE_ZIP = "application/zip";
    public static final String ZIP_EXTENSION = ".zip";
    public static final String CASE_STRUCTURED_DATA = "CaseStructuredData";
    public static final String CASE_AT_A_GLANCE = "CaseAtAGlance";
    @Inject
    @ArchiveBlobContainer
    private BlobContainerClient blobContainerClient;

    @Inject
    private ArchiveAzureBlobConfiguration archiveAzureBlobConfiguration;

    @Inject
    private ZipCreator zipCreator;

    @Inject
    private AlfrescoReadService alfrescoReadService;

    @Inject
    private StorageFileRetriever storageFileRetriever;

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @Inject
    private UtcClock clock;

    public void upload(final InputStream file, final String destinationFileName, final String documentContentType) {
        try {
            logger.info("Uploading {} file to azure blob storage at {}", destinationFileName, clock.now());
            blobContainerClient.getBlobClient(destinationFileName)
                    .uploadWithResponse(
                            new BlobParallelUploadOptions(file)
                                    .setHeaders(new BlobHttpHeaders().setContentType(documentContentType)),
                            archiveAzureBlobConfiguration.getTransferTimeout(),
                            NONE);
        } catch (final Exception e) {
            throw new AzureBlobClientException("Error while uploading file to azure blob storage", e);
        }
    }

    private void copyInputStreamToFile(final InputStream documentInputStream, final File file) {
        try (final FileOutputStream outputStream = new FileOutputStream(file, false)) {
            documentInputStream.transferTo(outputStream);
        } catch (final IOException e) {
            throw new ResourceNotFoundException("Error copying material to file {}", e);
        }
    }

    @SuppressWarnings("squid:S00112")
    public void createAndUploadZip(final ZipMaterial zipMaterial) throws Exception {
        final List<Path> filePaths = new ArrayList<>();
        final Map<String, File> zipFiles = new HashMap<>();
        if (nonNull(zipMaterial.getMaterialIds())) {
            zipMaterial.getMaterialIds().forEach(materialId -> {
                final Optional<MaterialView> materialView = alfrescoReadService.getDataById(materialId);
                logger.info("MaterialView is available {} for MaterialId {}", materialView.isPresent(), materialId);
                zipMaterials(zipFiles, filePaths, materialId, materialView);
            });
        }

        for (final UUID fileId : zipMaterial.getFileIds()) {
            final Optional<StoredFile> storedFileOpt = storageFileRetriever.retrieve(StoragePath.internal(), fileId);
            logger.info("StoredFile is available {} for FileId {}", storedFileOpt.isPresent(), fileId);
            if (storedFileOpt.isPresent()) {
                try (final StoredFile storedFile = storedFileOpt.get()) {
                    final String filename = storedFile.getMetadata().getOrDefault(METADATA_FILENAME, "");
                    final String fileExtension = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1) : "";
                    zipFiles(zipFiles, filePaths, fileId, storedFile, fileExtension);
                }
            }
        }

        final String dateString = clock.now().format(ARCHIVE_DATE_FORMATTER);
        final String zipFilePrefix = CASE_ARCHIVE_FILE_PREFIX + zipMaterial.getCaseURN() + "-" + dateString;
        final File f = File.createTempFile(zipFilePrefix, ZIP_EXTENSION);
        zipCreator.zipFiles(zipFiles, f);
        try (final InputStream fis = new FileInputStream(f)) {
            upload(fis, f.getName(), CONTENT_TYPE_ZIP);
        } finally {
            filePaths.forEach(tempFile -> {
                try {
                    delete(tempFile);
                } catch (final IOException e) {
                    throw new TempFileCreationException(format("Cannot delete temporary file %s", tempFile), e);
                }
            });
        }
    }

    private void zipFiles(final Map<String, File> zipFiles, final List<Path> filePaths, final UUID fileId, final StoredFile storedFile, final String filExtn) {
        try {
            if ("pdf".equals(filExtn)) {
                final File file = File.createTempFile(CASE_AT_A_GLANCE, "pdf");
                copyInputStreamToFile(storedFile.getInputStream(), file);
                zipFiles.put(file.getName().substring(0, CASE_AT_A_GLANCE.length()).concat(".pdf"), file);
                filePaths.add(file.toPath());
            }
            if ("txt".equals(filExtn)) {
                final File file = File.createTempFile(CASE_STRUCTURED_DATA, "json");
                copyInputStreamToFile(storedFile.getInputStream(), file);
                zipFiles.put(file.getName().substring(0, CASE_STRUCTURED_DATA.length()).concat(".json"), file);
                filePaths.add(file.toPath());
            }
        } catch (final IOException e) {
            throw new FileRetrieveException(format("file can't be created for fileId %s", fileId), e);
        }
    }

    private void zipMaterials(final Map<String, File> zipFiles, final List<Path> filePaths, final UUID materialId, final Optional<MaterialView> materialView) {
        if (materialView.isPresent()) {
            try {
                final String fileName = materialView.get().getFileName();
                final int lastIndex = fileName.lastIndexOf('.');
                final File file;
                if (lastIndex == -1) {
                    file = File.createTempFile(fileName, null);
                } else {
                    file = File.createTempFile(fileName.substring(0, lastIndex), "." + fileName.substring(lastIndex + 1));
                }
                copyInputStreamToFile(materialView.get().getDocumentInputStream(), file);
                zipFiles.put(fileName, file);
                filePaths.add(file.toPath());
            } catch (final IOException e) {
                throw new TempFileCreationException("Cannot create tempFile Error: ", e);
            }
        } else {
            throw new MaterialNotFoundException(format("Material not found for materialId %s", materialId));
        }
    }
}
