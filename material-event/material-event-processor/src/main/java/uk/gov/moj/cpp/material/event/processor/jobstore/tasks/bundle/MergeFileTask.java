package uk.gov.moj.cpp.material.event.processor.jobstore.tasks.bundle;

import static java.lang.String.format;
import static java.nio.file.Files.delete;
import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.INPROGRESS;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.FAILED_MERGE_FILE_TASK;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.MERGE_FILE_TASK;
import static uk.gov.moj.cpp.material.event.processor.jobstore.tasks.UploadMaterialTaskNames.UPLOAD_BUNDLE_TO_ALFRESCO_TASK;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.moj.cpp.jobstore.api.annotation.Task;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.material.client.MaterialClient;
import uk.gov.moj.cpp.material.event.processor.error.MaterialNotFoundException;
import uk.gov.moj.cpp.material.event.processor.error.SystemUserIdNotAvailableException;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.FailedBundleUploadJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.MergeFileJobData;
import uk.gov.moj.cpp.material.event.processor.jobstore.jobdata.bundle.UploadBundleToAlfrescoJobData;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;

@Task(MERGE_FILE_TASK)
public class MergeFileTask implements ExecutableTask {

    public static final String EXTENSION_PDF = ".pdf";
    public static final String MATERIAL_PREFIX = "material_%s";
    public static final String BUNDLE_PREFIX = "bundle_%s";
    public static final String FILE_NAME = "fileName";
    public static final String MEDIA_TYPE = "mediaType";
    public static final String APPLICATION_PDF = "application/pdf";

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private FileService fileService;

    @Inject
    private UtcClock clock;

    @SuppressWarnings({"squid:S1312"})
    @Inject
    private Logger logger;

    @Inject
    private MaterialClient materialClient;

    @Inject
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Override
    @SuppressWarnings("squid:S2221")
    public ExecutionInfo execute(final ExecutionInfo executionInfo) {

        final MergeFileJobData mergeFileJobData = jsonObjectConverter.convert(executionInfo.getJobData(), MergeFileJobData.class);

        File mergedDocument = null;
        try {
            final UUID bundledMaterialId = mergeFileJobData.getBundledMaterialId();
            final String bundledMaterialName = mergeFileJobData.getBundledMaterialName();

            final List<String> materialList = getMaterials(mergeFileJobData.getMaterialIds());
            mergedDocument = mergeDocuments(bundledMaterialId, materialList);

            final Long fileSize = mergedDocument.length();
            final int pageCount = getPageCount(mergedDocument);

            final UUID fileStoreId = storeMergedDocument(mergedDocument);

            final UploadBundleToAlfrescoJobData uploadBundleToAlfrescoJobData = new UploadBundleToAlfrescoJobData(
                    bundledMaterialId,
                    bundledMaterialName,
                    mergeFileJobData.getMaterialIds(),
                    fileStoreId,
                    fileSize,
                    pageCount,
                    mergeFileJobData.getEventMetadata());

            return executionInfo()
                    .withExecutionStatus(INPROGRESS)
                    .withNextTask(UPLOAD_BUNDLE_TO_ALFRESCO_TASK)
                    .withNextTaskStartTime(clock.now())
                    .withJobData(objectToJsonObjectConverter.convert(uploadBundleToAlfrescoJobData))
                    .build();

        } catch (final Exception e) {
            logger.error("Error!! merging material bundle id: {}", mergeFileJobData.getBundledMaterialId(), e);

            final FailedBundleUploadJobData failedBundleUploadJobData = new FailedBundleUploadJobData(
                    mergeFileJobData.getBundledMaterialId(),
                    mergeFileJobData.getMaterialIds(),
                    Optional.empty(),
                    mergeFileJobData.getEventMetadata(),
                    BundleErrorType.MERGE_FILE_ERROR,
                    e.getMessage(),
                    clock.now());

            return executionInfo()
                    .withExecutionStatus(INPROGRESS)
                    .withNextTask(FAILED_MERGE_FILE_TASK)
                    .withNextTaskStartTime(clock.now())
                    .withJobData(objectToJsonObjectConverter.convert(failedBundleUploadJobData))
                    .build();
        } finally {
            deleteFile(mergedDocument);
        }
    }


    private List<String> getMaterials(final List<UUID> materialIds) throws IOException {
        final UUID systemUserId = serviceContextSystemUserProvider.getContextSystemUserId().orElseThrow(SystemUserIdNotAvailableException::new);
        final List<String> materialList = new ArrayList<>();
        for (final UUID materialId : materialIds) {
            Response response = null;
            try {
                response = materialClient.getMaterialAsPdf(materialId, systemUserId);
                final Response.Status materialResponseStatus = fromStatusCode(response.getStatus());
                if (OK.equals(materialResponseStatus)) {
                    materialList.add(response.getLocation().toString());
                } else {
                    logger.error("Error reading the material {}, response status={}", materialId, response.getStatus());
                    throw new MaterialNotFoundException("Error reading material " + response.getStatus());
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }

        return materialList;
    }

    private File mergeDocuments(UUID bundledMaterialId, final List<String> materialList) throws IOException {
        final File tempFile = File.createTempFile(format(BUNDLE_PREFIX, bundledMaterialId), EXTENSION_PDF);
        final PDFMergerUtility pdfMerger = new PDFMergerUtility();
        pdfMerger.setDestinationFileName(tempFile.getAbsolutePath());
        for (final String material : materialList) {
            final InputStream input = new URL(material).openStream();
            pdfMerger.addSource(input);
        }
        pdfMerger.mergeDocuments(null);
        return tempFile;
    }

    private UUID storeMergedDocument(File mergedDocument) throws IOException, FileServiceException {
        final JsonObject metaData = createObjectBuilder()
                .add(FILE_NAME, mergedDocument.getName())
                .add(MEDIA_TYPE, APPLICATION_PDF)
                .build();

        UUID fileStoreId;
        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(mergedDocument))) {
            fileStoreId = fileService.store(metaData, inputStream);
        }
        return fileStoreId;
    }

    private void deleteFile(File mergedDocument) {
        if (nonNull(mergedDocument)) {
            try {
                delete(mergedDocument.toPath());
            } catch (IOException e) {
                logger.error("Failed to delete mergedDocument from temp location: {}", mergedDocument.getAbsolutePath(), e);
            }
        }
    }

    private int getPageCount(final File mergedDocument) throws IOException {
        try (PDDocument pdDocument = PDDocument.load(mergedDocument, MemoryUsageSetting.setupTempFileOnly())) {
            return pdDocument.getNumberOfPages();
        }
    }
}