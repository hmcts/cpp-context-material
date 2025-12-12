package uk.gov.moj.cpp.material.command.handler.azure.service;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.Objects.nonNull;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.moj.cpp.material.command.handler.ZipMaterial;
import uk.gov.moj.cpp.material.command.handler.azure.service.exception.AzureBlobClientException;
import uk.gov.moj.cpp.material.command.handler.azure.service.exception.FileRetrieveException;
import uk.gov.moj.cpp.material.command.handler.azure.service.exception.MaterialNotFoundException;
import uk.gov.moj.cpp.material.command.handler.azure.service.exception.ResourceNotFoundException;
import uk.gov.moj.cpp.material.command.handler.azure.service.exception.TempFileCreationException;
import uk.gov.moj.cpp.material.query.service.AlfrescoReadService;
import uk.gov.moj.cpp.material.query.view.MaterialView;
import uk.gov.moj.cpp.platform.data.utils.service.ZipCreator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AzureArchiveBlobClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureArchiveBlobClientService.class);
    public static final String CASE_ARCHIVE_FILE_PREFIX = "CaseArchive-";
    public static final String CONTENT_TYPE_ZIP = "application/zip";
    public static final String ZIP_EXTENSION = ".zip";
    public static final String CASE_STRUCTURED_DATA = "CaseStructuredData";
    public static final String CASE_AT_A_GLANCE = "CaseAtAGlance";

    @Inject
    @Value(key = "archiving.archivingConnectionString")
    private String storageConnectionString;

    @Inject
    @Value(key = "archiving.archivingContainerName")
    private String containerName;

    @Inject
    private ZipCreator zipCreator;

    @Inject
    private AlfrescoReadService alfrescoReadService;

    @Inject
    private FileService fileService;

    private CloudBlobContainer container = null;

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    final List<Path> filePaths = new ArrayList<>();

    private void connect() {
        try {
            final CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            final CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference(containerName);
        } catch (InvalidKeyException ex) {
            throw new AzureBlobClientException("Invalid connection string", ex);
        } catch (URISyntaxException ex) {
            throw new AzureBlobClientException("Connection URI parse error", ex);
        } catch (StorageException ex) {
            throw new AzureBlobClientException(format(
                    "Error returned from azure service. Http code: %d and error code: %s",
                    ex.getHttpStatusCode(), ex.getErrorCode()), ex);
        }
    }

    /**
     * Upload a file to Azure blob storage and generate SAS URL
     *
     * @param file                File to upload
     * @param fileSize            Size of file to upload
     * @param destinationFileName file name
     * @return SAS Download URL String
     * @throws AzureBlobClientException
     */
    public void upload(final InputStream file, long fileSize, String destinationFileName, final String documentContentType) {
        try {
            connect();
            final CloudBlockBlob fileBlob = container.getBlockBlobReference(destinationFileName);
            fileBlob.getProperties().setContentType(documentContentType);
            LOGGER.info("Uploading {} file to azure blob storage on {}", destinationFileName, now());
            fileBlob.upload(file, fileSize);
            filePaths.forEach(tempFile -> {
                try {
                     Files.delete(tempFile);
                } catch (IOException e) {
                    throw new TempFileCreationException(String.format("Cannot delete temporary file %s", tempFile), e);
                }
            });
        } catch (StorageException ex) {
            throw new AzureBlobClientException(format(
                    "Error returned from azure service. Http code: %d and error code: %s",
                    ex.getHttpStatusCode(), ex.getErrorCode()), ex);
        } catch (URISyntaxException ex) {
            throw new AzureBlobClientException("Connection URI parse error", ex);
        } catch (IOException ex) {
            throw new AzureBlobClientException("Error while uploading file to azure blob storage", ex);
        }
    }

    private void copyInputStreamToFile(final InputStream documentInputStream, final File file) {
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            final byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = documentInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (IOException e) {
            throw new ResourceNotFoundException("Error copying material to file {}", e);
        }
    }

    @SuppressWarnings("squid:S00112")
    public void createAndUploadZip(final ZipMaterial zipMaterial) throws Exception {
        final Map<String, File> zipFiles = new HashMap<>();
        if (nonNull(zipMaterial.getMaterialIds())) {
            zipMaterial.getMaterialIds().forEach(materialId -> {
                final Optional<MaterialView> materialView = alfrescoReadService.getDataById(materialId);
                LOGGER.info("MaterialView is available {} for MaterialId {}", materialView.isPresent(), materialId);
                zipMaterials(zipFiles, materialId, materialView);
            });
        }

        zipMaterial.getFileIds().forEach(fileId -> {
            try {
                final Optional<FileReference> fileReference = fileService.retrieve(fileId);
                fileService.retrieveMetadata(fileId).ifPresent(m -> {
                    final String filExtn =  m.getString("conversionFormat");
                    LOGGER.info("FileReference is available {} for FileId {}", fileReference.isPresent(), fileId);
                    zipFiles(zipFiles, fileId, fileReference, filExtn);
                });
            } catch (FileServiceException e) {
                throw new FileRetrieveException(format("file not found for fileId %s", fileId), e);
            }
        });

        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        final String dateString = dateFormat.format(new Date());
        final String zipFilePrefix = CASE_ARCHIVE_FILE_PREFIX + zipMaterial.getCaseURN() + "-" + dateString;
        final File f = File.createTempFile(zipFilePrefix, ZIP_EXTENSION);
        zipCreator.zipFiles(zipFiles, f);
        final InputStream fis = new FileInputStream(f);
        upload(fis, f.length(), f.getName(), CONTENT_TYPE_ZIP);
    }

    private void zipFiles(final Map<String, File> zipFiles, final UUID fileId, final Optional<FileReference> fileReference, final String filExtn) {
        if (fileReference.isPresent()) {
            try {
                if ("pdf".equals(filExtn)) {
                    final File file = File.createTempFile(CASE_AT_A_GLANCE, "pdf");
                    copyInputStreamToFile(fileReference.get().getContentStream(), file);
                    zipFiles.put(file.getName().substring(0, CASE_AT_A_GLANCE.length()).concat(".pdf"), file);
                    filePaths.add(file.toPath());
                }
                if ("txt".equals(filExtn)) {
                    final File file = File.createTempFile(CASE_STRUCTURED_DATA, "json");
                    copyInputStreamToFile(fileReference.get().getContentStream(), file);
                    zipFiles.put(file.getName().substring(0, CASE_STRUCTURED_DATA.length()).concat(".json"), file);
                    filePaths.add(file.toPath());
                }
            } catch (IOException e) {
                throw new FileRetrieveException(format("file can't be created for fileId %s", fileId), e);
            }
        }
    }

    private void zipMaterials(final Map<String, File> zipFiles, final UUID materialId, final Optional<MaterialView> materialView) {
        if (materialView.isPresent()) {
            try {
                final String fileName = materialView.get().getFileName();
                final int lastIndex = fileName.lastIndexOf('.');
                final File file;
                if (lastIndex == -1) {
                    file = File.createTempFile(fileName, null);
                } else {
                    file = File.createTempFile(fileName.substring(0, lastIndex), "."+ fileName.substring(lastIndex + 1));
                }
                copyInputStreamToFile(materialView.get().getDocumentInputStream(), file);
                zipFiles.put(fileName, file);
                filePaths.add(file.toPath());
            } catch (IOException e) {
                throw new TempFileCreationException("Cannot create tempFile Error: ", e);
            }

        } else {
            throw new MaterialNotFoundException(format("Material not found for materialId %s", materialId));
        }
    }
}